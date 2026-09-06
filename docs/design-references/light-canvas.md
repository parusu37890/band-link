# ライト配色の参照メモ（2026-09-06）

Refero掲載のスタイルから、明るい配色の候補を確認したときの抜き書き。原文の転載ではなく、
色・数値の要点だけをこちらで書き出したもの。判断の根拠を残すための記録で、指示書ではない。

## 候補として見たもの

| スタイル | Referoの記述 | Band Linkとの相性 |
|---|---|---|
| [Anthropic](https://styles.refero.design/style/d469cba4-c448-4a43-a033-883f8bfcdc42) | Scientific field journal on warm background | **採用。** 暖色の紙面・細い罫・アクセント1色という構成が、いまの罫線ベースのUIとそのまま重なる |
| [Awesomic](https://styles.refero.design/style/8512e28d-5385-4c20-a336-214568c4370c) | Editorial magazine meets marketplace dashboard | 一覧の密度の考え方（セクション間は広く、カード内部は詰める）は参考になるが、角丸36pxのカード主体で今の罫線構成と合わない |
| [Cursor](https://styles.refero.design/style/4e3b4717-84c8-4599-baaf-a343c3d619b6) / [ONE](https://styles.refero.design/style/71745af1-2e53-4925-992e-82773e55ccd6) | 紙白の編集レイアウト | 方向性は近い。Anthropicとほぼ同系統のため一本化した |

## Anthropicから読み取った値

- Canvas `#f0eee6`（parchment）、Card `#faf9f5`、Dark inversion `#141413`
- Hairline `#cccbc8`、Muted `#b0aea5`、Outlined button border `#87867f`
- アクセントは1色のみ（clay `#d97757`、hover `#c6613f`）
- ナビゲーション・バッジの角丸は `0`、カードは24px
- 余白の基準単位4px、セクション間80〜120px

## Band Linkでそのまま使わなかった点

- **アクセント色**：clayは大きなCTA向けで、本文リンクに使うと明るい紙面で4.5:1に届かない。
  Band Linkはダーク時からセージ系を持っていたので、その色調を保ったまま濃くした苔色を使う。
- **セリフ書体**：日本語の本文が主体で、和文セリフを前提にできないため採用しない。
- **キャンバスの明度**：`#f0eee6`より少し明るい`#f4f3ee`にし、読む面（パネル・入力欄）は白にした。
  「背景は白のほうがいい」という要望に対して、面は白・地は暖色の紙という切り分けにしている。
- **角丸**：Anthropicのカード24pxは取らず、既存の3pxを維持する。
