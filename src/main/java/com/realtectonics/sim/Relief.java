package com.realtectonics.sim;

import java.util.Arrays;

/**
 * The planet map seen from the side (the 3D view on the planet screen): each cell's colour raised to the height the
 * game builds it, in the game's own proportions (a cell is blocksPerCell blocks across). Drawn as a voxel-space
 * heightfield: per image column, cells front to back, each filling the pixels above what nearer land already covers.
 * ponytail: one height per cell (sim metres, not the blocks' relief); fine for a rough look, use Terrain.column for detail.
 */
public final class Relief {
    public static final int SKY = 0x101820;

    /** Each cell's height above sea level in cells, as the game builds it; the sea flat at sea level. */
    public static float[] heights(PlanetData d, float mountainScale, int blocksPerCell) {
        float[] out = new float[d.w * d.h];
        for (int g = 0; g < out.length; g++)
            out[g] = d.metres[g] <= 0 ? 0 : (Terrain.peakY(d.metres[g], mountainScale) - Terrain.SEA_Y) / (float) blocksPerCell;
        return out;
    }

    /**
     * rgb (w x h cells) on an outW x outH image, looking at cell (cu, cv): yaw 0 faces north (as the flat map), pitch is
     * the view's angle above the horizon (PI/2 straight down), radius the cells across half the image. Wraps east-west.
     */
    public static int[] render(int[] rgb, float[] height, int w, int h, double cu, double cv, double yaw, double pitch, double radius, int outW, int outH) {
        int[] out = new int[outW * outH];
        Arrays.fill(out, SKY);
        float top = 0;
        for (float x : height) top = Math.max(top, x);
        double s = outW / (2 * radius), sinP = Math.sin(pitch), cosP = Math.cos(pitch), cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        double db = 1 / (s * sinP), near = -outH / 2.0 / (s * sinP) - top * cosP / sinP, far = outH / 2.0 / (s * sinP); // one pixel of flat land a step
        for (int px = 0; px < outW; px++) {
            double a = (px + 0.5 - outW / 2.0) / s;
            int covered = outH; // the highest row nearer land has filled
            for (double b = near; b < far && covered > 0; b += db) {
                double u = cu + a * cosY + b * sinY, v = cv + a * sinY - b * cosY;
                if (v < 0 || v >= h) continue; // past the poles
                // bilinear between the four nearest cells, so a close view isn't a stack of boxes
                double fu = u - 0.5, fv = Math.clamp(v - 0.5, 0, h - 1.001);
                int iu = (int) Math.floor(fu), iv = (int) fv, g0 = Math.floorMod(iu, w) + iv * w, g1 = Math.floorMod(iu + 1, w) + iv * w;
                double tx = fu - iu, ty = fv - iv;
                double z = (height[g0] * (1 - tx) + height[g1] * tx) * (1 - ty) + (height[g0 + w] * (1 - tx) + height[g1 + w] * tx) * ty;
                int base = (int) (outH / 2.0 - b * sinP * s) + 1, sy = Math.max(0, (int) (outH / 2.0 - (b * sinP + z * cosP) * s));
                if (sy >= covered) continue;
                int c = mix(mix(rgb[g0], rgb[g1], tx), mix(rgb[g0 + w], rgb[g1 + w], tx), ty);
                for (int y = sy, end = Math.min(covered, base); y < end; y++) out[px + y * outW] = c; // down to its own foot: no smear past the poles
                covered = sy;
            }
        }
        return out;
    }

    private static int mix(int a, int b, double t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t), g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        return r << 16 | g << 8 | (int) ((a & 255) * (1 - t) + (b & 255) * t);
    }
}
