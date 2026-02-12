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

package com.railwayteam.railways.content.fuel.tank;

import com.railwayteam.railways.mixin.AccessorContraption;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FuelTankMovementBehavior implements MovementBehaviour {
    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
		if (!context.world.isClientSide)
			return;
		if (context.contraption == null || context.localPos == null)
			return;

		AbstractContraptionEntity entity = ((AccessorContraption) context.contraption).railways$getEntity();
		if (entity == null)
			return;

		BlockEntity be = context.contraption.getOrCreateClientContraptionLazy().getBlockEntity(context.localPos);
		if (be instanceof FuelTankBlockEntity fuelTank) {
			LerpedFloat level = fuelTank.getFluidLevel();
			if (level != null)
				level.tickChaser();
		}
    }
}
