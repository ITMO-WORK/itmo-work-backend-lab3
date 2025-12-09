create table if not exists company_status
(
    id bigserial primary key,
    status varchar(50) not null unique
);

insert into company_status (status) values
        ('PENDING_VERIFICATION'),
        ('APPROVED'),
        ('REJECTED'),
        ('DELETED')
on conflict (status) do nothing;


create table if not exists companies
(
    id uuid primary key default gen_random_uuid(),
    name varchar(320) not null,
    email varchar(320) not null unique,
    description text,
    status_id bigint references company_status(id) not null
);

create table if not exists user_companies
(
    id bigserial primary key,
    user_id uuid not null,
    company_id uuid references companies(id) on delete cascade not null
);