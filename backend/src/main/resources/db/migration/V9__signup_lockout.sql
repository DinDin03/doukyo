-- Locks a pending sign-up after too many wrong codes.
--
-- Previously the row was DELETED on the last failed attempt, which is not a
-- lockout at all: the caller simply requested a new code and got a fresh set of
-- attempts. Keeping the row and recording a deadline is what makes the limit
-- mean something — and startSignUp has to honour it too, or "resend" bypasses it.
ALTER TABLE pending_signups ADD COLUMN locked_until TIMESTAMPTZ;
