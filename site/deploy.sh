#!/usr/bin/env bash
# 构建站点并发布到 gh-pages 分支；GitHub Pages 从该分支根目录输出。
# 发布前先在本地预览：./build.sh && python3 -m http.server -d .build/pages 8000
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo=$(cd "$here/.." && pwd)
build="$here/.build/pages"
tmp="$here/.build/gh-pages"

"$here/build.sh"

git -C "$repo" worktree remove --force "$tmp" >/dev/null 2>&1 || true
rm -rf "$tmp"
git -C "$repo" worktree add -B gh-pages "$tmp" HEAD >/dev/null

git -C "$tmp" rm -rf . >/dev/null
rsync -a --delete --exclude=.git "$build/" "$tmp/"
git -C "$tmp" add -A
git -C "$tmp" commit -m "site: 发布官网与截图" >/dev/null
git -C "$tmp" push -f origin gh-pages

git -C "$repo" worktree remove --force "$tmp" >/dev/null
echo "已推送 gh-pages。Pages 构建完成后访问：https://selwiro.github.io/pagelence/"
