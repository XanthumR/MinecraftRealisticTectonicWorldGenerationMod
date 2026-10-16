#!/bin/sh
# One dev-server run for the C2ME comparison: speed (warm-up + three areas), then block counts in three 32x32 areas
# that must match between runs (same seed, so the same blocks), then the log's errors. rcon must be on.
# Usage: sh tools/c2metest.sh label
cd "$(dirname "$0")/.." || exit 1
rm -rf run-server/world
(./gradlew runServer > run/gradle-server.log 2>&1 &)
until grep -qE "Done \(|BUILD FAILED|Crash" run/gradle-server.log 2>/dev/null; do sleep 3; done
grep -E "Done \(" run/gradle-server.log
python tools/genspeed.py 300 300 20 > /dev/null
for a in "100 100" "-200 100" "100 -300"; do python tools/genspeed.py $a 30; done
for a in "0 0" "-448 -544" "-352 -32"; do
  python tools/blockcount.py $a minecraft:grass_block minecraft:dirt minecraft:water minecraft:air minecraft:oak_log minecraft:birch_log minecraft:spruce_log minecraft:gravel | tr '\n' ' ' > "out/c2me_$1_$(echo $a | tr ' ' '_').txt"
done
cat out/c2me_$1_*.txt; echo
echo "errors: $(grep -cE 'ERROR|Exception' run/gradle-server.log)"
grep -E "ERROR|Exception" run/gradle-server.log | grep -v RegisterSpawnPlacementsEvent | head -5
python tools/rcon.py stop > /dev/null
sleep 10
rm -rf run-server/world
