# Planet formats

Every change to how terrain looks bumps `PlanetData.FORMAT` and is gated by a `Terrain` flag set as `data.format >= N`,
so worlds keep the look they were created with (see CLAUDE.md). This is what each format added, and **why**: most later
formats fix something the user spotted in a screenshot, and the cause is the useful part when a similar bug turns up.

Sim changes (Planet, Hydrology, Climate) need no flag: their results are saved in `planet.bin`, so they reach new worlds only.
New saved arrays need a load branch in `PlanetData.load` (`format >= N`).

| Format | Flag | What | Why / lesson | Check |
|---|---|---|---|---|
| 2 | | Volcanoes saved | | |
| 3 | | Rock provinces (`Rock`, `Geology.provinces`) | | `rock.jsh` |
| 4 | | Volcano bases saved | | |
| 5 | | Glaciers and fjords (`PlanetData.ice`) | | `fjord*.jsh` |
| 6 | | Continental shelves | Old worlds keep their block heights via the format | `shelf.jsh` |
| 7 | `lowlandRivers` | Meanders and deltas | | `delta*.jsh` |
| 8 | | Ocean currents, monsoon; ice cap below -14 °C | | `climstats.jsh` |
| 9 | | Compact shield volcanoes (spread 2.2) | | `volc.jsh` |
| 10 | `organic` | Spline rivers, creeks, ragged deltas, jittered biomes, ridged detail | User: world "feels very artificial" (washboard, straight rivers) | `look.jsh` |
| 11 | `landforms` | Waterfalls, gorges, canyons, varied coasts, reefs and atolls, salt flats, dunes, rift valleys, cirques and horns, floodplains, karst, alluvial fans, hot springs; `Column.cover` | | `spillscan.jsh` |
| 12 | `richBiomes` | River, shore, ocean-temperature and mountain biomes | | `biomecount.jsh` |
| 13 | `forestVariety` | Forest patches, badlands | | |
| 14 | `gullies` | Stamped gullies (wave pattern down slopes) | Replaced at 33: an even comb | |
| 15 | `massifs` | Ridged 128-512 block relief on high ground, rare ice spikes | User wanted real eroded ranges | |
| 16 | `wildRivers` | Twisting, pinching, ragged rivers; creeks from flow 60 | Flow 20 made a parallel comb | `rivercheck.jsh` |
| 17 | `folds` | Foothills, fold ridges along range contours, scree, ponds | Ponds judged on finished ground (`pondLevel`) after a pond on a pillar | `pondscan.jsh`, `folds.jsh` |
| 18 | `snowLine` | Snow and water ice from the planet's climate, not biome snow lines | | `snowspots.jsh` |
| 19 | `talus` | Cell talus pass, capped massif relief | Needle peaks: block relief (massif + valley cut), not cells | `needles.jsh` |
| 20 | `perchedBanks` | Banks above lower land hold water only 4 blocks, then ease down | A wall at bank width | `banks.jsh` |
| 21 | `calderas` | Stamped calderas and cinder cones | Stamps sliced mountains: user asked to simulate instead | `calderas.jsh` |
| 22 | `simCalderas` | Calderas collapse in `Hydrology.collapseCalderas`; cones built at 2/3 of erosion; summit sharpening in `surface()` | "Simulate, don't stamp" | `simcaldera.jsh` |
| 23 | `glacierFlow` | Glaciers by mass balance (`Terrain.glaciers`); troughs widen with depth | Spill checks count ice as a wall since | `glacierflow.jsh` |
| 24 | `seasons` | Saves `amplitude`, `summerRain`: Köppen biomes, glacier melt by summer, frost scree, flood-season braided rivers | | `seasonsmap.jsh` |
| 25 | `gentleShores` | Lake shore band slopes; `springLevel`; desert edges mixed (`dryEdge`, `aridity`) | Cliffs where lakes cut hills | `shores.jsh` |
| 26 | `ownCraters` | Craters only where their own cone is the land | Round pits bored into neighbouring peaks | |
| 27 | `iceSheets` | One smooth glacier surface (`iceTop`); ends where summers exceed 15 °C | Glaciers looked like separate tubes | |
| 28 | `allTroughs`, `nearestLakes` | Deepest of all troughs cuts; each lake its own level with sills | "Mountain cut in half" by two troughs; horns clearing lake water | `lakeleak.jsh` |
| 29 | `rockfall` | Block-scale rockfall over 256-block tiles (`relaxedColumn`), scree | Idealised walls and curves | `rockfall.jsh` (seams 0) |
| 30 | `solidShores` | Stable rockfall rate 0.05; no summit sharpening over lakes/calderas; lakes leave high land; drowned summits keep no crater | Spires = rockfall overshoot (rate × 8 neighbours must be ≤ 1/2); neighbours' peaks rebuilt over a caldera | `overshoot.jsh` |
| 31 | `rampShores` | Lake cut, ice front and crater limits ramp instead of switching | **Any per-column on/off threshold on height makes a cliff where it flips: blend with smoothstep** | `topmap.jsh` walls |
| 32 | `faults` | Strike-slip faults: saves `faultSide`, `faultSlip`; gouge erodes ×9 (`Hydrology.FAULT_GOUGE`); scarp up to 10 blocks | Land-to-land boundaries are rare in this plate model; rift escarpments not done | `faults.jsh` |
| 33 | `simGullies` | Gullies cut by water at block scale in `relaxTile` (D8, area from 24 blocks upslope) | Stamped gullies were an even comb ("stripes") | `rockfall.jsh`, `hillshade.jsh` |
| 34 | `foldedStrata` | Rock beds fold in collision belts; beds 3-10 blocks | Level stripes across a collision range | `strata.jsh` |
| 35 | `deepTrenches` | Sim: new crust joins both plates (`Planet.spreadEvenly`), so ridges sit mid-ocean; ridge crest by √distance to the axis (`Planet.spreading`); trenches to 7.5 km along the hinge, 2 cells half-wide so arc volcanoes stand behind them (`Planet.trenches`). Terrain: below 3 km 100 m/block so trenches fit above bedrock | Ridges were half-ridges glued to a coast, ending in a 2 km cliff, because new crust stuck only to the plate that moved off | `seafloor.jsh` (cells 6-8 km > 0) |
| 36 | `variedVolcanoes` | Each volcano its own width, steepness, lobes and (30% of arcs) a horseshoe collapse scar (`Volcano.surfaceMetres(dx, dz, varied)`); profile in blocks; crater pools at the lowest rim point | "Every volcano looks the same"; cones shaped in metres stood as spires on tiny islands because the sea takes fewer blocks per metre than land (island 78 → 283 blocks across for a cone from 3.3 km deep) | `volcvar.jsh` |
| 37 | `variedShores` | Beach width from shelter (`bay`: land mask blurred wide) and river sand (`silt`) with noise; shingle (gravel) on exposed headlands; gullies need `GULLY_THRESHOLD` of stream power and stay off the beach | "Beaches too uniform", 1-block holes in the sand: sub-block gully cuts rounded into scattered pits | `hillshade.jsh`, `rockfall.jsh` |
| 38 | `wetBanks` | Sim: lake-effect rain (`Climate.lakeEffect`, second pass after the rivers: lakes evaporate along the same winds). Terrain: big rivers add up to 700 mm of groundwater to their banks' biome rain (green strips through deserts); active craters melt ice within 2.5 crater radii (`craterHeat`) | User: "does water affect biomes" (only the sea did); glacier ice filled an erupting crater as a wall round the lava | `wetness.jsh` |
| 39 | | Sim only: supercontinent cycle. One ragged supercontinent (`continents`: rough warped noise, persistence 0.68) torn by plates grown through tough/weak ground (`growPlates`, 40% of plates born inside it); `mantleFlow` drives continental plates apart for 100 steps, then back together until gathered, and breaks them up a last time 150 steps before the end (`plan`) | "Continent shapes too blobby": smooth noise blobs on random plates. Lake-effect rain capped at 1000 mm | `cycle.jsh` |
| 40 | `flowingIce` | Ice needs `ICE_SHED` 5 blocks of flow-fed thickness; steep cells (> 0.6) avalanche their snow down; ice surface smoothed over ice cells; below a 1 °C summer at the surface bare blue ice (`Cover.BARE_ICE`), rubble on thin edges and medial moraines (`Cover.MORAINE`, cobbled deepslate) down the centreline below glacier junctions (`merged`) | "Ice too blobby, too much": every cell above the snow line had ice, domed per cell, all white | `icelook.jsh` |
| 41 | `spreadFlow` | Gully runoff divides between all lower neighbours (MFD, `GULLY_SPREAD` 1.1) over heights box-averaged twice over 5 blocks; only stream power above the 9x9 local mean cuts (`GULLY_RELIEF` 4) | "Slope too stairy": single-direction routing over integer steps split even slopes into parallel lines every few blocks, a comb of 1-2 block grooves | `comb.jsh` |
| 42 | `smoothCones` | Block-scale cone and crater scaled by the share of the volcano erosion left (`Cone.kept`); collapse scars of even width (`Volcano.SMOOTH_SCARS`); crater walls rise to their own side's rim; summit sharpening fades across a lake's shore band; whole lakes get the river biome | Crater "towers" with lava on top: the ideal peak and crater stamped over a summit the rivers wore from 3515 m to 590 m. Scar edges were an angle, a blade at the summit. Lakes had patchy water colour | `lakebiome.jsh`, `topmap.jsh` walls |
| 43 | `fastMath` | Distances by `Math.sqrt(a*a+b*b)` (`Terrain.hyp`), 0 of 4.2M columns differ; `sealCavesFromWater` fills air only from the lowest neighbouring ground up, so caves under seas stay, dry | Speed: Math.hypot was ~20% of a column; sealing scanned every block down to bedrock under all water | `relaxhash.jsh` |
| 44 | `softBadlands` | Badlands only where a desert stands above 800 m on sedimentary rock (`Rock.BASIN`); other high deserts stay desert. Climate (sim only): desert belt (`Climate.band`, SUBSIDENCE 0.9 at 26°), summer flow, recycling, föhn, monsoon to 20°. Later, sim only: the belt widened to 12° and lifted on east coasts poleward of 24° (`eastOcean`: Uruguay, Natal, the Carolinas), and a high plateau draws the monsoon up to 20° farther across ±12° of longitude (`plateauReach`: all of India). Then processes, not fits: trade air blocked by a range without passes turns poleward along it (`barrierAhead`, Sheppard's dividing streamline: Chaco and Pampas from the Amazon, Puna dry); a continent covering most latitudes poleward reverses the winds (`monsoonRegime`: Arabia dry, no warm current off Oman); the monsoon evaporates wet land as well as sea and recycles its rain (Sahel, central Africa). Then equatorward coast winds on the highs' eastern flank (`eastFlank`, fenced by a 1 km marine layer) and the mass elevation effect (`massElevation`: broad plateaus 30% less lapse cooling, so less ice on random planets' high plateaus). Earth 75.1 / 52.2 / 41.5% agree | Random planets had 1-3% desert: the subtropics rained too much, and every desert above 500 m became badlands | `koppen.py` (71% main groups, then 72%), `tune.sh` |
| 45 | `longWinters` | Snow on the ground, ice on lakes and rivers, and snowy taiga only where frozen two-thirds of the year (`Terrain.winterLook`: mean + half the seasonal swing below 0), not half (the annual mean). Climate (sim only): every per-cell rate turned into a per-km rate (`REFERENCE_WIDTH` 1024), so Small, Medium and Large planets get the same climate; blurs and window extremes made O(cells), so Huge simulates in 9 s. Also `activeSummits`: an erupting volcano's summit buries the massif's ridges within ~2 crater radii, which had stood over the rim and left the crater dry | User: Canada's lakes all frozen and its forest snowy coniferous (boreal Canada is frozen ~55% of the year, with 16-18 °C summers); India more desert on a Large planet (Earth main groups 66.5% at 2048 vs 75.1% at 1024; Siberia 26%) | `canada.jsh` (Canada snowy taiga 62% -> 0%, Arctic stays 84% snow), `tune.sh -R-Dwidth=2048` |

Earth planet (`PlanetSettings.earth`, sim only, no format): real elevation and lakes, PB2002 plates; its fields come from
the data instead of plate motion. Lessons: averaging real relief into cells closes river gorges, so fill hollows (only real
lakes stay, cut in); below-sea land the ocean does not reach is dry (deep basins become lakes: the Caspian).

## Recurring causes of "unnatural" spots
- **Stamps.** A shape laid per block from an ideal formula (crater, peak, wave) cuts through whatever is there. Prefer the sim's cells or a block-scale simulation (rockfall, gullies).
- **Grid aliasing.** Routing water or anything else one-way over integer block steps lines up with the steps: on an even slope it makes evenly spaced parallel grooves. Spread the flow and smooth wider than the step period (format 41).
- **Rounding.** A fractional cut or rise that rounds to a block in some columns and not their neighbours leaves 1-block pits or bumps: give small effects a threshold (format 37).
- **Thresholds.** `if (height > limit)` switches whole columns: a wall appears where it flips (formats 30 → 31).
- **Unstable explicit updates.** Synchronous diffusion over 8 neighbours needs a rate ≤ 1/16 per neighbour (format 30).
- **Neighbour sums.** Features from several sources (troughs, cones) must blend (max/min of all), not take the nearest (format 28).
- **Long-range dependencies on tiles.** Anything in `relaxTile` must depend only on blocks within its margin, or tiles disagree (seams). Count upslope water a fixed number of steps (format 33).
