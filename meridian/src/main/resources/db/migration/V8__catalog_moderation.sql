-- BR-CAT-05/06/08/09 (partial), AC-CAT-01 to 04: images, a publish step, price history, and the
-- prohibited-items check.
--
-- BR-CAT-06's full six states (Draft, Pending Review, Active, Out of Stock, Delisted, Rejected)
-- are not all implemented — see ProductStatus's javadoc for the reduced set kept for Phase 2 and
-- what is deliberately deferred.

-- A product is created DRAFT now, not ACTIVE — AC-CAT-02 requires publishing without an image to
-- be refused, which is only enforceable if there is a state before "visible to buyers" for that
-- refusal to leave the product in.
ALTER TABLE product DROP CONSTRAINT product_status_check;
ALTER TABLE product ADD CONSTRAINT product_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'DELISTED'));

-- BR-CAT-05: at least one image before a product may publish. blob_reference is opaque, resolved
-- through shared.BlobStorage — never a filesystem path or URL, matching verification_document.
CREATE TABLE product_image (
    id             UUID         NOT NULL,
    product_id     UUID         NOT NULL,
    blob_reference TEXT         NOT NULL,
    content_type   TEXT         NOT NULL,
    -- Display order among a product's images; the lowest is the primary image shown in listings.
    sort_order     INTEGER      NOT NULL,
    uploaded_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT product_image_pkey PRIMARY KEY (id),
    CONSTRAINT product_image_product_fkey FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE INDEX product_image_by_product_idx ON product_image (product_id, sort_order);

-- BR-CAT-09, AC-CAT-04: every price change is retrievable — previous price, new price, actor,
-- timestamp. Append-only, matching the audit-log pattern used throughout this codebase
-- (auth_event, order_event, payout_detail_access_event).
CREATE TABLE product_price_change (
    id                     UUID         NOT NULL,
    product_id             UUID         NOT NULL,
    previous_price_minor_units BIGINT   NOT NULL,
    new_price_minor_units  BIGINT       NOT NULL,
    currency               TEXT         NOT NULL,
    actor_account_id       UUID         NOT NULL,
    changed_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT product_price_change_pkey PRIMARY KEY (id),
    CONSTRAINT product_price_change_product_fkey FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE INDEX product_price_change_by_product_idx ON product_price_change (product_id, changed_at DESC);

-- BR-CAT-08, AC-CAT-03: the prohibited-items list. OPEN-07 records that the real business list has
-- not been drafted; these are placeholder entries so the mechanism is real and testable ahead of
-- that decision — see ProhibitedTermRepository's javadoc.
CREATE TABLE prohibited_term (
    id        UUID NOT NULL,
    term      TEXT NOT NULL,
    added_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT prohibited_term_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX prohibited_term_key ON prohibited_term (LOWER(term));

INSERT INTO prohibited_term (id, term, added_at) VALUES
    (gen_random_uuid(), 'firearm', now()),
    (gen_random_uuid(), 'counterfeit', now()),
    (gen_random_uuid(), 'live animal', now());
