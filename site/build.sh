#!/usr/bin/env bash
# 把 site/ 构建成可直接发布的静态站点：HTML、CSS、图标与三张截图。
# 截图只在 docs/screenshots/ 维护，构建时按固定文件名复制到站点根目录，
# 这样 README 里的 https://<owner>.github.io/<repo>/<name>.png 可以一直有效。
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
out="$here/.build/pages"

rm -rf "$out"
mkdir -p "$out"

cp "$here/index.html" "$here/styles.css" "$here/icon.svg" "$here/robots.txt" "$here/sitemap.xml" "$out/"

for name in library reader word-panel; do
  src="$here/../docs/screenshots/$name.png"
  if [[ ! -f "$src" ]]; then
    echo "缺少截图：$src" >&2
    exit 1
  fi
  cp "$src" "$out/$name.png"
done

# 站点没有以下划线开头的目录，加 .nojekyll 让 Pages 原样输出。
touch "$out/.nojekyll"

echo "站点已生成：$out"
du -sh "$out"
