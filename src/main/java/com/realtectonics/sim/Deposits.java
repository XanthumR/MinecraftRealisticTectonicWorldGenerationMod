package com.realtectonics.sim;

import java.util.List;

/**
 * Ore deposits by geological province (format 68), on top of the vanilla ores: what the rock's history concentrated.
 * Each chunk gets perChunk veins of each (the fraction as a chance of one more) between minY and maxY. The mod layer
 * picks the blocks by name.
 */
public final class Deposits {
    public enum Form {
        VEIN, // a blob of ore in the rock
        SEAM, // a flat bed across the chunk: coal laid down as peat in old swamps
        PIPE  // a narrow vertical pipe of diamond-bearing kimberlite, blasted up from the mantle under old cratons
    }

    public record Deposit(String ore, float perChunk, int minY, int maxY, int size, Form form) {}

    private static final List<Deposit> OCEANIC = List.of(
            new Deposit("copper", 1.5f, -48, 40, 6, Form.VEIN));      // black smokers on the ridge left copper sulphides (Cyprus)
    private static final List<Deposit> CRATON = List.of(
            new Deposit("iron", 5, -24, 72, 10, Form.VEIN),           // banded iron, from the first oxygen in old seas (Pilbara)
            new Deposit("gold", 1.5f, -48, 32, 6, Form.VEIN),         // greenstone belts (the Witwatersrand, the Yilgarn)
            new Deposit("diamond", 0.06f, -58, 16, 0, Form.PIPE));    // kimberlite pipes only pierce thick old roots
    private static final List<Deposit> OROGEN = List.of(
            new Deposit("emerald", 3, 0, 200, 3, Form.VEIN),          // beryl in the baked rock of collision belts
            new Deposit("lapis", 1.5f, -32, 32, 6, Form.VEIN),        // lazurite in metamorphosed limestone (Afghanistan)
            new Deposit("gold", 1, -16, 64, 5, Form.VEIN));           // orogenic gold in the shear zones
    private static final List<Deposit> ARC = List.of(
            new Deposit("copper", 10, -16, 96, 12, Form.VEIN),        // porphyry copper above the diving plate (the Andes): about twice vanilla's
            new Deposit("gold", 2, -32, 48, 6, Form.VEIN),            // and the gold that comes with it
            new Deposit("redstone", 1.5f, -48, 16, 6, Form.VEIN));
    private static final List<Deposit> BASIN = List.of(
            new Deposit("coal", 1, 0, 60, 2, Form.SEAM),              // size: the seam's thickness
            new Deposit("iron", 1, 0, 60, 6, Form.VEIN));             // bog iron and ironstone in the beds

    private Deposits() {}

    public static List<Deposit> of(Rock r) {
        return switch (r) {
            case OCEANIC -> OCEANIC;
            case CRATON -> CRATON;
            case OROGEN -> OROGEN;
            case ARC -> ARC;
            case BASIN -> BASIN;
            case MIXED -> List.of();
        };
    }
}
