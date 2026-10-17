package com.realtectonics.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/** Client setup: the planet map key. The world-type editor is wired in by PresetEditorMixin. */
public final class RealTectonicsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(MapScreen.KEY);
        ClientTickEvents.END_CLIENT_TICK.register(MapScreen::onClientTick);
    }
}
