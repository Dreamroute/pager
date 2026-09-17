create table smart_user (
    id bigint primary key,
    name varchar(64)
);

create table smart_addr (
    id bigint primary key,
    user_id bigint,
    name varchar(64)
);
