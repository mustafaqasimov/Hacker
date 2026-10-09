CREATE TABLE organization (
  id uuid PRIMARY KEY,
  name varchar(120) NOT NULL CHECK (length(btrim(name))>0),
  created_by uuid NOT NULL REFERENCES user_account(id),
  archived boolean NOT NULL DEFAULT false,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE org_membership (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organization(id),
  user_id uuid NOT NULL REFERENCES user_account(id),
  role varchar(24) NOT NULL CHECK (role IN ('STUDENT','TEACHER','ORG_ADMIN')),
  status varchar(24) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (organization_id,id),
  UNIQUE (organization_id,user_id)
);
CREATE INDEX org_membership_user_idx ON org_membership(user_id,status);
CREATE INDEX org_membership_admin_idx ON org_membership(organization_id,role,status);
CREATE TABLE study_group (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organization(id),
  name varchar(120) NOT NULL CHECK (length(btrim(name))>0),
  archived boolean NOT NULL DEFAULT false,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (organization_id,id)
);
CREATE UNIQUE INDEX study_group_name_idx ON study_group(organization_id,lower(name)) WHERE NOT archived;
CREATE TABLE group_student (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  group_id uuid NOT NULL,
  membership_id uuid NOT NULL,
  FOREIGN KEY (organization_id,group_id) REFERENCES study_group(organization_id,id),
  FOREIGN KEY (organization_id,membership_id) REFERENCES org_membership(organization_id,id),
  UNIQUE (organization_id,group_id,membership_id)
);
CREATE INDEX group_student_member_idx ON group_student(organization_id,membership_id);
CREATE TABLE teacher_assignment (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL,
  group_id uuid NOT NULL,
  membership_id uuid NOT NULL,
  FOREIGN KEY (organization_id,group_id) REFERENCES study_group(organization_id,id),
  FOREIGN KEY (organization_id,membership_id) REFERENCES org_membership(organization_id,id),
  UNIQUE (organization_id,group_id,membership_id)
);
CREATE INDEX teacher_assignment_member_idx ON teacher_assignment(organization_id,membership_id);
CREATE TABLE org_invitation (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organization(id),
  email varchar(254) NOT NULL CHECK (email=lower(btrim(email))),
  role varchar(24) NOT NULL CHECK (role IN ('STUDENT','TEACHER','ORG_ADMIN')),
  token_hash varchar(64) NOT NULL UNIQUE,
  created_by uuid NOT NULL REFERENCES user_account(id),
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  revoked boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX org_invitation_pending_idx ON org_invitation(organization_id,email) WHERE NOT revoked AND consumed_at IS NULL;
CREATE TABLE organization_event (
  id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES organization(id),
  actor_id uuid NOT NULL REFERENCES user_account(id),
  resource_id uuid NOT NULL,
  context_id uuid,
  event_type varchar(64) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX organization_event_org_time_idx ON organization_event(organization_id,created_at);

-- SET LOCAL prevents tenant state escaping a transaction into the connection pool.
-- Runtime must be a separate NOBYPASSRLS, non-superuser role. The app sets scope only
-- after checking the authenticated user's membership or a verified invitation.
ALTER TABLE organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization FORCE ROW LEVEL SECURITY;
ALTER TABLE org_membership ENABLE ROW LEVEL SECURITY;
ALTER TABLE org_membership FORCE ROW LEVEL SECURITY;
CREATE POLICY membership_read ON org_membership FOR SELECT USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
  OR user_id=nullif(current_setting('app.actor_id',true),'')::uuid
);
CREATE POLICY membership_write ON org_membership FOR ALL USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
) WITH CHECK (organization_id=nullif(current_setting('app.organization_id',true),'')::uuid);
CREATE POLICY organization_read ON organization FOR SELECT USING (
  id=nullif(current_setting('app.organization_id',true),'')::uuid
  OR EXISTS (SELECT 1 FROM org_membership m WHERE m.organization_id=organization.id
    AND m.user_id=nullif(current_setting('app.actor_id',true),'')::uuid AND m.status='ACTIVE')
);
CREATE POLICY organization_insert ON organization FOR INSERT WITH CHECK (
  created_by=nullif(current_setting('app.actor_id',true),'')::uuid
);
CREATE POLICY organization_update ON organization FOR UPDATE USING (
  id=nullif(current_setting('app.organization_id',true),'')::uuid
) WITH CHECK (id=nullif(current_setting('app.organization_id',true),'')::uuid);

ALTER TABLE study_group ENABLE ROW LEVEL SECURITY;
ALTER TABLE study_group FORCE ROW LEVEL SECURITY;
CREATE POLICY group_tenant ON study_group FOR ALL USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
) WITH CHECK (organization_id=nullif(current_setting('app.organization_id',true),'')::uuid);
ALTER TABLE group_student ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_student FORCE ROW LEVEL SECURITY;
CREATE POLICY group_student_tenant ON group_student FOR ALL USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
) WITH CHECK (organization_id=nullif(current_setting('app.organization_id',true),'')::uuid);
ALTER TABLE teacher_assignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_assignment FORCE ROW LEVEL SECURITY;
CREATE POLICY teacher_tenant ON teacher_assignment FOR ALL USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
) WITH CHECK (organization_id=nullif(current_setting('app.organization_id',true),'')::uuid);
ALTER TABLE org_invitation ENABLE ROW LEVEL SECURITY;
ALTER TABLE org_invitation FORCE ROW LEVEL SECURITY;
CREATE POLICY invitation_read ON org_invitation FOR SELECT USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
  OR token_hash=nullif(current_setting('app.invitation_digest',true),'')
);
CREATE POLICY invitation_write ON org_invitation FOR ALL USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
) WITH CHECK (organization_id=nullif(current_setting('app.organization_id',true),'')::uuid);
ALTER TABLE organization_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_event FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_event_read ON organization_event FOR SELECT USING (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
);
CREATE POLICY organization_event_append ON organization_event FOR INSERT WITH CHECK (
  organization_id=nullif(current_setting('app.organization_id',true),'')::uuid
  AND actor_id=nullif(current_setting('app.actor_id',true),'')::uuid
);

-- Lifetime creation quota remains visible to its creator even after membership removal.
-- This prevents bypassing the public creation limit by leaving previously created tenants.
CREATE TABLE organization_creator_quota (
  user_id uuid PRIMARY KEY REFERENCES user_account(id),
  created_count integer NOT NULL CHECK (created_count BETWEEN 0 AND 3)
);
ALTER TABLE organization_creator_quota ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_creator_quota FORCE ROW LEVEL SECURITY;
CREATE POLICY creator_quota_self ON organization_creator_quota FOR ALL USING (
  user_id=nullif(current_setting('app.actor_id',true),'')::uuid
) WITH CHECK (user_id=nullif(current_setting('app.actor_id',true),'')::uuid);
