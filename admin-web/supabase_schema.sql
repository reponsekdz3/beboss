-- BeBoss Enterprise Licensing, Single Master Admin Auth & Synchronous Branch DataShare Schema (Supabase PostgreSQL)
-- Usage:
--   * Admin Management (Shops, All Shop Users, Payments, Unlock Codes, Branch Snapshots, Audit Logs)
--     uses Supabase Auth + PostgreSQL RLS locked strictly to the single master admin (`reponsekdz@gmail.com`).
--   * Cross-Branch Internet Auto-Sync uses encrypted/signed `branch_sync_snapshots` while all POS,
--     Inventory, Sales, Customers, Purchases, and Debts execute 100% offline-first in Room SQLite.
--
-- Run this entire script once in your Supabase SQL Editor.

-- 1. Single Master Admin Registry (Linked strictly to reponsekdz@gmail.com in auth.users)
create table if not exists public.admin_profiles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    email text not null unique,
    full_name text not null default 'BeBoss Master Admin',
    role text not null default 'SUPER_ADMIN',
    is_active boolean not null default true,
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create or replace function public.is_beboss_admin()
returns boolean
language sql
security definer
stable
as $$
  select exists (
    select 1 from public.admin_profiles
    where user_id = auth.uid()
      and lower(email) = 'reponsekdz@gmail.com'
      and is_active = true
  );
$$;

-- 2. Registered Shop Accounts & Monthly Subscription Tracking
create table if not exists public.shop_accounts (
    id text primary key,
    shop_name text not null,
    owner_name text not null default '',
    owner_username text not null,
    owner_phone text not null,
    device_challenge text not null default '',
    branch_count integer not null default 1,
    worker_count integer not null default 1,
    monthly_fee_rwf integer not null default 5000,
    subscription_status text not null default 'ACTIVE',
    subscription_expires_at bigint not null default 0,
    updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

-- 3. Shop Users Directory (Owners, Managers & Cashiers across all shops — NO PIN/password hashes stored!)
create table if not exists public.shop_users (
    id text primary key,
    shop_sync_key text not null,
    shop_name text not null default '',
    user_id text not null,
    full_name text not null default '',
    username text not null,
    phone text not null default '',
    role text not null default 'CASHIER',
    assigned_branch_id text not null default '',
    assigned_branch_name text not null default 'All Branches',
    is_active boolean not null default true,
    updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

-- 4. USSD & WhatsApp Payment Verification Claims
create table if not exists public.payment_requests (
    id text primary key,
    shop_name text not null,
    owner_username text not null,
    owner_phone text not null,
    device_challenge text not null default '',
    provider text not null default 'MTN MoMo',
    ussd_code text not null default '',
    payer_phone text not null default '',
    amount_rwf integer not null default 5000,
    duration_months integer not null default 1,
    branch_count integer not null default 1,
    worker_count integer not null default 1,
    telco_reference text not null default '',
    status text not null default 'PENDING_VERIFICATION',
    generated_unlock_code text not null default '',
    verified_by_admin text not null default '',
    rejection_reason text not null default '',
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
    updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

alter table public.payment_requests
    add column if not exists rejection_reason text not null default '';

-- 5. Cryptographic Account-Bound Unlock Codes Ledger (BB2-...)
create table if not exists public.unlock_codes (
    id text primary key,
    unlock_code text not null unique,
    shop_username text not null,
    shop_phone text not null,
    device_challenge text not null default '',
    tier_code text not null default 'SINGLE',
    plan_days integer not null default 30,
    amount_rwf integer not null default 5000,
    status text not null default 'ISSUED',
    issued_by_admin text not null default 'BeBoss Master Admin',
    issued_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
    redeemed_at bigint
);

-- 6. Synchronous Multi-Branch Auto-Sync Snapshots (Relays compressed BBSHARE1 payloads across devices)
create table if not exists public.branch_sync_snapshots (
    id text primary key,
    shop_sync_key text not null,
    device_id text not null,
    branch_id text not null default 'main_branch',
    branch_name text not null default 'Main Store',
    shop_name text not null default '',
    owner_username text not null default '',
    payload_token text not null,
    records_summary text not null default '',
    updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

-- 7. Admin Audit Trail
create table if not exists public.admin_audit_logs (
    id text primary key,
    admin_email text not null,
    action text not null,
    target_id text not null default '',
    details text not null default '',
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create index if not exists idx_shop_accounts_username on public.shop_accounts(owner_username);
create index if not exists idx_shop_users_shop_key on public.shop_users(shop_sync_key);
create index if not exists idx_shop_users_username on public.shop_users(username);
create index if not exists idx_shop_users_updated on public.shop_users(updated_at desc);
create index if not exists idx_payment_requests_status on public.payment_requests(status);
create index if not exists idx_payment_requests_username on public.payment_requests(owner_username);
create index if not exists idx_unlock_codes_code on public.unlock_codes(unlock_code);
create index if not exists idx_unlock_codes_username on public.unlock_codes(shop_username);
create index if not exists idx_branch_sync_shop_key on public.branch_sync_snapshots(shop_sync_key);
create index if not exists idx_branch_sync_updated on public.branch_sync_snapshots(updated_at desc);
create index if not exists idx_admin_audit_created on public.admin_audit_logs(created_at desc);

alter table public.admin_profiles enable row level security;
alter table public.shop_accounts enable row level security;
alter table public.shop_users enable row level security;
alter table public.payment_requests enable row level security;
alter table public.unlock_codes enable row level security;
alter table public.branch_sync_snapshots enable row level security;
alter table public.admin_audit_logs enable row level security;

-- Single Master Admin Policies (strictly reponsekdz@gmail.com):
drop policy if exists "Read own or admin profiles" on public.admin_profiles;
create policy "Read own or admin profiles" on public.admin_profiles
    for select using (
        auth.uid() = user_id and lower(email) = 'reponsekdz@gmail.com'
    );

drop policy if exists "Bootstrap or admin insert profile" on public.admin_profiles;
create policy "Bootstrap or admin insert profile" on public.admin_profiles
    for insert with check (
        auth.uid() = user_id
        and lower(email) = 'reponsekdz@gmail.com'
    );

drop policy if exists "Admin update profiles" on public.admin_profiles;
create policy "Admin update profiles" on public.admin_profiles
    for update using (public.is_beboss_admin()) with check (public.is_beboss_admin());

-- Helper reading the caller's X-Shop-Sync-Key request header for multi-tenant RLS isolation:
create or replace function public.current_shop_sync_key()
returns text
language sql
stable
as $$
  select coalesce(
    nullif(trim((current_setting('request.headers', true)::json ->> 'x-shop-sync-key')), ''),
    ''
  );
$$;

create or replace function public.computed_shop_sync_key(p_username text, p_phone text)
returns text
language sql
immutable
as $$
  select lower(coalesce(nullif(trim(p_username), ''), 'owner'))
    || '_'
    || coalesce(nullif(right(regexp_replace(coalesce(p_phone, ''), '[^0-9]', '', 'g'), 9), ''), '000000000');
$$;

-- Shop Accounts Policies (Tenant-scoped via X-Shop-Sync-Key; Master Admin has full management & delete)
drop policy if exists "Shops and admins manage shop_accounts" on public.shop_accounts;
drop policy if exists "Shop terminals read shop_accounts" on public.shop_accounts;
create policy "Shop terminals read shop_accounts" on public.shop_accounts
    for select using (
        public.is_beboss_admin()
        or id = public.current_shop_sync_key()
    );
drop policy if exists "Shop terminals upsert shop_accounts" on public.shop_accounts;
create policy "Shop terminals upsert shop_accounts" on public.shop_accounts
    for insert with check (
        public.is_beboss_admin()
        or (id = public.current_shop_sync_key() and id <> '')
    );
drop policy if exists "Shop terminals or admin update shop_accounts" on public.shop_accounts;
create policy "Shop terminals or admin update shop_accounts" on public.shop_accounts
    for update using (
        public.is_beboss_admin()
        or id = public.current_shop_sync_key()
    ) with check (
        public.is_beboss_admin()
        or id = public.current_shop_sync_key()
    );
drop policy if exists "Master admin delete shop_accounts" on public.shop_accounts;
create policy "Master admin delete shop_accounts" on public.shop_accounts
    for delete using (public.is_beboss_admin());

-- Shop Users Directory Policies (Tenant-scoped via X-Shop-Sync-Key; Master Admin controls roles, status & deletion)
drop policy if exists "Read shop_users" on public.shop_users;
create policy "Read shop_users" on public.shop_users
    for select using (
        public.is_beboss_admin()
        or shop_sync_key = public.current_shop_sync_key()
    );
drop policy if exists "Upsert shop_users" on public.shop_users;
create policy "Upsert shop_users" on public.shop_users
    for insert with check (
        public.is_beboss_admin()
        or (shop_sync_key = public.current_shop_sync_key() and shop_sync_key <> '')
    );
drop policy if exists "Update shop_users" on public.shop_users;
create policy "Update shop_users" on public.shop_users
    for update using (
        public.is_beboss_admin()
        or shop_sync_key = public.current_shop_sync_key()
    ) with check (
        public.is_beboss_admin()
        or shop_sync_key = public.current_shop_sync_key()
    );
drop policy if exists "Master admin delete shop_users" on public.shop_users;
create policy "Master admin delete shop_users" on public.shop_users
    for delete using (public.is_beboss_admin());

-- Payment Requests Policies (Tenant-scoped by owner_username + owner_phone matching X-Shop-Sync-Key)
drop policy if exists "Shop terminals submit and read payment_requests" on public.payment_requests;
create policy "Shop terminals submit and read payment_requests" on public.payment_requests
    for select using (
        public.is_beboss_admin()
        or public.computed_shop_sync_key(owner_username, owner_phone) = public.current_shop_sync_key()
    );

drop policy if exists "Shop terminals insert payment_requests" on public.payment_requests;
create policy "Shop terminals insert payment_requests" on public.payment_requests
    for insert with check (
        public.is_beboss_admin()
        or (
            public.computed_shop_sync_key(owner_username, owner_phone) = public.current_shop_sync_key()
            and public.current_shop_sync_key() <> ''
            and status = 'PENDING_VERIFICATION'
        )
    );

drop policy if exists "Shop redeem or admin update payment_requests" on public.payment_requests;
create policy "Shop redeem or admin update payment_requests" on public.payment_requests
    for update using (
        public.is_beboss_admin()
        or public.computed_shop_sync_key(owner_username, owner_phone) = public.current_shop_sync_key()
    )
    with check (
        public.is_beboss_admin()
        or (
            status = 'REDEEMED'
            and public.computed_shop_sync_key(owner_username, owner_phone) = public.current_shop_sync_key()
        )
    );

drop policy if exists "Master admin delete payment_requests" on public.payment_requests;
create policy "Master admin delete payment_requests" on public.payment_requests
    for delete using (public.is_beboss_admin());

-- Unlock Codes Policies (Only authenticated Master Admin issues/revokes/deletes codes; owning shop reads & marks REDEEMED)
drop policy if exists "Read unlock_codes" on public.unlock_codes;
create policy "Read unlock_codes" on public.unlock_codes
    for select using (
        public.is_beboss_admin()
        or public.computed_shop_sync_key(shop_username, shop_phone) = public.current_shop_sync_key()
    );

drop policy if exists "Admin insert unlock_codes" on public.unlock_codes;
create policy "Admin insert unlock_codes" on public.unlock_codes
    for insert with check (public.is_beboss_admin());

drop policy if exists "Shop redeem or admin update unlock_codes" on public.unlock_codes;
create policy "Shop redeem or admin update unlock_codes" on public.unlock_codes
    for update using (
        public.is_beboss_admin()
        or public.computed_shop_sync_key(shop_username, shop_phone) = public.current_shop_sync_key()
    )
    with check (
        public.is_beboss_admin()
        or (
            status = 'REDEEMED'
            and public.computed_shop_sync_key(shop_username, shop_phone) = public.current_shop_sync_key()
        )
    );

drop policy if exists "Master admin delete unlock_codes" on public.unlock_codes;
create policy "Master admin delete unlock_codes" on public.unlock_codes
    for delete using (public.is_beboss_admin());

-- Branch Sync Snapshots Policies (Strictly isolated by shop_sync_key header or Master Admin)
drop policy if exists "Branch devices relay snapshots" on public.branch_sync_snapshots;
create policy "Branch devices relay snapshots" on public.branch_sync_snapshots
    for select using (
        public.is_beboss_admin()
        or shop_sync_key = public.current_shop_sync_key()
    );
drop policy if exists "Branch devices insert snapshots" on public.branch_sync_snapshots;
create policy "Branch devices insert snapshots" on public.branch_sync_snapshots
    for insert with check (
        public.is_beboss_admin()
        or (shop_sync_key = public.current_shop_sync_key() and shop_sync_key <> '')
    );
drop policy if exists "Branch devices update snapshots" on public.branch_sync_snapshots;
create policy "Branch devices update snapshots" on public.branch_sync_snapshots
    for update using (
        public.is_beboss_admin()
        or shop_sync_key = public.current_shop_sync_key()
    ) with check (
        public.is_beboss_admin()
        or shop_sync_key = public.current_shop_sync_key()
    );
drop policy if exists "Master admin delete snapshots" on public.branch_sync_snapshots;
create policy "Master admin delete snapshots" on public.branch_sync_snapshots
    for delete using (public.is_beboss_admin());

-- Admin Audit Logs Policies (Strict authenticated Master Admin access only)
drop policy if exists "Admin read audit_logs" on public.admin_audit_logs;
create policy "Admin read audit_logs" on public.admin_audit_logs
    for select using (public.is_beboss_admin());

drop policy if exists "Admin insert audit_logs" on public.admin_audit_logs;
create policy "Admin insert audit_logs" on public.admin_audit_logs
    for insert with check (public.is_beboss_admin());
