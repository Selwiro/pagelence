#!/usr/bin/env bash
# One-shot content + unit-test gate for the Android project.
# Usage: ECDICT_CSV=/path/to/ecdict.csv tools/ci_check.sh
set -euo pipefail
cd "$(dirname "$0")/.."

python3 tools/check_strings.py
python3 tools/migrate/import_reference_data.py --check
python3 tools/build_alice_book.py --check
python3 tools/build_dolittle_book.py --check
python3 tools/build_shelf.py --check
python3 tools/build_assessment_bank.py --check
if ! python3 -c "import wordfreq" 2>/dev/null; then
  echo "skip: word-levels/word-frequencies --check (pip install wordfreq==3.0.2)"
elif [[ -z "${PAGELENCE_WEB_WORD_LEVELS:-}" || -z "${PAGELENCE_CEFR_WORDNET_TSV:-}" ]]; then
  echo "skip: word-levels --check (set PAGELENCE_WEB_WORD_LEVELS and PAGELENCE_CEFR_WORDNET_TSV)"
  python3 tools/build_word_frequencies.py --check
else
  python3 tools/build_word_levels.py --check
  python3 tools/build_word_frequencies.py --check
fi
if [[ -n "${ECDICT_CSV:-}" ]]; then
  python3 tools/build_dictionaries.py --ecdict-csv "$ECDICT_CSV" --check
else
  echo "skip: build_dictionaries --check (set ECDICT_CSV to include it)"
fi
python3 tools/build_full_dictionary.py --check
python3 tools/validate/validate_content.py
./gradlew :core:test :data:test --console=plain
