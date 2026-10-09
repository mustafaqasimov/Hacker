# İcra və davam nöqtəsi

9 oktyabr 2026. Platformanın arxitekturası və ilk iki domain modulu — **auth və organization** — implementasiya edilib. Tam production platforma hələ tamamlanmayıb. Boş controller/service skeletləri yaradılmayıb; qalan modullar sənədlərdə plan kimi göstərilir.

## Hazır olanlar

- 13 modulun arxitekturası, Mermaid modul və hədəf ER diaqramları, bütün nəzərdə tutulan endpoint-lərin kataloqu.
- Java 21 / Spring Boot 3.5.16 / Maven: `auth`, `organization`, `app` və ayrıca `migration` executable.
- Flyway V1/V2; auth üçün 5, organization üçün 8 entity; repository, service, controller, DTO, MapStruct və validasiya.
- Auth: qeydiyyat, email verification/resend, login, JWT access, opaque refresh, rotation/replay detection, logout/logout-all, password forgot/reset/change, profil.
- Organization: təşkilat, membership rolları, qrup, müəllim/tələbə təyinatı, email-bound birdəfəlik dəvət, son admin qorunması, optimistic version, arxivləmə, creation quota, audit.
- PostgreSQL RLS və composite FK; məhdud runtime DB rolu, production role guard, ayrıca migration prosesi; API migration parolunu almır.
- BCrypt, şifrəli transactional mail outbox, SMTP retry, credential retention, Bucket4j/Redis limitləri, CORS, headers, JSON ölçüsü, RFC 7807.
- Dev/test/prod konfiqurasiyası, dev Swagger/OpenAPI, Actuator/Micrometer, Prometheus/Grafana, JSON loglar.
- Docker Compose, CI build/test/coverage/Docker-image-build, README və API nümunələri, scoring xəritəsi.
- AI hint JSON Schema və ilkin system prompt **müqaviləsi**; runtime AI implementasiyası deyil.

## Faktiki yoxlamalar

| Yoxlama | Nəticə |
|---|---|
| `mvn -B -ntp clean verify` | BUILD SUCCESS |
| Unit testlər | 42 keçdi |
| PostgreSQL/Redis Testcontainers integration testləri | 34 keçdi |
| Cəmi | 76 keçdi, 0 failed, 0 skipped |
| Backend line coverage — auth, organization, app, migration birlikdə | 584 / 651 = 89,71% |
| Minimum 80% line coverage gate | Keçdi |
| Docker image build və təmiz DB-də V1/V2 migration | Keçdi |
| Compose SMTP/API/organization smoke ssenarisi | 20 yoxlama keçdi |
| App readiness, PostgreSQL və Redis health | Sağlam |
| Ayrı migration servisi | Exit code 0 |
| Prometheus `up{job="hacktrain"}` | 1 |
| Grafana health | database ok |

Cari sübutlar: [test nəticələri](docs/evidence/backend-tests.csv), [coverage sayğacları](docs/evidence/backend-coverage.json), [real Compose smoke](docs/evidence/compose-smoke.txt). İlk auth mərhələsinin ayrıca tarixi snapshot-ları `auth-tests.csv` və `auth-coverage.json`-da saxlanır; onlar cari ümumi nəticə deyil. HTML hesabatları build-dən sonra `app/target/site/jacoco-aggregate/index.html` və `migration/target/site/jacoco/index.html`-dədir.

Coverage yalnız yazılmış kod üçündür, gələcək modulların tamamlanma faizi deyil. Branch/method coverage ayrıca JSON-da verilir; line coverage ilə eyniləşdirilmir. CI faylı hazırdır, amma GitHub-da workflow run başladılmayıb; nəticələr burada lokal real build/test gedişində alınıb.

Təhlükəsizlik sınaqları refresh replay və paralel rotation, password/token revocation, blocked user, foreign origin, forged forwarded IP, Redis outage, böyük JSON, AES-GCM tamper, email token expiry/reuse; həmçinin başqa tenantı oxuma/yazma, pool scope sıfırlanması, composite FK, eyni vaxtda son adminlərin silinməsi, lock gözləyərkən admin hüququnun ləğvi, dəvət email/expiry/replay, müəllimin yalnız öz qruplarını görməsi və runtime audit silməsinin rəddini yoxlayır. Tenant testlərinin DB rolu həqiqətən NOSUPERUSER/NOBYPASSRLS-dir.

İş zamanı aşkar edilərək düzəldilən xətalar: Testcontainers import toqquşması, mock SMTP health contributor konfiqurasiyası, Compose tmpfs YAML ayrılması, mövcud Vite 8080 portu ilə toqquşma və yalnız internal şəbəkələrdə host portların açılmaması. Backend localhost:8081 istifadə edir; data/ops şəbəkələri daxili, localhost girişləri ayrıca edge şəbəkəsindədir.

## Davam nöqtəsi

**Növbəti: `course` modulunun V3 migrasiyası və domain modelindən başlamaq.**

Ardıcıllıq: course → lab → attempt → ai → assessment → report → gamification → billing → notification → audit → admin. Tamamlanan Flyway versiyalarını növbəti dəyişiklikdə redaktə etməyin; yeni versiya yaradın. Modulların statusu runtime OpenAPI-dəki real endpoint-lərlə uyğun saxlanmalıdır.

Qalan işlər: həmin modulların entity/migration/API/service/testləri; kurs/mövzu/tapşırıq/məqsəd; real A/B/C laboratoriyalar və izolə runner; provider/fallback/RAG; prompt DB/A-B; schema/evidence/secret runtime validation; SSE; AI büdcə/cache/usage/cost; 20×3 AI eval HTML/CSV; əlavə tenant və session isolation; PDF/CSV; billing; GDPR export/deletion/consent; 500 aktiv istifadəçi yük testi; AI Grafana dashboard; registry/deploy pipeline; frontend inteqrasiyası və real pilot.

Production hazırlığı bütöv platforma üçün təsdiqlənməyib. AI-yə real sorğu göndərilməyib, AI keyfiyyəti/xərci və 500 istifadəçi tutumu ölçülməyib. Heç bir deploy/publish edilməyib. `auth_mail_outbox`, `auth_event` və `organization_event` hazır modulların infrastrukturna aiddir; ayrıca notification/audit modulunun tamamlanması sayılmır.
