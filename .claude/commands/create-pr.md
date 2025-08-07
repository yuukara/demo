---
allowed-tools: Bash(gh:*), Bash(git:*)
description: "Create a Pull Request"
---

コンテキストを確認し、PRの作成と表示を行います。

# コンテキスト

- !`git status`で現在のGitの状態を確認してください
- !`git diff HEAD`で現在の変更点を確認してください
- !`git branch --show-current`で現在のブランチを確認してください
- !`git log --oneline -10`で直近のコミットを確認してください

# PRの作成

現在のブランチをリモートリポジトリにプッシュします。
上記の変更に基づいて、以下のコマンドを使用してPRを作成します。

```powershell
# Windows PowerShell環境用
$TARGET_BRANCH = if ($args.Count -gt 0) { $args[0] } else { "main" }

# PRタイトルとボディの準備
$PR_TITLE = "fix: resolve code review issues and improve security"
$PR_BODY = @"
## やったこと
- セキュリティ修正（パスワード環境変数化完了）
- コード品質向上（Java警告解消）
- EmployeeService リファクタリング
- 入力値検証の追加

## 背景
- コードレビューで指摘されたセキュリティ・品質問題の解決
- 保守性向上のためのリファクタリング実施

## 動作確認
- [ ] 単体テスト実行: ``.\mvnw test``
- [ ] 統合テスト実行: ``.\mvnw verify``
- [ ] アプリケーション起動確認: ``.\mvnw spring-boot:run``

## 参考情報
- 現在のブランチ: fix/code-review-issues
- CLAUDE.mdの規約に準拠した実装
- セキュリティベストプラクティス適用済み
"@

# PRの作成（ドラフトとして）
gh pr create --draft --base $TARGET_BRANCH --title $PR_TITLE --body $PR_BODY
```

# PRの表示

- PRの作成が完了したら!`gh pr view --web`でPRをブラウザで表示します

## 実行例

```powershell
# 1. まず現在のブランチをプッシュ
git push -u origin fix/code-review-issues

# 2. PRを作成
gh pr create --draft --base main --title "fix: resolve code review issues and improve security" --body "..."

# 3. PRをブラウザで確認
gh pr view --web
```

## トラブルシューティング

- GitHub CLIが認証されていない場合: `gh auth login`
- リモートブランチが存在しない場合: 先に`git push -u origin <branch-name>`でプッシュ
- PRが既に存在する場合: `gh pr edit`で編集可能
