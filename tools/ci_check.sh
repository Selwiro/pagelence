#!/usr/bin/env bash
# 本地与 CI 共用的一道门：内容产物、单测、App 编译与 lint。
#
# 用法：
#   tools/ci_check.sh                              # 严格模式：缺依赖就失败
#   PAGELENCE_ALLOW_MISSING=1 tools/ci_check.sh    # 本地临时跳过依赖外部数据的检查
#
# 外部数据（wordfreq、ECDICT CSV、词级来源）不在仓库里，严格模式下缺失会直接
# 失败并列出缺什么——「静默跳过」会让一次全绿的检查实际上什么都没查。
set -euo pipefail
cd "$(dirname "$0")/.."

allow_missing="${PAGELENCE_ALLOW_MISSING:-0}"

missing() {
  local what="$1"
  if [[ "$allow_missing" == "1" ]]; then
    echo "skip: $what（PAGELENCE_ALLOW_MISSING=1）" >&2
  else
    echo "error: 缺少 $what" >&2
    echo "       安装后重跑，或显式设置 PAGELENCE_ALLOW_MISSING=1 跳过。" >&2
    exit 1
  fi
}

python3 tools/check_strings.py
python3 tools/migrate/import_reference_data.py --check
python3 tools/build_alice_book.py --check
python3 tools/build_dolittle_book.py --check
python3 tools/build_shelf.py --check
python3 tools/build_assessment_bank.py --check
python3 tools/build_full_dictionary.py --check
python3 tools/build_lexicon.py --check

if ! python3 -c "import wordfreq" 2>/dev/null; then
  missing "wordfreq 3.0.2（pip install wordfreq==3.0.2）"
elif [[ -z "${PAGELENCE_WEB_WORD_LEVELS:-}" || -z "${PAGELENCE_CEFR_WORDNET_TSV:-}" ]]; then
  missing "词级来源 PAGELENCE_WEB_WORD_LEVELS 与 PAGELENCE_CEFR_WORDNET_TSV"
  python3 tools/build_word_frequencies.py --check
else
  python3 tools/build_word_levels.py --check
  python3 tools/build_word_frequencies.py --check
fi

if [[ -n "${ECDICT_CSV:-}" ]]; then
  python3 tools/build_dictionaries.py --ecdict-csv "$ECDICT_CSV" --check
else
  missing "ECDICT_CSV（完整 ECDICT CSV 路径）"
fi

python3 tools/validate/validate_content.py

# :presentation 必须保持 Android-free，M5 才能直接复用。
if grep -rqE "^import (android|androidx)\." presentation/src/main/kotlin; then
  echo "error: :presentation 引入了 Android 依赖" >&2
  grep -rnE "^import (android|androidx)\." presentation/src/main/kotlin >&2
  exit 1
fi

# App 与 UI 必须真的编译：只跑 :core/:data 单测时，界面层的编译错误会漏过去。
./gradlew :core:test :data:test :sync:test --console=plain
./gradlew :app-phone:assembleDebug --console=plain
./gradlew :app-phone:lintDebug --console=plain
