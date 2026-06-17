#!/usr/bin/env bash
# Launches Snake 3D Deluxe via the JavaFX Maven plugin.
set -euo pipefail
cd "$(dirname "$0")"
exec mvn javafx:run
