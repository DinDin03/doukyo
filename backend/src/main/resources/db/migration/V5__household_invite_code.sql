ALTER TABLE households ADD COLUMN invite_code VARCHAR(10) NOT NULL;
ALTER TABLE households ADD CONSTRAINT uq_households_invite_code UNIQUE (invite_code);
