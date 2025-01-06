-- SQL для создания таблицы users
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       role VARCHAR(50),
                       CONSTRAINT username_unique UNIQUE (username)
);

-- SQL для создания таблицы files
CREATE TABLE files (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       path VARCHAR(1024) NOT NULL,
                       size BIGINT NOT NULL,
                       user_id BIGINT NOT NULL,
                       uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                       CONSTRAINT unique_file_path UNIQUE (path, name)
);