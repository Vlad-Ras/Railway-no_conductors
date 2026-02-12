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

package com.railwayteam.railways.mixin.client;

import com.google.common.collect.ImmutableList;
import com.railwayteam.railways.mixin_interfaces.ILimited;
import com.railwayteam.railways.registry.CRPackets;
import com.railwayteam.railways.util.packet.StationLimitPacket;
import com.simibubi.create.content.trains.entity.Train;
import net.createmod.catnip.platform.CatnipServices;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.content.trains.station.AbstractStationScreen;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationScreen;
import com.simibubi.create.content.trains.station.TrainEditPacket;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = StationScreen.class, remap = false)
public abstract class MixinStationScreen extends AbstractStationScreen {
    @Shadow private EditBox trainNameBox;

    @Unique
    private Checkbox railways$limitCheckbox;
    @Unique
    private List<ResourceLocation> railways$iconTypes;
    @Unique
    private ScrollInput railways$iconTypeScroll;

    private MixinStationScreen(StationBlockEntity te, GlobalStation station) {
        super(te, station);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/station/StationScreen;tickTrainDisplay()V"), remap = true)
    private void railways$initCheckbox(CallbackInfo ci) {
        int x = guiLeft;
        int y = guiTop;

        boolean limitEnabled = station != null && ((ILimited) station).isLimitEnabled();
        int checkboxX = x + 100;
        int checkboxY = y + 102;
        railways$limitCheckbox = Checkbox.builder(
                Component.translatable("railways.station.train_limit").withStyle(ChatFormatting.WHITE),
                Minecraft.getInstance().font)
            .pos(checkboxX, checkboxY)
            .selected(limitEnabled)
            .onValueChange((checkbox, selected) ->
                CRPackets.PACKETS.send(new StationLimitPacket(blockEntity.getBlockPos(), selected)))
            .build();
        addRenderableWidget(railways$limitCheckbox);
        addRenderableOnly(new TooltipArea(checkboxX, checkboxY, 55, 16)
            .withTooltip(ImmutableList.of(
                Component.translatable("railways.station.train_limit.tooltip.1")
                    .withStyle(ChatFormatting.GRAY),
                Component.translatable("railways.station.train_limit.tooltip.2")
                    .withStyle(ChatFormatting.GRAY)
            )));

        railways$iconTypes = new ArrayList<>(TrainIconType.REGISTRY.keySet());
        railways$iconTypeScroll = new ScrollInput(x + 4, y + 17, 160, 14)
            .withRange(0, railways$iconTypes.size())
            .inverted()
            .titled(Component.literal("Train Icon").withStyle(s -> s.withColor(0xFFFFFF)))
            .calling(idx -> {
                Train imminentTrain = getImminent();
                if (imminentTrain == null)
                    return;
                ResourceLocation iconId = railways$iconTypes.get(idx);
                TrainIconType iconType = TrainIconType.byId(iconId);
                imminentTrain.icon = iconType;
                CatnipServices.NETWORK.sendToServer(
                    new TrainEditPacket.Serverbound(
                        imminentTrain.id,
                        imminentTrain.name.getString(),
                        iconId,
                        imminentTrain.mapColorIndex
                    )
                );
            });
        railways$iconTypeScroll.active = false;
    }

    @Inject(method = "tickTrainDisplay", at = @At("HEAD"))
    private void railways$tickIconScroll(CallbackInfo ci) {
        if (railways$iconTypeScroll == null)
            return;

        Train train = displayedTrain.get();

        if (train == null) {
            if (railways$iconTypeScroll.active) {
                railways$iconTypeScroll.active = false;
                removeWidget(railways$iconTypeScroll);
            }

            Train imminentTrain = getImminent();

            if (imminentTrain != null) {
                railways$iconTypeScroll.active = true;
                int idx = railways$iconTypes.indexOf(imminentTrain.icon.getId());
                if (idx >= 0)
                    railways$iconTypeScroll.setState(idx);
                addRenderableWidget(railways$iconTypeScroll);
            }
        }
    }
}
