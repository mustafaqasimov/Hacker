# Qiymətləndirmə meyarları və sübutlar

Bu cədvəl bal proqnozu deyil. Hazır kod, planlaşdırılmış funksiya və hələ əldə edilməmiş istifadəçi sübutu ayrılır.

| Meyar | Maksimum | Cavabdeh modul | Tələb olunan sübut | Bu hissənin vəziyyəti |
|---|---:|---|---|---|
| İstifadəçi dəyəri | 25 | attempt, assessment, report | Yeni variantda köməksiz nəticə, müəllim vaxtı, razılıqlı pilot | Arxitektura/API planı var; pilot və modul nəticəsi yoxdur |
| Prototip və AI | 30 | lab, ai, course | Real lab → cəhd → uyğun hint → B variantı → sübutlu report | Kurs frontend-i və OpenAI adapteri var; API açarı/live AI nəticəsi, lab və pilot yoxdur |
| Keyfiyyət yoxlaması | 20 | auth və bütün domain modulları | Unit/integration, 20×3 AI eval, failure/retest, tenant isolation | Auth, organization, RLS/tenant və migration testləri, coverage gate var; faktiki nəticə STATUS-da |
| Həyata keçirilmə | 15 | billing, ai, ops | Token/lab xərci, SLA, deploy/rollback, pilot tərəf | Auth/organization Docker/CI/monitoring konfiqurasiyası var; xərc/pilot yoxdur |
| Orijinallıq | 10 | assessment, ai, report | Cəhdə bağlı müdaxilə + yeni variantda müstəqil tətbiq, müqayisə | Dizayn mövcuddur; müqayisə nəticəsi yoxdur |

Şəkildəki 25/30/20/15/10 bölgüsü istifadə edilib. Funksiya siyahısı və mock AI cavabları istifadəçi faydası və model keyfiyyətinin sübutu sayılmır.
