create table metadata_types
(
    id varchar primary key not null
);

create table entitytypes
(
    id          varchar primary key not null,
    description varchar
);

create table entities
(
    id            uuid primary key not null,
    entitytype_id varchar,
    entity_id     uuid             not null,
    UNIQUE (entity_id),
    constraint entity_entitytypes_fk foreign key (entitytype_id) references entitytypes (id) on update RESTRICT on delete CASCADE
);

create table metadata
(
    id         uuid primary key not null,
    entity_id  uuid,
    parent     uuid,
    created_by varchar          not null,
    category   varchar          not null
);
create table items
(
    id          uuid primary key not null,
    metadata_id uuid             not null,
    name        varchar          not null,
    value       varchar          not null,
    constraint fk_items_metadata foreign key (metadata_id) references metadata (id) on delete cascade
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
create table entities_attachments
(
    entity_id     uuid not null,
    attachment_id uuid not null,
    UNIQUE (entity_id, attachment_id),
    constraint fk_entities_attachments_entities foreign key (entity_id) references entities (id) on delete CASCADE,
    constraint fk_entities_attachments_attachments foreign key (attachment_id) references attachments (id) on delete CASCADE
);


create table audit
(
    id          uuid primary key not null,
    metadata_id uuid             not null,
    action      varchar          not null,
    username    varchar          not null,
    time        timestamptz      not null DEFAULT CURRENT_TIMESTAMP
);
-- sys_id_origin : System to which the message is associated at the time of creation
create table messages
(
    id            uuid primary key not null,
    predecessor   uuid,
    seq_nr        integer          not null,
    subject       varchar,
    content       varchar          not null,
    audience      varchar          not null,
    username      varchar          not null,
    sys_id_origin varchar,
    created_at    timestamptz DEFAULT CURRENT_TIMESTAMP
);
create table messages_entities
(
    message_id uuid not null,
    entity_id  uuid not null,
    constraint fk_message_entity foreign key (entity_id) references entities (id),
    constraint fk_message_message foreign key (message_id) references messages (id)
);
