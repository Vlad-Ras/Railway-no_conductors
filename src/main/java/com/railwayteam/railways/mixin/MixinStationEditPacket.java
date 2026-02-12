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

package com.railwayteam.railways.mixin;

import com.railwayteam.railways.mixin_interfaces.ILimited;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationEditPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Create's StationEditPacket.
 *
 * Note: In Create 1.21, the packet serialization system changed from writeSettings/readSettings
 * to StreamCodec. Station limiting is now handled by a separate StationLimitPacket.
 * This mixin only handles the applySettings hook for compatibility.
 */
@Mixin(value = StationEditPacket.class, remap = false)
public abstract class MixinStationEditPacket implements ILimited {
    private Boolean limitEnabled;

    @Override
    public void setLimitEnabled(boolean limitEnabled) {
        this.limitEnabled = limitEnabled;
    }

    @Override
    public boolean isLimitEnabled() {
        return limitEnabled;
    }

    // Note: writeSettings and readSettings injections removed for Create 1.21 compatibility.
    // Create 1.21 uses StreamCodec for packet serialization instead of these methods.
    // Station limiting is now handled via StationLimitPacket sent from MixinStationScreen.

    @Inject(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/trains/station/StationBlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"), remap = true)
    private void applyLimit(ServerPlayer player, StationBlockEntity te, CallbackInfo ci) {
        if (limitEnabled != null) {
            GlobalStation station = te.getStation();
            TrackGraphLocation graphLocation = te.edgePoint.determineGraphLocation();
            if (station != null && graphLocation != null) {
                ((ILimited) station).setLimitEnabled(limitEnabled);
                Create.RAILWAYS.sync.pointAdded(graphLocation.graph, station);
                Create.RAILWAYS.markTracksDirty();
            }
        }
    }
}
