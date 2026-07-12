create table customer (
    id          uuid primary key default gen_random_uuid(),
    email       varchar(255) not null,
    full_name   varchar(255) not null,
    created_at  timestamptz  not null default now(),
    constraint uq_customer_email unique (email)
);

create table category (
    id        uuid primary key default gen_random_uuid(),
    parent_id uuid references category (id),
    name      varchar(100) not null,
    constraint uq_category_parent_name unique (parent_id, name)
);

create index idx_category_parent on category (parent_id);

create table product (
    id          uuid primary key default gen_random_uuid(),
    category_id uuid not null references category (id),
    sku         varchar(32)   not null,
    name        varchar(255)  not null,
    price       numeric(12,2) not null,
    stock_qty   integer       not null default 0,
    version     integer       not null default 0,
    constraint uq_product_sku unique (sku),
    constraint ck_product_price_nonneg check (price >= 0),
    constraint ck_product_stock_nonneg check (stock_qty >= 0)
);

create index idx_product_category on product (category_id);

create table orders (
    id          uuid primary key default gen_random_uuid(),
    customer_id uuid not null references customer (id),
    status      varchar(20) not null,
    placed_at   timestamptz not null default now(),
    constraint ck_orders_status check (status in ('NEW', 'PAID', 'SHIPPED', 'CANCELLED'))
);

create index idx_orders_customer on orders (customer_id);

create index idx_orders_status_placed_at on orders (status, placed_at desc);

create table order_line (
    id         uuid primary key default gen_random_uuid(),
    order_id   uuid not null references orders (id),
    product_id uuid not null references product (id),
    qty        integer       not null,
    unit_price numeric(12,2) not null,
    constraint uq_order_line_order_product unique (order_id, product_id),
    constraint ck_order_line_qty_positive check (qty > 0),
    constraint ck_order_line_price_nonneg check (unit_price >= 0)
);

create index idx_order_line_order   on order_line (order_id);
create index idx_order_line_product on order_line (product_id);

create table review (
    id          uuid primary key default gen_random_uuid(),
    product_id  uuid not null references product (id),
    customer_id uuid not null references customer (id),
    rating      smallint not null,
    body        text,
    created_at  timestamptz not null default now(),
    constraint uq_review_product_customer unique (product_id, customer_id),
    constraint ck_review_rating_range check (rating between 1 and 5)
);

create index idx_review_product  on review (product_id);
create index idx_review_customer on review (customer_id);

create table tag (
    id   uuid primary key default gen_random_uuid(),
    name varchar(50) not null,
    constraint uq_tag_name unique (name)
);

create table product_tag (
    product_id uuid not null references product (id),
    tag_id     uuid not null references tag (id),
    primary key (product_id, tag_id)
);

create index idx_product_tag_tag on product_tag (tag_id);
