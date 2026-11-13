# tools

Run jshell scripts from the project root after `./gradlew compileJava -q`:

    "/c/Program Files/Java/jdk-21.0.12/bin/jshell" -R-Xmx6g [-R-Dkey=val ...] --class-path build/classes/java/main tools/X.jsh

Most build seed 777 at `PlanetSettings.DEFAULT` (`new Terrain(d, 777, 1, 1, 2)`); images go to `out/`.
Only the scripts below are maintained. The rest are one-off experiments from past formats (see docs/FORMATS.md); read
one before trusting it.

## Checks (run after any terrain change; expected result in brackets)
| Script | Checks |
|---|---|
| `rockfall.jsh` | Tiles agree where they overlap (seams 0), tile time, leak edges (0); default spot is seed 777's highest ice-free peak (spawn moves with the climate, so a fixed spot can end up on flat land) |
| `overshoot.jsh` | Rockfall never piles rubble above its source (0) |
| `lakeleak.jsh` | Lake water never next to lower dry ground (seed 777: 1, a lake stepping into its own outlet river, at 42 1309 since format 97; seeds 1-4: 0); the thorough one |
| `cenotes.jsh` | Cenotes (format 96) over an area against format 95: new pools by size, water and rim, rubble beside, spilling edges and the new ones (0), pools with two levels (0): `-Dplanet -Dseed -Dcx -Dcz -Dn` |
| `oases.jsh` | Oases (format 95) over the whole planet round every `oasisField` cell: spilling edges round each (0) and pools not brim-full (0): `-Dplanet -Dseed -Dfmt` |
| `falls.jsh` | Water spilling onto dry land (baseline 2 since format 97's natural cones reshaped seed 777, 16 on the planet since colliding land stays with its plate, 5 since format 79; 12 column edges since the latest climate changes reshaped seed 777, 4 and 23 before; perched river banks, open) |
| `pondscan.jsh` | Pond spills (0) |
| `calderas.jsh` | Caldera lake spills (0); every crater's lava or rainwater pool spilling (0 at format 83-84, 3 at 82, 1 from 85: a river's perched bank over a blown summit; from 97's new planet 6, a river's perched bank at 15675 5297; `-Dfmt=N`); from format 81 supervolcanoes (seed 777: 3), lava domes (46, 47 from 85, 57 from 97) and domes in a lake or lava (0; 1 from 97, as at 96 on that planet) |
| `maars.jsh` | Format 88 maars: lakes, dry, breached (seed 777: 19, 4, 105; 13, 7, 118 on format 97's planet), maar lake spill edges (0) and posts after rockfall (4; 0 from 97); `-Dplanet -Dfmt` |
| `travertine.jsh` | Format 91 travertine terraces: sites (seed 777: 7, first at x 22352 z -1408), pool columns, spill edges after rockfall (0); `-Dplanet -Dfmt` |
| `deposits.jsh` | Format 92 ore deposits: cells per kind (seed 777: porphyry 46, kimberlite 18), the deposits map `out/deposits_map.png`, vertical slices through a porphyry stock, a kimberlite pipe and an iron field (`out/ore_*.png`); `-Dplanet -Dfmt` |
| `orebudget.jsh` | Format 92 ore budget: deposit ore per land chunk averaged over the planet, plus vanilla's thinned ores, against vanilla (seed 777, 4 s: coal 0.74x, iron 1.42x, copper 0.39x, gold 0.47x, redstone 0.50x, lapis 0.40x, diamond 0.47x; caves not cut); `-Dplanet -Dfmt` |
| `orecount.py` | Dev server (rcon on): ores per chunk over an n x n chunk square, `python tools/orecount.py x z n` (destroys them: a test world only). A/B a format by rewriting the first int of `world/realtectonics/planet.bin` (gzip) and deleting `region/` |
| `relief.jsh` | The planet screen's 3D map (`sim/Relief`) as `out/relief_whole.png` and `out/relief_close.png`: `-Dplanet=out/x.bin`, class path `build/classes/java/main;build/resources/main` |
| `cavegen.py`, `cavecheck.py`, `caverender.py` | Format 98 caves. `cavegen.py` (dev server, rcon on) generates three seed 777 squares and stops the server: flat land with rivers (x 256 z 768), mountains (x 0 z 256), a dry plain (x 512 z 768), 256 blocks each. `python tools/cavecheck.py <world> 256 768 256 0 256 256 512 768 256` counts cave mouths, water columns with cave air within 12 blocks under the bed, air beside water and cave air per column (format 97: mouths 17, 33, 16, thin 16, 0, 4; format 98: mouths 0, 34, 0, thin 0, cave air the same). `caverender.py <world> x0 z0 size prefix [z [y0 y1]]` draws the mouths on a map and a cut |
| `blown.jsh` | Format 85: generates seed 777 into `out/p85_777.bin` and lists the blown volcanoes (15) in block coordinates with their debris lobes, for `uview3d`/`hillshade` |
| `biomeedge.jsh` | Desert next to snow (0%) |
| `tune.sh` | Earth climate vs the real Köppen map in one go (`earthclim.jsh` → `koppen.py` → `kregion.py`); pass `-R-D` knobs. `earthclim.jsh -Dseed=N` + `kdry.py` give a random planet's desert shares, `kshares.py` Earth's per region, `climrow.jsh` rain along one latitude, `climtime.jsh` Huge simulation time, `randmap.jsh` a random planet's map and ice share (`-Dwidth`, `-Dmedium=true`); `-R-Dwidth=2048` on `tune.sh` checks planet size doesn't change the climate |
| `faultblocks.jsh` | Format 75: share of land back-arc extension stretched and the strongest spots in blocks (seed 777: 2.1%, 0.5% over half) |
| `landforms.jsh` | One example spot of each tectonic landform on a planet (fold, volcano, plateau, trench, arc, ridge, shelf, transform), in blocks and cells (`-Dplanet -Dseed`) |
| `seafloor.py` | Shaded relief of a planet's cells with the sea drained, like a bathymetry map: `python tools/seafloor.py planet.bin out.png cellX cellY [width] [height]` (`trench` for the deepest sea floor) |
| `plateaus.py` | High, flat land with steep edges away from volcanoes: `python tools/plateaus.py planet.bin [count]` |
| `horstspots.jsh` + `annotate.py` | Labels a `uview3d.jsh` render of fault blocks (format 76+): `horstspots.jsh` (same `-Dplanet -Dseed -Dcx -Dcz -Dn`) finds the highest horst and the scarp and graben next to it as image pixels; `python tools/annotate.py in.png out.png "title" spots.txt` draws labelled arrows |
| `boxspill.jsh` | Water next to lower dry ground in one box (`-Dplanet -Dfmt -Dcx -Dcz -Dn`): spill check for a new feature's own area, run at the old and new format |
| `wetgorges.jsh` | Format 74: river cells a wet gorge cut over 3 blocks and the deepest spots in blocks, for `uview3d.jsh` (seed 777: 277 cells, deepest 36; 230 dry canyon cells) |
| `planethash.jsh` | Hash and time of a whole simulated planet (`-Dwidth`, `-Dmedium=true`, `-Dseed`): simulation refactors must keep it (seed 777 since format 97's natural cones and tuyas: 512 `926a24c0efe276c`, 1024 `188337843d740cbb`, 2048 `28180ac1a048110d`; before, since colliding land stays with the plate that held it: `55f9494524cd676d`, `654fdc58787c77d6`, `319a98c38651d6e`) |
| `continents.jsh` | Land share above 500/1000/2000/3000 m, top, rain, volcanoes, and a plate map (`-Dwidth`, `-Dmedium=true`, `-Dseed`): Medium-sized continents should match Medium (>2000 m: Medium seeds 1-6 mean 4.6%, Large medium-continents 5 seeds 5.2%) |
| `stutter.jsh` | Render-thread stalls while 4 worldgen threads pull new tiles: run with and without `-R-XX:+UseZGC` (G1: 10 frames over 10 ms, max ~20 ms; ZGC: 0, max 6.4 ms) |
| `baselevel.jsh` | Format 46: share of columns base level raises over an area, and by how much (any planet: `-Dplanet -Dseed -Dzoom -Dcx -Dcz -Dn`) |
| `canada.jsh` | Format 45: snowy taiga, snow cover and frozen water over a lat/lon box of the Earth planet, formats 44 vs 45 |
| `volcvar.jsh` | Volcano islands bigger than the metre profile (ratio > 1.5), lobes and variety between cones (> 0.1) |
| `wetness.jsh` | Lake-effect rain near lakes (> 0) and fewer dry biomes beside big rivers at 38 than 37 |
| `cycle.jsh` | Plate snapshots through the run and how gathered the continents are (`-Dsteps=600 -Devery=100` shows a full cycle) |
| `icelook.jsh` | Glacier cover and thickness 39 vs 40, bare ice and moraines (reference numbers in the script) |
| `comb.jsh` | Share of an even slope the gully pass changes, 40 vs 41 (reference in the script) |
| `lakebiome.jsh` | Lake columns with the river biome, 41 vs 42 (want all at 42) |
| `faults.jsh` | Fault cells lie in a valley (< 0 m), scarp at the fastest land fault (> 0) |
| `genbench.jsh` | 8-thread chunk throughput, `-Df1 -Df2` formats; a new format should not be slower |
| `coltime.jsh` | µs per column |
| `valleys.jsh` | Format 100 valleys: builds an area at `-Dfa` (99) and `-Dfb` (100), build times, histogram of ground lowered, `out/valleys_cut.png` (red lowered, blue raised); `-Dcx -Dcz -Dn -Dplanet` |
| `relaxhash.jsh` | Time for a 2048² area through the rockfall tiles and a hash of the result: unchanged by pure speed work (`-Dfmt`) |
| `relaxprof.jsh` | Where `relaxedColumns` spends its time (JFR inside jshell): own time by line and time inside our methods |
| `chunkbench.jsh` | Chunk-generation stand-in: 16 biome lookups then 18x18 columns per chunk; `-Dcached=true` checks `biomeAtCached` against `biomeAt` (0 mismatches) |
| `jfrstutter.py` | Stutter report for a JFR recording of the running game (`jcmd <pid> JFR.start settings=profile duration=60s filename=x.jfr`) |
| `genspeed.py`, `genab.sh` | Whole-pipeline chunks/s on the dev server (rcon on): one area, or warm-up plus three fresh areas |
| `c2metest.sh label` | Speed plus block counts in three land areas and the log's errors, to compare a mod in `run-server/mods` with none (C2ME 0.4.0-alpha.0.123: 181 vs 91 chunks/s, terrain identical, trees differ slightly) |
| `needles.jsh`, `walls.jsh`, `screecount.jsh` | Steep spots, lowland walls, scree share |

## Looking at terrain
| Script | Shows |
|---|---|
| `view3d.jsh` | Isometric block view, seed 777 (`-Dcx -Dcz -Dn -Dv -Dname -Dfmt`) |
| `uview3d.jsh` | The same for any planet file (`-Dplanet -Dseed -Dms -Dzoom -Drw`, `-Dstart=lat,lon` for an Earth world's start place, e.g. China `35,105`); salt white, scree tan |
| `topmap.jsh` | Fast top-down map of any planet, walls red (`-Dst` stride for big areas) |
| `hillshade.jsh` | Top-down relief of the game terrain (relaxedColumn): gullies, scarps |
| `strata.jsh` | Cross-section of rock beds through a fold belt |
| `uprofile.jsh` | Two formats side by side along a line: ground, water, ice, cover |
| `seafloor.jsh` | Whole-planet sea floor from cells (ridges pale, trenches purple) and a depth histogram; from format 35 cells 6-8 km deep exist |
| `spot.jsh`, `spotdepth.jsh`, `folds.jsh`, `foldsdiff.jsh`, `seasonsmap.jsh` | Older top-down views |

## Real terrain
- `python tools/earthdata.py`: rebuilds the Earth planet's data in `src/main/resources/realtectonics/earth/` (elevation, PB2002
  plates and steps, lakes); `earth.jsh` generates it and writes the game's map preview (`out/earth_map.png`). Class path
  `"build/classes/java/main;build/resources/main"`; `lakeleak.jsh -Dplanet=out/earth.bin` checks its lakes (0).
- `gamedem.jsh` then `python tools/realdem.py`: the game's mountain boxes (`-Dtarget=2500` for ordinary belts, default
  the highest) against the Alps, Himalaya, Andes, Rockies and Appalachians from public AWS terrarium tiles, on the same
  1.22 km grid: heights, slopes, local relief, spectral slope; hillshades in `out/dem/compare.png`.

- `climexport.jsh` then `python tools/koppen.py`: the Earth planet's simulated climate classified by Beck et al. 2018's own
  Köppen rules and compared with their real 0.5° map: agreement for main groups (71.3%), second level (47.8%), all 30
  (37.7%), by region, confusion, maps in `out/koppen/`.

## The user's worlds
- `python tools/nbt.py "run/saves/<world>/level.dat"`: seed, planet settings (width, mountain_scale, river_width, zoom
  if not 1) and the player's last position and yaw/pitch. Yaw 0 faces +z (south), 90 faces -x, 180 north, -90 east.
- Load `run/saves/<world>/realtectonics/planet.bin`; its first int is the format the world uses.
- Recipe for "is this natural?" screenshots: player position → `topmap.jsh` around it → find the feature → `uview3d.jsh`
  to match the screenshot → `uprofile.jsh` across it (`-Dfa` old vs `-Dfb` new format) → reflect into `Terrain`
  (`cubic`, `surface`, `nearestLake`, `warpU/V` are private; `setAccessible(true)`) to find which step makes it.
- `trace_instrument.py`: temporary prints in Terrain (back it up first, restore after).

## Editing and the dev server
- `edit_glacier.py`: `exec(open('tools/edit_glacier.py').read().split("C = ")[0])`, then `edit(path, [(old, new), ...])`;
  each `old` must occur once and match whole lines including trailing comments. The `edit_*.py` files are past edits.
- `rcon.py`, `topblock.py`, `whatblock.py`, `probe.py`: talk to the dev server (see CLAUDE.md).
- `blockcount.py x z block...`: counts blocks in a 32x32 area on the dev server (swaps to a marker and back; test worlds only).
