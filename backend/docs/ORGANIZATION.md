# Organization modulu

Modul təşkilatları, üzvlükləri, qrupları və dəvətləri idarə edir. IdentityDirectory public portu ilə auth-a müraciət edir; auth persistence-ə giriş etmir. Təşkilat admini dəvət yaratdıqda API cavabında birdəfəlik token alır və onu dəvət alana ayrıca paylaşır; tətbiq email göndərmir. Dəvəti qəbul edən hesabın email-i dəvət email-i ilə uyğun olmalıdır. Controller yalnız JWT-dən actor ID götürür. Frontend-dən gələn rol və tenant header-i icazə mənbəyi deyil.

## İcazə modeli

| Əməliyyat | ORG_ADMIN | TEACHER | STUDENT |
|---|---|---|---|
| Öz təşkilatını oxumaq | Bəli | Bəli | Bəli |
| Təşkilatı dəyişmək/arxivləmək | Bəli | Xeyr | Xeyr |
| Üzvlükləri/dəvətləri idarə etmək | Bəli | Xeyr | Xeyr |
| Qrup yaratmaq/dəyişmək/təyinat etmək | Bəli | Xeyr | Xeyr |
| Qrupu görmək | Hamısı | Təyin olunduğu | Üzv olduğu |
| Tələbə/müəllim siyahısını görmək | Hamısı | Təyin olunduğu qrup | Xeyr |

TEACHER və ORG_ADMIN təşkilata aid rollardır; auth platform_role sahəsinə yazılmır. Bir istifadəçi müxtəlif təşkilatlarda müxtəlif rol daşıya bilər. SUPER_ADMIN üçün gizli tenant bypass yoxdur. Platforma admin imkanları sonrakı ayrıca admin modulunda auditlə verilməlidir.

Mutasiyalar təşkilat row lock-u ilə seriallaşdırılır. Lock-u gözləyən adminin rol/statusu lock alındıqdan sonra yenidən DB-dən oxunur. Son aktiv ORG_ADMIN silinə və ya aşağı rola keçirilə bilməz. İki paralel silmə sorğusu da bu invariantı pozmur. Rename və membership dəyişiklikləri DTO-dakı `version` ilə optimistic concurrency yoxlayır; köhnə revision 409 qaytarır.

Üzvlük silinməsi INACTIVE edir, qrup və təşkilat silinməsi arxivləyir. Tarixçə qorunur. İnaktiv üzvlüyün qrup/müəllim təyinatları silinir; yenidən dəvət qəbul ediləndə əvvəlki müəllim hüquqları avtomatik bərpa olunmur. Qrupa tələbə əlavə etmək yalnız aktiv STUDENT üçün mümkündür. Müəllim kimi aktiv TEACHER/ORG_ADMIN təyin edilə bilər.

## Tenant izolyasiyası

V2 migrasiyasında bütün təşkilat cədvəlləri üçün ENABLE/FORCE ROW LEVEL SECURITY var. TenantScope actor/organization/invitation digest-i parametrli `set_config(..., true)` ilə yalnız mövcud tranzaksiyaya yazır; pool-a vəziyyət daşınmır. Service əvvəl actor-un öz membership-ini yoxlayır, sonra tenant scope açır. Dəvət qəbulu əvvəl token digest-i ilə məhdud lookup edir, təsdiqlənmiş hesab email-i ilə uyğunluğu yoxlayır, sonra təşkilat lock-u altında istifadə edir.

Qrup üzvlüyü və müəllim təyinatında `(organization_id, group_id)` və `(organization_id, membership_id)` composite foreign key-ləri var. Repository predicate unudulsa belə seçilmiş tenant RLS-dən keçir; bir tenantın qrupuna başqa tenantın membership-i bağlana bilməz. Eyni tenant daxilində müəllimin qrup təyinatı service səviyyəsində ayrıca yoxlanır.

Runtime DB rolu NOSUPERUSER, NOBYPASSRLS və tenant cədvəllərinin sahibi olmamalıdır. `ProductionRlsGuard` prod profilində yanlış rolu startup zamanı rədd edir. Compose-da runtime və migration rolları ayrıdır: `migrate` birdəfəlik prosesi schema-nı qurur, `app` onun uğurlu bitməsini gözləyir və yüksək səlahiyyətli parolu almır. Runtime-a DDL hüququ verilmir.

`organization_event` runtime rolu üçün append-only-dir: SELECT/INSERT RLS qaydaları var, UPDATE/DELETE yoxdur. Actor, resurs, əməliyyat, vaxt və təyinatlar üçün group context ID saxlanır. Bunlar hazır organization auditi sayılır; platformanın ayrıca audit axtarışı/retention API-si hələ yoxdur.

## Dəvətlər və limitlər

Dəvət 32 random baytdan alınır; DB-də yalnız SHA-256 hash saxlanır. Token yaradılış cavabında bir dəfə qaytarılır, TTL üç gündür. Qəbul edən əvvəl qeydiyyatdan keçib daxil olmalı, hesab email-i dəvət email-inə uyğun olmalıdır. Eyni təşkilat/email üçün yeni dəvət əvvəlki istifadə edilməmiş linki ləğv edir. Link bir dəfə işləyir; vaxtı bitmiş, ləğv edilmiş və işlənmiş token 404 qaytarır. Başqa email sahibi qəbul edə bilməz. Mövcud aktiv üzvün rolu dəvətlə dəyişdirilmir; bunun üçün audit edilən membership endpoint-i var.

Redis dəvət limitini təşkilat/admin üzrə 50/saat tətbiq edir, Redis kəsiləndə əməliyyat 503 ilə bağlanır. Public təşkilat yaratma limiti hesabın ömrü ərzində üçdür; ayrıca creation counter üzvlükdən çıxmaqla limitin keçilməsinə imkan vermir. Bu abuse limitidir, gələcək billing entitlement limitinin əvəzi deyil. Planlara uyğun dəyişmə ayrıca migrasiya və billing işi olacaq.

List endpoint-ləri `page` (0–10000) və `size` (1–100) götürür; nəticə `{items,page,size,total}`-dır. `createdAt,id` ilə sabit sıralanır. Bütün POST/PATCH DTO-ları unknown-field rejection və bean validation-dan keçir. Digər tenantda olmayan resursla eyni 404 davranışı göstərilir.

## API nümunələri

Auth-dan alınmış access token ilə:

```http
POST /api/v1/organizations
Authorization: Bearer ACCESS_TOKEN
Content-Type: application/json

{"name":"Təlim mərkəzi"}
```

Təşkilat yaradan istifadəçi ORG_ADMIN olur. Müəllim dəvəti:

```http
POST /api/v1/organizations/ORG_ID/invitations
Authorization: Bearer ADMIN_ACCESS_TOKEN
Content-Type: application/json

{"email":"teacher@example.com","role":"TEACHER"}
```

Email linkinin `#token=` hissəsi həmin email sahibi tərəfindən `POST /api/v1/invitations/accept`-ə `{ "token": "LINK_TOKEN" }` kimi göndərilir. Cavab membership `id` və `organizationId` qaytarır. Təyinat path-ində global user ID deyil, **membership ID** istifadə edilir:

```http
PUT /api/v1/organizations/ORG_ID/groups/GROUP_ID/teachers/MEMBERSHIP_ID
Authorization: Bearer ADMIN_ACCESS_TOKEN
```

Tam endpoint siyahısı `docs/API.md` və dev `/v3/api-docs`-dədir. SQL migration/entity modelinin uyğunluğu real PostgreSQL-də `ddl-auto=validate` ilə yoxlanır.
