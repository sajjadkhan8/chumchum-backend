-- Enforce one account row per creator/platform.
-- If older data contains duplicates, keep the most recently updated row.
delete from core.social_accounts sa
using (
    select id
    from (
        select
            id,
            row_number() over (
                partition by creator_id, lower(platform)
                order by updated_at desc nulls last, created_at desc nulls last, id desc
            ) as duplicate_rank
        from core.social_accounts
    ) ranked
    where duplicate_rank > 1
) duplicates
where sa.id = duplicates.id;

update core.social_accounts
set platform = lower(platform)
where platform <> lower(platform);

create unique index if not exists uk_social_accounts_creator_platform
    on core.social_accounts(creator_id, lower(platform));
