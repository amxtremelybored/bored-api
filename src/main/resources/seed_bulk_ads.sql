-- Seed Bulk Ad Items
-- This script assumes you have at least one Ad in the 'ads' table.
-- It inserts a bulk item pointing to an existing Ad.

INSERT INTO bulk_ad_items (ad_id, sort_order, is_active)
SELECT id, 1, true
FROM ads
LIMIT 1;

-- If you want more items, uncomment and run:
-- INSERT INTO bulk_ad_items (ad_id, sort_order, is_active)
-- SELECT id, 2, true
-- FROM ads
-- ORDER BY created_at DESC
-- LIMIT 1 OFFSET 1;
