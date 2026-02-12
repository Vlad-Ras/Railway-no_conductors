/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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

package com.railwayteam.railways.util.packet;

import com.railwayteam.railways.mixin_interfaces.ILimited;
import com.railwayteam.railways.multiloader.C2SPacket;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StationLimitPacket implements C2SPacket {
    private final BlockPos pos;
    private final boolean limitEnabled;

    public StationLimitPacket(BlockPos pos, boolean limitEnabled) {
        this.pos = pos;
        this.limitEnabled = limitEnabled;
    }

    public StationLimitPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.limitEnabled = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(limitEnabled);
    }

    @Override
    public void handle(ServerPlayer sender) {
        if (sender.level().isClientSide())
            return;

        BlockEntity be = sender.level().getBlockEntity(pos);
        if (!(be instanceof StationBlockEntity stationBE))
            return;

        GlobalStation station = stationBE.getStation();
        TrackGraphLocation graphLocation = stationBE.edgePoint.determineGraphLocation();

        if (station != null && graphLocation != null) {
            ((ILimited) station).setLimitEnabled(limitEnabled);
            Create.RAILWAYS.sync.pointAdded(graphLocation.graph, station);
            Create.RAILWAYS.markTracksDirty();
        }
    }
}
