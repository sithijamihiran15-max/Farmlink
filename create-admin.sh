#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
java CompileProject.java
java -cp 'build/classes:lib/*' com.farmlink.AdminSetup
