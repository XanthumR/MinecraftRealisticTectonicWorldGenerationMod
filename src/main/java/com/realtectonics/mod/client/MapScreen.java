package com.realtectonics.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.realtectonics.mod.MapTeleport;
import com.realtectonics.mod.RealTectonics;
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

/** The whole planet on one screen (M), with you on it. Scroll to zoom, right-click to teleport, G for the geology view. */
public final class MapScreen extends Screen {
    static final KeyMapping KEY = new KeyMapping("key.realtectonics.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KeyMapping.Category.MISC);
    private static final int MAX_ZOOM = 16;

    // The maps only change with the world, so their textures outlive the screen. [0] biomes, [1] geology.
    private static Terrain texturesFor;
    private static final Identifier[] textures = new Identifier[2];
    private static boolean geologyView;

    private int zoom = 1;
    private View view; // as drawn last frame, so clicks land where the map showed

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
            geologyView = !geologyView;
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
        int mode = geologyView ? 1 : 0;
        if (textures[mode] == null)
            textures[mode] = upload("planet_map_" + mode, t.mapColors(geologyView), w, h, null);
        Identifier texture = textures[mode];

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

        double lat = t.latitude(player.getZ()), lon = t.longitude(player.getX());
        g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        String here = geologyView ? t.column((int) player.getX(), (int) player.getZ()).rock().toString() : t.biomeAt((int) player.getX(), (int) player.getZ()).toString();
        g.centeredText(font, String.format("%.2f°%s %.2f°%s  |  %s  |  zoom x%d (scroll)  |  G: %s map  |  M to close",
                Math.abs(lat), lat >= 0 ? "N" : "S", Math.abs(lon), lon >= 0 ? "E" : "W", here, zoom,
                geologyView ? "biome" : "geology"), width / 2, y0 + mapH + 6, 0xFFFFFFFF);
        int[] hover = view.blockAt(mouseX, mouseY, player);
        if (hover != null)
            g.centeredText(font, String.format("x %d, z %d  |  right-click to teleport", hover[0], hover[1]), width / 2, y0 + mapH + 18, 0xFFAAAAAA);
    }
}
