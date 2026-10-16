#!/bin/sh
# Whole-pipeline generation speed on a fresh dev server (rcon must be enabled in run-server/server.properties):
# one warm-up area, then three measured ones, far apart so nothing is shared. Prints chunks/s per area.
# Usage: sh tools/genab.sh   (stops the server and deletes run-server/world afterwards)
cd "$(dirname "$0")/.." || exit 1
rm -rf run-server/world
(./gradlew runServer > run/gradle-server.log 2>&1 &)
until grep -qE "Done \(|Exception|BUILD FAILED|Crash" run/gradle-server.log 2>/dev/null; do sleep 3; done
python tools/genspeed.py 300 300 20 > /dev/null
for a in "100 100" "-200 100" "100 -300"; do python tools/genspeed.py $a 30; done
python tools/rcon.py stop > /dev/null
sleep 10
rm -rf run-server/world
