create table public.content_category
(
    id                uuid                     default gen_random_uuid() not null
        primary key,
    name              varchar(255)                                       not null
        unique,
    emoji             varchar(16),
    is_content_loaded boolean                  default false             not null,
    content_loaded_at timestamp with time zone,
    created_at        timestamp with time zone default now()             not null,
    updated_at        timestamp with time zone default now()             not null
);

alter table public.content_category
    owner to postgres;



create table public.topic_contents
(
    id            serial
        primary key,
    topic_id      integer                  not null
        references public.topics
            on delete cascade,
    content_index integer                  not null,
    content       text                     not null,
    created_at    timestamp with time zone not null,
    constraint uq_topic_content_index
        unique (topic_id, content_index)
);

alter table public.topic_contents
    owner to postgres;

create index ix_topic_contents_topic_id
    on public.topic_contents (topic_id);

create index ix_topic_contents_id
    on public.topic_contents (id);


create table public.topics
(
    id                serial
        primary key,
    name              varchar(512)             not null,
    emoji             varchar(16),
    is_content_loaded boolean                  not null,
    content_loaded_at timestamp with time zone,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null,
    category_id       uuid
);

alter table public.topics
    owner to postgres;

create index ix_topics_id
    on public.topics (id);

create unique index ix_topics_name
    on public.topics (name);


create table public.user_content_views
(
    id               bigserial
        primary key,
    user_profile_id  bigint                                 not null
        constraint fk_ucv_user_profile
            references public.user_profiles
            on delete cascade,
    topic_id         bigint                                 not null
        constraint fk_ucv_topic
            references public.topics
            on delete cascade,
    topic_content_id bigint                                 not null
        constraint fk_ucv_topic_content
            references public.topic_contents
            on delete cascade,
    viewed_at        timestamp with time zone default now() not null,
    constraint uq_ucv_user_content
        unique (user_profile_id, topic_content_id)
);

alter table public.user_content_views
    owner to postgres;

create table public.user_preferences
(
    id              bigserial
        primary key,
    user_profile_id bigint                                 not null
        constraint fk_user_preferences_user_profile
            references public.user_profiles
            on delete cascade,
    category_id     uuid                                   not null
        constraint fk_user_preferences_category
            references public.content_category,
    created_at      timestamp with time zone default now() not null
);

alter table public.user_preferences
    owner to postgres;

create table public.user_profiles
(
    id           bigint                   default nextval('user_profiles_id_seq1'::regclass) not null
        primary key,
    uid          varchar(128)                                                                not null
        unique,
    firebase_uid varchar(128),
    display_name varchar(255),
    age          integer,
    phone        varchar(32),
    email        varchar(255),
    photo_url    varchar(1024),
    status       varchar(32)              default 'ACTIVE'::character varying                not null,
    created_at   timestamp with time zone default now()                                      not null,
    updated_at   timestamp with time zone default now()                                      not null
);

alter table public.user_profiles
    owner to postgres;


create table public.super_category
(
    id         uuid                     default gen_random_uuid() not null
        primary key,
    name       varchar(255)                                       not null
        unique,
    emoji      varchar(16),
    created_at timestamp with time zone default now()             not null,
    updated_at timestamp with time zone default now()             not null
);

alter table public.super_category
    owner to postgres;

alter table public.content_category
    add column parent_id uuid
        references public.super_category;
