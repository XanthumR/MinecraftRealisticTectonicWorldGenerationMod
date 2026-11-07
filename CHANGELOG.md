# Changelog

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
