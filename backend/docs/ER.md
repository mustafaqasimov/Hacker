# Hədəf məlumat modeli

Aşağıdakı diaqram bütün platforma üçün nəzərdə tutulur. Auth cədvəlləri `V1__identity.sql` və email göndərişindən qalan obyektləri təmizləyən `V4__remove_email_delivery.sql`, təşkilat/üzvlük/qrup/dəvət/audit/creation-quota cədvəlləri `V2__organizations.sql`-dədir. Kurs/mövzu/tapşırıq/məqsəd/təyinat hadisələri `V3__courses.sql`-dədir. Qalan cədvəllər modul implementasiyası ilə ardıcıl migrasiyalarda yaradılacaq; diaqram migration kimi təqdim edilmir. Bütün tenant cədvəllərində organization_id, created_at, optimistic version, tenant daxilində unique və composite foreign key nəzərdə tutulur.

```mermaid
erDiagram
  USER_ACCOUNT ||--o{ REFRESH_SESSION : owns
  USER_ACCOUNT ||--o{ AUTH_EVENT : generates
  USER_ACCOUNT ||--o{ MEMBERSHIP : joins
  USER_ACCOUNT ||--o| ORGANIZATION_CREATOR_QUOTA : limits
  ORGANIZATION ||--o{ ORGANIZATION_EVENT : audits
  ORGANIZATION ||--o{ MEMBERSHIP : grants
  ORGANIZATION ||--o{ STUDY_GROUP : contains
  STUDY_GROUP ||--o{ GROUP_MEMBER : contains
  MEMBERSHIP ||--o{ GROUP_MEMBER : participates
  STUDY_GROUP ||--o{ TEACHER_ASSIGNMENT : has
  MEMBERSHIP ||--o{ TEACHER_ASSIGNMENT : teaches
  ORGANIZATION ||--o{ INVITATION : issues
  ORGANIZATION ||--o{ COURSE : owns
  COURSE ||--o{ TOPIC : contains
  TOPIC ||--o{ TASK : contains
  TOPIC ||--o{ SKILL : defines
  TASK ||--o{ LEARNING_OBJECTIVE : targets
  STUDY_GROUP ||--o{ COURSE_ENROLLMENT : studies
  COURSE ||--o{ COURSE_ENROLLMENT : enrolls
  TASK ||--o{ LAB_TEMPLATE : provides
  LAB_TEMPLATE ||--o{ LAB_VARIANT : has
  LAB_VARIANT ||--o{ LAB_SESSION : runs
  MEMBERSHIP ||--o{ LAB_SESSION : solves
  LAB_SESSION ||--o{ ATTEMPT : records
  ATTEMPT ||--o{ VERIFICATION_RESULT : verifies
  ATTEMPT ||--o{ SKILL_EVIDENCE : supports
  SKILL ||--o{ SKILL_EVIDENCE : measures
  MEMBERSHIP ||--o{ MASTERY : earns
  SKILL ||--o{ MASTERY : scores
  TOPIC ||--o{ KNOWLEDGE_REVISION : explains
  KNOWLEDGE_REVISION ||--o{ KNOWLEDGE_CHUNK : embeds
  PROMPT_VERSION ||--o{ AI_REQUEST : instructs
  LAB_SESSION ||--o{ AI_REQUEST : requests
  AI_REQUEST ||--o{ AI_PROVIDER_CALL : consumes
  AI_REQUEST ||--o{ AI_EVIDENCE : cites
  ATTEMPT ||--o{ AI_EVIDENCE : referenced
  ORGANIZATION ||--o{ AI_BUDGET : limits
  ORGANIZATION ||--o{ EXPERIMENT : designs
  EXPERIMENT ||--o{ PILOT_ASSIGNMENT : assigns
  MEMBERSHIP ||--o{ PILOT_ASSIGNMENT : participates
  MEMBERSHIP ||--o{ CONSENT : accepts
  MEMBERSHIP ||--o{ REPORT_JOB : requests
  MEMBERSHIP ||--o{ POINT_LEDGER : earns
  MEMBERSHIP ||--o{ BADGE_AWARD : receives
  MEMBERSHIP ||--o{ STREAK : maintains
  ORGANIZATION ||--|| SUBSCRIPTION : purchases
  PLAN ||--o{ SUBSCRIPTION : defines
  SUBSCRIPTION ||--o{ ACTIVE_STUDENT_USAGE : meters
  SUBSCRIPTION ||--o{ INVOICE : bills
  INVOICE ||--o{ PAYMENT_EVENT : reconciles
  MEMBERSHIP ||--o{ NOTIFICATION : receives
  ORGANIZATION ||--o{ AUDIT_EVENT : records
  USER_ACCOUNT ||--o{ DATA_EXPORT_JOB : requests
  USER_ACCOUNT ||--o{ DELETION_REQUEST : requests
  USER_ACCOUNT {
    uuid id PK
    string email UK
    string password_hash
    boolean blocked
    bigint token_version
    string platform_role
    timestamptz created_at
  }
  REFRESH_SESSION {
    uuid id PK
    uuid user_id FK
    uuid family_id
    string token_hash UK
    timestamptz expires_at
    timestamptz used_at
    boolean revoked
  }
  MEMBERSHIP {
    uuid id PK
    uuid organization_id FK
    uuid user_id FK
    string role
    string status
  }
  LAB_SESSION {
    uuid id PK
    uuid organization_id FK
    uuid student_id FK
    uuid variant_id FK
    integer generation
    string stage
    string state
    timestamptz expires_at
  }
  ATTEMPT {
    uuid id PK
    uuid organization_id FK
    uuid session_id FK
    integer generation
    string request_redacted
    string response_summary
    string explanation
    integer hint_level
    string status
    timestamptz created_at
  }
  AI_PROVIDER_CALL {
    uuid id PK
    uuid request_id FK
    string provider
    string model
    bigint input_tokens
    bigint output_tokens
    bigint latency_ms
    decimal cost
    string tariff_version
    string outcome
  }
  KNOWLEDGE_CHUNK {
    uuid id PK
    uuid revision_id FK
    uuid organization_id FK
    string text
    vector embedding
    string embedding_model
  }
```
