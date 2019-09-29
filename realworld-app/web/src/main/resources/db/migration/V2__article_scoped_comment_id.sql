-- don't really delete individual article comments in order to maintain article_scoped_ids
-- it's ok to delete all article's comments when article is deleted
ALTER TABLE article_comments ADD COLUMN deleted BOOLEAN DEFAULT FALSE;

CREATE VIEW article_comments_view AS
  SELECT
    *,
    row_number() OVER (PARTITION BY article_id ORDER BY created_at ASC) AS article_scoped_id
  FROM article_comments
;
