> Cari yeniləmə: **Groq aktivdir** (`openai/gpt-oss-20b`). Backend-only `.env.groq.local` konfiqurasiyası istifadə olunur. Java adapterindən canlı cavab alınıb və JSON Schema/səviyyə validasiyası keçib. Claude kredit maneəsi aktiv Groq provider-inə aid deyil. Pentest/test suite işlədilməyib.

> Son yeniləmə: Claude adapteri əlavə edildi və aktiv provider seçildi. Açar ayrı, ignore edilən `.env.claude.local` faylındadır. Model endpoint-i HTTP 200 qaytarıb, generasiya isə API kredit balansına görə HTTP 400 ilə bloklanıb. Canlı uğurlu AI cavabı hələ alınmayıb. Kredit əlavə etmək lazımdır. Pentest/test suite icra edilməyib.

# HackTrain — cari icra statusu

## Son istifadəçi göstərişi

E-poçt göndərişi və SMTP inteqrasiyası çıxarılıb; email yalnız hesab identifikatoru kimi qalır. Email verification və recovery endpoint-ləri silinib. Təşkilat dəvət tokeni adminə bir dəfə qaytarılır ki, ayrıca paylaşılsın. V4 migration köhnə outbox/action-token cədvəlləri və email verification flag-ini silir.

## Hazır kod

- `auth`: email/şifrə qeydiyyatı və birbaşa giriş; JWT/refresh, logout və şifrə dəyişmə. Email yalnız hesab identifikatorudur; göndəriş, verification və email recovery yoxdur.
- `organization`: təşkilat, tenant scope/RLS, üzvlər, qruplar, müəllim/tələbə təyinatları, email-ə bağlanan dəvətlər; admin birdəfəlik dəvət tokenini alıb əl ilə paylaşır.
- `course`: V3 Flyway, Course/Topic/Task/LearningObjective/CourseGroup/CourseEvent; MapStruct, DTO, validation, API. Qaralama redaktə/silmə, tamlıq yoxlaması ilə publish, archive, qrup təyinatı. Yayımlanmış məzmun dəyişməzdir. Tələbə yalnız aktiv qrupuna təyin olunmuş yayımlanmış kursları görür.
- `ai`: OpenAI Responses adapteri, backend-only kontekst, schema validator, səviyyə 1, Redis limitləri, circuit breaker, timeout, token/latency logları və işarələnmiş static fallback. Claude açarı konfiqurasiya olunub; API krediti çatmır.
- React frontend: auth, account, organization, group, invitation, course editor/catalog/detail və tapşırıq daxilində AI köməkçisi. API tokenləri yalnız yaddaşdadır.
- Compose backend stack, migration job, CI konfiqurasiyası, Prometheus/Grafana auth/organization infrastrukturu əvvəlki işdən qalır.

## Yoxlama nəticələrinin sərhədi

Auth+organization əvvəlki vəziyyətdə 76 test/89.71% coverage ilə yoxlanmışdı. Course və membership əlavə olunduqdan sonra, son dayandırma göstərişindən ƏVVƏL Maven clean verify keçib. Frontend HTTP adapterinin 10 unit testi həmin vaxt keçib. AI, e-poçt təsdiqinin çıxarılması və son konfiqurasiya dəyişiklikləri bu tarixi nəticələrə daxil deyil.

Lokal Docker image yenilənib; V3 migrasiya job-u tamamlanıb və app container healthy vəziyyətdədir. Backend konfiqurasiyası git-dən kənar, 0600 icazəli `backend/.env` faylında saxlanır. Claude açarı `.env.claude.local` faylındadır.

Bu dəyişiklikdən sonra `mvn -B -ntp -pl app -am test` Docker Maven image ilə uğurla tamamlanıb; unit testlər keçib və integration test mənbələri test compile-dən keçib. Failsafe/Testcontainers integration testləri bu run-da işə salınmayıb. Docker Compose dev/prod konfiqurasiyaları validasiya edilib, smoke script sintaksis yoxlamasından keçib. AI eval, pentest, yük testi və canlı model çağırışı aparılmayıb. Bütöv platforma üçün production hazırlığı təsdiqlənməyib.

## Davam nöqtəsi

Əvvəl Anthropic hesabına API krediti əlavə edilməlidir. Provider və model artıq konfiqurasiya edilib. Secret-lər frontend-ə və ya çata yazılmamalıdır.

Qalan modullar: lab → attempt → assessment → report → gamification → billing → notification → audit → admin. AI üzrə qalanlar: Claude/Gemini və provider fallback, pgvector RAG, real attempt sübutları, səviyyə 2/3 icazə siyasəti, prompt DB/A-B, DB usage/xərc/cache, SSE, 20×3 eval. GDPR/pilot, 500 istifadəçi yük testi və production deploy tamamlanmayıb.

V1/V2/V3 migrasiyalarını növbəti işdə redaktə etməyin. Email çatdırılmasını silən V4 əlavə edildi; yeni DB dəyişiklikləri V5-dən başlamalıdır.

Ətraflı [AI inteqrasiyası](docs/AI-INTEGRATION.md), [frontend](../docs/FRONTEND.md), [API](docs/API.md). Tarixi sübutlar `docs/evidence/`-də saxlanılır.
