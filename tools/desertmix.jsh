import com.realtectonics.sim.*;
// Top-down surface around a desert edge, using the generator's dryEdge rule: sand, coarse dirt, grass.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = 256; var img = new java.awt.image.BufferedImage(n * 2, n * 2, 1);
for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) { int x = cx - n / 2 + i, z = cz - n / 2 + j; var c = t.column(x, z); int rgb;
  if (c.water() >= c.ground()) rgb = 0x3c78dc; else { boolean desert = c.biome() == Biome.DESERT, steppe = c.biome() == Biome.STEPPE || c.biome() == Biome.GRASSLAND;
    float dry = t.aridity(x, z, c.ground()), patch = 0.5f + 0.5f * t.patch(x, z); int SAND = 0xe6d296, COARSE = 0x8a6a4a, GRASS = 0x7cae4a;
    if (desert) { float open = Math.clamp((dry - 0.3f) / 0.2f, 0, 1); rgb = patch < open * 0.5f ? GRASS : patch < open ? COARSE : SAND; }
    else if (steppe) { float bare = Math.clamp((0.8f - dry) / 0.3f, 0, 1); rgb = patch < bare * 0.4f ? SAND : patch < bare * 0.7f ? COARSE : GRASS; }
    else rgb = c.biome().color; }
  for (int k = 0; k < 4; k++) img.setRGB(i * 2 + k % 2, j * 2 + k / 2, rgb); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/desertmix.png"));
/exit
