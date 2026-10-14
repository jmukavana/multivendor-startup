-- BR-ADM-08: a session issued for an admin acting as another account, for support purposes.
-- Nullable: every session before this one, and every ordinary login session after it, has no
-- impersonator — only a session iam.internal.AuthenticationService.issueImpersonationSession
-- creates carries one. References account, not a separate admin table, because the impersonator
-- is just another account — the same one BR-ADM-07's audit log already records this action
-- against.
ALTER TABLE session_token
    ADD COLUMN impersonator_account_id UUID
        CONSTRAINT session_token_impersonator_fkey REFERENCES account (id);

-- BR-ADM-08's "visible to the vendor": the query behind iam.internal
-- .AuthenticationService.impersonationHistory scans only the much smaller set of sessions that
-- are actually impersonations, ordered the way it reads them back.
CREATE INDEX session_token_impersonation_idx ON session_token (account_id, issued_at DESC)
    WHERE impersonator_account_id IS NOT NULL;
