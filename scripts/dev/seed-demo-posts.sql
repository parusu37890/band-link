-- Band Link local demo data. Safe to run repeatedly: demo rows are identified by email and the 【デモ NN】 title prefix.
-- Titles and bodies are deliberately varied so the recruitment list can be judged at realistic volume
-- (40 near-identical rows made it impossible to tell whether the list reads well).
-- Password for all demo users: password
DO $$
BEGIN
  INSERT INTO users (username, email, password_hash, email_verified_at, status, role, created_at)
  VALUES
    ('夜更かしスタジオ', 'demo01@bandlink.local', '$2a$10$Y3HGLN2ZiuUrMMSrJIp7wOrZUw208XqZ0egyqq/GzsEBwqXqEYyma', now(), 'ACTIVE', 'USER', now()),
    ('ミッドナイト・コード', 'demo02@bandlink.local', '$2a$10$Y3HGLN2ZiuUrMMSrJIp7wOrZUw208XqZ0egyqq/GzsEBwqXqEYyma', now(), 'ACTIVE', 'USER', now()),
    ('青いアンプ', 'demo03@bandlink.local', '$2a$10$Y3HGLN2ZiuUrMMSrJIp7wOrZUw208XqZ0egyqq/GzsEBwqXqEYyma', now(), 'ACTIVE', 'USER', now()),
    ('週末セッション', 'demo04@bandlink.local', '$2a$10$Y3HGLN2ZiuUrMMSrJIp7wOrZUw208XqZ0egyqq/GzsEBwqXqEYyma', now(), 'ACTIVE', 'USER', now())
  ON CONFLICT (email) DO UPDATE SET email_verified_at = COALESCE(users.email_verified_at, now()), status = 'ACTIVE';

  -- Demo profiles. Without these every public profile renders as six "未設定" rows,
  -- which makes the profile screen impossible to judge. Photos are left unset on purpose.
  UPDATE users u SET bio = v.bio, age = v.age, gender = v.gender, experience_years = v.years, video_url = v.video
  FROM (VALUES
    -- video_url is left NULL: demo accounts should not link to a real person's video.
    ('demo01@bandlink.local','平日は会社員、金曜の夜から日曜にかけてスタジオにこもっています。90年代のオルタナやグランジが原点で、最近は轟音の中にメロディが残る曲を作りたいと思っています。演奏の上手さより、同じ音量で長く続けられる相手を探しています。',29,'男性',8,NULL),
    ('demo02@bandlink.local','ベース歴5年。指弾き中心で、リズム隊としてどっしり支えるのが好きです。コピーよりオリジナルをやりたい気持ちが強く、いまはギターとドラムの2人で曲を貯めています。来年には小さくてもいいのでライブをやりたい。',26,'女性',5,NULL),
    ('demo03@bandlink.local','ドラム歴12年。ブルースとジャズを行き来しています。手数で押すより、隙間を大事にしたい派です。年齢や経験は問いません、音を聴き合える方と一緒にやれたら嬉しいです。',38,'男性',12,NULL),
    ('demo04@bandlink.local','鍵盤を担当しています。シティポップやAORのような、少し都会的な響きが好きです。譜面もコードもだいたい対応できます。土日のどちらかで、無理のないペースで活動したいと思っています。',24,'女性',3,NULL)
  ) AS v(email, bio, age, gender, years, video)
  WHERE u.email = v.email;

  -- demo01 is the operator account so the admin screen (reports, suspensions) can be exercised locally.
  UPDATE users SET role = 'ADMIN' WHERE email = 'demo01@bandlink.local';
  UPDATE users SET role = 'USER' WHERE email IN ('demo02@bandlink.local','demo03@bandlink.local','demo04@bandlink.local');

  -- Spread last_login_at so every activity bucket (and the 'no recent sign-in' case) is visible locally.
  UPDATE users SET last_login_at = now() - v.ago FROM (VALUES
    ('demo01@bandlink.local', interval '2 hours'),
    ('demo02@bandlink.local', interval '5 days'),
    ('demo03@bandlink.local', interval '20 days'),
    ('demo04@bandlink.local', interval '200 days')
  ) AS v(email, ago) WHERE users.email = v.email;

  DELETE FROM user_parts WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'demo0%@bandlink.local');
  DELETE FROM user_genres WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'demo0%@bandlink.local');
  DELETE FROM user_stances WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'demo0%@bandlink.local');
  DELETE FROM user_prefectures WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'demo0%@bandlink.local');
  -- These four profiles were written before the master lists changed, so 作詞作曲 and
  -- 初心者同士で合わせたい existed but no demo profile carried them — the two options added last
  -- could not be seen anywhere except on posts. Every part and every stance now appears at least
  -- once, and each choice is one the profile text already supports.
  INSERT INTO user_parts (user_id, part_id)
  SELECT u.id, p.id FROM (VALUES
    ('demo01@bandlink.local','ギター'),('demo01@bandlink.local','ボーカル'),
    ('demo02@bandlink.local','ベース'),('demo02@bandlink.local','作詞作曲'),
    ('demo03@bandlink.local','ドラム'),('demo04@bandlink.local','キーボード')
  ) AS v(email, part) JOIN users u ON u.email = v.email JOIN parts p ON p.name = v.part ON CONFLICT DO NOTHING;
  INSERT INTO user_genres (user_id, genre_id)
  SELECT u.id, g.id FROM (VALUES
    ('demo01@bandlink.local','邦ロック'),('demo01@bandlink.local','ハードロック／メタル'),('demo01@bandlink.local','パンク／メロコア'),
    ('demo02@bandlink.local','洋ロック'),('demo02@bandlink.local','ポップス'),('demo02@bandlink.local','R&B'),('demo02@bandlink.local','ボカロ'),
    ('demo03@bandlink.local','ブルース'),('demo03@bandlink.local','ジャズ'),('demo03@bandlink.local','ファンク／ソウル'),
    ('demo04@bandlink.local','ポップス'),('demo04@bandlink.local','クラシック'),('demo04@bandlink.local','フォーク／カントリー'),('demo04@bandlink.local','アニソン')
  ) AS v(email, genre) JOIN users u ON u.email = v.email JOIN genres g ON g.name = v.genre ON CONFLICT DO NOTHING;
  INSERT INTO user_stances (user_id, stance_id)
  SELECT u.id, s.id FROM (VALUES
    ('demo01@bandlink.local','趣味でも本格的に取り組みたい'),
    ('demo02@bandlink.local','プロを目指したい'),('demo02@bandlink.local','インディーズとして活動したい'),
    ('demo03@bandlink.local','趣味で楽しみたい'),
    ('demo04@bandlink.local','趣味で楽しみたい'),('demo04@bandlink.local','初心者同士で合わせたい')
  ) AS v(email, stance) JOIN users u ON u.email = v.email JOIN stances s ON s.name = v.stance ON CONFLICT DO NOTHING;
  INSERT INTO user_prefectures (user_id, prefecture_id)
  SELECT u.id, pr.id FROM (VALUES
    ('demo01@bandlink.local','東京都'),('demo02@bandlink.local','神奈川県'),
    ('demo03@bandlink.local','大阪府'),('demo04@bandlink.local','東京都')
  ) AS v(email, pref) JOIN users u ON u.email = v.email JOIN prefectures pr ON pr.name = v.pref ON CONFLICT DO NOTHING;

  -- part/genre are chosen to match what each post actually says, so the list does not
  -- contradict itself (a "looking for a vocalist" post tagged DJ reads as generated filler).
  CREATE TEMP TABLE demo_copy (n int primary key, title text, body text, part text, genre text) ON COMMIT DROP;
  INSERT INTO demo_copy (n, title, body) VALUES
   (1,'週末だけ、轟音を鳴らせる場所を探しています','平日は別の仕事をしていて、土日のどちらかだけスタジオに入っています。轟音でも大丈夫な箱を確保できるので、音量を気にせず鳴らしたい方に来てほしいです。まずは好きなアルバムの話からしましょう。'),
   (2,'ベース募集。オリジナルを作って、来年ライブに出たい','ギターとドラムの2人で、月2回スタジオに入っています。コピーは一通りやったので、そろそろ自分たちの曲を作りたい段階です。作曲経験は問いません。音を出しながら決めていければ。'),
   (3,'ドラム叩けます。落ち着いたバンドを探しています','社会人5年目、経験は10年ほど。派手なプレイより、曲を支える方が好きです。無理のないペースで長く続けられるバンドに参加したいです。'),
   (4,'キーボード募集：シティポップ寄りのアレンジをしたい','8ビートの曲に鍵盤を足して、もう少し都会的な響きにしたいと思っています。譜面はざっくりでも大丈夫です。コードが読めれば十分。'),
   (5,'ボーカル探しています（女性ボーカル曲を中心に）','演奏だけ3人揃っていて、歌が乗れば形になります。キーは合わせて作り直せます。音源をお送りできるので、聴いてから決めてください。'),
   (6,'ギター1本、参加できるバンドを探しています','ブルース由来のフレーズが好きです。歪みは控えめ、音量も常識的にやります。年齢は気にしません。'),
   (7,'月1回でいい。長く続けられるメンバーを募集','家庭も仕事もあるので、頻度は月1回で固定したいです。その代わり、来た日はきっちり合わせたい。同じ感覚の方がいたら。'),
   (8,'パンク。速い曲を、ちゃんと速く演奏したい','テンポが落ちないバンドをやりたいです。ライブは年に数本でいいので、練習は詰めたい。ベースとドラムを探しています。'),
   (9,'ジャズのスタンダードを一緒に。セッション仲間募集','完全に趣味です。決まった曲を数曲、じっくりやるのが好きです。上手さより、聴き合える人がいいです。'),
   (10,'アニソンのコピーバンド、ギター募集中','ライブハウスよりイベント出演を中心に考えています。衣装などは各自自由。曲は相談して決めましょう。'),
   (11,'ベースを始めて2年。バンドで揉まれたいです','家で弾いているだけでは限界を感じています。下手でも構わないと言ってくれるバンドを探しています。練習はします。'),
   (12,'ハードロック。ツインギターにしたいので、もう1人','リフを重ねたいので、ソロを弾きたい方より、刻める方が合うと思います。機材は貸し借りできます。'),
   (13,'DTMで作った曲を、生の音で鳴らしたい','宅録で20曲ほど作っています。打ち込みを人の演奏に置き換えたいので、アレンジを一緒に考えてくれる方を探しています。'),
   (14,'ドラム募集。フォーク寄り、静かな曲が多いです','大きな音は出しません。ブラシやリムショットが好きな方に向いていると思います。'),
   (15,'キーボードで参加できるバンドを探しています','ファンクやソウル寄りの曲が好きです。クラビやローズの音を足せます。リズム隊が固まっているバンドだと嬉しいです。'),
   (16,'弾き語りから、バンド編成にしたい','1人でライブをしてきましたが、厚みが欲しくなりました。曲はあります。一緒に育ててくれる人を探しています。'),
   (17,'週2で入れる方、募集します。本気でやりたい','曲作りからライブまで、時間をかけたいです。プロを目指すというより、納得できるものを作りたい。'),
   (18,'クラシック出身。ポップスのバンドに入りたい','譜面は読めますが、コード進行から作るやり方は不慣れです。教えてもらいながらやりたいです。'),
   (19,'R&Bをバンドの編成でやりたい','打ち込みのトラックにギターとドラムを重ねる形を考えています。ジャンルにこだわらない方だと話が早いです。'),
   (20,'ボーカル（男性）募集。歌詞は書ける方だと嬉しい','曲はできていて、メロディも仮で乗っています。言葉を乗せてくれる人を探しています。'),
   (21,'スタジオ代は折半で。気楽にやれるバンド募集','趣味の範囲で、月2〜3回。飲みに行くのも含めて楽しめる感じがいいです。'),
   (22,'ドラマー募集：8ビートがしっかりしていれば十分','技術より、テンポが安定している方を探しています。難しいことはやりません。'),
   (23,'ギター（リード）を探しています。オリジナル中心','ソロを任せられる方に来てほしいです。曲は月に1〜2曲のペースで増えています。'),
   (24,'バンドを組み直します。心機一転、メンバー募集','前のバンドは解散しました。曲と機材は残っているので、また一から始めたいです。'),
   (25,'ベース。ライブ経験少なめですが、参加希望です','スタジオには通っています。まずは人前で演奏する経験を積みたいです。'),
   (26,'シンセを弾ける方を探しています','生ドラムとシンセを合わせた編成を考えています。機材の話ができる方だと助かります。'),
   (27,'ドラムで参加できるバンドを探しています','手数より、しっかりしたビートを置くのが好きです。リズムを厚くしたいバンドがあれば声をかけてください。'),
   (28,'40代からのバンド。同世代の方に来てほしい','無理はしません。曲は昔よく聴いていたものが中心になると思います。'),
   (29,'オンラインで相談してから、スタジオで会いたい','いきなり集まるのは気を使うので、まず通話で方向性を話せたらと思っています。'),
   (30,'ボーカル参加希望。歌えるバンドを探しています','キーは高めです。音源をお渡しできるので、合うかどうか判断してください。'),
   (31,'ギターとベース、同時募集。ドラムと歌はいます','2人同時でも、片方ずつでも構いません。まずは1回音を合わせましょう。'),
   (32,'R&B寄りのグルーヴを作りたい。ベース募集','後ろに乗るリズムが好きな方だと合うと思います。難しいことはしません。'),
   (33,'コピーからでいいので、まず音を合わせたい','いきなりオリジナルは重いので、好きな曲を数曲コピーするところから始めたいです。'),
   (34,'ドラム（参加希望）。平日夜なら動けます','土日は予定が読めないので、平日の夜に練習できるバンドを探しています。'),
   (35,'キーボード参加希望。伴奏に回るのが好きです','前に出るより、全体を支える役割が向いていると思っています。'),
   (36,'メタル。ドラムを探しています。ツーバス歓迎','速い曲をやりたいです。音源をお送りできます。'),
   (37,'ゆるく、でも続けたい。ギター募集','解散しないバンドを作りたいです。頻度より継続を大事にしたい。'),
   (38,'ライブ出演が決まっています。急ぎでベース募集','2ヶ月後に対バンがあります。曲は5曲、音源とタブ譜があります。'),
   (39,'作詞作曲で参加できるバンドを探しています','詞も曲も書きます。演奏はしませんが、曲づくりのところで力になれます。まず何曲か聴いてください。'),
   (40,'初心者歓迎。一緒に上手くなっていければ','全員が上手い必要はないと思っています。続けられる人を探しています。');

  UPDATE demo_copy SET part = v.part, genre = v.genre FROM (VALUES
   (1,'ギター','ハードロック／メタル'),(2,'ベース','邦ロック'),(3,'ドラム','邦ロック'),(4,'キーボード','ポップス'),
   (5,'ボーカル','ポップス'),(6,'ギター','ブルース'),(7,'ボーカル','フォーク／カントリー'),(8,'ベース','パンク／メロコア'),
   (9,'キーボード','ジャズ'),(10,'ギター','アニソン'),(11,'ベース','邦ロック'),(12,'ギター','ハードロック／メタル'),
   (13,'作詞作曲','ボカロ'),(14,'ドラム','フォーク／カントリー'),(15,'キーボード','ファンク／ソウル'),(16,'ボーカル','フォーク／カントリー'),
   (17,'ギター','洋ロック'),(18,'キーボード','クラシック'),(19,'作詞作曲','R&B'),(20,'ボーカル','邦ロック'),
   (21,'ベース','ポップス'),(22,'ドラム','ポップス'),(23,'ギター','洋ロック'),(24,'ドラム','邦ロック'),
   (25,'ベース','ポップス'),(26,'キーボード','ポップス'),(27,'ドラム','ファンク／ソウル'),(28,'ベース','洋ロック'),
   (29,'ギター','ポップス'),(30,'ボーカル','ボカロ'),(31,'ギター','邦ロック'),(32,'ベース','R&B'),
   (33,'ドラム','洋ロック'),(34,'ドラム','邦ロック'),(35,'キーボード','ポップス'),(36,'ドラム','ハードロック／メタル'),
   (37,'ギター','アニソン'),(38,'ベース','洋ロック'),(39,'作詞作曲','ポップス'),(40,'キーボード','ポップス')
  ) AS v(n, part, genre) WHERE demo_copy.n = v.n;

  -- Refresh existing demo rows in place so re-running does not duplicate them.
  -- activity_frequency is refreshed too: it is only set on INSERT, so without this a demo row
  -- keeps whatever value it was created with even after the list it came from has changed.
  UPDATE posts p SET title = format('【デモ %s】%s', lpad(d.n::text, 2, '0'), d.title), content = d.body,
    activity_frequency = (ARRAY['WEEKLY_2PLUS','WEEKLY_1','MONTHLY_2_3','MONTHLY_1','BIMONTHLY_1'])[(d.n % 5) + 1]
  FROM demo_copy d
  WHERE p.title LIKE format('【デモ %s】%%', lpad(d.n::text, 2, '0'));

  INSERT INTO posts (user_id, type, title, content, area_sub, activity_frequency, status, created_at, updated_at, expires_at, rank_updated_at)
  SELECT u.id,
    CASE WHEN s.n % 3 = 0 THEN 'WANTS_TO_JOIN' ELSE 'MEMBER_WANTED' END,
    format('【デモ %s】%s', lpad(s.n::text, 2, '0'), d.title),
    d.body,
    CASE WHEN s.n % 4 = 0 THEN '駅から徒歩10分圏内' WHEN s.n % 4 = 1 THEN '市内スタジオ中心' WHEN s.n % 4 = 2 THEN 'オンライン相談可' ELSE '近隣エリア' END,
    (ARRAY['WEEKLY_2PLUS','WEEKLY_1','MONTHLY_2_3','MONTHLY_1','BIMONTHLY_1'])[(s.n % 5) + 1],
    'OPEN', now() - (s.n || ' days')::interval, now() - (s.n || ' days')::interval,
    now() + interval '30 days' - (s.n || ' days')::interval, now() - (s.n || ' hours')::interval
  FROM generate_series(1,40) AS s(n)
  JOIN demo_copy d ON d.n = s.n
  JOIN users u ON u.email = 'demo' || lpad((((s.n - 1) % 4) + 1)::text, 2, '0') || '@bandlink.local'
  WHERE NOT EXISTS (SELECT 1 FROM posts p WHERE p.title LIKE format('【デモ %s】%%', lpad(s.n::text, 2, '0')));

  -- Rebuild part/genre for demo posts so they always match the current copy.
  DELETE FROM post_parts WHERE post_id IN (SELECT id FROM posts WHERE title LIKE '【デモ %】%');
  DELETE FROM post_genres WHERE post_id IN (SELECT id FROM posts WHERE title LIKE '【デモ %】%');
  DELETE FROM post_stances WHERE post_id IN (SELECT id FROM posts WHERE title LIKE '【デモ %】%');
  INSERT INTO post_parts (post_id, part_id)
  SELECT p.id, pa.id FROM posts p
  JOIN demo_copy d ON p.title LIKE format('【デモ %s】%%', lpad(d.n::text, 2, '0'))
  JOIN parts pa ON pa.name = d.part
  ON CONFLICT DO NOTHING;
  INSERT INTO post_genres (post_id, genre_id)
  SELECT p.id, g.id FROM posts p
  JOIN demo_copy d ON p.title LIKE format('【デモ %s】%%', lpad(d.n::text, 2, '0'))
  JOIN genres g ON g.name = d.genre
  ON CONFLICT DO NOTHING;
  -- 並び順の剰余で選ぶと、廃止予定の選択肢がテーブルに残っている間はそれも拾ってしまう。
  -- 名前で指定して、いま有効な5つだけを順番に当てる。
  INSERT INTO post_stances (post_id, stance_id)
  SELECT p.id, s.id
  FROM posts p
  JOIN LATERAL (SELECT (ARRAY['初心者同士で合わせたい','趣味で楽しみたい','趣味でも本格的に取り組みたい',
                              'インディーズとして活動したい','プロを目指したい'])[(p.id % 5) + 1] AS name) pick ON TRUE
  JOIN stances s ON s.name = pick.name
  WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
  INSERT INTO post_prefectures (post_id, prefecture_id)
  SELECT p.id, (SELECT id FROM prefectures ORDER BY display_order, id OFFSET ((p.id % 47)) LIMIT 1)
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
  INSERT INTO post_age_ranges (post_id, age_range)
  SELECT p.id, CASE WHEN p.id % 4 = 0 THEN 'ANY' WHEN p.id % 4 = 1 THEN 'S20' WHEN p.id % 4 = 2 THEN 'S30' ELSE 'S40' END
  FROM posts p WHERE p.title LIKE '【デモ %】%' ON CONFLICT DO NOTHING;
END $$;
