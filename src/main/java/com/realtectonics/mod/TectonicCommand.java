package com.realtectonics.mod;

import com.realtectonics.sim.Survey;
import com.realtectonics.sim.Terrain;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /tectonic: what the planet says about where you stand (Survey). Anyone may ask; it changes nothing. */
final class TectonicCommand {
    private TectonicCommand() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tectonic").executes(ctx -> {
            Terrain t = RealTectonics.terrainOrNull();
            if (t == null) {
                ctx.getSource().sendFailure(Component.literal("This isn't a Tectonic Planet world."));
                return 0;
            }
            Vec3 at = ctx.getSource().getPosition();
            for (String line : Survey.at(t, (int) Math.floor(at.x), (int) Math.floor(at.z)))
                ctx.getSource().sendSuccess(() -> Component.literal(line), false);
            return 1;
        }));
    }
}
