# Changelog

## Unreleased

- **Caldera rims**: no more thin walls of spires round a caldera; the rim is a crest over the caldera's cliff with the
  volcano's flank falling away outside (new worlds only).
- **Natural maar and crater shapes**: maars and volcanoes' summit craters are drawn out and scalloped, with frayed
  edges, instead of perfect circles (new worlds only).
- **Taller ridge mountains**: where continents are still colliding, the range keeps rising while rivers cut it, and the
  thickened crust spreads a few cells inland, so young ranges stand 3-7.7 km high with snow and glaciers instead of
  wearing down to ~1.2 km hills; old ranges still wear down. Valley relief is capped so tall ranges read as massifs
  with ridges, not spires (new worlds only).
- **Deeper valleys**: on Small and Medium planets valleys cut 1.5x as deep into mountain belts and the ridges stand
  1.5x as high over them, so ranges read as peaks and branching valleys instead of rounded swells (new worlds only).
- **Back-arc seas** (like the Sea of Japan): where the crust behind an ocean trench is stretched hard, the volcanic arc
  tears off its continent and drifts out toward the trench, and new sea floor opens behind it, leaving an island chain
  with a young sea between it and the mainland (new worlds only).
- **Land no longer shrinks as the planet ages**: colliding continents used to eat each other for hundreds of steps
  (30% land to 17% over 1200 steps). Volcanic arcs behind ocean trenches now build new crust that rises as island
  chains (Japan, the Aleutians) and welds onto continents in collisions, and colliding continents brake much harder:
  ~31% land at the default 300 steps, ~27% at 1200 (new worlds only).
- **Steps slider up to 1200** (was 600), for older, more worn planets.
- **Tilted fault blocks** (like the Sierra Nevada): where the crust behind a trench starts to stretch, a block tilts up
  with a long back slope toward the trench and a steep scarp over the basins (new worlds only).
- **Dome mountains**: a few oval domes deep inside continents, far from any plate edge (like the Black Hills or
  the Adirondacks): a forested granite core drained by radial valleys, ringed by hogback ridges whose steep faces look
  in toward it (new worlds only).
- **Hotspot volcanoes spaced out**: a plate drifting slowly over a hotspot builds one bigger shield volcano instead
  of a heap of cones 30-60 blocks apart; the next one rises at least 128 blocks on (new worlds only).
- **No crater shafts on slopes**: a volcano on a steep hillside keeps a crater open downhill instead of a round
  pit hundreds of blocks deep into the hill (new worlds only).
- **Rivers rise as creeks**: a river starts as a narrow creek that widens downstream, with uneven banks, instead
  of a full-width round end in a smooth bowl (new worlds only).
- **Rivers reach their lakes**: a river flowing into a lake no longer stops 10-15 blocks short of the water on a
  dry flat (new worlds only).
- **Ragged rift coasts**: where a supercontinent breaks apart, the new coasts get capes, bays and islands like
  its outer coasts, instead of smooth curves (new worlds only).
- **Badlands blend into desert**: no more square patches of desert sand inside the badlands, and their edges no
  longer step along the planet's cells (new worlds only).
- **No pine forests on badlands plains** with William Wythers' Overhauled Overworld: its mesa-top pines grew on any
  badlands between y 100 and 150, which on our terrain is most of them. Nor its sweet berry thickets on the red sand.
- **No mud on windswept hills** with WWOO: its ground repainting (volcanic mud and basalt on windswept hills, crusts,
  snow and scree spreads, river carvers, salt lakes, tepuis) is left out; it was made for its own terrain. Its trees,
  plants and decorations stay.

## 0.1.3 (2026-11-12)

New worlds only; existing worlds keep their terrain.

- **Vanilla Backport support** (NeoForge 1.21.1), and the same biomes from the game itself on Fabric 26.2:
  - pale gardens in about a fifth of dark forests;
  - dappled forest in about a fifth of temperate woods (Vanilla Backport only; 26.2 has none);
  - sulfur caves deep under volcanic arcs and volcanoes, their floors and ceilings crusted with sulfur and
    cinnabar, so sulfur pools and spikes grow;
  - Vanilla Backport's leaf litter, bushes, wildflowers, fallen trees and mobs appear in our biomes too.
- **Valleys in the mountains** (Small and Medium planets): high land is now shaped by water, with ridges, spurs and
  side valleys branching like a tree down to the lowlands, instead of smooth swells with noise on top. World
  creation takes a few seconds longer (Medium: about 8 s for the terrain). Large and Huge keep their mountains.
- **Faster world creation**: the terrain's setup now runs compiled (Medium 4.4 s to 1.6 s).
- **Caves stay deep under rivers, lakes, lava and ice**, and beside them: no more rivers floating over a cavern.
- **Cave entrances only on steep ground** (hillsides, cliffs, mountains); on flat land caves keep a roof of the
  land's own ground, not bare stone.
- **3D map** on the planet screen: drag to turn, right-drag to move, scroll to zoom; heights in the game's own proportions.
- **William Wythers' Overhauled Overworld** works alongside: its ground disks and extra ores are left out (they
  halved generation speed and repainted the slopes, scree and snow our terrain already lays).

## 0.1.2 (2026-11-07)

Worlds made with 0.1.1 keep their look: every terrain change below only applies to worlds created with this version,
so old worlds get no chunk borders. NeoForge 1.21.1 and Fabric 26.2 builds have the same changes.

### Volcanoes
- **Supervolcanoes**: continental hotspots and volcanic arcs on thick crust collapse into huge calderas instead of
  building a cone. Rings of lakes round a resurgent island (like Toba) or dry basins.
- **Ignimbrite plains** round supervolcanoes: the eruption's ash flows filled the valleys to a near-level plain that
  rivers have since cut canyons into. Soil over pale tuff.
- **Fairy chimneys**: tuff columns with dark andesite caps on valley sides cut into those ignimbrite plains.
- **Lava domes**: lumpy, flat-topped mounds in the craters of many arc volcanoes; on erupting ones they plug the vent.
- **Blown volcanoes**: half the scarred arc volcanoes blew out sideways like Mount St Helens, leaving a horseshoe
  crater and a hummocky debris avalanche beyond its mouth.
- **Ash fall**: erupting arc volcanoes leave bare tuff downwind (east under the westerlies, west under the trades).
- **Maars**: explosion craters with tuff rings round young volcanoes in wet country, often holding a lake.
- Magma patches now burn only round the crater; the flanks keep a few vents.
- **Natural volcano shapes**: small bulges on the flanks, cones drawn out along their line of vents, gullies
  (barrancos) down the upper slopes, and a ragged summit edge instead of a clean circle.
- **Tuyas**: volcanoes that erupted under the Ice Age ice sheets are flat-topped table mountains with cliff sides, a
  scree apron and a small cone where they broke through the ice.

### Water, karst and desert
- **Oases** in warm deserts, where a dry valley gathers enough groundwater from its catchment: brim-full pools of
  different sizes and shapes (round pools, long lakes between dunes, broad lakes) in a green oasis biome whose belt
  grows with the water. Vanilla uses sparse jungle there, Biomes O' Plenty its palm-lined tropics.
- **Cenotes of four kinds**: narrow deep wells, sheer-walled cylinders, shallow basins with sloping banks, and
  half-fallen caves with a rubble ramp down to the water. Some keep an islet of fallen roof, shafts are deeper on high
  ground, outlines are irregular, and **vines hang down their walls**. Each cenote has one water level.
- **Travertine terraces**: white calcite pools where hot springs rise on rift faults.
- Desert **dune seas removed** (their block steps striped the sand).

### Ores
- **Ore deposits where the geology put them**: porphyry copper and gold under old volcanoes (with rusty gossan over
  shallow ones), gold and redstone veins round active volcanoes, diamond-bearing kimberlite pipes on old continental
  cores, folded iron, coal and lapis beds, and placer gold in river beds below a deposit. Vanilla's scattered ores
  are thinned to about a third elsewhere.
- A planet map layer shows the deposits, and `/tectonic` names the nearest of each kind.

### Glaciers
- Moraines on glacier edges lie in ragged clumps of mixed rock (cobbled deepslate, stone, andesite, tuff) instead of
  an even dark ring round every snout.

### Planet map
- Drag to pan (wraps east-west), scroll to zoom about the cursor, `C` to come back to yourself.
- Shift-click sets a mark; the hover line shows the distance from it in blocks, the short way round the planet.

### Fixes
- A small plate no longer ploughs a channel of new sea floor through a continent when they collide: the plate that
  holds the land keeps it (simulation only, existing worlds unchanged).
- Crater rims follow the land round them: no more deep shafts with crumbling pillars under glacial horns.
- Erupting hotspot volcanoes no longer dig their flanks below the planet's surface (a 45-block wall round a caldera lake).
- Rivers are no longer drawn across lakes (they stood as straight ridges in deep caldera lakes).
- Lava flows no longer fill river channels, and lake shores no longer cut river banks below the water: two spots
  where water spilled onto dry land.

### Performance
- Chunk generation hands its terrain work to its own thread pool, as vanilla does, instead of holding up every
  chunk's steps: 16-36% faster on the dev server.
