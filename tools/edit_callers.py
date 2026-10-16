def edit(p, pairs):
    s = open(p, encoding='utf-8').read()
    for a, b in pairs:
        assert s.count(a) == 1, (p, a)
        s = s.replace(a, b)
    open(p, 'w', encoding='utf-8', newline='').write(s)


edit('src/main/java/com/realtectonics/sim/Prototype.java', [
    ("""        BufferedImage biomes = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        biomes.setRGB(0, 0, w, h, data.biomeColors(), 0, w);
        ImageIO.write(biomes, "png", new File(out, "biomes.png"));
        BufferedImage geology = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        geology.setRGB(0, 0, w, h, data.geologyColors(), 0, w);
        ImageIO.write(geology, "png", new File(out, "geology.png"));
        t = System.nanoTime();
        Terrain terrain = new Terrain(data, seed);
""", """        Terrain terrain = new Terrain(data, seed);
        for (boolean geology : new boolean[]{false, true}) {
            BufferedImage map = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            map.setRGB(0, 0, w, h, terrain.mapColors(geology), 0, w);
            ImageIO.write(map, "png", new File(out, geology ? "geology.png" : "biomes.png"));
        }
        t = System.nanoTime();
"""),
])
edit('src/main/java/com/realtectonics/mod/client/PlanetScreen.java', [
    ("""            int[] rgb = d.biomeColors();
            int spawn = new Terrain(d, s, ps.mountainScale()).spawnCell;""",
     """            Terrain terrain = new Terrain(d, s, ps.mountainScale());
            int[] rgb = terrain.mapColors(false);
            int spawn = terrain.spawnCell;"""),
])
edit('src/main/java/com/realtectonics/mod/client/MapScreen.java', [
    ("""geologyView ? t.data.geologyColors() : t.data.biomeColors()""", """t.mapColors(geologyView)"""),
])
print("edited")
