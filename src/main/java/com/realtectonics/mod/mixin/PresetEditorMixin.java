package com.realtectonics.mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.realtectonics.mod.client.PlanetScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adds the "Customize" screen for Tectonic Planet worlds. Vanilla builds its editors with Map.of in PresetEditor's
 * static initializer, and Fabric API has no hook for them, so the map is copied with ours added.
 */
@Mixin(PresetEditor.class)
interface PresetEditorMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"))
    private static Map<Object, Object> realtectonics$addPlanetEditor(Map<Object, Object> editors) {
        Map<Object, Object> all = new HashMap<>(editors);
        all.put(Optional.of(PlanetScreen.PRESET), (PresetEditor) PlanetScreen::create);
        return Map.copyOf(all);
    }
}
