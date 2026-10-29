package com.realtectonics.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What the planet says about one spot, as lines of text (/tectonic): the plate and how it moves, the rock and how it
 * formed, the nearest plate boundary and what happens there, the nearest volcano and glacier.
 */
public final class Survey {
    static final double YEARS_PER_STEP = 4e5;    // ponytail: the sim keeps no clock; 0.4 My a step puts the mean plate at Earth's ~4 cm/yr
    static final double EARTH_KM = 40_075;       // the planet is Earth-sized round the equator
    static final int BOUNDARY_REACH = 64;        // cells searched for the nearest plate boundary
    static final int GLACIER_REACH = 128;        // cells searched for the nearest glacier

    private Survey() {}

    public static List<String> at(Terrain t, int x, int z) {
        PlanetData d = t.data;
        double[] p = t.mapPosition(x, z);
        int w = d.w, h = d.h, cu = (int) p[0] % w, cv = Math.clamp((int) p[1], 0, h - 1), g = cu + cv * w;
        double kmPerCell = EARTH_KM / w;
        Terrain.Column c = t.column(x, z);
        List<String> out = new ArrayList<>();
        out.add(String.format("%s, %.0f m, %.1f °C, %.0f mm of rain a year", t.biomeAt(x, z), d.metres[g], d.temperature[g], d.precipitation[g]));
        out.add("Rock: " + describe(c.rock()) + (c.volcanic() ? ", fresh volcanic ground" : ""));
        if (d.format < 64) {
            out.add("This world's planet was made before plates were saved (format " + d.format + "): no plate details.");
            return out;
        }
        int mine = d.plate[g] & 255;
        double vx = d.plateVx[mine], vy = d.plateVy[mine];
        boolean moving = Math.hypot(vx, vy) > 0;
        out.add(String.format("Plate %d", mine) + (moving ? String.format(", drifting %s at %.1f cm/yr", compass(vx, vy), cmPerYear(Math.hypot(vx, vy), kmPerCell))
                : " (Earth: plate motion not simulated)"));

        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int dy = -BOUNDARY_REACH; dy <= BOUNDARY_REACH; dy++)
            for (int dx = -BOUNDARY_REACH; dx <= BOUNDARY_REACH; dx++) {
                int y = cv + dy, e = Math.floorMod(cu + dx, w) + y * w;
                double dist = Math.hypot(dx, dy);
                if (y >= 0 && y < h && dist < bestD && (d.plate[e] & 255) != mine) { best = e; bestD = dist; }
            }
        if (best < 0) out.add("No plate boundary within " + blocks(BOUNDARY_REACH, t) + " blocks.");
        else {
            int other = d.plate[best] & 255;
            double dx = wrap(best % w - cu, w), dy = best / w - cv, n = Math.max(1e-9, Math.hypot(dx, dy));
            double rx = d.plateVx[other] - vx, ry = d.plateVy[other] - vy; // the other plate as this one sees it
            double closing = -(rx * dx + ry * dy) / n, sliding = Math.abs(rx * dy - ry * dx) / n;
            boolean landHere = d.metres[g] >= 0, landThere = d.metres[best] >= 0;
            String what = !moving ? "plate " + other + " meets this one"
                    : Math.abs(closing) < sliding ? String.format("sliding past plate %d at %.1f cm/yr: a strike-slip fault", other, cmPerYear(sliding, kmPerCell))
                    : closing < 0 ? String.format("pulling apart from plate %d at %.1f cm/yr: %s", other, cmPerYear(-closing, kmPerCell), landHere && landThere ? "a rift valley" : "a spreading ridge")
                    : String.format("converging with plate %d at %.1f cm/yr: %s", other, cmPerYear(closing, kmPerCell),
                    landHere && landThere ? "continents collide and mountains rise" : landHere || landThere ? "ocean crust dives under, with a trench and volcanoes" : "one ocean plate dives under the other: an island arc");
            out.add(String.format("Boundary %d blocks %s, %s", blocks(bestD, t), compass(dx, dy), what));
        }

        Volcano near = null;
        double nearD = Double.MAX_VALUE;
        for (Volcano v : d.volcanoes) {
            double dist = Math.hypot(wrap(v.u() - p[0], w), v.v() - p[1]);
            if (dist < nearD) { near = v; nearD = dist; }
        }
        if (near != null) out.add(String.format("Nearest volcano: %d blocks %s, %s%s", blocks(nearD, t), compass(wrap(near.u() - p[0], w), near.v() - p[1]),
                near.active() ? "active" : "extinct", near.hotspot() ? " (a hotspot)" : " (above a subducting plate)"));

        int ice = -1;
        double iceD = Double.MAX_VALUE;
        for (int dy = -GLACIER_REACH; dy <= GLACIER_REACH; dy++)
            for (int dx = -GLACIER_REACH; dx <= GLACIER_REACH; dx++) {
                int y = cv + dy, e = Math.floorMod(cu + dx, w) + y * w;
                double dist = Math.hypot(dx, dy);
                if (y >= 0 && y < h && dist < iceD && d.ice[e] == Hydrology.GLACIATED) { ice = e; iceD = dist; }
            }
        out.add(ice < 0 ? "No glacier within " + blocks(GLACIER_REACH, t) + " blocks."
                : iceD == 0 ? "Glacier ice here." : String.format("Nearest glacier: %d blocks %s", blocks(iceD, t), compass(wrap(ice % w - cu, w), ice / w - cv)));
        return out;
    }

    static String describe(Rock r) {
        return switch (r) {
            case OCEANIC -> "ocean crust, basalt made at a spreading ridge";
            case CRATON -> "an old continental core, granite that has stood still for ages";
            case OROGEN -> "a collision belt, rock folded and baked where continents met";
            case ARC -> "a volcanic arc, andesite and ash from a plate diving underneath";
            case BASIN -> "a sedimentary basin, sandstone and limestone laid down by rivers and seas";
            case MIXED -> "mixed rock (an old world, before rock provinces)";
        };
    }

    /** Cells per step to cm a year. */
    static double cmPerYear(double cells, double kmPerCell) {
        return cells * kmPerCell * 1e5 / YEARS_PER_STEP;
    }

    static int blocks(double cells, Terrain t) {
        return (int) Math.round(cells * t.blocksPerCell);
    }

    static double wrap(double du, int w) {
        return ((du % w) + 1.5 * w) % w - w / 2.0;
    }

    /** Map direction: +x east, +y south (as block z). */
    static String compass(double dx, double dy) {
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return names[Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dx, -dy)) / 45), 8)];
    }
}
