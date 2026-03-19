#!/bin/bash
# This script removes the dev-private-key.pem from git history.
# CAUTION: This rewrites git history. All collaborators must re-clone after this.
# Run this only once, before the v1.0 public release.

set -euo pipefail

echo "=== Removing dev-private-key.pem from git history ==="
echo "WARNING: This rewrites history. All collaborators must re-clone."
read -p "Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi

# Install git-filter-repo if not available
if ! command -v git-filter-repo &> /dev/null; then
    pip install git-filter-repo
fi

git filter-repo --invert-paths --path services/api-gateway/src/main/resources/META-INF/resources/dev-private-key.pem --force

echo "=== Done. Now force-push all branches: ==="
echo "git push origin --force --all"
echo "git push origin --force --tags"
echo ""
echo "All collaborators must re-clone the repository."
