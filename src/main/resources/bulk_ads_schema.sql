-- Add last_bulk_ad_shown_time to user_profiles
ALTER TABLE user_profiles ADD COLUMN last_bulk_ad_shown_time TIMESTAMP WITH TIME ZONE;

-- Create bulk_ad_items table
CREATE TABLE bulk_ad_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id UUID NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bulk_ad_items_sort_order ON bulk_ad_items(sort_order);
