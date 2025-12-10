#!/bin/bash
set -e

echo "=== Building Docker images (with in-container build) ==="
DOCKER_BUILDKIT=1 docker-compose build

echo "=== Starting services ==="
docker-compose up -d

echo "=== Done! ==="