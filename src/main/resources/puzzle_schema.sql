-- Puzzle Categories
CREATE TABLE IF NOT EXISTS puzzle_category (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    emoji VARCHAR(10),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Puzzle Content
CREATE TABLE IF NOT EXISTS puzzle_content (
    id BIGSERIAL PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES puzzle_category(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    options TEXT, -- Stored as JSON string (List<String>)
    difficulty_level INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User Puzzle Views (Tracking what user has seen/answered)
CREATE TABLE IF NOT EXISTS user_puzzle_view (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    puzzle_content_id BIGINT NOT NULL REFERENCES puzzle_content(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_correct BOOLEAN, -- NULL if just viewed, TRUE/FALSE if answered
    UNIQUE(user_profile_id, puzzle_content_id)
);

CREATE INDEX IF NOT EXISTS idx_puzzle_content_category ON puzzle_content(category_id);
CREATE INDEX IF NOT EXISTS idx_user_puzzle_view_user ON user_puzzle_view(user_profile_id);
CREATE INDEX IF NOT EXISTS idx_user_puzzle_view_puzzle ON user_puzzle_view(puzzle_content_id);

-- Default Category: Generic Puzzles
INSERT INTO puzzle_category (id, name, emoji, description)
VALUES ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Generic Puzzles', '🧩', 'Fun generic puzzles!')
ON CONFLICT (name) DO NOTHING;
