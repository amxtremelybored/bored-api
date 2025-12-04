-- Table: fun_category
CREATE TABLE IF NOT EXISTS fun_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: fun_content
CREATE TABLE IF NOT EXISTS fun_content (
    id BIGSERIAL PRIMARY KEY,
    category_id UUID REFERENCES fun_category(id),
    content TEXT NOT NULL,
    source VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_fun_view
CREATE TABLE IF NOT EXISTS user_fun_view (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT REFERENCES user_profiles(id),
    fun_content_id BIGINT REFERENCES fun_content(id),
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_liked BOOLEAN,
    UNIQUE(user_profile_id, fun_content_id)
);

-- Insert default category if not exists
INSERT INTO fun_category (name, description)
VALUES ('Anecdotes', 'Fun anecdotes and gags')
ON CONFLICT (name) DO NOTHING;
