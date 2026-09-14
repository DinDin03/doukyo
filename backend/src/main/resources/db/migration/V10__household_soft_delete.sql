-- The last member leaving soft deletes a household instead of dropping it, so
-- whoever left can restore it within 30 days (HouseholdService.RESTORE_WINDOW_DAYS).
--
-- No query needs a `deleted_at IS NULL` filter to hide a deleted household: it
-- has no members, and every read goes through the membership check. The one
-- entry point that doesn't is joining by invite code, which filters explicitly.
ALTER TABLE households ADD COLUMN deleted_at TIMESTAMPTZ;
-- Who may restore it. Nobody else is left to ask.
ALTER TABLE households ADD COLUMN deleted_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
