-- BR-IAM-04, AC-VND-03: "vendor staff" is a relationship to one specific vendor, not a global
-- capability — the same account can be staff for one vendor and unrelated to another. iam.Role's
-- VENDOR_STAFF constant names the actor from BRD Section 8; whether a given caller IS staff for
-- the vendor in front of them is answered by this table, not by holding that role globally.

CREATE TABLE vendor_staff (
    vendor_id   UUID NOT NULL,
    account_id  UUID NOT NULL,
    added_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT vendor_staff_pkey PRIMARY KEY (vendor_id, account_id),
    CONSTRAINT vendor_staff_vendor_fkey FOREIGN KEY (vendor_id) REFERENCES vendor (id),
    CONSTRAINT vendor_staff_account_fkey FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE INDEX vendor_staff_by_account_idx ON vendor_staff (account_id);
