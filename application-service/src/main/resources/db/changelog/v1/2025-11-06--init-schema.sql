create table if not exists application_status(
    id bigserial primary key,
    status varchar(50) not null unique
    );

insert into application_status (status) values
    ('NEW'),
    ('VIEWED'),
    ('REJECTED'),
    ('ACCEPTED')
    on conflict (status) do nothing;

create table if not exists applications(
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    vacancy_id uuid not null,
    cover_letter text,
    status_id bigint not null references application_status(id),
    created_at timestamp not null default NOW(),
    updated_at timestamp not null default NOW()
);