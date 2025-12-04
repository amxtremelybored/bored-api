-- Table: doyouknow_category
CREATE TABLE IF NOT EXISTS doyouknow_category (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Table: doyouknow_content
CREATE TABLE IF NOT EXISTS doyouknow_content (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 category_id UUID REFERENCES doyouknow_category(id),
    fact TEXT NOT NULL,
    source VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Table: user_doyouknow_view
CREATE TABLE IF NOT EXISTS user_doyouknow_view (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   user_profile_id BIGINT REFERENCES user_profiles(id),
    doyouknow_content_id BIGINT REFERENCES doyouknow_content(id),
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            is_liked BOOLEAN,
                            UNIQUE(user_profile_id, doyouknow_content_id)
    );

-- Insert default category if not exists
INSERT INTO doyouknow_category (name, description)
VALUES ('General Facts', 'Interesting general knowledge facts')
    ON CONFLICT (name) DO NOTHING;
