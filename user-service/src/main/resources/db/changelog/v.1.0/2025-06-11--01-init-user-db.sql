create table if not exists users
(
    id uuid primary key default gen_random_uuid(),
    fullname varchar(255) not null,
    password varchar(255) not null,
    email varchar(320) not null unique
)