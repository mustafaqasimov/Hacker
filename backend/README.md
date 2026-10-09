> Cari dəyişiklik: e-poçt təsdiqi çıxarılıb; course və OpenAI kurs köməkçisi, React inteqrasiyası əlavə edilib. Əvvəlki test göstəriciləri son AI dəyişikliklərinin sübutu deyil. [Cari status](STATUS.md), [işə salma](../README.md).

# HackTrain Java backend

Java 21 / Spring Boot 3.5 modular monolith. Bu təhvil hissəsi platformanın arxitekturasını, auth və organization implementasiyasını əhatə edir. Bütün platforma hələ production-ready deyil. Dəqiq icra, test nəticələri və davam nöqtəsi [STATUS.md](STATUS.md)-dədir. Mövcud React/Supabase frontend bu backend-ə hələ keçirilməyib.

## Sənədlər

1. [Arxitektura və modul diaqramı](docs/ARCHITECTURE.md)
2. [ER diaqramı](docs/ER.md)
3. [Tam API kataloqu — hazır və planlaşdırılmış endpoint-lər](docs/API.md)
4. [AI JSON Schema və system prompt](contracts/ai/README.md)
5. [Qiymətləndirmə meyarları və sübut xəritəsi](docs/SCORING.md)

## Lokal quraşdırma

Docker Desktop/Engine və Compose v2+ lazımdır. Terminalda bu qovluğa keçin. Secret-lər `.env`-dən gəlir; repoda real secret yoxdur. İlk dəfə `.env.example` faylını `.env` kimi kopyalayın və aşağıdakı dəyişənləri yaradın. JWT_SECRET üçün `openssl rand -base64 32`, parollar üçün ayrı `openssl rand -hex 24` nəticələrindən istifadə edin. Mövcud `.env` faylını yenidən yaratmayın.

```sh
cp .env.example .env
# .env daxilində POSTGRES_PASSWORD, RUNTIME_DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET
# və GRAFANA_PASSWORD üçün ayrı random dəyərlər yazın.
docker compose up --build -d
```

Bu Compose hazırkı auth/organization tətbiqi və ayrıca migration job, PostgreSQL, Redis, Prometheus və Grafana-nı qaldırır. Tətbiq SMTP və məktub göndərişindən istifadə etmir. Lab runner hələ əlavə edilməyib. Bu **lokal dev** topologiyasıdır: DB migration və runtime user ayrıdır. Production üçün idarə olunan DB credential-ları, TLS ingress, backup və şəbəkə sərhədləri tələb olunur; dev compose-u internetə açmayın.

- API: `http://localhost:8081` (mövcud Vite frontend 8080 portundadır)
- Grafana: `http://localhost:3000`, user `admin`, parol `.env`-dəki GRAFANA_PASSWORD
- Prometheus və management 9091 yalnız daxili ops şəbəkəsindədir.
- Dev OpenAPI JSON: `/v3/api-docs`; interaktiv Swagger UI: `/swagger-ui/index.html`. Authorize düyməsinə access token daxil edin. Production profilində hər ikisi söndürülüb.

```sh
docker compose ps
docker compose logs --tail=100 app
docker compose down
```

`down` DB volume-larını saxlayır. Credential-ları loglara yazmayın.

## Java ilə build və test

JDK 21, Maven 3.9.11+ və işlək Docker lazımdır. Testcontainers PostgreSQL və Redis-i özü qaldırır; servis qapalıdırsa integration testlər skip edilmədən uğursuz olur.

```sh
mvn -B -ntp clean verify
python3 scripts/check-coverage.py
```

Surefire unit testləri, Failsafe `*IT` inteqrasiya testlərini işlədir. JaCoCo unit və integration coverage-ni `app/target/site/jacoco-aggregate/index.html`-də birləşdirir. Gate API/domain aggregate və ayrıca migrator kodunun birlikdə sətir coverage-ni minimum 80% tələb edir; gələcək yazılmamış modulları ölçmür. `mvn test` təkbaşına integration testi işlətdiyini göstərmir.

Bu sessiyada Java/Maven sistemə global quraşdırılmayıb; müvəqqəti `/tmp/hacktrain-tools` altında istifadə olunub. Gələcəkdə adi JDK/Maven quraşdırılması və ya Docker build istifadə edin.

GitHub Actions build, unit/integration test, 80% coverage gate, test artefaktları və Docker image build işlədir. Registry push/deploy və mock AI eval bu hissədə əlavə edilməyib; deploy ünvanı/registry hesabı seçilməyib.

## Auth API ssenarisi

Bütün body-lər JSON, maksimum 16 KiB, naməlum sahələr rədd edilir. Parol minimum 12 simvol, maksimum UTF-8 ilə 72 baytdır. Email ünvanı login və hesab identifikatorudur; qeydiyyatdan sonra birbaşa daxil olmaq olar. İstifadəçi qeydiyyatda rol seçə bilməz.

```sh
curl -i http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"student@example.com","password":"ReplaceWithYourStrongPass123!"}'
```

```sh
curl -s http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"student@example.com","password":"ReplaceWithYourStrongPass123!"}'
curl -s http://localhost:8081/api/v1/auth/me \
  -H 'Authorization: Bearer ACCESS_TOKEN'
curl -s http://localhost:8081/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"token":"REFRESH_TOKEN"}'
```

Token placeholder-lərini faktiki cavabla əvəz edin. Refresh çağırışından sonra köhnə token atılır. Eyni köhnə token yenidən təqdim edilərsə həmin ailənin yeni refresh token-i də ləğv edilir. Client refresh sorğularını seriallaşdırmalıdır. Ayrı cihazın ayrıca token ailəsi təsirlənmir. Access token logout/refresh replay-dən sonra maksimum 10 dəqiqə yaşaya bilər; logout-all, password change və hesab bloklanması növbəti sorğuda access token-i də rədd edir.

Şifrə dəyişmək üçün autentifikasiya olunmuş `/auth/change-password` endpoint-indən istifadə edin. Email vasitəsilə verification və şifrə bərpası endpoint-ləri yoxdur. Baza authority STUDENT-dir; təşkilat üzrə TEACHER/ORG_ADMIN organization modulunda membership ilə verilir.

Access token-i browser yaddaşında saxlayın. Bu JSON bearer API cookie authentication istifadə etmir. Persistent browser refresh saxlamaq üçün ayrıca BFF/HttpOnly cookie + CSRF adapteri hələ lazımdır. Hazırkı frontend-də localStorage ilə production sessiya inteqrasiyası etməyin.

Rate limit: IP başına 20/dəq və 100/saat; login hesab başına 10/dəq və 50/saat; qeydiyyat hesab başına 5/saat. Redis kəsiləndə auth fail-closed olaraq 503 verir. Backend client X-Forwarded-For başlığına etibar etmir; production reverse proxy real IP siyasəti ayrıca konfiqurasiya və test olunmalıdır. NAT arxasındakı böyük təlim qrupları üçün limitlər ölçülüb yenidən tənzimlənməlidir.

## Environment müqaviləsi

| Dəyişən | Məna |
|---|---|
| SPRING_PROFILES_ACTIVE | dev, test və ya prod |
| DB_URL / DB_USER / DB_PASSWORD | Runtime PostgreSQL JDBC və credential |
| POSTGRES_PASSWORD / RUNTIME_DB_PASSWORD | Compose migration/bootstrap və ayrıca runtime rol parolları |
| REDIS_URL | Redis URI; prod-da TLS və credential, URI-safe password |
| JWT_SECRET | Base64, minimum 32 random bayt, yalnız serverdə |
| JWT_ISSUER / JWT_AUDIENCE | Default hacktrain / hacktrain-api |
| CORS_ORIGINS | Vergüllə ayrılmış konkret origin-lər, wildcard qadağandır |

Render-də `REDIS_URL`-u Render Environment bölməsində ayrıca secret kimi təyin edin. Upstash istifadə edilirsə, Upstash konsolundakı hazırkı host və token ilə TLS URI istifadə olunmalıdır: `rediss://default:<token>@<host>:6379`. Host hissəsi DNS-də həll olunmalıdır; köhnə və ya səhv hostname tətbiqin startını dayandırır. Dəyəri dəyişdikdən sonra servisi yenidən deploy edin. Redis bağlantısı rate limit-lər üçün məcburidir; Redis xətasında auth sorğuları təhlükəsizlik üçün rədd edilir.

AuthRetention vaxtı keçmiş refresh token-ləri bir günlük buffer ilə təmizləyir; istifadə edilmiş refresh hash-ləri ailə bitənədək saxlanır.

## Production qəbul şərtləri

Hazır auth testləri tam platformanın qəbulunu əvəz etmir. Daha geniş domain tenant izolyasiyası, lab runner hardening, AI evidence və secret-leak testləri, müəllim approval workflow, billing provider webhook-ları, GDPR workflow, backup restore, secret rotation, dependency/container CVE scan, 500 aktiv istifadəçi yük sınağı və staging e2e keçməlidir. Real provayder eval nəticəsi və pilot olmadan AI keyfiyyəti və istifadəçi faydası iddiası verilmir.

## Compose smoke yoxlaması

Lokal Compose servisləri qalxdıqdan sonra `python3 scripts/smoke-auth.py` ayrıca sintetik hesab yaradır, login, profil, refresh rotation/replay revocation, organization invitation və Grafana health-i yoxlayır. Credential/token-lər ekrana yazılmır. Bu script lokal dev üçündür, real istifadəçi/pilot məlumatı yaratmır.


## Təşkilat və migration axını

[Organization modulunun icazələri və API nümunələri](docs/ORGANIZATION.md) ayrıca verilib. Auth/organization-dan sonra növbəti kod modulu course-dur. Təşkilat yaratmaq, müəllim/tələbə dəvət etmək, qrup yaratmaq və membership ID ilə təyinat etmək artıq işləyən API-lərdir.

Flyway artıq API startup-ında işləmir. Compose `migrate` servisini uğurla tamamlayıb sonra `app`-ı açır. Java ilə ayrı deploy zamanı əvvəl `migration/target/migration-1.0.0-SNAPSHOT.jar`-ı migration roluna aid DB_URL/DB_USER/DB_PASSWORD ilə işlədin; sonra API JAR-ını yalnız runtime credential ilə başladın. Migration parolu API environment-inə verilməməlidir. Yeni V4 migration email outbox/action token cədvəllərini və təsdiq flag-ini silir; production DB-də tətbiq etməzdən əvvəl backup saxlayın. Production rolunun cədvəl sahibi, superuser və ya BYPASSRLS olması startup-da rədd edilir.

PostgreSQL init script-i yalnız yeni data volume-da rol yaradır. Mövcud DB-yə keçiddə operator runtime rolunu ayrıca yaratmalı və migration rolunun gələcək cədvəlləri üçün default privileges verməlidir; real məlumat volume-nu silmək migration üsulu deyil. Dev Compose-un frontend origin-i mövcud Vite konfiqurasiyasına uyğun `http://localhost:8080`, backend portu `8081`-dir.
