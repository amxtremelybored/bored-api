CREATE TABLE IF NOT EXISTS app_config (
    config_key VARCHAR(255) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS notification_content (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    viewed BOOLEAN DEFAULT FALSE
);

-- Seed some initial config
INSERT INTO app_config (config_key, config_value) VALUES ('NOTI_TIMES', '09:00,18:00') ON CONFLICT DO NOTHING;

-- Seed some sample notifications
INSERT INTO notification_content (title, body) VALUES 
('Take a break!', 'Remember to stretch and drink some water.'),
('Did you know?', 'Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still perfectly edible.'),
('Quote of the day', 'The only way to do great work is to love what you do. - Steve Jobs')
ON CONFLICT DO NOTHING;
