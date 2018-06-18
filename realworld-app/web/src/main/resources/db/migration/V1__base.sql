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
