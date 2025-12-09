alter table vacancies
    alter column salary_from TYPE integer using salary_from::integer,
    alter column salary_to TYPE integer using salary_to::integer;