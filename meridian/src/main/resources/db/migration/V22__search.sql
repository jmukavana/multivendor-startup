-- BR-SCH-06: every keyword search that returned nothing, for merchandising review. Not logged for
-- a plain filter/browse with no keyword — there is no search term to review in that case — and not
-- logged for a search that returned results, since only the zero-result ones are what the
-- requirement asks for.

CREATE TABLE search_log (
    id          UUID NOT NULL,
    keyword     TEXT NOT NULL,
    searched_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT search_log_pkey PRIMARY KEY (id)
);

CREATE INDEX search_log_by_searched_at_idx ON search_log (searched_at DESC);
