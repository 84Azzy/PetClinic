-- Existing databases only. Back up the database before running this script once.
-- Fresh environments already receive these columns from db/schema.sql or petclinic.sql.
-- Precondition: photo_url and avatar_url are NULL, or their files have already been migrated to
-- COS and the values replaced with object keys. See README.md for the required precheck query.

ALTER TABLE pet
  CHANGE COLUMN photo_url photo_object_key VARCHAR(255) NULL;

ALTER TABLE vet
  CHANGE COLUMN avatar_url avatar_object_key VARCHAR(255) NULL;
