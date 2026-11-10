#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
version=3.9.9
base="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/apache-maven-$version"
if [ ! -x "$base/bin/mvn" ]; then
  mkdir -p "$base"
  archive=$(mktemp)
  trap 'rm -f "$archive"' EXIT HUP INT TERM
  url="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.tar.gz"
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$url" -o "$archive"; else wget -q "$url" -O "$archive"; fi
  # Verify against the repository's published SHA-512 before extracting.
  if command -v curl >/dev/null 2>&1; then checksum=$(curl -fsSL "$url.sha512"); else checksum=$(wget -qO- "$url.sha512"); fi
  if command -v sha512sum >/dev/null 2>&1; then
    printf '%s  %s\n' "$checksum" "$archive" | sha512sum -c - >/dev/null
  else
    actual=$(shasum -a 512 "$archive" | cut -d ' ' -f 1)
    [ "$actual" = "$checksum" ] || { echo 'Falha na verificação do Maven.' >&2; exit 1; }
  fi
  tar -xzf "$archive" -C "$base" --strip-components=1
fi
exec "$base/bin/mvn" "$@"
