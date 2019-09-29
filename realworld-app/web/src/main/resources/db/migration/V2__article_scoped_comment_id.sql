CREATE VIEW article_comments_view AS
  SELECT
    *,
    row_number() OVER (PARTITION BY article_id ORDER BY created_at ASC) AS article_scoped_id
  FROM article_comments
;
-- TODO add deleted flag to article_comments table, don't really delete comment rows
--      in order to maintain article_scoped_id
