package com.realtectonics.sim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Everything the game needs from the simulation, per cell, in real units. Saved with the world so it runs once. */
public final class PlanetData {
    public static final int EROSION = 50;
    static final int EARTH_EROSION = 3; // passes on Earth's real relief: enough for rivers to find their beds and lakes to fill, not to wear the ranges down
    public static final double PLATES_SHARE = 0.6; // of the progress bar: plates take ~60% of the time, rivers the rest
    static final int ERODE_EVERY = 10;      // tectonic steps between erosion passes while the plates move
    static final float TECTONIC_EROSION = 1; // times Hydrology.ERODIBILITY; stronger flattens the continents and drains their lakes
    private static final int FORMAT = 70; // 2 volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers, 6 shelves, 7 deltas, 8 currents, 9 compact shields, 10 curved rivers, 11 landforms, 12 rich biomes, 13 forest variety, 14 gullies, 15 massifs, 16 wild rivers, 17 foothills and folds, 18 climate snow line, 19 talus, 20 perched banks, 21 calderas and cinder cones, 22 simulated calderas, 23 flowing glaciers (no new data), 24 seasons (amplitude, summerRain), 25 gentle lake shores, 26 craters only on their own volcano, 27 smooth glacier ice, 28 blended troughs and lake sills, 29 block-scale rockfall, 30 lakes leave high land standing, stable rockfall, no peaks over calderas or lakes, 31 lake shores ramp up to high land, 32 strike-slip faults (faultSide, faultSlip), 33 gullies cut by water at block scale, 34 folded strata, 35 ridges and trenches (abyss squeezed), 36 varied volcanoes, 37 varied beaches and no gully pits, 38 lake-effect rain, green riverbanks, ice-free active craters, 39 supercontinent cycle (sim only), 40 flowing ice, 41 runoff spreads over even slopes, 42 smooth collapse scars and crater floors, lakes one biome, 43 faster distances, 44 badlands only on high sedimentary rock, 45 snow, lake ice and snowy taiga only where frozen two-thirds of the year, 46 base level: relief never digs below rivers, 47 no gravel on top, 48 open river mouths, 49 no deltas, 50 river channels open into the sea, 51 oxbows only on floodplains, 52 braided rivers keep a channel down the middle, 53 biome ground where gravel was, 54 still one-block river steps, 55 glacier ice follows the slope, 56 over the blurred rock, 57 snow and firn on glaciers above the snow line, 58 magma patches on active volcanoes, 59 noise caves, 60 cave biomes, 61 base level fades from rivers, 62 plate sizes as Earth's (sim only), 63 cones fade across caldera walls, 64 plates, their motion and the drift recording saved (no terrain change), 65 orographic rain thins with height (sim only), 66 young rifts over plumes and stepped rift walls (riftFloor), 67 side valleys along every small drainage link, 68 ores by rock province, 69 rift walls terraced by height (no ribs), 70 no young rifts nor stepped rift walls (taken out again); older files load without them

    public final int w, h;
    public final float[] metres;        // ground, relative to sea level
    public final float[] lakeMetres;    // lake surface, NaN where there is no lake
    public final float[] flow;          // river discharge, see Hydrology.flow
    public final int[] receiver;        // next cell downstream, -1 at the sea
    public final float[] temperature;   // °C at the cell's own elevation
    public final float[] precipitation; // mm per year
    public final List<Volcano> volcanoes = new ArrayList<>();
    public final byte[] rock;           // Rock ordinal: the geological province
    public final byte[] ice;            // Hydrology.GLACIATED or FJORD, else 0
    public final float[] amplitude;     // °C half the summer-winter range (format 24; 0 before)
    public final float[] summerRain;    // share of the rain falling in the warm half of the year (format 24; 0.5 before)
    public final float[] faultSide;     // cells to the trace of a strike-slip fault, signed by side, up to Planet.FAULT_REACH (format 32)
    public final float[] faultSlip;     // that fault's slip rate in cells per step, spread over its zone; 0 away from faults (format 32)
    public final byte[] plate;          // plate under each cell at the end of the run, unsigned (format 64)
    public float[] plateVx = new float[0], plateVy = new float[0]; // each plate's motion, cells per step (format 64; 0 on Earth)
    public final List<Drift> drift = new ArrayList<>(); // the plates over the run, Drift.FRAMES of them (format 64; none on Earth)
    public final float[] riftFloor;     // 0 to 1 across a rift's floor, Hydrology.riftFloor (format 66; 0 before)
    public int format = FORMAT;         // of the file this planet came from: older planets keep their old block heights

    private PlanetData(int w, int h) {
        this.w = w;
        this.h = h;
        metres = new float[w * h];
        lakeMetres = new float[w * h];
        flow = new float[w * h];
        receiver = new int[w * h];
        temperature = new float[w * h];
        precipitation = new float[w * h];
        rock = new byte[w * h]; // all MIXED until set
        ice = new byte[w * h];
        amplitude = new float[w * h];
        summerRain = new float[w * h];
        java.util.Arrays.fill(summerRain, 0.5f);
        faultSide = new float[w * h];
        faultSlip = new float[w * h];
        plate = new byte[w * h];
        riftFloor = new float[w * h];
    }

    public static PlanetData from(Planet planet, Climate climate, Hydrology hy) {
        PlanetData d = new PlanetData(hy.w, hy.h);
        d.volcanoes.addAll(hy.volcanoes);
        for (int g = 0; g < d.metres.length; g++) {
            d.metres[g] = Planet.metres(hy.elevation[g]);
            d.lakeMetres[g] = hy.isLake(g) ? Planet.metres(hy.lakeLevel(g)) : Float.NaN;
        }
        System.arraycopy(hy.flow, 0, d.flow, 0, d.flow.length);
        System.arraycopy(hy.receiver, 0, d.receiver, 0, d.receiver.length);
        System.arraycopy(hy.ice, 0, d.ice, 0, d.ice.length);
        System.arraycopy(hy.riftFloor, 0, d.riftFloor, 0, d.riftFloor.length);
        // Climate saw the land before the last erosion, glaciers and volcanoes reshaped it: move each cell's temperature
        // to its final height, or a worn-down range keeps its peaks' cold and snowy taiga lands next to desert.
        for (int g = 0; g < d.temperature.length; g++)
            d.temperature[g] = climate.temperature[g] + Climate.LAPSE_RATE
                    * (Math.max(Planet.metres(planet.height[g] - Planet.SEA_LEVEL), 0) - Math.max(d.metres[g], 0));
        climate.lakeEffect(planet, hy::isLake);
        System.arraycopy(climate.precipitation, 0, d.precipitation, 0, d.precipitation.length);
        System.arraycopy(climate.amplitude, 0, d.amplitude, 0, d.amplitude.length);
        System.arraycopy(climate.summerRain, 0, d.summerRain, 0, d.summerRain.length);
        System.arraycopy(Geology.provinces(planet, d.metres, d.flow, d.lakeMetres), 0, d.rock, 0, d.rock.length);
        float[][] faults = planet.faultZones();
        System.arraycopy(faults[0], 0, d.faultSide, 0, d.faultSide.length);
        System.arraycopy(faults[1], 0, d.faultSlip, 0, d.faultSlip.length);
        for (int g = 0; g < d.plate.length; g++) d.plate[g] = (byte) planet.owner[g];
        d.plateVx = new float[planet.plates.length];
        d.plateVy = new float[planet.plates.length];
        for (int p = 0; p < planet.plates.length; p++) {
            d.plateVx[p] = planet.plates[p].vx;
            d.plateVy[p] = planet.plates[p].vy;
        }
        return d;
    }

    /** Runs the whole simulation. Reports progress 0..1; interrupting the thread cancels it between steps. */
    public static PlanetData generate(long seed, PlanetSettings settings, DoubleConsumer progress) {
        return generate(seed, settings, progress, p -> {});
    }

    /** As above, handing the planet to step after each tectonic step (on this thread), to watch the plates drift. */
    public static PlanetData generate(long seed, PlanetSettings settings, DoubleConsumer progress, Consumer<Planet> step) {
        Planet planet = settings.earth() ? Planet.earth(settings.width(), settings.height(), seed)
                : new Planet(settings.width(), settings.height(), settings.plates(), settings.oceanFraction(), settings.volcanoes(), seed);
        List<Drift> drift = new ArrayList<>();
        if (!planet.earth) {
            int steps = settings.steps();
            int[] done = {0};
            drift.add(Drift.of(planet));
            tectonics(planet, steps, f -> {
                progress.accept(PLATES_SHARE * f);
                if (++done[0] * (Drift.FRAMES - 1) / steps >= drift.size()) drift.add(Drift.of(planet)); // evenly, the last at the end
                step.accept(planet);
            });
        }
        Climate climate = new Climate(planet);
        Hydrology hy = new Hydrology(planet, climate.precipitation, climate.anomaly, climate.amplitude, planet.earth ? EARTH_EROSION : EROSION, seed,
                done -> progress.accept(PLATES_SHARE + (1 - PLATES_SHARE) * done));
        progress.accept(1);
        PlanetData d = from(planet, climate, hy);
        d.drift.addAll(drift);
        return d;
    }

    /** Plates moving, with rivers wearing the land down as it rises. Reports progress 0..1; interrupting cancels. */
    public static void tectonics(Planet planet, int steps, DoubleConsumer progress) {
        planet.plan(steps);
        for (int i = 0; i < steps; i++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            planet.step();
            if ((i + 1) % ERODE_EVERY == 0) Hydrology.erodeCrust(planet, 1, Hydrology.ERODIBILITY * TECTONIC_EROSION);
            progress.accept((i + 1) / (double) steps);
        }
    }

    private float[][] floats() {
        return new float[][]{metres, lakeMetres, flow, temperature, precipitation};
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(file))))) {
            out.writeInt(FORMAT);
            out.writeInt(w);
            out.writeInt(h);
            for (float[] a : floats()) for (float v : a) out.writeFloat(v);
            for (int v : receiver) out.writeInt(v);
            out.writeInt(volcanoes.size());
            for (Volcano v : volcanoes) {
                out.writeFloat(v.u());
                out.writeFloat(v.v());
                out.writeBoolean(v.hotspot());
                out.writeInt(v.age());
                out.writeFloat(v.size());
                out.writeFloat(v.baseMetres());
            }
            out.write(rock);
            out.write(ice);
            for (float v : amplitude) out.writeFloat(v);
            for (float v : summerRain) out.writeFloat(v);
            for (float v : faultSide) out.writeFloat(v);
            for (float v : faultSlip) out.writeFloat(v);
            out.write(plate);
            out.writeInt(plateVx.length);
            for (int p = 0; p < plateVx.length; p++) {
                out.writeFloat(plateVx[p]);
                out.writeFloat(plateVy[p]);
            }
            out.writeInt(drift.size());
            for (Drift f : drift) {
                out.writeInt(f.w());
                out.writeInt(f.h());
                out.write(f.ground());
                out.write(f.plate());
            }
            for (float v : riftFloor) out.writeFloat(v);
        }
    }

    public static PlanetData load(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file))))) {
            int format = in.readInt();
            if (format < 1 || format > FORMAT) throw new IOException("Unsupported planet file format " + format + ": " + file);
            PlanetData d = new PlanetData(in.readInt(), in.readInt());
            d.format = format;
            for (float[] a : d.floats()) for (int i = 0; i < a.length; i++) a[i] = in.readFloat();
            for (int i = 0; i < d.receiver.length; i++) d.receiver[i] = in.readInt();
            if (format >= 2)
                for (int n = in.readInt(); n > 0; n--) {
                    float u = in.readFloat(), v = in.readFloat();
                    boolean hotspot = in.readBoolean();
                    d.volcanoes.add(new Volcano(u, v, hotspot, in.readInt(), in.readFloat(), format >= 4 ? in.readFloat() : Float.NaN,
                            Volcano.spread(hotspot, format >= 9)));
                }
            if (format >= 3) in.readFully(d.rock);
            if (format >= 5) in.readFully(d.ice);
            if (format >= 24) {
                for (int i = 0; i < d.amplitude.length; i++) d.amplitude[i] = in.readFloat();
                for (int i = 0; i < d.summerRain.length; i++) d.summerRain[i] = in.readFloat();
            }
            if (format >= 32) {
                for (int i = 0; i < d.faultSide.length; i++) d.faultSide[i] = in.readFloat();
                for (int i = 0; i < d.faultSlip.length; i++) d.faultSlip[i] = in.readFloat();
            }
            if (format >= 64) {
                in.readFully(d.plate);
                d.plateVx = new float[in.readInt()];
                d.plateVy = new float[d.plateVx.length];
                for (int p = 0; p < d.plateVx.length; p++) {
                    d.plateVx[p] = in.readFloat();
                    d.plateVy[p] = in.readFloat();
                }
                for (int n = in.readInt(); n > 0; n--) {
                    int fw = in.readInt(), fh = in.readInt();
                    byte[] ground = new byte[fw * fh], plate = new byte[fw * fh];
                    in.readFully(ground);
                    in.readFully(plate);
                    d.drift.add(new Drift(fw, fh, ground, plate));
                }
            }
            if (format >= 66) for (int i = 0; i < d.riftFloor.length; i++) d.riftFloor[i] = in.readFloat();
            return d;
        }
    }
}
