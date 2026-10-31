# Realistic Tectonic World Generation (NeoForge 1.21.1)

A Minecraft mod that simulates a planet (plates → erosion and rivers → climate and biomes) and builds terrain from it.
The world is a cylinder: X wraps east-west, the poles are a barrier. The user wants **a balance of realism and
gameplay** and play-tests in the client themselves.

## Layout
- `src/main/java/com/realtectonics/sim`: the simulation. **No Minecraft classes here**, so it runs headless in jshell and tests.
  - `Planet`: plates, collisions, subduction, rifts, hotspots, volcanoes, the supercontinent cycle (`mantleFlow`, `plan`).
    Plates keep their crust in 64-cell tiles allocated only where they hold any (`Plate.get/set`), so ~250 plates fit on Huge.
    `scaleWidth` (`PlanetSettings.mediumContinents`, "Continents: Medium-sized"): on Large/Huge the continents, plates, noise
    and climate rates stay Medium-sized and there are (w/1024)² of each (one supercontinent per Medium's worth, `homes`;
    pieces drift from their own home, no gathering; the Plates slider counts per Medium, capped at 256 by the byte plate map).
    Sim only: old planets are saved, so no format bump. `planethash.jsh` must keep normal planets' hashes; `continents.jsh` compares relief.
  - `PlanetData`: the whole run (`generate`), the save file, and `FORMAT`.
  - `Hydrology`: Rho8 routing, stream-power erosion, glaciers, lakes, volcano cones.
  - `Climate`: yearly temperature (6.5 °C/km lapse), currents, rain with moisture recycling over land (capped 800 mm, plain rain only) and
    föhn drying in the lee, monsoon to 20°, a poleward summer flow off warm seas onto east coasts (`summerFlow`), a subtropical desert belt (`band`: sinking Hadley air
    at 26°, lifted on east coasts poleward of 24° with an ocean to the east), a monsoon drawn farther by a high plateau (`plateauReach`: Tibet) that draws on wet land as well as sea and recycles its own rain,
    blocking (`barrierAhead`: trade air below a pass-less range's crest less U/N turns aside and streams poleward along it, the jets
    east of the Andes and Rockies), monsoon regimes (`monsoonRegime`: a continent covering most latitudes poleward, i.e. Asia,
    reverses the winds, so no year-round trades and no warm western current there: Arabia dry), the eastern flank of the highs
    (`eastFlank`: equatorward coast winds off west coasts at 15-40°, under a 1 km marine layer, stop poleward flows: Baja, Sonora),
    the mass elevation effect (`massElevation`: broad plateaus win back 30% of the lapse cooling: Tibet, the Puna), lake-effect rain. Rates are per cell of a 1024 planet, converted per km (`per()`, `REFERENCE_WIDTH`):
    any new per-cell rate must go through that, or Large planets dry out; check with `tune.sh -R-Dwidth=2048` (a second pass once lakes exist). Checked against the real Köppen map on the Earth planet
    (`tools/koppen.py`): main groups 57% -> 75% agree, all 30 classes 29% -> 41.5%; tune with `tools/tune.sh -R-D...` (region numbers from `kregion.py`); random planets now 12-15% desert climate (Earth ~14%), and seasons: `amplitude` (half the summer-winter range) and `summerRain` (rain share in the warm half); `warmMean`/`frozenShare` turn them into melt and frost.
  - `Biome`, `Rock`, `Volcano`, `PlanetSettings`.
  - `EarthData`, `Planet.earth()`: the "Earth" planet (`PlanetSettings.earth`, world type "Tectonic Earth"): real elevation, PB2002 plates
    and boundary steps, Natural Earth lakes, from `src/main/resources/realtectonics/earth/` (built by `tools/earthdata.py`, in the
    planet's equal-area projection). No plate motion; Hydrology skips its made-up shaping, fills fake hollows (`drainEarth`) and
    erodes 3 passes. jshell needs `build/resources/main` on the class path for it (`tools/earth.jsh`).
  - `Terrain`: per-block columns (`Column`: ground/water/lava/ice/biome/fold/volcanic/rock/cover).
    `surface()` is the land before rivers; `column()` adds rivers, lakes, cones, ponds and the rest.
    `relaxedColumn()` (what the game uses, format 29+) runs block-scale rockfall over cached 256-block tiles (RELAX_TILE; output is identical for any tile size, `tools/relaxhash.jsh` proves it) (computed once, rows in parallel, early stop); seamless
    because updates are synchronous and the margin exceeds the rounds. From format 33 it first cuts gullies by water flow (`gullies()`: D8 routing, from 41 spread to all lower neighbours and cut only above the local mean, area gathered from GULLY_REACH blocks upslope only, so tiles stay seam-exact; `hillshade.jsh` shows them). Check it with `tools/rockfall.jsh` (seams 0) and `tools/overshoot.jsh` (rubble never above its source: 0). Rate per neighbour must stay <= 1/16 or it grows spires (format 29 bug).
- `src/main/java/com/realtectonics/mod`:
  - `TectonicChunkGenerator`: blocks, surfaces, snow, beardifier, cave sealing, post-decoration snow pass. `moddedGround()`
    lays BOP's own ground (podzol, coarse dirt, orange sand, terracotta bands, gravel) in its biomes, over plain grass and sand only.
  - `TectonicBiomeSource`: maps sim biomes to vanilla biomes, or with the `biomesoplenty` pack to realistic Biomes O' Plenty
    biomes: `biomesoplenty` one per climate (`biomesOPlenty()`), `biomesoplenty_varied` several per climate picked by
    `Terrain.variety` in ~340-block patches (`varied()`). The preset says `"pack": "auto"` (= varied when BOP is loaded), saved resolved at world creation; worlds without the field stay
    vanilla. Biome mods register into vanilla's multi-noise source, which ours replaces, so they appear only through this table.
  - `RealTectonics`: loads or saves `planet.bin` in the world folder.
  - `client/`: `PlanetScreen` sliders, `MapScreen`.

## The user's principle: simulate, don't stamp
The user wants **most things simulated**: features should come out of the planet simulation (plates, erosion, lakes,
climate), not be stamped into blocks afterwards. Stamped features cut through whatever is there; for example, the
format 21 calderas sliced mountains. Prefer to put a feature into the sim's cells (`Hydrology`/`Planet`) and let
routing, lakes and erosion act on it; keep per-block work for detail finer than a cell (32 blocks). Example: format 22
calderas collapse in `Hydrology.collapseCalderas` after erosion, never below the pre-volcano land, and their lakes
come from `markLakes`.

## Rules that keep old worlds intact
- Every change to how terrain looks goes behind a **format flag**:
  - Bump `PlanetData.FORMAT`, add a line to its comment, and add a `private final boolean x; // ...: format N` field in `Terrain`.
  - Set it as `x = data.format >= N` and gate the new code on it. Old worlds keep their look and get no chunk seams.
  - Current format: **75** (flat-topped fault blocks behind rolling-back trenches, `Planet.backArc`). Formats 65-71 (rain thinning with height, young rifts, side valleys, ores by province) were
    tried and reverted with git at the user's request: planets saved at 65-71 no longer load (`load()` refuses them);
    the next format is 76. `docs/FORMATS.md` says what each format added and why (most fix a bug the user saw);
    read its "recurring causes" before fixing an unnatural spot: stamps, on/off thresholds, unstable updates, tile seams.
- Noise and grid spacings must be **powers of two** (the planet wraps; the seam test fails otherwise).
- Water must never spill onto lower dry ground. Waterfalls dropping into lower water are fine. Every new water or terrain feature gets a spill check.
- `PlanetSettings` fields with codec defaults keep old worlds loading. `sameSimulation()` says which settings need a re-run.

## Scale facts (the answers to "why does X look like that")
- Cell = 32 × zoom blocks. Land is 15 m per block at Mountains 1.0.
- At Medium size and zoom 1, a block is about 1.2 km wide, so height is stretched about 80×. Huge with 4× is about 5×.
- So "thin, needle mountains" means scale first. World scale and the Mountains slider are the levers.
- Against real terrain (`tools/realdem.py`, Medium, 1.22 km grid): game mountain belts have mean slope ~0.7° vs 4.5-10° real,
  relief within 10 km ~150 m vs 700-1500 m, spectral slope 3.6-4.4 vs 2.4-3.1 (too little 1-10 km valley relief); the
  highest game plateaus sit at ~7 km median vs Tibet's ~5 km.
- Temperature falls about 0.1 °C per block at Mountains 1.0.

## Workflow and tools (scripts in `tools/`, renders in `out/`; `build/` is Gradle's and may be wiped)
- Speed: `tools/genab.sh` (whole pipeline on the dev server, ~92 chunks/s; Distant Horizons takes ~45% of the CPU there), `relaxhash.jsh`
  (terrain area time and an output hash that must not change in pure speed work). Profile with `jcmd <pid> JFR.start` or the
  `jdk.jfr.Recording` API inside jshell (the jshell JVM is killed, so -XX:StartFlightRecording never dumps).
- Compile: `./gradlew compileJava -q`. Tests: `./gradlew test -q`. Jar: `./gradlew build -q`.
- **`Terrain.column(double, double, boolean)` is ~7,600 bytes of bytecode (lake shores moved to `lakeShore`, output unchanged); the JIT never compiles a method over 8,000**
  (HotSpot's DontCompileHugeMethods), and the generator would then run interpreted, many times slower. Put new features in
  their own methods; check the size with `javap -c -p` (largest offset) after adding to it.
- Speed work must keep `relaxhash.jsh` hashes: format 46 `acba683bd8c9b0cb` (`-Dfmt=46`), the file's own format
  `d4fc4b2912dbcb9`. `relaxprof.jsh` shows where the time goes. Measured dead ends: per-thread memos of river splines and
  pond sites (no gain), tile work on half the cores (smooth but slow).
- jshell: `"/c/Program Files/Java/jdk-21.0.12/bin/jshell" -R-Xmx6g [-R-Dkey=val] --class-path build/classes/java/main tools/X.jsh`
  - Most scripts build seed 777 at `PlanetSettings.DEFAULT` with `new Terrain(d, 777, 1, 1, 2)`.
  - Set `d.format = N` before building a `Terrain` to compare before and after.
- Multi-line edits: a Python script that starts with `exec(open('tools/edit_glacier.py').read().split("C = ")[0])`, then calls `edit(path, [(old, new), ...])`.
  - Each `old` must occur exactly once.
  - **Match whole lines including trailing comments.** Inserting after a field without its comment moves the comment onto the new line.
  - Write these scripts with the Write tool; heredocs break on quotes.
- **Look at renders before claiming terrain looks right.** `tools/README.md` lists the maintained checks (run them after any
  terrain change), the renderers, and the recipe for the user's "is this natural?" screenshots (player position from
  `nbt.py` → `topmap.jsh` → `uview3d.jsh` → `uprofile.jsh` across the spot). The other ~190 scripts are one-offs.
- Spill checks must count glacier ice as a wall (`max(ground, water, ice)`). `falls.jsh` baseline is 4 column edges on seed 777 since format 62 (Earth-like plate sizes: a new planet), 6 since format 52 (braided rivers keep a channel), 12 since the blocking, monsoon-regime, coast-wind and plateau-warmth climate changes (4 and 23 before; rivers move with the rain) (most along river reaches whose banks stand 1-2
  blocks under the water: open, perched banks); `lakeleak.jsh` 0.
- Dev server, for in-game checks:
  - It runs in `run-server/`, because Sodium and Iris in `run/mods` crash a server.
  - Append `enable-rcon=true`, `rcon.password=dev` and `rcon.port=25575` to `run-server/server.properties`.
  - Start it with `./gradlew runServer > run/gradle-server.log 2>&1 &` and wait for `Done (`.
  - Send commands with `python tools/rcon.py "cmd"` (`forceload add`, `execute if block ...`). `tools/topblock.py x z` reads a column's top block.
  - Afterwards: `stop`, remove the rcon lines, and `rm -rf run-server/world`.
  - **Never kill the user's client**; only stop processes I started.
- Dev mods: Distant Horizons and Biomes O' Plenty (with TerraBlender, GlitchCore; pinned by Modrinth version id, since
  version numbers are shared with the Forge builds) come from `build.gradle` runtimeOnly, and Streams Reflowing (off: commented out in `build.gradle`; its own streams carved over our terrain, ~21% of
  generation CPU, about half the generation speed on land; `vanillaWaterFlow=false` in `run*/config/streamsreflowing.properties`, or our rivers' one-block steps spread as sheets). Sodium 0.8.12 and Iris 1.8.14 sit in `run/mods`. Don't add them to both.

## Style
- Ponytail: minimal code, reuse helpers in `Terrain` (`noise`/`fine`/`cubic`/`bilinear`/`smoothstep`/`hash01`/`wrapBlocks`/`cell`).
  - Mark shortcuts with `ponytail:` comments, and leave one runnable check per non-trivial feature.
- Comments explain the geology or the reason ("rubble into a river valley is carried away"), matching the existing dense, physical style.
- Constants are `static final` with a comment giving units and meaning.
- Replies to the user are short and plain (casual English, sometimes with typos). They can't see tool output; quote numbers.

## Roadmap
The user's "cons" list, in their order: 1-4 biome transitions, foothills and folds, scree, ponds (done); 5 decorated
rivers and oceans (skipped: they'll use other mods); 6 calderas (done); 7 glacier tongues (done, 23); 8 strike-slip
faults (done, 32; rift escarpments not done). 9 ridges and trenches (done, 35; no axial valleys, abyssal hills or outer rise yet). Also done on request: simulated gullies (33), folded strata (34), varied volcanoes and bigger volcanic islands (36), varied beaches and no gully pits (37), lake-effect rain, green riverbanks and ice-free active craters (38).
10 better plates: supercontinent cycle and ragged continents (39). Wet-country gorges (74), back-arc fault blocks (75; rifts and collapsing old ranges could feed the same field). Flowing-looking glaciers with bare ice and moraines (40). No gully comb on even slopes (41). Volcanoes as eroded as the sim left them, smooth scars, lakes one biome (42). Speed: plain sqrt distances, caves sealed from water only near it (43). Open: rift escarpments, ridge axial valleys, abyssal hills; climate's wettest cell reaches ~30,000 mm (orographic rain, unexamined).
The auto-memory `project-roadmap.md` keeps the user's preferences and their reference worlds.
