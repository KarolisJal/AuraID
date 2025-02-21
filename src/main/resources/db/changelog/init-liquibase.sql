-- Drop tables if they exist (to ensure clean state)
DROP TABLE IF EXISTS public.databasechangelog;
DROP TABLE IF EXISTS public.databasechangeloglock;

-- Create databasechangeloglock table
CREATE TABLE public.databasechangeloglock (
    id INTEGER NOT NULL,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP WITHOUT TIME ZONE,
    lockedby VARCHAR(255),
    CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id)
);

-- Initialize the lock table with a single row
INSERT INTO public.databasechangeloglock (id, locked) VALUES (1, FALSE);

-- Create databasechangelog table
CREATE TABLE public.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
); 