CREATE TYPE user_role as ENUM ('ADMIN', 'USER');

CREATE TABLE IF NOT EXISTS Users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role user_role NOT NULL DEFAULT 'USER',

    CONSTRAINT ck_username_blank CHECK (btrim(username)<> ''),
    CONSTRAINT ck_full_name_blank CHECK (btrim(full_name)<> '')
);

CREATE TABLE IF NOT EXISTS Tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,

    CONSTRAINT ck_name_blank CHECK (btrim(name)<> '')
);

CREATE TABLE IF NOT EXISTS Images (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES Users(id) ON DELETE CASCADE,
    description VARCHAR(120) NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_description_blank CHECK (btrim(description)<> '')
);

CREATE TABLE IF NOT EXISTS Image_Tag (
    image_id BIGINT NOT NULL REFERENCES Images(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES Tag(id) ON DELETE CASCADE,
    PRIMARY KEY (image_id, tag_id)
);