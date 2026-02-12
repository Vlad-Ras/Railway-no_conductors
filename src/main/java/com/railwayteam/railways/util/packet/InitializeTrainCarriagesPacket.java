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

import com.railwayteam.railways.mixin.AccessorTrain;
import com.railwayteam.railways.multiloader.S2CPacket;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Train;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InitializeTrainCarriagesPacket implements S2CPacket {
    final UUID trainId;
    final int numberOfCarriages;
    final List<Integer> carriageSpacing;

    public InitializeTrainCarriagesPacket(Train train) {
        this.trainId = train.id;
        this.numberOfCarriages = train.carriages.size();
        this.carriageSpacing = new ArrayList<>(train.carriageSpacing);
    }

    public InitializeTrainCarriagesPacket(FriendlyByteBuf buf) {
        this.trainId = buf.readUUID();
        this.numberOfCarriages = buf.readInt();
        int spacingSize = buf.readInt();
        this.carriageSpacing = new ArrayList<>();
        for (int i = 0; i < spacingSize; i++) {
            this.carriageSpacing.add(buf.readInt());
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.trainId);
        buffer.writeInt(this.numberOfCarriages);
        buffer.writeInt(this.carriageSpacing.size());
        for (int spacing : this.carriageSpacing) {
            buffer.writeInt(spacing);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(Minecraft mc) {
        Level level = mc.level;
        if (level != null) {
            Train train = CreateClient.RAILWAYS.trains.get(trainId);
            if (train != null) {
                train.carriageSpacing.clear();
                train.carriageSpacing.addAll(this.carriageSpacing);
                
                double[] stressArray = new double[numberOfCarriages];
                for (int i = 0; i < numberOfCarriages; i++) {
                    stressArray[i] = 0;
                }
                ((AccessorTrain) train).railways$setStress(stressArray);
            }
        }
    }
}
