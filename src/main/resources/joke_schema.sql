-- Table: joke_category
CREATE TABLE IF NOT EXISTS joke_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: joke_content
CREATE TABLE IF NOT EXISTS joke_content (
    id BIGSERIAL PRIMARY KEY,
    category_id UUID REFERENCES joke_category(id),
    setup TEXT NOT NULL,
    punchline TEXT NOT NULL,
    source VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_joke_view
CREATE TABLE IF NOT EXISTS user_joke_view (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT REFERENCES user_profiles(id),
    joke_content_id BIGINT REFERENCES joke_content(id),
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_liked BOOLEAN,
    UNIQUE(user_profile_id, joke_content_id)
);

-- Insert default category if not exists
INSERT INTO joke_category (name, description)
VALUES ('Generic Jokes', 'General purpose jokes for everyone')
ON CONFLICT (name) DO NOTHING;
