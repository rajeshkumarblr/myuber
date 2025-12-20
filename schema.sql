-- Teams
CREATE TABLE teams (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE
);

-- Players
CREATE TABLE players (
    id SERIAL PRIMARY KEY,
    team_id INT REFERENCES teams(id),
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL -- Batsman, Bowler, All-Rounder, Wicket-Keeper
);

-- Matches
CREATE TABLE matches (
    id SERIAL PRIMARY KEY,
    team1_id INT REFERENCES teams(id),
    team2_id INT REFERENCES teams(id),
    match_date TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'SCHEDULED'
);

-- User Fantasy Teams
CREATE TABLE user_teams (
    id SERIAL PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    match_id INT REFERENCES matches(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Players in User Fantasy Teams
CREATE TABLE user_team_players (
    id SERIAL PRIMARY KEY,
    user_team_id INT REFERENCES user_teams(id),
    player_id INT REFERENCES players(id)
);

-- Match Stats (for calculating points)
CREATE TABLE player_match_stats (
    id SERIAL PRIMARY KEY,
    match_id INT REFERENCES matches(id),
    player_id INT REFERENCES players(id),
    runs INT DEFAULT 0,
    wickets INT DEFAULT 0,
    catches INT DEFAULT 0,
    points INT DEFAULT 0
);
