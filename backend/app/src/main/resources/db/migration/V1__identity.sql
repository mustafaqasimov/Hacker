CREATE TABLE user_account (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE CHECK (email = lower(btrim(email))),
    password_hash varchar(100) NOT NULL,
    email_verified boolean NOT NULL DEFAULT false,
    blocked boolean NOT NULL DEFAULT false,
    token_version bigint NOT NULL DEFAULT 0 CHECK (token_version >= 0),
    platform_role varchar(32) NOT NULL DEFAULT 'STUDENT' CHECK (platform_role IN ('STUDENT','SUPER_ADMIN')),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE refresh_session (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES user_account(id),
    family_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    revoked boolean NOT NULL DEFAULT false
);
CREATE INDEX refresh_session_family_idx ON refresh_session(family_id);
CREATE INDEX refresh_session_user_idx ON refresh_session(user_id);
CREATE INDEX refresh_session_expiry_idx ON refresh_session(expires_at);
CREATE TABLE action_token (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES user_account(id),
    purpose varchar(32) NOT NULL CHECK (purpose IN ('VERIFY_EMAIL','RESET_PASSWORD')),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz
);
CREATE INDEX action_token_user_idx ON action_token(user_id);
CREATE INDEX action_token_expiry_idx ON action_token(expires_at);
CREATE TABLE auth_mail_outbox (
    id uuid PRIMARY KEY,
    recipient varchar(254) NOT NULL,
    subject varchar(255) NOT NULL,
    encrypted_body text,
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at timestamptz NOT NULL,
    sent_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (sent_at IS NOT NULL OR encrypted_body IS NOT NULL)
);
CREATE INDEX auth_mail_pending_idx ON auth_mail_outbox(next_attempt_at) WHERE sent_at IS NULL AND attempts < 10;
CREATE TABLE auth_event (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES user_account(id),
    event_type varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX auth_event_user_time_idx ON auth_event(user_id,created_at);
