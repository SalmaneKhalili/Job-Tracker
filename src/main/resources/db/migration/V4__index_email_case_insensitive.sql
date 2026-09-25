-- Emails are normalised to lower case by AuthService; this index makes that
-- guarantee hold in the database too, so the uniqueness survives concurrent
-- registrations that differ only by case (a plain unique(email) would not).
CREATE UNIQUE INDEX idx_users_email_lower ON users (lower(email));

-- Listing is always "one user's applications, newest first"; the composite
-- index serves the common unfiltered page. The status filter is served by
-- idx_applications_user_status, and company lookups benefit from a trigram-free
-- prefix match on the normalised value below.
CREATE INDEX idx_applications_user_company ON applications (user_id, company);
