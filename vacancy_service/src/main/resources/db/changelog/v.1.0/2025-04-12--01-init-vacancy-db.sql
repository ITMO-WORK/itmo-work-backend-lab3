create table if not exists currencies(
    id bigserial primary key,
    currency varchar(3) not null unique
);

insert into currencies (currency) values
    ('RUB'),
    ('USD'),
    ('EUR')
on conflict (currency) do nothing;

create table if not exists vacancy_status(
    id bigserial primary key,
    status varchar(50) not null unique
);

insert into vacancy_status (status) values
    ('PUBLISHED'),
    ('DRAFT'),
    ('CLOSED')
on conflict (status) do nothing;

create table if not exists vacancies(
    id uuid primary key default gen_random_uuid(),
    company_id uuid not null,
    title varchar(255) not null,
    description text,
    salary_from numeric(12, 0) check (salary_from >= 0),
    salary_to numeric(12, 0) check (salary_to >= 0),
    currency_id bigint not null references currencies(id),
    created_at timestamp not null default NOW(),
    status_id bigint not null references vacancy_status(id),
    check (salary_from is NULL or salary_to is NULL or salary_from <= salary_to)
);
