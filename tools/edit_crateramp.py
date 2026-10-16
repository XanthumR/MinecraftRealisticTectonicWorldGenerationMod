exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            } else if (d < c.craterRadius && !(ownCraters && ground > coneY(c, d) + CRATER_OWN) && !drowned(c)) {
                // From format 26 a crater is only cut where this volcano is the land: where a neighbour in the chain or a
                // mountain stands higher, the crater ends against it instead of boring a round pit into it.
                ground = Math.round(rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);""",
 """            } else if (d < c.craterRadius && !(ownCraters && ground > coneY(c, d) + CRATER_OWN + (rampShores ? c.craterDepth : 0)) && !drowned(c)) {
                // From format 26 a crater is only cut where this volcano is the land: where a neighbour in the chain or a
                // mountain stands higher, the crater ends against it instead of boring a round pit into it.
                // From format 31 it fades out over a crater's depth of rise, so it meets the neighbour as a slope, not a wall.
                float kept = rampShores ? smoothstep((ground - coneY(c, d) - CRATER_OWN) / c.craterDepth) : 0;
                int pit = Math.round(rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);
                ground = Math.round(pit + kept * Math.max(0, ground - pit));"""),
])
print("ok")
