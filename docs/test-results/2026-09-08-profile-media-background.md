# プロフィール音源URL・募集詳細背景の確認（2026-09-08）

## 実施内容

- `User` とプロフィールAPIに YouTube / TikTok / SoundCloud / Spotify / Apple Music の個別URL項目を追加。
- プロフィール設定画面で5サービスと既存互換のその他URLを個別入力できることを、ログイン済みのローカルブラウザで確認。
- 公開プロフィールAPIが5項目をJSONへ含め、メールアドレス・パスワードなど非公開情報を含めない契約テストを追加。
- 募集詳細の主要ブロックの計算済み背景色を確認し、すべて `rgb(248, 247, 243)`（`--bg`）で統一されていることを確認。

## 結果

- JUnit: **36件成功、失敗0、エラー0、スキップ0**。
- JavaScript: `node --check src/main/resources/static/js/account.js`、`media-embed.js` 成功。
- 差分: `git diff --check` 成功。
- コンパイル: `mvnw.cmd -B -DskipTests compile` 成功。
- ブラウザ: 8080の最新コードで `/posts/42`、`/users/37`、ログイン後の `/settings/profile` を確認。設定画面に5サービスのURL欄、詳細画面に統一背景を確認。

## 未実施

- 実際の各サービスのiframe再生（外部ネットワーク・ログインを要するため）。URL解析と安全な埋め込み先生成は既存の `media-embed.js` を使用。
- Safari、実機、スクリーンリーダー、8081、Elasticsearch/Kibana。
