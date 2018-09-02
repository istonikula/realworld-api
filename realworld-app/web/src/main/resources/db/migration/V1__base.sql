CREATE TABLE users (
  id UUID NOT NULL,
  email TEXT NOT NULL,
  token TEXT NOT NULL,
  username TEXT NOT NULL,
  password TEXT NOT NULL,
  bio TEXT,
  image TEXT,

  CONSTRAINT pk$users PRIMARY KEY (id),
  CONSTRAINT unq$email UNIQUE (email),
  CONSTRAINT unq$username UNIQUE (username)
);

CREATE TABLE follows (
  followee UUID NOT NULL,
  follower UUID NOT NULL,

  CONSTRAINT pk$follows PRIMARY KEY (follower, followee),
  CONSTRAINT fk$follower FOREIGN KEY (follower) REFERENCES users ON DELETE CASCADE,
  CONSTRAINT fk$followee FOREIGN KEY (followee) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE articles (
  id UUID NOT NULL,
  slug TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  body TEXT NOT NULL,
  author UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT pk$articles PRIMARY KEY (id),
  CONSTRAINT unq$slug UNIQUE (slug),
  CONSTRAINT fk$author FOREIGN KEY (author) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE tags (
  name TEXT NOT NULL,

  CONSTRAINT pk$tags PRIMARY KEY (name)
);

CREATE TABLE article_tags (
  article_id UUID NOT NULL,
  tag TEXT NOT NULL,

  CONSTRAINT pk$acticle_tags PRIMARY KEY (article_id, tag),
  CONSTRAINT fk$article_id FOREIGN KEY (article_id) REFERENCES articles ON DELETE CASCADE,
  CONSTRAINT fk$tag FOREIGN KEY (tag) REFERENCES tags ON DELETE CASCADE
);

CREATE TABLE article_favorites (
  article_id UUID NOT NULL,
  user_id UUID NOT NULL,

  CONSTRAINT pk$article_favorites PRIMARY KEY (article_id, user_id),
  CONSTRAINT fk$article_id FOREIGN KEY (article_id) REFERENCES articles ON DELETE CASCADE,
  CONSTRAINT fk$user_id FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

-- NOTE: for acticle scoped comment ids, separate counter table having row for each article would work.
-- Details:
-- https://dba.stackexchange.com/questions/93296/how-can-i-implement-a-sequence-for-each-foreign-key-value
CREATE TABLE article_comments (
  id BIGSERIAL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  body TEXT NOT NULL,
  author UUID NOT NULL,
  article_id UUID NOT NULL,

  CONSTRAINT pk$article_comments PRIMARY KEY (id),
  CONSTRAINT fk$article_id FOREIGN KEY (article_id) REFERENCES articles ON DELETE CASCADE
);
