-- Quiz Category Table
CREATE TABLE IF NOT EXISTS quiz_category (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    emoji VARCHAR(16),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Quiz Content Table
CREATE TABLE IF NOT EXISTS quiz_content (
    id BIGSERIAL PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES quiz_category(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    options TEXT, -- Stored as JSON string
    difficulty_level INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User Quiz View Table (Tracking which quizzes a user has seen)
CREATE TABLE IF NOT EXISTS user_quiz_view (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    quiz_content_id BIGINT NOT NULL REFERENCES quiz_content(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_correct BOOLEAN, -- Optional: did they answer correctly?
    UNIQUE(user_profile_id, quiz_content_id) -- Prevent duplicate views
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_quiz_content_category ON quiz_content(category_id);
CREATE INDEX IF NOT EXISTS idx_user_quiz_view_user ON user_quiz_view(user_profile_id);
CREATE INDEX IF NOT EXISTS idx_user_quiz_view_quiz ON user_quiz_view(quiz_content_id);

-- Default Category: General Knowledge
INSERT INTO quiz_category (id, name, emoji, description)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'General Knowledge', '🧠', 'Test your general knowledge!')
ON CONFLICT (name) DO NOTHING;
