-- Spring Modulith event publication registry.
--
-- ADR-004 makes domain events the mechanism for cross-module notification, and REG-05 requires
-- invoice transmission to be durable and retrying. Both rest on this table: an event whose
-- listener has not completed is a row here, and it must survive a restart. It is therefore a
-- production business table and is created by migration, never by schema auto-generation.
--
-- Mapped by org.springframework.modulith.events.jpa.DefaultJpaEventPublication. The column set is
-- taken from Hibernate's own schema export for that entity; the string types deliberately differ.
-- Hibernate defaults String to varchar(255), which is too small for a serialised event payload.
-- TEXT costs nothing extra in PostgreSQL and removes a truncation failure mode on the retry path.

CREATE TABLE event_publication (
    id                     UUID                     NOT NULL,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts    INTEGER                  NOT NULL DEFAULT 0,
    status                 VARCHAR(255)
        CONSTRAINT event_publication_status_check
            CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED')),
    CONSTRAINT event_publication_pkey PRIMARY KEY (id)
);

-- Incomplete publications are read on every restart and by the resubmission path, and are a small
-- fraction of the table once it has been running. A partial index keeps that lookup independent of
-- how many completed events have accumulated.
CREATE INDEX event_publication_incomplete_idx
    ON event_publication (publication_date)
    WHERE completion_date IS NULL;

-- Completion is looked up by listener and event identity.
CREATE INDEX event_publication_by_listener_idx
    ON event_publication (listener_id, serialized_event);

-- Supports purging or archiving completed publications by age.
CREATE INDEX event_publication_by_completion_date_idx
    ON event_publication (completion_date);
