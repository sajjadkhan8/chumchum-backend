create table dispute_cases (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references orders(id) on delete cascade,
    title varchar(200) not null,
    description text not null,
    status varchar(40) not null default 'OPEN',
    priority varchar(20) not null default 'normal',
    assigned_admin_id uuid references users(id) on delete set null,
    resolution varchar(40) not null default 'NONE',
    resolution_notes text,
    resolved_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table admin_audit_logs (
    id uuid primary key default gen_random_uuid(),
    admin_id uuid not null references users(id) on delete restrict,
    action varchar(80) not null,
    target_type varchar(60) not null,
    target_id varchar(100),
    details text,
    created_at timestamptz not null default now()
);

create index idx_dispute_cases_status_created on dispute_cases(status, created_at desc);
create index idx_dispute_cases_order on dispute_cases(order_id);
create index idx_admin_audit_logs_created on admin_audit_logs(created_at desc);
create index idx_admin_audit_logs_action on admin_audit_logs(action);
