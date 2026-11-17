#!/usr/bin/env bash
set -e

REPO_URL="${1:-https://github.com/akashdan/PeakCut_App.git}"

echo "Pushing PeakCut commit to $REPO_URL ..."
git init
git checkout -B main
git add .
git commit -m "feat: PeakCut Web & Android Material 3 app with GitHub Actions APK builder" || true
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"
git push -u origin main

echo "Done! Open https://github.com/akashdan/PeakCut_App/actions to download your compiled APK."
