-- Messages are shared household history, not the sender's private property:
-- deleting a user must not silently erase everyone else's conversation.
-- Matches the policy already used for money (expenses.paid_by is RESTRICT).
--
-- The principle: relationships CASCADE, content does not. memberships and
-- message_reads still cascade — they are links, meaningless without their owner.
-- messages.household_id also stays CASCADE: a thread has no meaning without
-- the house it belongs to.

ALTER TABLE messages DROP CONSTRAINT messages_sender_id_fkey;

ALTER TABLE messages ADD CONSTRAINT messages_sender_id_fkey
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE RESTRICT;
