-- One-time: the disciplines currently in the DB all belong to
-- "Ingineria Informației" (Limba Engleză). Stamp profile + teaching_language so
-- they land under that branch of the admin Discipline menu.
-- Facultativ courses (Limba Engleză, Ed. fizică, ...) stay facultativ — only
-- their profile/language is set, category is untouched.

update public.courses
set profile = 'Ingineria Informației',
    teaching_language = 'Engleză';
