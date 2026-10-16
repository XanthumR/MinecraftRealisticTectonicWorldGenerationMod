exec(open('tools/edit_glacier.py').read().split("C = ")[0])
edit('src/main/java/com/realtectonics/sim/Terrain.java', [
("""        double mu = low * MEANDER * (noise(x, z, 128, 11) + 0.5 * noise(x, z, 64, 16)), mv = low * MEANDER * (noise(x, z, 128, 12) + 0.5 * noise(x, z, 64, 17));""",
 """        double mu = low > 0 ? low * MEANDER * (noise(x, z, 128, 11) + 0.5 * noise(x, z, 64, 16)) : 0;
        double mv = low > 0 ? low * MEANDER * (noise(x, z, 128, 12) + 0.5 * noise(x, z, 64, 17)) : 0;"""),
])
print("ok")
