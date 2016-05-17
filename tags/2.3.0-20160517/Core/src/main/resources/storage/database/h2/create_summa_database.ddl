CREATE TABLE SUMMA_RECORDS (
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

CREATE UNIQUE INDEX i ON summa_records(id);
CREATE UNIQUE INDEX m ON summa_records(mtime);
CREATE INDEX b ON summa_records(base);
CREATE INDEX bdi ON summa_records(base,deleted,indexable);

CREATE TABLE summa_basestats (
base VARCHAR(31),
mtime BIGINT,
deletedindexables BIGINT,
nondeletedindexables BIGINT,
deletednonindexables BIGINT,
nonDeletedNonIndexables BIGINT,
valid INTEGER
);


CREATE TABLE  summa_relations (
parentId VARCHAR(255),
childId VARCHAR(255)
);

CREATE INDEX p ON summa_relations(parentId);
CREATE INDEX c ON summa_relations(childId);
CREATE UNIQUE INDEX pc ON summa_relations(parentId,childId);
