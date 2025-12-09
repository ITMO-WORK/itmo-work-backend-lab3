create table if not exists roles(
    id bigserial primary key,
    name varchar(50) not null unique
);

insert into roles (name) values
     ('ROLE_USER'),
     ('ROLE_ADMIN'),
     ('ROLE_MANAGER'),
     ('ROLE_EMPLOYEE'),
     ('ROLE_COMPANY_OWNER')
    on conflict (name) do nothing;

create table if not exists users
(
    id uuid primary key default gen_random_uuid(),
    fullname varchar(255) not null,
    password varchar(255) not null,
    email varchar(320) not null unique
);

create table user_roles (
    id bigserial primary key,
    user_id uuid not null references users(id) on delete cascade,
    role_id bigint not null references roles(id) on delete cascade
);