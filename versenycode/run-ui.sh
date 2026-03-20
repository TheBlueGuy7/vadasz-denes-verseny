#!/usr/bin/env bash
cd "$(dirname "$0")"

# Build JARs if not present
if [ ! -f target/rover-ui.jar ]; then
    echo "Building..."
    nix-shell -p maven --run "mvn package -q -DskipTests"
fi

# NixOS needs native libs on LD_LIBRARY_PATH for JavaFX
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:+$LD_LIBRARY_PATH:}$(nix-build --no-out-link '<nixpkgs>' -A libx11)/lib:$(nix-build --no-out-link '<nixpkgs>' -A libxxf86vm)/lib:$(nix-build --no-out-link '<nixpkgs>' -A libGL)/lib:$(nix-build --no-out-link '<nixpkgs>' -A gtk3)/lib:$(nix-build --no-out-link '<nixpkgs>' -A glib.out)/lib:$(nix-build --no-out-link '<nixpkgs>' -A libxtst)/lib"

java -jar target/rover-ui.jar "$@"
