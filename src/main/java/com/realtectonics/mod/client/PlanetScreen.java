package com.realtectonics.mod.client;

import com.realtectonics.mod.RealTectonics;
import com.realtectonics.mod.TectonicChunkGenerator;
import com.realtectonics.sim.Drift;
import com.realtectonics.sim.PlanetData;
import com.realtectonics.sim.PlanetSettings;
import com.realtectonics.sim.Terrain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;

import java.util.Arrays;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.DoubleConsumer;

/**
 * The "Customize" screen for Tectonic Planet worlds: a live map of the planet the chosen seed and settings make.
 * The simulation runs on a background thread; the new world reuses its result.
 */
@EventBusSubscriber(modid = RealTectonics.MODID, value = Dist.CLIENT)
public final class PlanetScreen extends Screen {
    static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, ResourceLocation.fromNamespaceAndPath(RealTectonics.MODID, "planet"));
    private static final int RESTART_DELAY = 10; // ticks after the last slider change before re-simulating

    @SubscribeEvent
    static void registerEditor(RegisterPresetEditorsEvent event) {
        event.register(PRESET, (parent, context) -> new PlanetScreen(parent,
                context.selectedDimensions().overworld() instanceof TectonicChunkGenerator g ? g.planet() : PlanetSettings.DEFAULT));
    }

    private record Result(long seed, PlanetSettings settings, PlanetData data, int[] rgb, float landFraction, float peakMetres) {}

    private final CreateWorldScreen parent;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Tectonic planet preview");
        t.setDaemon(true);
        return t;
    });
    private PlanetSettings settings;
    private long seed;
    private Future<?> job;
    private volatile double progress;
    private volatile int[] drift;    // the plates as they stand while the simulation runs, shown until it finishes
    private volatile int[] drawn;   // the frame on screen; a new one is coloured only once it is drawn
    private int driftW, driftH;
    private volatile Result finished;
    private Result shown;
    private ResourceLocation textureId;
    private int restartIn = -1;

    private PlanetScreen(CreateWorldScreen parent, PlanetSettings settings) {
        super(Component.literal("Tectonic Planet"));
        this.parent = parent;
        this.settings = settings;
    }

    @Override
    protected void init() {
        if (job == null) {
            // The preview must be the world you get, so a blank seed box gets a concrete seed now.
            OptionalLong typed = WorldOptions.parseSeed(parent.getUiState().getSeed());
            seed = typed.isPresent() ? typed.getAsLong() : WorldOptions.randomSeed();
            parent.getUiState().setSeed(Long.toString(seed));
            simulate();
        }
        int cx = width / 2, y = 24 + mapWidth() / 2 + 8;
        addRenderableWidget(new Slider(cx - 155, y, "Plates", 6, 60, settings.plates(), false, true, v -> settings = settings.withPlates((int) v)));
        addRenderableWidget(new Slider(cx + 5, y, "Ocean", 0.5, 0.85, settings.oceanFraction(), true, true, v -> settings = settings.withOceanFraction((float) v)));
        y += 24;
        addRenderableWidget(CycleButton.<Integer>builder(w -> Component.literal(w < 1024 ? "Small" : w == 1024 ? "Medium" : w == 2048 ? "Large (slow)" : "Huge (~2 min, 4 GB RAM)"))
                .withValues(Arrays.stream(PlanetSettings.WIDTHS).boxed().toList())
                .withInitialValue(settings.width())
                .create(cx - 155, y, 150, 20, Component.literal("Size"), (button, w) -> {
                    settings = settings.withWidth(w);
                    restartIn = RESTART_DELAY;
                }));
        addRenderableWidget(new Slider(cx + 5, y, "Mountains", 0.5, 2, settings.mountainScale(), true, false, v -> settings = settings.withMountainScale((float) v)));
        y += 24;
        addRenderableWidget(CycleButton.<Integer>builder(z -> Component.literal("x" + z + (z == 1 ? " (compact)" : "")))
                .withValues(Arrays.stream(PlanetSettings.ZOOMS).boxed().toList())
                .withInitialValue(settings.zoom())
                .create(cx - 155, y, 150, 20, Component.literal("World scale"), (button, z) -> settings = settings.withZoom(z)));
        addRenderableWidget(new Slider(cx + 5, y, "Volcanoes", 0, 2, settings.volcanoes(), true, true, v -> settings = settings.withVolcanoes((float) v)));
        y += 24;
        addRenderableWidget(new Slider(cx - 155, y, "Steps", 100, 600, settings.steps(), false, true, v -> settings = settings.withSteps((int) v)));

        addRenderableWidget(new Slider(cx + 5, y, "Rivers", 0.5, 4, settings.riverWidth(), true, false, v -> settings = settings.withRiverWidth((float) v)));
        y += 24;
        addRenderableWidget(CycleButton.onOffBuilder(settings.earth()) // Earth: real continents and plates; plates, ocean, volcanoes and steps then do nothing
                .create(cx - 155, y, 150, 20, Component.literal("Earth"), (button, on) -> {
                    settings = settings.withEarth(on);
                    restartIn = RESTART_DELAY;
                }));
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("New planet"), b -> {
            seed = WorldOptions.randomSeed();
            parent.getUiState().setSeed(Long.toString(seed));
            simulate();
        }).bounds(cx - 155, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> apply()).bounds(cx - 50, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(cx + 55, y, 100, 20).build());
    }

    private int mapWidth() {
        return Math.max(100, Math.min(width - 40, (height - 24 - 8 - 6 * 24 - 8) * 2));
    }

    private void simulate() {
        if (job != null) job.cancel(true);
        restartIn = -1;
        progress = 0;
        drift = null;
        long s = seed;
        PlanetSettings ps = settings;
        job = worker.submit(() -> {
            PlanetData d = PlanetData.generate(s, ps, p -> progress = p, planet -> {
                if (drift != null && drift != drawn) return;
                driftW = planet.w / 2;
                driftH = planet.h / 2;
                drift = Drift.of(planet).colors();
            });
            Terrain terrain = new Terrain(d, s, ps.mountainScale(), ps.zoom(), ps.riverWidth());
            int[] rgb = terrain.mapColors(false);
            int spawn = terrain.spawnCell;
            int land = 0;
            float peak = 0;
            for (int g = 0; g < rgb.length; g++) {
                if (d.metres[g] >= 0) land++;
                peak = Math.max(peak, d.metres[g]);
            }
            for (int k = -3; k <= 3; k++) { // spawn marker: a red plus
                rgb[Math.floorMod(spawn % d.w + k, d.w) + spawn / d.w * d.w] = 0xFF0000;
                rgb[spawn % d.w + Math.clamp(spawn / d.w + k, 0, d.h - 1) * d.w] = 0xFF0000;
            }
            finished = new Result(s, ps, d, rgb, land / (float) rgb.length, peak);
        });
    }

    private boolean stale(Result r) {
        return r == null || r.seed != seed || !r.settings.sameSimulation(settings);
    }

    @Override
    public void tick() {
        if (restartIn > 0 && --restartIn == 0) simulate();
        Result r = finished;
        if (r != shown && !stale(r)) show(r);
        else if (stale(shown) && drift != null && drift != drawn) { // the next frame of the plates drifting
            drawn = drift;
            textureId = MapScreen.upload("realtectonics_planet_preview", drawn, driftW, driftH, textureId);
        }
    }

    private void show(Result r) {
        textureId = MapScreen.upload("realtectonics_planet_preview", r.rgb, r.data.w, r.data.h, textureId);
        shown = r;
        drawn = null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        int mapW = mapWidth(), mapH = mapW / 2, x = (width - mapW) / 2, y = 24;
        if (stale(shown) && drawn != null) g.blit(textureId, x, y, mapW, mapH, 0, 0, driftW, driftH, driftW, driftH);
        else if (shown != null) g.blit(textureId, x, y, mapW, mapH, 0, 0, shown.data.w, shown.data.h, shown.data.w, shown.data.h);
        else g.fill(x, y, x + mapW, y + mapH, 0xFF101820);
        String status = stale(shown) ? String.format(progress < PlanetData.PLATES_SHARE ? "Plates drifting... %d%%" : "Rivers, climate and biomes... %d%%", (int) (progress * 100))
                : String.format("Seed %d  |  %d%% land  |  highest peak %.1f km, y %d  |  + spawn", seed, Math.round(shown.landFraction * 100),
                shown.peakMetres / 1000, Terrain.peakY(shown.peakMetres, settings.mountainScale()));
        g.drawCenteredString(font, status, width / 2, y + mapH - 12, 0xFFFFFF);
    }

    private void apply() {
        PlanetSettings chosen = settings;
        parent.getUiState().updateDimensions((access, dims) -> dims.overworld() instanceof TectonicChunkGenerator g
                ? dims.replaceOverworldGenerator(access, g.withPlanet(chosen)) : dims);
        if (!stale(shown)) RealTectonics.offerPreview(seed, chosen, shown.data);
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        worker.shutdownNow();
        if (textureId != null) minecraft.getTextureManager().release(textureId);
    }

    private final class Slider extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final boolean percent, resimulate;
        private final DoubleConsumer apply;

        Slider(int x, int y, String label, double min, double max, double initial, boolean percent, boolean resimulate, DoubleConsumer apply) {
            super(x, y, 150, 20, Component.empty(), (initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.percent = percent;
            this.resimulate = resimulate;
            this.apply = apply;
            updateMessage();
        }

        private double current() {
            double v = min + value * (max - min);
            return percent ? Math.round(v * 100) / 100.0 : Math.round(v);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + (percent ? Math.round(current() * 100) + "%" : (int) current())));
        }

        @Override
        protected void applyValue() {
            apply.accept(current());
            if (resimulate) restartIn = RESTART_DELAY;
        }
    }
}
