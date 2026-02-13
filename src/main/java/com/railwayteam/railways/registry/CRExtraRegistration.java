/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
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

package com.railwayteam.railways.registry;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.distant_signals.SignalDisplaySource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;


public class CRExtraRegistration {
    public static boolean registeredSignalSource = false;
    public static boolean registeredVentAsCopycat = false;

    public static void addVentAsCopycat(BlockEntityType<?> object) {
        // Vent block was removed from this fork
    }

    public static void addSignalSource(Block block) {
        if (registeredSignalSource) return;
        SignalDisplaySource source = new SignalDisplaySource();
        Railways.registrate().displaySource("track_signal_source", () -> source).register();
        DisplaySource.BY_BLOCK.add(block, source);
        registeredSignalSource = true;
    }    public static void platformSpecificRegistration() {
    com.railwayteam.railways.registry.neoforge.CRExtraRegistrationImpl.platformSpecificRegistration();
    }
}
