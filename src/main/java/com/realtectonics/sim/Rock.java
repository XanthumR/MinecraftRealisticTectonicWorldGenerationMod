package com.realtectonics.sim;

/** Geological provinces: what the bedrock is, decided by the plate history. The mod layer picks the blocks. */
public enum Rock {
    MIXED(0x888888),    // planets saved before provinces existed: the old all-purpose mix
    OCEANIC(0x34404f),  // ocean crust: basalt and its deep, dark relatives
    CRATON(0xd29b98),   // old, stable continental cores: granite
    OROGEN(0x7d5fa8),   // collision belts: folded, metamorphosed rock
    ARC(0xc0623a),      // behind subduction zones: andesite and volcanic ash
    BASIN(0xe0c97a);    // lowlands where sediment collects: sandstone and limestone

    public final int color;

    Rock(int color) {
        this.color = color;
    }

    private static final Rock[] VALUES = values();

    public static Rock of(byte ordinal) {
        return VALUES[ordinal];
    }
}
