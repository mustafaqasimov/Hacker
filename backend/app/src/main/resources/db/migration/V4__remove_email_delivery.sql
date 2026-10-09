DROP TABLE auth_mail_outbox;
DROP TABLE action_token;
ALTER TABLE user_account DROP COLUMN email_verified;
