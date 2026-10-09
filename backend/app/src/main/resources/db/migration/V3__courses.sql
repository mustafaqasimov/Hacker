CREATE TABLE course (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id),
 title varchar(160) NOT NULL CHECK(length(trim(title))>0), description varchar(2000) NOT NULL,
 difficulty varchar(20) NOT NULL CHECK(difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
 status varchar(20) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),
 version bigint NOT NULL DEFAULT 0, content_revision bigint NOT NULL DEFAULT 0, created_by uuid NOT NULL REFERENCES user_account(id), created_at timestamptz NOT NULL,
 UNIQUE(organization_id,id)
);
CREATE INDEX course_org_status_idx ON course(organization_id,status,created_at,id);
CREATE TABLE course_topic (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, course_id uuid NOT NULL,
 title varchar(160) NOT NULL CHECK(length(trim(title))>0), position integer NOT NULL CHECK(position>=0),
 FOREIGN KEY(organization_id,course_id) REFERENCES course(organization_id,id) ON DELETE CASCADE,
 UNIQUE(organization_id,id), UNIQUE(course_id,position)
);
CREATE TABLE course_task (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, topic_id uuid NOT NULL,
 title varchar(160) NOT NULL CHECK(length(trim(title))>0), instructions varchar(6000) NOT NULL CHECK(length(trim(instructions))>0),
 difficulty varchar(20) NOT NULL CHECK(difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
 position integer NOT NULL CHECK(position>=0),
 FOREIGN KEY(organization_id,topic_id) REFERENCES course_topic(organization_id,id) ON DELETE CASCADE,
 UNIQUE(organization_id,id), UNIQUE(topic_id,position)
);
CREATE TABLE learning_objective (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, task_id uuid NOT NULL,
 skill_code varchar(80) NOT NULL CHECK(skill_code ~ '^[A-Z][A-Z0-9_]{1,79}$'),
 description varchar(600) NOT NULL CHECK(length(trim(description))>0), position integer NOT NULL CHECK(position>=0),
 FOREIGN KEY(organization_id,task_id) REFERENCES course_task(organization_id,id) ON DELETE CASCADE,
 UNIQUE(task_id,skill_code), UNIQUE(task_id,position)
);
CREATE TABLE course_group (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, course_id uuid NOT NULL, group_id uuid NOT NULL,
 FOREIGN KEY(organization_id,course_id) REFERENCES course(organization_id,id) ON DELETE CASCADE,
 FOREIGN KEY(organization_id,group_id) REFERENCES study_group(organization_id,id),
 UNIQUE(course_id,group_id)
);
CREATE INDEX course_group_lookup_idx ON course_group(organization_id,group_id,course_id);
CREATE TABLE course_event (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization(id), actor_id uuid NOT NULL REFERENCES user_account(id),
 course_id uuid NOT NULL, event_type varchar(60) NOT NULL, created_at timestamptz NOT NULL
);
-- Audit references retain deleted draft IDs. No course FK by design.
DO $$ DECLARE tab text; BEGIN
 FOREACH tab IN ARRAY ARRAY['course','course_topic','course_task','learning_objective','course_group'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',tab);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',tab);
  EXECUTE format('CREATE POLICY tenant_scope ON %I FOR ALL USING (organization_id=nullif(current_setting(''app.organization_id'',true),'''')::uuid) WITH CHECK (organization_id=nullif(current_setting(''app.organization_id'',true),'''')::uuid)',tab);
 END LOOP;
END $$;
ALTER TABLE course_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_event FORCE ROW LEVEL SECURITY;
CREATE POLICY event_read ON course_event FOR SELECT USING (organization_id=nullif(current_setting('app.organization_id',true),'')::uuid);
CREATE POLICY event_append ON course_event FOR INSERT WITH CHECK (
 organization_id=nullif(current_setting('app.organization_id',true),'')::uuid AND actor_id=nullif(current_setting('app.actor_id',true),'')::uuid
);
