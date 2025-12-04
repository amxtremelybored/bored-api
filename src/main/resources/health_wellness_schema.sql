-- Table: health_wellness_category
CREATE TABLE IF NOT EXISTS health_wellness_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: health_wellness_content
CREATE TABLE IF NOT EXISTS health_wellness_content (
    id BIGSERIAL PRIMARY KEY,
    category_id UUID REFERENCES health_wellness_category(id),
    tip TEXT NOT NULL,
    source VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_health_wellness_view
CREATE TABLE IF NOT EXISTS user_health_wellness_view (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT REFERENCES user_profiles(id),
    health_wellness_content_id BIGINT REFERENCES health_wellness_content(id),
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_liked BOOLEAN,
    UNIQUE(user_profile_id, health_wellness_content_id)
);

-- Insert default categories if not exists
INSERT INTO health_wellness_category (name, description) VALUES
('Makeup', 'Tips and tricks for makeup'),
('Beauty', 'General beauty advice'),
('Fitness', 'Fitness and workout tips')
ON CONFLICT (name) DO NOTHING;
