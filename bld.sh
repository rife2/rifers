#!/usr/bin/env sh
java -jar "`dirname "$0"`/lib/bld/bld-wrapper.jar" "$0" --build rifers.RifersBuild "$@"