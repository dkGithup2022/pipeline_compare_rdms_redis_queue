create table coupon
(
    discount_amount      int                              not null,
    issued_quantity      int                              not null,
    min_available_amount int                              not null,
    total_quantity       int                              null,
    date_issue_ended     datetime(6)                      not null,
    date_issue_started   datetime(6)                      not null,
    id                   bigint                           not null
        primary key,
    title                varchar(255)                     not null,
    type                 enum ('FIRST_COME_FIRST_SERVED') not null
);


create table coupon_issue
(
    coupon_id    bigint      not null,
    created_date datetime(6) null,
    date_issued  datetime(6) not null,
    date_used    datetime(6) null,
    id           bigint      not null
        primary key,
    updated_date datetime(6) null,
    user_id      bigint      not null
);


CREATE INDEX idx_user_id ON coupon_issue(user_id);
CREATE INDEX idx_coupon_id ON coupon_issue(coupon_id);
