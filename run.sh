#!/usr/bin/env bash
# Launches Snake 3D Deluxe on Linux/macOS.
set -e
cd "$(dirname "$0")"
mvn javafx:run
