#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "FarmLink - local Java website"
if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
  echo "ERROR: Install JDK 17 or newer and make both java and javac available in your PATH." >&2
  exit 1
fi
if [[ ! -f lib/hsqldb.jar || ! -f web/index.html ]]; then
  echo "ERROR: Project files are missing. Extract the entire ZIP and run this script inside FarmLink/." >&2
  exit 1
fi
mkdir -p build/classes data
find src/main/java -name '*.java' > build/java-sources.txt
echo "Step 1: Compiling Java backend..."
if ! javac --release 17 -encoding UTF-8 -cp lib/hsqldb.jar -d build/classes @build/java-sources.txt; then
  echo "ERROR: Java compile failed. Read the message above." >&2
  exit 1
fi
echo "Step 2: Starting FarmLink. Open the URL printed below in your browser."
java -cp 'build/classes:lib/*' com.farmlink.Main
