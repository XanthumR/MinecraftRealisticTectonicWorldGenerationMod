exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            float spread = (float) (Math.abs(angle) / DELTA_SPREAD), front = d.radius * (1 + 0.2f * noise(x, z, 64, 13)) * (1 - 0.3f * spread * spread);
            float beyond = (float) r - front + (spread > 1 ? (spread - 1) * d.radius : 0);""",
 """            float spread = (float) (Math.abs(angle) / DELTA_SPREAD); // a rounded lobe: the front closes in at the sides
            float front = d.radius * (1 + 0.2f * noise(x, z, 64, 13)) * (float) Math.sqrt(Math.max(0, 1 - spread * spread));
            float beyond = (float) r - front;"""),
("""        River river = nearestRiver(u + low * MEANDER * noise(x, z, 128, 11), v + low * MEANDER * noise(x, z, 128, 12), (byte) 0, Hydrology.RIVER_AREA)""",
 """        double mu = low * MEANDER * (noise(x, z, 128, 11) + 0.5 * noise(x, z, 48, 16)), mv = low * MEANDER * (noise(x, z, 128, 12) + 0.5 * noise(x, z, 48, 17));
        River river = nearestRiver(u + mu, v + mv, (byte) 0, Hydrology.RIVER_AREA)"""),
])
print("ok")
