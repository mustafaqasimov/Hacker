# AI müqaviləsinin statusu

Schema və ilkin system prompt bu mərhələdə dizayn artefaktıdır. Provider, JSON Schema runtime validator, evidence/hint/secret yoxlaması, RAG və eval runner hələ icra edilməyib. Prompt təkbaşına təhlükəsizlik təminatı deyil. Backend `fallback`, request ID, prompt version, approved source revision və usage metadata-nı validasiyadan SONRA response envelope-a əlavə etməlidir; model bu sahələri seçmir.

Plan sənədində 20 konkret giriş yoxdur: 6+4+4+3+3 kateqoriyası verilir. Növbəti AI mərhələsində bu bölgüdə konkret fixture-lər hazırlanacaq, müəllim tərəfindən yoxlanacaq, hərəsi üç dəfə işlədiləcək. Mock testləri provider inteqrasiyasını yoxlayır; texniki düzgünlük üzrə real model eval-ını əvəz etmir.
