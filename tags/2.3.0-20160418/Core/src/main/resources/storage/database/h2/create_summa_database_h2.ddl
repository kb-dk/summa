CREATE TABLE IF NOT EXISTS SUMMA_RECORDS (
id VARCHAR(255) PRIMARY KEY,
base VARCHAR(31), 
deleted INTEGER, 
indexable INTEGER, 
hasRelations INTEGER,
data  BYTEA,
ctime BIGINT,
mtime BIGINT,
meta  BYTEA
);

CREATE UNIQUE INDEX IF NOT EXISTS i ON summa_records(id);
CREATE UNIQUE INDEX IF NOT EXISTS m ON summa_records(mtime);
CREATE INDEX IF NOT EXISTS b ON summa_records(base);
CREATE INDEX IF NOT EXISTS bdi ON summa_records(base,deleted,indexable);

CREATE TABLE IF NOT EXISTS summa_basestats (
base VARCHAR(31),
mtime BIGINT,
deletedindexables BIGINT,
nondeletedindexables BIGINT,
deletednonindexables BIGINT,
nonDeletedNonIndexables BIGINT,
valid INTEGER
);


CREATE TABLE IF NOT EXISTS summa_relations (
parentId VARCHAR(255),
childId VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS p ON summa_relations(parentId);
CREATE INDEX IF NOT EXISTS c ON summa_relations(childId);
CREATE UNIQUE INDEX IF NOT EXISTS pc ON summa_relations(parentId,childId);
