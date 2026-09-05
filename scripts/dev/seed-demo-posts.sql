-- Band Link local demo data. Safe to run repeatedly: demo rows are identified by email/title.
-- Password for all demo users: password
DO $$
BEGIN
  INSERT INTO users (username, email, password_hash, email_verified_at, status, role, created_at)
  VALUES
    ('夜更かしスタジオ', 'demo01@bandlink.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', now(), 'ACTIVE', 'USER', now()),
    ('ミッドナイト・コード', 'demo02@bandlink.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', now(), 'ACTIVE', 'USER', now()),
    ('青いアンプ', 'demo03@bandlink.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', now(), 'ACTIVE', 'USER', now()),
    ('週末セッション', 'demo04@bandlink.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', now(), 'ACTIVE', 'USER', now())
  ON CONFLICT (email) DO UPDATE SET email_verified_at = COALESCE(users.email_verified_at, now()), status = 'ACTIVE';

  INSERT INTO posts (user_id, type, title, content, area_sub, activity_frequency, status, created_at, updated_at, expires_at, rank_updated_at)
  SELECT u.id,
    CASE WHEN s.n % 3 = 0 THEN 'WANTS_TO_JOIN' ELSE 'MEMBER_WANTED' END,
    format('【デモ %s】%sを一緒に鳴らしませんか', lpad(s.n::text, 2, '0'), CASE WHEN s.n % 3 = 0 THEN '新しい曲' ELSE '週末の音' END),
    format('好きな音楽について話しながら、無理なく活動できる仲間を探しています。デモ投稿%sです。まずはメッセージでやりたいことを聞かせてください。', lpad(s.n::text, 2, '0')),
    CASE WHEN s.n % 4 = 0 THEN '駅から徒歩10分圏内' WHEN s.n % 4 = 1 THEN '市内スタジオ中心' WHEN s.n % 4 = 2 THEN 'オンライン相談可' ELSE '近隣エリア' END,
    (ARRAY['WEEKLY_2PLUS','WEEKLY_1','MONTHLY_2_3','MONTHLY_1','IRREGULAR','NEGOTIABLE'])[(s.n % 6) + 1],
    'OPEN', now() - (s.n || ' days')::interval, now() - (s.n || ' days')::interval,
    now() + interval '30 days' - (s.n || ' days')::interval, now() - (s.n || ' hours')::interval
  FROM generate_series(1,40) AS s(n)
  JOIN users u ON u.email = 'demo' || lpad((((s.n - 1) % 4) + 1)::text, 2, '0') || '@bandlink.local'
  WHERE NOT EXISTS (SELECT 1 FROM posts p WHERE p.title = format('【デモ %s】%sを一緒に鳴らしませんか', lpad(s.n::text, 2, '0'), CASE WHEN s.n % 3 = 0 THEN '新しい曲' ELSE '週末の音' END));

  INSERT INTO post_parts (post_id, part_id)
  SELECT p.id, (SELECT id FROM parts ORDER BY display_order, id OFFSET ((p.id % 10)) LIMIT 1)
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
  INSERT INTO post_genres (post_id, genre_id)
  SELECT p.id, (SELECT id FROM genres ORDER BY display_order, id OFFSET ((p.id % 14)) LIMIT 1)
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
  INSERT INTO post_stances (post_id, stance_id)
  SELECT p.id, (SELECT id FROM stances ORDER BY display_order, id OFFSET ((p.id % 4)) LIMIT 1)
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
  INSERT INTO post_prefectures (post_id, prefecture_id)
  SELECT p.id, (SELECT id FROM prefectures ORDER BY display_order, id OFFSET ((p.id % 47)) LIMIT 1)
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
  INSERT INTO post_age_ranges (post_id, age_range)
  SELECT p.id, CASE WHEN p.id % 4 = 0 THEN 'ANY' WHEN p.id % 4 = 1 THEN 'S20' WHEN p.id % 4 = 2 THEN 'S30' ELSE 'S40' END
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
END $$;

