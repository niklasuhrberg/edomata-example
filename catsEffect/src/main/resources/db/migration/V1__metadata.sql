create table metadata_types (
    id varchar primary key not null
);

create table metadata (
  id uuid primary key not null,
  entity_id uuid ,
  parent uuid,
  created_by varchar not null,
  category varchar not null
);
create table attachments
(
    id            uuid primary key not null,
    status        varchar          not null,
    name          varchar          not null,
    fileExtension varchar          not null,
    description   varchar          not null,
    content_type  varchar          not null,
    location      varchar          not null,
    version       integer                   DEFAULT 0,
    created_at    timestamptz      not null DEFAULT CURRENT_TIMESTAMP
);