-- マスタ（パート・ジャンル）の入れ替えを既存DBへ反映する。新しく作るDBには
-- MasterDataInitializer が最初から新しい一覧を入れるので、このスクリプトは不要。
--
-- MasterDataInitializer は不足分を足すだけで、消しも直しもしない（利用者が追加した選択肢を
-- 勝手に消さないための設計で、MasterDataInitializerTest が固定している）。そのため
-- 統合と削除はここで行う。何度実行しても同じ結果になる。
--
-- 実行前に scripts/dev/seed-demo-posts.sql を流し、デモ投稿を残る選択肢へ振り直しておくこと。
-- 廃止する選択肢への参照が1件でも残っていれば、何も消さずに中止する。

BEGIN;

-- 統合。アプリを一度起動した後だと新しい名前が既に別行として入っているため、単なる改名では
-- 名前の一意制約に当たる。参照を新しい行へ付け替えてから古い行を消す。
DO $$
DECLARE m record;
BEGIN
  FOR m IN SELECT * FROM (VALUES
      ('ロック', '邦ロック'),
      ('パンク', 'パンク／メロコア'),
      ('アニソン／ゲーム音楽', 'アニソン')
    ) AS t(old_name, new_name)
  LOOP
    CONTINUE WHEN NOT EXISTS (SELECT 1 FROM genres WHERE name = m.old_name);

    IF NOT EXISTS (SELECT 1 FROM genres WHERE name = m.new_name) THEN
      UPDATE genres SET name = m.new_name WHERE name = m.old_name;   -- 新しい行がまだ無い場合
    ELSE
      INSERT INTO post_genres (post_id, genre_id)
        SELECT pg.post_id, (SELECT id FROM genres WHERE name = m.new_name)
          FROM post_genres pg JOIN genres g ON g.id = pg.genre_id
         WHERE g.name = m.old_name
        ON CONFLICT DO NOTHING;
      INSERT INTO user_genres (user_id, genre_id)
        SELECT ug.user_id, (SELECT id FROM genres WHERE name = m.new_name)
          FROM user_genres ug JOIN genres g ON g.id = ug.genre_id
         WHERE g.name = m.old_name
        ON CONFLICT DO NOTHING;
      DELETE FROM post_genres WHERE genre_id IN (SELECT id FROM genres WHERE name = m.old_name);
      DELETE FROM user_genres WHERE genre_id IN (SELECT id FROM genres WHERE name = m.old_name);
      DELETE FROM genres      WHERE name = m.old_name;
    END IF;
  END LOOP;
END $$;

-- 活動スタンスの統合。廃止したもの・名前を変えたものを、段階として一番近い残る選択肢へ寄せる。
-- そのまま消すと、選んでいた募集のスタンスが空になる（requirements 5章）。
DO $$
DECLARE m record;
BEGIN
  FOR m IN SELECT * FROM (VALUES
      ('プロとして活動中', 'プロを目指したい'),
      ('初心者同士で音を出したい', '初心者同士で合わせたい')
    ) AS t(old_name, new_name)
  LOOP
    CONTINUE WHEN NOT EXISTS (SELECT 1 FROM stances WHERE name = m.old_name);

    IF NOT EXISTS (SELECT 1 FROM stances WHERE name = m.new_name) THEN
      UPDATE stances SET name = m.new_name WHERE name = m.old_name;   -- 新しい行がまだ無い場合
    ELSE
      INSERT INTO post_stances (post_id, stance_id)
        SELECT ps.post_id, (SELECT id FROM stances WHERE name = m.new_name)
          FROM post_stances ps JOIN stances t ON t.id = ps.stance_id
         WHERE t.name = m.old_name
        ON CONFLICT DO NOTHING;
      INSERT INTO user_stances (user_id, stance_id)
        SELECT us.user_id, (SELECT id FROM stances WHERE name = m.new_name)
          FROM user_stances us JOIN stances t ON t.id = us.stance_id
         WHERE t.name = m.old_name
        ON CONFLICT DO NOTHING;
      DELETE FROM post_stances WHERE stance_id IN (SELECT id FROM stances WHERE name = m.old_name);
      DELETE FROM user_stances WHERE stance_id IN (SELECT id FROM stances WHERE name = m.old_name);
      DELETE FROM stances      WHERE name = m.old_name;
    END IF;
  END LOOP;
END $$;

-- 統合先の無い選択肢の削除。こちらは付け替え先が決められないので、参照が残っていれば中止する。
DO $$
DECLARE
  retired_parts  CONSTANT text[] := ARRAY['DJ','管楽器','弦楽器','パーカッション','その他'];
  retired_genres CONSTANT text[] := ARRAY['ヒップホップ','電子音楽','その他'];
  retired_stances CONSTANT text[] := ARRAY['プロとして活動中'];
  part_posts int; part_users int; genre_posts int; genre_users int;
  stance_posts int; stance_users int; orphaned int;
BEGIN
  SELECT count(*) INTO part_posts  FROM post_parts pp  JOIN parts p  ON p.id = pp.part_id   WHERE p.name = ANY(retired_parts);
  SELECT count(*) INTO part_users  FROM user_parts up  JOIN parts p  ON p.id = up.part_id   WHERE p.name = ANY(retired_parts);
  SELECT count(*) INTO genre_posts FROM post_genres pg JOIN genres g ON g.id = pg.genre_id  WHERE g.name = ANY(retired_genres);
  SELECT count(*) INTO genre_users FROM user_genres ug JOIN genres g ON g.id = ug.genre_id  WHERE g.name = ANY(retired_genres);
  SELECT count(*) INTO stance_posts FROM post_stances ps JOIN stances t ON t.id = ps.stance_id WHERE t.name = ANY(retired_stances);
  SELECT count(*) INTO stance_users FROM user_stances us JOIN stances t ON t.id = us.stance_id WHERE t.name = ANY(retired_stances);

  -- 募集はパート・ジャンルを1つ以上持つ必要がある（requirements 5章）。
  -- 消すと項目が空になる募集の件数も一緒に報告する。
  SELECT count(*) INTO orphaned
    FROM posts po
   WHERE (EXISTS (SELECT 1 FROM post_parts pp JOIN parts p ON p.id = pp.part_id
                   WHERE pp.post_id = po.id AND p.name = ANY(retired_parts))
          AND NOT EXISTS (SELECT 1 FROM post_parts pp JOIN parts p ON p.id = pp.part_id
                           WHERE pp.post_id = po.id AND NOT (p.name = ANY(retired_parts))))
      OR (EXISTS (SELECT 1 FROM post_genres pg JOIN genres g ON g.id = pg.genre_id
                   WHERE pg.post_id = po.id AND g.name = ANY(retired_genres))
          AND NOT EXISTS (SELECT 1 FROM post_genres pg JOIN genres g ON g.id = pg.genre_id
                           WHERE pg.post_id = po.id AND NOT (g.name = ANY(retired_genres))))
      OR (EXISTS (SELECT 1 FROM post_stances ps JOIN stances t ON t.id = ps.stance_id
                   WHERE ps.post_id = po.id AND t.name = ANY(retired_stances))
          AND NOT EXISTS (SELECT 1 FROM post_stances ps JOIN stances t ON t.id = ps.stance_id
                           WHERE ps.post_id = po.id AND NOT (t.name = ANY(retired_stances))));

  IF part_posts + part_users + genre_posts + genre_users + stance_posts + stance_users > 0 THEN
    RAISE EXCEPTION
      '廃止する選択肢がまだ参照されています（パート: 募集 % 件・プロフィール % 件、ジャンル: 募集 % 件・プロフィール % 件、'
      '活動スタンス: 募集 % 件・プロフィール % 件、うち消すと項目が空になる募集 % 件）。'
      'seed-demo-posts.sql を流すか、該当データを振り直してから再実行してください。',
      part_posts, part_users, genre_posts, genre_users, stance_posts, stance_users, orphaned;
  END IF;

  DELETE FROM parts   WHERE name = ANY(retired_parts);
  DELETE FROM genres  WHERE name = ANY(retired_genres);
  DELETE FROM stances WHERE name = ANY(retired_stances);

  RAISE NOTICE 'パート: %',   (SELECT string_agg(name, ', ' ORDER BY display_order) FROM parts);
  RAISE NOTICE 'ジャンル: %', (SELECT string_agg(name, ', ' ORDER BY display_order) FROM genres);
  RAISE NOTICE '活動スタンス: %', (SELECT string_agg(name, ', ' ORDER BY display_order) FROM stances);
END $$;

-- 並び順。MasterDataInitializer は新規作成時にしか display_order を入れないため、統合で
-- 残った行は古い順番のままになる。ここで一覧と同じ並びに揃える。
-- 並びは MasterDataInitializer の add(...) の文字列と一致させること。
WITH ordered AS (
  SELECT name, ord - 1 AS ord
    FROM unnest(ARRAY['ボーカル','ギター','ベース','ドラム','キーボード','作詞作曲']) WITH ORDINALITY AS t(name, ord)
)
UPDATE parts p SET display_order = o.ord FROM ordered o WHERE o.name = p.name;

WITH ordered AS (
  SELECT name, ord - 1 AS ord
    FROM unnest(ARRAY['ポップス','邦ロック','洋ロック','アニソン','ボカロ','ハードロック／メタル','パンク／メロコア',
                      'ジャズ','ブルース','ファンク／ソウル','R&B','フォーク／カントリー','クラシック']) WITH ORDINALITY AS t(name, ord)
)
UPDATE genres g SET display_order = o.ord FROM ordered o WHERE o.name = g.name;

WITH ordered AS (
  SELECT name, ord - 1 AS ord
    FROM unnest(ARRAY['初心者同士で合わせたい','趣味で楽しみたい','趣味でも本格的に取り組みたい',
                      'インディーズとして活動したい','プロを目指したい']) WITH ORDINALITY AS t(name, ord)
)
UPDATE stances s SET display_order = o.ord FROM ordered o WHERE o.name = s.name;

COMMIT;
