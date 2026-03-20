#!/usr/bin/env bash
cd "$(dirname "$0")"

# Build JARs if not present
if [ ! -f target/rover-headless.jar ]; then
    echo "Building..."
    nix-shell -p maven --run "mvn package -q -DskipTests"
fi

java -jar target/rover-headless.jar "$@"
