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

package com.railwayteam.railways.content.smokestack.block.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.content.smokestack.block.DieselSmokeStackBlock;
import com.railwayteam.railways.content.smokestack.block.be.DieselSmokeStackBlockEntity;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.createmod.catnip.math.AngleHelper;

public class DieselSmokeStackRenderer extends SmartBlockEntityRenderer<DieselSmokeStackBlockEntity> {
    public DieselSmokeStackRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DieselSmokeStackBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        Direction dir = be.getBlockState().getValue(DieselSmokeStackBlock.FACING);

        SuperByteBuffer byteBuffer = CachedBuffers.partial(CRBlockPartials.DIESEL_STACK_FAN, be.getBlockState());

        byteBuffer.light(light);

        float fanAngle = (float) be.getFanRotation(be.getRpm(partialTicks));

        // Orient fan to face each direction using single-axis rotations
        // Model default: fan faces UP (+Y)
        byteBuffer.translate(0.5, 0.5, 0.5);

        switch (dir) {
            case UP -> {} // No rotation needed
            case DOWN -> byteBuffer.rotateXDegrees(180);
            case NORTH -> byteBuffer.rotateXDegrees(90);
            case SOUTH -> byteBuffer.rotateXDegrees(-90);
            case EAST -> byteBuffer.rotateZDegrees(-90);
            case WEST -> byteBuffer.rotateZDegrees(90);
        }

        // Fan spin around facing direction
        byteBuffer.rotateYDegrees(fanAngle);

        // N/S need Y offset (in local coords) to compensate - local Y becomes world Z after X rotation
        float yOffset = switch (dir) {
            case NORTH -> 0.8f;
            case SOUTH -> 0.8f;
            default -> 0f;
        };

        byteBuffer.translate(-0.5, -0.5 + yOffset, -0.5);

        byteBuffer.renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }
}
