/*
 * Steam 'n' Rails
 * Copyright (c) 2025 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.railwayteam.railways.ponder;

import com.railwayteam.railways.ponder.scenes.DoorScenes;
import com.railwayteam.railways.ponder.scenes.TrainScenes;
import com.railwayteam.railways.registry.CRBlocks;
import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;


public class CRPonderIndex {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // 1.21+: Ponder helpers are keyed by ResourceLocation; adapt registrate entries via key function
        PonderSceneRegistrationHelper<RegistryEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        // Train semaphore overview
        HELPER.forComponents(CRBlocks.SEMAPHORE)
            .addStoryBoard("train_semaphore", TrainScenes::signaling);

        // Train coupler usage
        HELPER.forComponents(CRBlocks.TRACK_COUPLER)
            .addStoryBoard("train_coupler", TrainScenes::coupling);
        // Sliding and train doors: mode explanation
        HELPER.forComponents(
            AllBlocks.ANDESITE_DOOR,
            AllBlocks.BRASS_DOOR,
            AllBlocks.COPPER_DOOR,
            AllBlocks.TRAIN_DOOR,
            AllBlocks.FRAMED_GLASS_DOOR
        ).addStoryBoard("door_modes", DoorScenes::modes);

        // Track switches
        HELPER.forComponents(CRBlocks.ANDESITE_SWITCH, CRBlocks.BRASS_SWITCH)
            .addStoryBoard("train_switch", TrainScenes::trackSwitch);
    }
}
