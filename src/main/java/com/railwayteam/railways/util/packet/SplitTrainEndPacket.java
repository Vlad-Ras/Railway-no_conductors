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

import com.railwayteam.railways.multiloader.S2CPacket;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Train;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.UUID;

public class SplitTrainEndPacket implements S2CPacket {
    final UUID newTrainId;
    final UUID originalTrainOwner;
    final double speed;
    final boolean doubleEnded;

    public SplitTrainEndPacket(UUID newTrainId, UUID originalTrainOwner, double speed, boolean doubleEnded) {
        this.newTrainId = newTrainId;
        this.originalTrainOwner = originalTrainOwner;
        this.speed = speed;
        this.doubleEnded = doubleEnded;
    }

    public SplitTrainEndPacket(FriendlyByteBuf buf) {
        this.newTrainId = buf.readUUID();
        this.originalTrainOwner = buf.readUUID();
        this.speed = buf.readDouble();
        this.doubleEnded = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.newTrainId);
        buffer.writeUUID(this.originalTrainOwner);
        buffer.writeDouble(this.speed);
        buffer.writeBoolean(this.doubleEnded);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(Minecraft mc) {
        Level level = mc.level;
        if (level != null) {
            Train train = CreateClient.RAILWAYS.trains.get(newTrainId);
            // Creating a placeholder Train here can race and leave it with empty carriage data.
            if (train == null)
                return;
            train.doubleEnded = this.doubleEnded;
            train.speed = this.speed;
        }
    }
}

