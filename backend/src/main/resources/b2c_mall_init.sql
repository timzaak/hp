drop table if exists product;
create table if not exists product(
    id serial primary key,
    name text not null,
    snap_id int not null,
    stock int not null default 0,
    price numeric(10,2) not null,
    is_on boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table product is '商品';
comment on column product.name is '商品名称';
comment on column product.stock is '库存';
comment on column product.is_on is 'true:上架, false:下架';

drop table if exists product_snapshot;
create table if not exists product_snapshot(
    id serial primary key,
    product_id integer not null,
    data jsonb not null,
    created_at timestamptz not null default now()
);

create index if not exists product_snapshot_product_id_index on product_snapshot(product_id);

comment on table product_snapshot is '商品快照';
comment on column product_snapshot.data is '商品快照内容';


drop table if exists user_coupon;
create table if not exists user_coupon(
    id bigserial primary key,
    user_id int not null,
    -- type smallint not null, --
    rule jsonb not null,
    status smallint not null default 0,
    ref_id text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table user_coupon is '用户优惠券';
comment on column user_coupon.rule is '优惠优惠规则';
comment on column user_coupon.status is '状态，0：未使用，1：已使用';
comment on column user_coupon.ref_id is '使用源';
create index user_coupon_ref_index on user_coupon(ref_id) where ref_id is not null;

drop table if exists bonus;
create table if not exists bonus(
    user_id int not null,
    amount int not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table bonus is '积分';
create unique index bonus_user_id_index on bonus(user_id);

drop table if exists order_1;
create table if not exists order_1(
    id  bigserial primary key,
    user_id int not null,
    info jsonb not null,
    total_amount numeric(10,2) not null,
    status smallint not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table order_1 is '订单：纯数据库事务实现下单';
comment on column order_1.info is '所有关于计算总价相关的信息';

--- create product for test
create or replace procedure create_product(IN name text, IN price numeric(10,2))
    language plpgsql
as $$
    declare
        product_id int;
        product_snap_id int;
    begin
        product_id := nextval('product_id_seq');
        product_snap_id := nextval('product_snapshot_id_seq');
        insert into product(id, name, snap_id, price, is_on) values (product_id, name, product_snap_id, price, true);
        insert into product_snapshot(id, product_id, data) values (product_snap_id, product_id, jsonb_build_object('name', name, 'price', price));
    end;
$$;


call create_product('test_product_1', 1.11);
call create_product('test_product_2', 2.22);
call create_product('test_product_3', 3.33);
call create_product('test_product_4', 4.44);
call create_product('test_product_5', 5.55);
call create_product('test_product_6', 6.66);
call create_product('test_product_7', 7.77);
call create_product('test_product_8', 8.88);
call create_product('test_product_9', 9.99);
call create_product('test_product_10', 10.00);

drop table if exists tcc_fence_log;
-- 这是 用来做 Seata TCC 幂等
CREATE TABLE IF NOT EXISTS public.tcc_fence_log
(
    xid              VARCHAR(128)  NOT NULL,
    branch_id        BIGINT        NOT NULL,
    action_name      VARCHAR(64)   NOT NULL,
    status           SMALLINT      NOT NULL,
    gmt_create       TIMESTAMP(3)  NOT NULL,
    gmt_modified     TIMESTAMP(3)  NOT NULL,
    CONSTRAINT pk_tcc_fence_log PRIMARY KEY (xid, branch_id)
);
CREATE INDEX idx_gmt_modified ON public.tcc_fence_log (gmt_modified);
CREATE INDEX idx_status ON public.tcc_fence_log (status);
