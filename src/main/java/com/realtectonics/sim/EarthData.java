package com.realtectonics.sim;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;

/**
 * Earth as it is today, for the "Earth" planet: elevation from the AWS terrarium tiles, plates and their boundaries
 * from PB2002 (Bird 2003, via github.com/fraxen/tectonicplates). tools/earthdata.py builds the files under
 * /realtectonics/earth/ in the planet's own projection (columns by longitude from -180, equal-area rows as
 * Climate.latitude), so they only need resampling to the planet's width.
 */
final class EarthData {
    static final int SOURCE_W = 2048, SOURCE_H = 1024;
    // PB2002 step classes, in the order of tools/earthdata.py.
    static final int RIDGE = 0, OCEAN_TRANSFORM = 1, SUBDUCTION = 2, RIFT = 3, CONTINENT_TRANSFORM = 4, CONTINENT_COLLISION = 5, OCEAN_COLLISION = 6;
    static final float SLIP_PER_MM = 0.01f;  // cells per step of the sim's fault slip for each mm/yr of real sideways motion (35 mm/yr San Andreas: 0.35)
    static final float BELT_ACTIVITY = 10;   // collision history marked on a colliding boundary: Geology's blur widens it into a mountain belt
    static final float TRENCH_ACTIVITY = 3 * Planet.ARC_ACTIVITY; // subduction history marked along a trench: arc rock behind it
    static final float OROGEN_METRES = 1500, OROGEN_REACH_KM = 1000; // high ground this close to a converging boundary is a mountain belt
    static final float INLAND_LOW = 1;      // metres given to ground below sea level that the ocean does not reach
    static final float INLAND_LAKE = -100;  // metres: a cut-off basin this deep (cell mean) is a lake, shallower ones (Qattara, Turpan) dry
    static final float OVERRIDING_METRES = -3000; // beside a trench, cells above this are the overriding plate (arc or continent), the rest the sea floor diving

    final int w, h, plateCount;
    final float[] elevation, slip, subduction, collision, spreading; // metres; cells/step of fault slip; the simulation's histories
    final int[] plate;
    final boolean[] lake; // Natural Earth's lakes: at least half the cell is lake

    /** Earth resampled to w x h cells (w a power of two from 512 to 4096, h = w / 2). */
    EarthData(int w, int h) {
        this.w = w;
        this.h = h;
        short[] src = new short[SOURCE_W * SOURCE_H];
        byte[] plates = new byte[SOURCE_W * SOURCE_H];
        try (DataInputStream in = open("elevation.i16.gz")) {
            for (int i = 0; i < src.length; i++) src[i] = in.readShort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] lakes = new byte[SOURCE_W * SOURCE_H];
        try (DataInputStream in = open("plates.u8.gz"); DataInputStream inLakes = open("lakes.u8.gz")) {
            in.readFully(plates);
            inLakes.readFully(lakes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        lake = new boolean[w * h];
        elevation = new float[w * h];
        plate = new int[w * h];
        int count = 0;
        double fx = SOURCE_W / (double) w, fy = SOURCE_H / (double) h;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                // The mean of the source cells under this one (one source cell, nearest, when the planet is finer).
                int x0 = (int) (x * fx), x1 = Math.max(x0 + 1, (int) ((x + 1) * fx)), y0 = (int) (y * fy), y1 = Math.max(y0 + 1, (int) ((y + 1) * fy));
                double sum = 0;
                int wet = 0;
                for (int sy = y0; sy < y1; sy++)
                    for (int sx = x0; sx < x1; sx++) { sum += src[sx + sy * SOURCE_W]; wet += lakes[sx + sy * SOURCE_W]; }
                elevation[x + y * w] = (float) (sum / ((x1 - x0) * (y1 - y0)));
                lake[x + y * w] = 2 * wet >= (x1 - x0) * (y1 - y0);
                int p = plates[(int) ((x + 0.5) * fx) + (int) ((y + 0.5) * fy) * SOURCE_W] & 0xff;
                plate[x + y * w] = p == 255 ? 0 : p;
                count = Math.max(count, plate[x + y * w] + 1);
            }
        plateCount = count;
        // Below sea level is sea only where the ocean reaches: the Caspian depression, the Dead Sea and the Qattara are
        // dry land (the Caspian then a lake, from its lake cells), not drowned at sea level.
        boolean[] ocean = new boolean[w * h];
        int[] queue = new int[w * h];
        int n = 0, deepest = 0;
        for (int g = 0; g < w * h; g++) if (elevation[g] < elevation[deepest]) deepest = g;
        ocean[deepest] = true;
        queue[n++] = deepest;
        for (int i = 0; i < n; i++) {
            int c = queue[i], x = c % w, y = c / w;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? c - w : -1, y < h - 1 ? c + w : -1})
                if (nb >= 0 && !ocean[nb] && elevation[nb] < 0) { ocean[nb] = true; queue[n++] = nb; }
        }
        for (int g = 0; g < w * h; g++)
            if (elevation[g] < 0 && !ocean[g]) { // deep cut-off basins hold water (the Caspian's floor, the Dead Sea); shallow ones are dry
                if (elevation[g] < INLAND_LAKE) lake[g] = true;
                elevation[g] = INLAND_LOW;
            }
        slip = new float[w * h];
        subduction = new float[w * h];
        collision = new float[w * h];
        spreading = new float[w * h];
        float[] stepSlip = new float[w * h];
        try (DataInputStream in = open("steps.f32.gz")) {
            while (true) { // to the end of the file
                float lon0, lat0, lon1, lat1, kind, diverge, right;
                try {
                    lon0 = in.readFloat(); lat0 = in.readFloat(); lon1 = in.readFloat(); lat1 = in.readFloat();
                    kind = in.readFloat(); diverge = in.readFloat(); right = in.readFloat();
                } catch (java.io.EOFException end) {
                    break;
                }
                mark((int) kind, lon0, lat0, lon1, lat1, Math.abs(right) * SLIP_PER_MM, stepSlip);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Mountain belts: the high ground near converging boundaries is the folded, thickened crust of the collision
        // (Himalaya and Tibet, Alps, Zagros, Andes), wider than the boundary line PB2002 draws.
        int[] reach = new int[w * h], wave = new int[w * h];
        java.util.Arrays.fill(reach, Integer.MAX_VALUE);
        int m = 0;
        for (int g = 0; g < w * h; g++) if (collision[g] > 0 || subduction[g] > 0) { reach[g] = 0; wave[m++] = g; }
        int belt = Math.round(OROGEN_REACH_KM / (40030f / w));
        for (int i = 0; i < m; i++) {
            int c = wave[i], x = c % w, y = c / w;
            if (reach[c] >= belt) continue;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? c - w : -1, y < h - 1 ? c + w : -1})
                if (nb >= 0 && reach[nb] == Integer.MAX_VALUE) { reach[nb] = reach[c] + 1; wave[m++] = nb; }
        }
        for (int g = 0; g < w * h; g++) if (reach[g] <= belt && elevation[g] > OROGEN_METRES) collision[g] = BELT_ACTIVITY;
        // Fault slip sits on the plate edge itself, as the simulation's own strikeSlip puts it.
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int g = x + y * w;
                boolean edge = false;
                for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : g, y < h - 1 ? g + w : g})
                    edge |= plate[nb] != plate[g];
                if (edge) slip[g] = stepSlip[g];
            }
    }

    /** One boundary step, drawn into the fields along its length (and a cell either side, as cells are coarse). */
    private void mark(int kind, float lon0, float lat0, float lon1, float lat1, float slipCells, float[] stepSlip) {
        double x0 = col(lon0), y0 = row(lat0), x1 = col(lon1), y1 = row(lat1);
        if (x1 - x0 > w / 2.0) x1 -= w; // across the date line
        if (x0 - x1 > w / 2.0) x1 += w;
        int n = (int) Math.ceil(2 * Math.hypot(x1 - x0, y1 - y0)) + 1;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            int cx = (int) Math.floor(x0 + (x1 - x0) * t), cy = (int) Math.floor(y0 + (y1 - y0) * t);
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int y = cy + dy;
                    if (y < 0 || y >= h) continue;
                    int g = Math.floorMod(cx + dx, w) + y * w;
                    switch (kind) {
                        case SUBDUCTION -> { if (elevation[g] > OVERRIDING_METRES) subduction[g] = TRENCH_ACTIVITY; }
                        case CONTINENT_COLLISION, OCEAN_COLLISION -> { if (dx == 0 && dy == 0) collision[g] = BELT_ACTIVITY; }
                        case RIDGE, RIFT -> spreading[g] = 2 * Planet.RIDGE_ACTIVITY;
                        case OCEAN_TRANSFORM, CONTINENT_TRANSFORM -> stepSlip[g] = Math.max(stepSlip[g], slipCells);
                        default -> { }
                    }
                }
        }
    }

    private double col(double lon) {
        return (lon + 180) / 360 * w;
    }

    private double row(double lat) {
        return (1 - Math.sin(Math.toRadians(lat))) * h / 2;
    }

    private static DataInputStream open(String name) throws IOException {
        InputStream raw = EarthData.class.getResourceAsStream("/realtectonics/earth/" + name);
        if (raw == null) throw new IOException("missing Earth data " + name + ": run tools/earthdata.py");
        return new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw), 1 << 16));
    }
}
