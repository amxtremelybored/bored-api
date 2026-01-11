-- FIX SCRIPT: Setup 24h Bulk Ads & Reset User
-- Run this to configure the two ads for 24h display and reset your cooldown.

-- 1. Reset Cooldown for User 13 (so you can test immediately)
UPDATE user_profiles SET last_bulk_ad_shown_time = NULL WHERE id = 13;

-- 2. Clear existing slot mappings for these 2 ads (clean slate)
DELETE FROM ad_slot_mappings 
WHERE ad_id IN ('c058d243-a6e0-45be-bf75-457efcf1cab3', 'c058d243-a6e0-45be-bf75-457efcf1cab1');

-- 3. Map both ads to Slot 3 (24 Hours / 00:00 - 23:59)
INSERT INTO ad_slot_mappings (ad_id, slot_id) VALUES 
('c058d243-a6e0-45be-bf75-457efcf1cab3', 3),
('c058d243-a6e0-45be-bf75-457efcf1cab1', 3);

-- 4. Ensure they are in the Bulk Items list (Clean & Re-insert)
DELETE FROM bulk_ad_items 
WHERE ad_id IN ('c058d243-a6e0-45be-bf75-457efcf1cab3', 'c058d243-a6e0-45be-bf75-457efcf1cab1');

INSERT INTO bulk_ad_items (ad_id, sort_order, is_active) VALUES 
('c058d243-a6e0-45be-bf75-457efcf1cab3', 1, true),  -- Summer Sale (1st)
('c058d243-a6e0-45be-bf75-457efcf1cab1', 2, true);  -- Winter Sale (2nd)
