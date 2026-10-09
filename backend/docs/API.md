# API kataloqu

Baza `/api/v1`. **Hazır:** auth, organization, course və kurs AI köməkçisi endpoint-ləri. Digər bölmələr tam platformanın planlaşdırılmış API müqaviləsidir, mövcud controller kimi təqdim edilmir. OpenAPI runtime sənədi yalnız implementasiya olunan endpoint-ləri göstərir.

İcra edilmiş organization list sorğuları `page` (0–10000), `size` (1–100), nəticə `{items,page,size,total}`; vaxt UTC ISO-8601; ID UUID; səhvlər `application/problem+json`. Mutasiyalarda uyğun olduqda `Idempotency-Key`, optimistic revision/If-Match tələb olunur. API version major dəyişiklik üçün artırılır. Rol yalnız tenant scope daxilində verilir; T müəllim yalnız təyin olunduğu qrupa baxır. S tələbə, T müəllim, O təşkilat admini, P platforma admini. A həmin təşkilatın aktiv üzvüdür. Public endpoint-də rol qəbul edilmir.

## Auth — icra edilib

| Method | Yol | Giriş və nəticə | İcazə |
|---|---|---|---|
| POST | /auth/register | email,password → 202 | Public |
| POST | /auth/login | email,password → accessToken,refreshToken,expiresIn | Public |
| POST | /auth/refresh | token → rotasiya edilmiş token cütü | Public token |
| POST | /auth/logout | token → 204, token ailəsi ləğv olunur | Public token |
| POST | /auth/logout-all | 204, token version artır | Authenticated |
| POST | /auth/change-password | currentPassword,newPassword → 204 | Authenticated |
| GET | /auth/me | təhlükəsiz profil DTO | Authenticated |

## Organization — icra edilib

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| POST | /organizations | Təşkilat yarat | Authenticated, creation quota |
| GET | /organizations | Öz təşkilatları | Authenticated |
| GET/PATCH | /organizations/{org} | Təşkilatı oxu/dəyiş | A/O |
| POST | /organizations/{org}/archive | Arxivlə | O |
| GET | /organizations/{org}/membership | Öz aktiv üzvlüyü və rol | A |
| GET | /organizations/{org}/members | Üzvlər | O |
| PATCH/DELETE | /organizations/{org}/members/{member} | Rol/status, çıxar; son admin qorunur | O |
| POST/GET | /organizations/{org}/invitations | Dəvət yarat/siyahı; yaratma cavabında bir dəfəlik token qaytarılır, admin onu əl ilə paylaşır | O |
| DELETE | /organizations/{org}/invitations/{invitation} | Dəvəti ləğv et | O |
| POST | /invitations/accept | token və hesab email-i dəvət email-i ilə uyğun olmalıdır | Authenticated |
| POST/GET | /organizations/{org}/groups | Qrup yarat/siyahı | O / təyinatına görə A |
| GET/PATCH/DELETE | /organizations/{org}/groups/{group} | Qrup oxu / yenilə / arxivlə | Təyinatına görə A / O / O |
| POST/DELETE | /organizations/{org}/groups/{group}/students/{member} | Üzvlük | O |
| PUT/DELETE | /organizations/{org}/groups/{group}/teachers/{member} | Müəllim təyinatı | O |

Təyinat path-lərində `{member}` təşkilat membership ID-sidir. Əlavə hazır GET endpoint-ləri: `/organizations/{org}/groups/{group}/students` və `/organizations/{org}/groups/{group}/teachers` (O və həmin qrupa təyin edilmiş T).

Aşağıdakı bölmələrdə `/{org}` prefiksi `/organizations/{org}` deməkdir.

## Course — icra edilib

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| POST/GET | /{org}/courses | Qaralama yarat / görünən kurslar | T,O / A |
| GET | /{org}/courses/{course} | Nested mövzu/tapşırıq/məqsədlərlə kurs | T,O / təyin edilmiş S |
| PUT | /{org}/courses/{course} | `version,title,description,difficulty,topics` ilə qaralama aggregate yenilə | T,O |
| DELETE | /{org}/courses/{course}?version=N | Yalnız qaralamanı sil | T,O |
| POST | /{org}/courses/{course}/publish | `{version}`; tamlığı yoxla və yayımla | T,O |
| POST | /{org}/courses/{course}/archive | `{version}`; arxivlə | T,O |
| GET | /{org}/courses/{course}/groups | Kurs təyinatları | T,O |
| PUT/DELETE | /{org}/courses/{course}/groups/{group} | Aktiv qrupa təyin et/çıxar | O və həmin qrupun T-si |

Məzmun bir draft aggregate kimi dəyişir; ayrıca topic/task CRUD route-ları yoxdur. `topics[].tasks[].objectives[]` strukturu validasiya olunur. Yayımlanmış məzmun dəyişməzdir. List `{items,page,size,total}`; `page=0..10000`, `size=1..100`. Kurs body-si 16 KiB ümumi API həddinə tabedir.

## Kurs AI köməkçisi — icra edilib

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| GET | /ai/status | Provider konfiqurasiyası, model, allowedHintLevel | Authenticated |
| POST | /{org}/courses/{course}/tasks/{task}/ai/hint | `{explanation}` → strukturlaşdırılmış səviyyə 1 ipucu | Yayımlanmış kursa girişi olan A |

[AI inteqrasiyasının sərhədləri](AI-INTEGRATION.md): bu kurs köməkçisidir, hələ lab/attempt/RAG deyil.

## Lab və attempt — planlaşdırılıb

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| POST/GET | /{org}/lab-templates | Allowlist template registry | O/P |
| GET/PATCH | /{org}/lab-templates/{template} | Template revision | O/P |
| POST/GET | /{org}/lab-templates/{template}/variants | A/B/C və yoxlayıcı revision | T,O |
| POST | /{org}/lab-sessions | task ID-dən sessiya, server variant seçir | S |
| GET | /{org}/lab-sessions/{session} | Status/deadline; secret yoxdur | Owner/T |
| POST | /{org}/lab-sessions/{session}/stop | İdempotent stop | Owner/O |
| POST | /{org}/lab-sessions/{session}/reset | Yeni generation | Owner |
| POST | /{org}/lab-sessions/{session}/repair | Allowlist düzəliş seçimi | Owner |
| POST | /{org}/lab-sessions/{session}/attempts | Sorğu+izah → backend icrası/yoxlaması | Owner |
| GET | /{org}/lab-sessions/{session}/attempts | Cəhd tarixçəsi | Owner/T |
| GET | /{org}/attempts/{attempt} | Redaktə edilmiş sübut | Owner/T |
| PATCH | /{org}/attempts/{attempt}/explanation | İzah revision, tarixçə qorunur | Owner |

Runner private mTLS API: `PUT /internal/sessions/{session}` (start), `DELETE /internal/sessions/{session}` (stop), `POST /internal/sessions/{session}/reset`, `POST /internal/sessions/{session}/execute` (attempt ID + generation + fixed operation), `GET /internal/sessions/{session}`. Bu API internetə açılmır, sərbəst URL/image/command qəbul etmir.

## AI — planlaşdırılıb

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| POST | /{org}/lab-sessions/{session}/hints | İdempotent AI request → 202 request ID | Owner, assisted stage |
| GET | /{org}/ai-requests/{request}/events | Authenticated SSE; validated hint | Owner |
| GET | /{org}/ai-requests/{request} | Nəticə, fallback, prompt versiyası | Owner/T |
| POST | /{org}/attempts/{attempt}/evaluate-explanation | Sübutlu rubric qiymətləndirməsi | Owner/T |
| GET | /{org}/students/{member}/recommendations | Növbəti tapşırıq | Owner/T |
| POST/GET | /{org}/knowledge | Müəllim məzmunu/revision | T,O |
| GET/PATCH | /{org}/knowledge/{document} | Mətn yeniləməsi, yeni approval tələb olunur | T,O |
| POST | /{org}/knowledge/{document}/approve | Tədris təsdiqi | T,O |
| POST | /{org}/knowledge/{document}/revoke | Approval və cache invalidation | T,O |
| POST/GET | /admin/prompts | Versiyalı prompt | P |
| POST | /admin/prompts/{version}/activate | Kontrollu yayımlama | P |
| POST/GET | /admin/prompt-experiments | A/B assignment | P |
| GET | /{org}/ai-usage | Token, xərc, gecikmə, limit | O |

## Assessment, report və pilot — planlaşdırılıb

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| GET/PUT | /{org}/topics/{topic}/skills | Versiyalı bacarıq matrisi | A/T,O |
| POST | /{org}/lab-sessions/{session}/independent-assessment | Yeni unseen variant, hints disabled | Owner |
| GET | /{org}/students/{member}/mastery | Sübutlu mastery | Owner/T |
| POST | /{org}/skill-evidence/{evidence}/review | Müəllim review, original qorunur | T |
| GET | /{org}/students/{member}/report | Cəhdlərə bağlı hesabat | Owner/T |
| GET | /{org}/groups/{group}/analytics | Mastery, mistakes, hints, median, risk | T,O |
| POST | /{org}/reports/exports | PDF/CSV job | T,O |
| GET | /{org}/reports/exports/{job} | Status və qısaömürlü download | Requester |
| POST/GET | /{org}/pilots | Pilot quruluşu | T,O |
| POST | /{org}/pilots/{pilot}/assignments | Counterbalanced assignment | T,O |
| POST | /{org}/pilots/{pilot}/consents | Versiyalı razılıq | S |
| DELETE | /{org}/pilots/{pilot}/consents/me | Razılığı geri götür | S |
| GET | /{org}/pilots/{pilot}/metrics | AI/static və müstəqil nəticə | T,O |

## Qalan modullar — planlaşdırılıb

| Method | Yol | Əməliyyat | İcazə |
|---|---|---|---|
| GET | /{org}/students/{member}/achievements | Xal, nişan, streak | Owner/T |
| GET | /{org}/leaderboard | Tenant/qrup scope | A |
| GET | /billing/plans | Aktiv planlar | Authenticated |
| GET/PUT | /{org}/subscription | Abunə oxu/dəyiş | O |
| GET | /{org}/billing/usage | Aktiv tələbə istifadəsi/limit | O |
| GET | /{org}/invoices | Hesab-fakturalar | O |
| POST | /billing/webhooks/{provider} | Signature+replay yoxlaması | Provider signature |
| GET | /notifications | Öz bildirişləri | Authenticated |
| PATCH | /notifications/{notification} | Oxundu | Owner |
| GET/PUT | /notification-preferences | Kanal seçimi | Authenticated |
| GET | /{org}/audit-events | Filtrli audit | O |
| GET | /admin/audit-events | Platforma auditi | P |
| GET | /admin/users | Platforma hesabları | P |
| POST | /admin/users/{user}/block | Blok və token revoke | P |
| POST | /admin/users/{user}/unblock | Aktivləşdirmə | P |
| GET | /admin/ai-costs | Provayder/model xərc paneli | P |
| GET | /admin/lab-fleet | Runner capacity/orphans | P |
| POST | /me/data-exports | Məlumat export job | Authenticated + recent auth |
| GET | /me/data-exports/{job} | Export status | Owner |
| POST | /me/deletion-requests | Silmə workflow | Authenticated + recent auth |
| GET | /me/deletion-requests/{request} | Status/retention | Owner |

Operations: ayrıca 9091 portunda `GET /actuator/health/liveness`, `GET /actuator/health/readiness`, `GET /actuator/prometheus`. Public ingress bu portu və `/actuator/**` yolunu açmamalıdır. Dev profilində `/v3/api-docs` və `/swagger-ui/index.html` açıqdır; əməliyyatlar ayrıca Bearer token tələb edir. Prod profilində dokumentasiya endpoint-ləri söndürülür.
