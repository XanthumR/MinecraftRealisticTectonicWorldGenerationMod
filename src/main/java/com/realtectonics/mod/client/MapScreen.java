package com.realtectonics.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.realtectonics.mod.MapTeleport;
import com.realtectonics.mod.RealTectonics;
import com.realtectonics.sim.Drift;
import com.realtectonics.sim.MapLayers;
import com.realtectonics.sim.PlanetData;
import com.realtectonics.sim.Terrain;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

/** The whole planet on one screen (M), with you on it. Scroll to zoom, right-click to teleport, G for the next layer, R to replay the plates drifting. */
public final class MapScreen extends Screen {
    static final KeyMapping KEY = new KeyMapping("key.realtectonics.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KeyMapping.Category.MISC);
    private static final int MAX_ZOOM = 16;
    private static final String[] LAYERS = {"biome", "geology", "plates", "rain", "temperature"};
    private static final double REPLAY_SECONDS = 8; // the whole run, start to end
    private static final int ARROW_PIXELS = 40;    // arrow length of a plate moving one cell a step

    // The maps only change with the world, so their textures outlive the screen, one per LAYERS.
    private static Terrain texturesFor;
    private static final Identifier[] textures = new Identifier[LAYERS.length];
    private static int layer;

    private int zoom = 1;
    private View view; // as drawn last frame, so clicks land where the map showed
    private long replayStart = -1; // System.nanoTime() the replay began, or -1
    private int[][] replayColors;  // each Drift frame coloured, made on the first replay
    private Identifier replayTexture;
    private int replayShown = -1;  // the blend step on the replay texture now

    /** The map rectangle on screen, and which cells of the planet it shows. */
    private record View(int x0, int y0, int mapW, int mapH, int left, int top, float scale, int w, double playerU, double playerV, int blocksPerCell) {
        /** Block x, z under a screen point, on the copy of the planet nearest the player; null off the map. */
        int[] blockAt(double mx, double my, LocalPlayer player) {
            if (mx < x0 || my < y0 || mx >= x0 + mapW || my >= y0 + mapH) return null;
            double u = left + (mx - x0) / scale, v = top + (my - y0) / scale;
            double du = ((u - playerU) % w + w * 1.5) % w - w / 2.0; // the short way round the planet
            return new int[]{(int) Math.floor(player.getX() + du * blocksPerCell),
                    (int) Math.floor(player.getZ() + (v - playerV) * blocksPerCell)};
        }
    }

    static void onClientTick(Minecraft mc) {
        while (KEY.consumeClick())
            if (mc.gui.screen() == null && mc.player != null) mc.gui.setScreen(new MapScreen());
    }

    /** Uploads an RGB-per-pixel map as a texture, replacing (and freeing) the previous one. */
    static Identifier upload(String name, int[] rgb, int w, int h, Identifier previous) {
        NativeImage image = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int c = rgb[x + y * w];
                image.setPixel(x, y, 0xFF000000 | c); // ARGB
            }
        Minecraft mc = Minecraft.getInstance();
        if (previous != null) mc.getTextureManager().release(previous);
        Identifier id = Identifier.fromNamespaceAndPath(RealTectonics.MODID, name);
        mc.getTextureManager().register(id, new DynamicTexture(() -> name, image));
        return id;
    }

    private MapScreen() {
        super(Component.translatable("key.realtectonics.map"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent key) {
        if (KEY.matches(key)) {
            onClose();
            return true;
        }
        if (key.key() == GLFW.GLFW_KEY_G) {
            Terrain t = RealTectonics.terrainOrNull();
            do layer = (layer + 1) % LAYERS.length;
            while (layer == 2 && t != null && t.data.format < 64); // no plates saved in older worlds
            return true;
        }
        if (key.key() == GLFW.GLFW_KEY_R) {
            replayStart = replayStart < 0 ? System.nanoTime() : -1;
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        int[] target = click.button() == 1 && view != null && minecraft.player != null ? view.blockAt(click.x(), click.y(), minecraft.player) : null;
        if (target == null) return super.mouseClicked(click, doubleClick);
        ClientPlayNetworking.send(new MapTeleport(target[0], target[1])); // the server checks permission and finds the surface
        onClose();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        zoom = Math.clamp(scrollY > 0 ? zoom * 2L : zoom / 2, 1, MAX_ZOOM);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Terrain t = RealTectonics.terrainOrNull();
        LocalPlayer player = minecraft.player;
        if (player == null || t == null || !minecraft.hasSingleplayerServer()) {
            // ponytail: multiplayer clients don't have the planet; send biomeColors() from the server if it's wanted there
            g.centeredText(font, "The planet map works in single-player Tectonic Planet worlds.", width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        int w = t.data.w, h = t.data.h;
        if (texturesFor != t) { // a different world: drop the old maps
            for (int i = 0; i < textures.length; i++) {
                if (textures[i] != null) minecraft.getTextureManager().release(textures[i]);
                textures[i] = null;
            }
            texturesFor = t;
        }
        if (textures[layer] == null) {
            int[] rgb = switch (layer) {
                case 2 -> MapLayers.plates(t.data);
                case 3 -> MapLayers.rain(t.data);
                case 4 -> MapLayers.temperature(t.data);
                default -> t.mapColors(layer == 1);
            };
            textures[layer] = upload("planet_map_" + layer, rgb, w, h, null);
        }
        Identifier texture = textures[layer];
        if (replayStart >= 0 && replay(g, t.data)) return;

        int mapW = Math.min(width - 20, (height - 56) * 2), mapH = mapW / 2, x0 = (width - mapW) / 2, y0 = 22;
        double[] p = t.mapPosition(player.getX(), player.getZ());
        int viewW = w / zoom, viewH = h / zoom;
        int left = zoom == 1 ? 0 : Math.floorMod((int) Math.round(p[0] - viewW / 2.0), w);
        int top = zoom == 1 ? 0 : Math.clamp((int) Math.round(p[1] - viewH / 2.0), 0, h - viewH);
        float scale = mapW / (float) viewW;
        view = new View(x0, y0, mapW, mapH, left, top, scale, w, p[0], p[1], t.blocksPerCell);
        // The planet wraps east-west, so a view across the 180° seam is drawn in two pieces.
        int firstW = Math.min(viewW, w - left), firstPx = Math.round(firstW * scale);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x0, y0, left, top, firstPx, mapH, firstW, viewH, w, h);
        if (firstW < viewW) g.blit(RenderPipelines.GUI_TEXTURED, texture, x0 + firstPx, y0, 0, top, mapW - firstPx, mapH, viewW - firstW, viewH, w, h);

        // You: a red dot, with a white tick pointing where you face.
        double du = ((p[0] - left) % w + w) % w;
        int px = x0 + (int) (du * scale), py = y0 + (int) ((p[1] - top) * scale);
        double yaw = Math.toRadians(player.getYRot()), dx = -Math.sin(yaw), dz = Math.cos(yaw);
        for (int i = 4; i <= 9; i++) {
            int tx = px + (int) Math.round(dx * i), ty = py + (int) Math.round(dz * i);
            g.fill(tx - 1, ty - 1, tx + 1, ty + 1, 0xFFFFFFFF);
        }
        g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFFFFFF);
        g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFE02020);
        if (layer == 2) { // each plate's motion: an arrow from its middle, a dot at the head
            float[][] centres = MapLayers.plateCentres(t.data);
            for (int i = 0; i < centres.length; i++) {
                if (Float.isNaN(centres[i][0]) || centres[i][1] < top || centres[i][1] >= top + viewH) continue;
                double cu = ((centres[i][0] - left) % w + w) % w;
                if (cu >= viewW) continue;
                int ax = x0 + (int) (cu * scale), ay = y0 + (int) ((centres[i][1] - top) * scale), len = Math.round(ARROW_PIXELS * (float) Math.hypot(t.data.plateVx[i], t.data.plateVy[i]));
                double ux = t.data.plateVx[i], uy = t.data.plateVy[i], n = Math.max(1e-9, Math.hypot(ux, uy));
                for (int k = 0; k <= len; k++) {
                    int tx = ax + (int) Math.round(ux / n * k), ty = ay + (int) Math.round(uy / n * k);
                    g.fill(tx - 1, ty - 1, tx + 1, ty + 1, 0xFF101010);
                }
                g.fill(ax - 2, ay - 2, ax + 2, ay + 2, 0xFF101010);
                int hx = ax + (int) Math.round(ux / n * len), hy = ay + (int) Math.round(uy / n * len);
                g.fill(hx - 3, hy - 3, hx + 3, hy + 3, 0xFFFFE040);
            }
        }

        double lat = t.latitude(player.getZ()), lon = t.longitude(player.getX());
        g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        int bx = (int) player.getX(), bz = (int) player.getZ(), cell = Math.floorMod((int) p[0], w) + Math.clamp((int) p[1], 0, h - 1) * w;
        String here = switch (layer) {
            case 1 -> t.column(bx, bz).rock().toString();
            case 2 -> "plate " + (t.data.plate[cell] & 255);
            case 3 -> Math.round(t.data.precipitation[cell]) + " mm/yr";
            case 4 -> String.format("%.1f °C", t.data.temperature[cell]);
            default -> t.biomeAt(bx, bz).toString();
        };
        g.centeredText(font, String.format("%.2f°%s %.2f°%s  |  %s  |  zoom x%d (scroll)  |  G: %s map  |  R: replay drift  |  M to close",
                Math.abs(lat), lat >= 0 ? "N" : "S", Math.abs(lon), lon >= 0 ? "E" : "W", here, zoom,
                LAYERS[(layer + (layer == 1 && t.data.format < 64 ? 2 : 1)) % LAYERS.length]), width / 2, y0 + mapH + 6, 0xFFFFFFFF);
        int[] hover = view.blockAt(mouseX, mouseY, player);
        if (hover != null)
            g.centeredText(font, String.format("x %d, z %d  |  right-click to teleport", hover[0], hover[1]), width / 2, y0 + mapH + 18, 0xFFAAAAAA);
    }

    /** The plates drifting, frame by frame and blended between, over the whole map; false once it has run its course. */
    private boolean replay(GuiGraphicsExtractor g, PlanetData d) {
        int mapW = Math.min(width - 20, (height - 56) * 2), mapH = mapW / 2, x0 = (width - mapW) / 2, y0 = 22;
        if (d.drift.isEmpty()) {
            if ((System.nanoTime() - replayStart) / 1e9 > 2) {
                replayStart = -1;
                return false;
            }
            g.centeredText(font, "This world has no drift recorded (made before it was saved, or Earth).", width / 2, y0 + mapH / 2, 0xFFFFFFFF);
            return true;
        }
        double at = (System.nanoTime() - replayStart) / 1e9 / REPLAY_SECONDS;
        if (at > 1.15) { // hold the last frame a moment, then back to the map
            replayStart = -1;
            return false;
        }
        if (replayColors == null) replayColors = d.drift.stream().map(Drift::colors).toArray(int[][]::new);
        Drift first = d.drift.getFirst();
        double f = Math.min(1, at) * (replayColors.length - 1);
        int k = Math.min((int) f, replayColors.length - 2), step = (int) Math.round(f * 8); // 8 blends between frames
        if (step != replayShown) {
            float b = (float) (f - k);
            int[] a = replayColors[k], c = replayColors[k + 1], rgb = new int[a.length];
            for (int i = 0; i < a.length; i++) {
                int r = Math.round((a[i] >> 16 & 255) * (1 - b) + (c[i] >> 16 & 255) * b), gr = Math.round((a[i] >> 8 & 255) * (1 - b) + (c[i] >> 8 & 255) * b);
                rgb[i] = r << 16 | gr << 8 | Math.round((a[i] & 255) * (1 - b) + (c[i] & 255) * b);
            }
            replayTexture = upload("planet_replay", rgb, first.w(), first.h(), replayTexture);
            replayShown = step;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, replayTexture, x0, y0, 0, 0, mapW, mapH, first.w(), first.h(), first.w(), first.h());
        g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        g.centeredText(font, String.format("The plates drifting: %d%% of the run  |  R to stop", Math.round(Math.min(1, at) * 100)), width / 2, y0 + mapH + 6, 0xFFFFFFFF);
        return true;
    }

    @Override
    public void removed() {
        if (replayTexture != null) minecraft.getTextureManager().release(replayTexture);
    }
}
