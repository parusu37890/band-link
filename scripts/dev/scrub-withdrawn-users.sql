-- 旧仕様で残った退会済みアカウントを互換整理する。新しい本人退会処理はこのSQLを使わない。
--
-- 以前の `withdraw` は status を WITHDRAWN にするだけで、メールアドレス・表示名・
-- パスワードのハッシュ・プロフィールがそのまま残っていた。そのアドレスでは二度と登録できず
-- （`existsByEmail` が退会済みの行に当たる）、募集も status が OPEN のままだった。
--
-- 旧仕様では行と会話を残していたため、このSQLも行を消さずに個人情報を整理する。
-- 新しい本人退会は AccountDeletionService が会話・メッセージを含めて物理削除する。

BEGIN;

-- 退会者の募集は「終了した」と自分で言えるようにする（ClosedReason.WITHDRAWN）。
UPDATE posts p SET status = 'CLOSED', closed_reason = 'WITHDRAWN', closed_at = COALESCE(closed_at, now())
  FROM users u
 WHERE u.id = p.user_id AND u.status = 'WITHDRAWN' AND p.status = 'OPEN';

-- プロフィールの紐付けを外す。
DELETE FROM user_parts       WHERE user_id IN (SELECT id FROM users WHERE status = 'WITHDRAWN');
DELETE FROM user_genres      WHERE user_id IN (SELECT id FROM users WHERE status = 'WITHDRAWN');
DELETE FROM user_stances     WHERE user_id IN (SELECT id FROM users WHERE status = 'WITHDRAWN');
DELETE FROM user_prefectures WHERE user_id IN (SELECT id FROM users WHERE status = 'WITHDRAWN');

-- 未使用のトークンも残す理由がない。
DELETE FROM email_verification_tokens WHERE user_id IN (SELECT id FROM users WHERE status = 'WITHDRAWN');
DELETE FROM password_reset_tokens     WHERE user_id IN (SELECT id FROM users WHERE status = 'WITHDRAWN');

DO $$
DECLARE scrubbed int;
BEGIN
  -- email は NOT NULL かつ一意なので空にできない。実在しない値へ退避して解放する
  -- （.invalid は RFC 2606 の予約TLD）。これで本人が同じアドレスで登録し直せる。
  UPDATE users
     SET email = 'withdrawn+' || id || '@invalid',
         password_hash = '(withdrawn)',
         username = '退会済みユーザー',
         bio = NULL, age = NULL, gender = NULL, experience_years = NULL,
         video_url = NULL, profile_image_url = NULL, email_verified_at = NULL
   WHERE status = 'WITHDRAWN'
     AND email NOT LIKE 'withdrawn+%@invalid';   -- 既に処理済みの行は触らない
  GET DIAGNOSTICS scrubbed = ROW_COUNT;
  RAISE NOTICE '退会済みアカウントを整理しました: % 件', scrubbed;
END $$;

COMMIT;
