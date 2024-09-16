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