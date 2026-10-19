#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GUI_DIR="$ROOT_DIR/JavaFXGUI"
OUT_DIR="$GUI_DIR/out"
DB_SETUP_SOURCE=""

for candidate in "$ROOT_DIR/GUI/DbSetup.java" "$ROOT_DIR/GUI/dbSetup.java"; do
  if [[ -f "$candidate" ]]; then
    DB_SETUP_SOURCE="$candidate"
    break
  fi
done

if [[ -z "$DB_SETUP_SOURCE" ]]; then
  echo "Database configuration source not found." >&2
  echo "Create the locally ignored GUI/dbSetup.java file (or GUI/DbSetup.java) with your database credentials." >&2
  echo "The launcher does not create or commit database credentials." >&2
  exit 1
fi

# Set JAVAFX_LIB if JavaFX is installed somewhere other than the common paths.
JAVAFX_LIB="${JAVAFX_LIB:-}"
if [[ -z "$JAVAFX_LIB" ]]; then
  for candidate in \
    "$ROOT_DIR/javafx-sdk-25/lib" \
    "$ROOT_DIR/../javafx-sdk-25/lib" \
    "$HOME/javafx-sdk-25/lib"; do
    if [[ -f "$candidate/javafx.controls.jar" ]]; then
      JAVAFX_LIB="$candidate"
      break
    fi
  done
fi

if [[ -z "$JAVAFX_LIB" || ! -f "$JAVAFX_LIB/javafx.controls.jar" ]]; then
  echo "JavaFX SDK not found. Install JavaFX 25 and run again." >&2
  echo "Or set JAVAFX_LIB to its lib directory, for example:" >&2
  echo "  JAVAFX_LIB=/path/to/javafx-sdk-25/lib ./run-gui.sh" >&2
  exit 1
fi

cd "$GUI_DIR"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

DB_SETUP_COMPILE_SOURCE="$DB_SETUP_SOURCE"
if [[ "$(basename "$DB_SETUP_SOURCE")" == "DbSetup.java" ]]; then
  DB_SETUP_COMPILE_SOURCE="$OUT_DIR/dbSetup.java"
  sed 's/\bDbSetup\b/dbSetup/g' "$DB_SETUP_SOURCE" > "$DB_SETUP_COMPILE_SOURCE"
fi

javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "postgresql-42.2.8.jar" \
  -d "$OUT_DIR" \
  *.java "$DB_SETUP_COMPILE_SOURCE"

exec java \
  --enable-native-access=javafx.graphics \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$OUT_DIR:.:postgresql-42.2.8.jar" \
  DatabaseApp
