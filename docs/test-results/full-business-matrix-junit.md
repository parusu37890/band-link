# Full business matrix JUnit

実行日: 2026-09-15

`docs/test-plan/full-case-inventory.csv` のうち、JUnitで検証するAPI・入力・サービス・認可・保存処理14,227ケースを、ケースIDごとのJUnit DynamicTestとして実行した。

## 実行方法

```powershell
Set-Location 'C:\Users\parus\Desktop\band'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test/run-full-business-matrix-junit.ps1
```

## 結果

- 検出: 14,227
- 実行: 14,227
- 成功: 14,227
- 失敗: 0
- 中断: 0
- スキップ: 0
- ケース別結果: `docs/test-results/api-case-adapters/full-business-matrix-results.csv`

| 機能 | ケース数 | 実行アダプター |
|---|---:|---|
| Authentication | 720 | AuthProfileFeedbackAdapter |
| Profile | 750 | AuthProfileFeedbackAdapter |
| Feedback | 1,200 | AuthProfileFeedbackAdapter |
| Recruitment-post | 3,024 | PostMessageStateAdapter |
| Direct-message | 3,528 | PostMessageStateAdapter |
| Search | 1,680 | SecuritySearchImageReportAdapter |
| Image-storage | 1,000 | SecuritySearchImageReportAdapter |
| Report-admin | 750 | SecuritySearchImageReportAdapter |
| Security | 1,575 | SecuritySearchImageReportAdapter |
| **合計** | **14,227** | |

## 検証方法

- CSV読込時に必須列、ケースID重複、JUnit対象件数14,227を検査する。
- 機能ごとに実DTO、Hibernate Validator、サービス、Controller、Security Filter、画像保存処理を呼び出す。
- ユーザー状態、入力状態、データ状態、操作の未知値は既定成功にせず、そのケースを失敗させる。
- 各DynamicTestが成功した場合だけケースIDを結果CSVへ記録する。
- CSRFは生トークンの成功、欠落・別セッション・期限切れ相当の拒否、再取得後の再試行を検証する。
- 投稿・DM・ブロック・画像・通報・検索は状態別の成功、拒否、保存・非保存、副作用を実コードまたはmock repositoryへの作用で検証する。

## 範囲

この結果は全14,659ケース中、JUnitで実行する14,227ケースの結果である。残る432ケースはPlaywright MCPまたは手動・非機能試験の対象であり、このJUnit結果には含めない。実PostgreSQL、実HTTPサーバー、実ブラウザーを必要とする確認は、結合試験とシステム試験の証跡を別に判定する。

WindowsではJava compiler終了時にJAR解放の`AccessDeniedException`警告が表示される場合がある。コンパイラー終了コード、JUnit集計、結果CSV件数をすべて検査し、警告だけをテスト成功の根拠にはしていない。`r`n