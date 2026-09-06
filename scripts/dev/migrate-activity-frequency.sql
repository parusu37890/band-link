-- 活動頻度から「不定期(IRREGULAR)」と「相談して決める(NEGOTIABLE)」を外し、
-- 「2か月に1回(BIMONTHLY_1)」を足すのに伴う、既存DBの移行。
--
-- 活動頻度はマスタテーブルではなく enum（ActivityFrequency）で、posts.activity_frequency に
-- 文字列で入っている。enum から値を消した状態で古い文字列が残っていると、その募集を
-- 読み込んだ時点で変換に失敗する。**アプリを新しい enum で起動する前に流すこと。**
--
-- 移行先について：外す2つはどちらも「頻度を決めていない」という意味で、新しい一覧には
-- 対応する値が無い。忠実な移行先が存在しないため、中庸な「月1回程度(MONTHLY_1)」へ寄せる。
-- デモ投稿は seed-demo-posts.sql を流せば新しい5値へ振り直される。

BEGIN;

-- 1. 先に値を移す。次のCHECK制約は新しい一覧しか許さないので、この順序でないと張り直せない。
DO $$
DECLARE moved int;
BEGIN
  UPDATE posts SET activity_frequency = 'MONTHLY_1'
   WHERE activity_frequency IN ('IRREGULAR', 'NEGOTIABLE');
  GET DIAGNOSTICS moved = ROW_COUNT;
  RAISE NOTICE '活動頻度を移行した募集: % 件', moved;
END $$;

-- 2. Hibernate は @Enumerated(STRING) の列に enum の値を並べた CHECK 制約を作るが、
--    ddl-auto: update は既存の制約を作り直さない。古い一覧のまま残るため、ここで張り直す。
--    これを忘れると、値を移行しても BIMONTHLY_1 の書き込みが
--    posts_activity_frequency_check で弾かれる（実際に踏んだ）。
ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_activity_frequency_check;
ALTER TABLE posts ADD CONSTRAINT posts_activity_frequency_check
  CHECK (activity_frequency IN ('WEEKLY_2PLUS','WEEKLY_1','MONTHLY_2_3','MONTHLY_1','BIMONTHLY_1'));

DO $$
BEGIN
  RAISE NOTICE '現在の内訳: %', (SELECT string_agg(activity_frequency || '=' || c, ', ' ORDER BY activity_frequency)
                                   FROM (SELECT activity_frequency, count(*) c FROM posts GROUP BY activity_frequency) t);
END $$;

COMMIT;
