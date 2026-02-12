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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.mixin_interfaces.IOccupiedCouplers;
import com.railwayteam.railways.mixin_interfaces.IUpdateCount;
import com.railwayteam.railways.content.coupling.TrainUtils;
import com.railwayteam.railways.mixin_interfaces.IHandcarTrain;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(TrainRelocator.class)
public class MixinTrainRelocator {
    @Unique
    private static final ThreadLocal<Deque<TrainStateSnapshot>> railways$preRelocateState = ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private record TrainStateSnapshot(boolean derailed, boolean invalid) {
    }

    @WrapOperation(
        method = "relocate",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/trains/entity/Train;findCollidingTrain(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;)Lnet/createmod/catnip/data/Pair;"
        )
    )
    private static Pair<Train, Vec3> railways$ignoreCrashedTrainCollisions(Train train, Level level, Vec3 start, Vec3 end,
                                                                          ResourceKey<Level> dimension,
                                                                          Operation<Pair<Train, Vec3>> original) {
        Pair<Train, Vec3> collision = original.call(train, level, start, end, dimension);
        if (collision == null)
            return null;

        Train other = collision.getFirst();
        if (other == null)
            return collision;

        // Create treats derailed trains as collidable, which can deadlock relocation when two trains crash into each
        // other (common when splitting/decoupling while moving). Ignore collisions with crashed/invalid trains so
        // the player can relocate them back onto the track.
        if (other.derailed || other.invalid || other.graph == null)
            return null;

        return collision;
    }

    @Inject(method = "relocate", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Train;collectInitiallyOccupiedSignalBlocks()V", shift = At.Shift.AFTER))
    private static void tryToApproachStation(Train train, Level level, BlockPos pos, BezierTrackPointLocation bezier,
                                             boolean bezierDirection, Vec3 lookAngle, boolean simulate,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!simulate && !level.isClientSide && !((IHandcarTrain) train).railways$isHandcar())
            TrainUtils.tryToParkNearby(train, 1.25);
    }

    @Inject(method = "relocate", at = @At("HEAD"))
    private static void railways$prepareRelocate(Train train, Level level, BlockPos pos, BezierTrackPointLocation bezier,
                                                 boolean bezierDirection, Vec3 lookAngle, boolean simulate,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (simulate || level.isClientSide)
            return;

        // Create's relocator can refuse/half-apply when a train is in a crashed state.
        // Temporarily clear the flags so relocation has a chance to run, then restore on failure.
        Deque<TrainStateSnapshot> stack = railways$preRelocateState.get();
        stack.push(new TrainStateSnapshot(train.derailed, train.invalid));
        train.derailed = false;
        train.invalid = false;
    }

    @Inject(method = "relocate", at = @At("RETURN"))
    private static void railways$finalizeRelocate(Train train, Level level, BlockPos pos, BezierTrackPointLocation bezier,
                                                  boolean bezierDirection, Vec3 lookAngle, boolean simulate,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (simulate || level.isClientSide)
            return;

        Deque<TrainStateSnapshot> stack = railways$preRelocateState.get();
        TrainStateSnapshot previous = stack.poll();
        if (stack.isEmpty())
            railways$preRelocateState.remove();
        if (previous == null)
            return;

        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            train.derailed = false;
            train.invalid = false;
            train.speed = 0;
            train.navigation = new Navigation(train);
            train.updateSignalBlocks = true;
            ((IOccupiedCouplers) train).railways$getOccupiedCouplers().clear();
            railways$rebindCarriages(train);
        } else {
            train.derailed = previous.derailed();
            train.invalid = previous.invalid();
        }
    }

    @Unique
    private static void railways$rebindCarriages(Train train) {
        for (int index = 0; index < train.carriages.size(); index++) {
            Carriage carriage = train.carriages.get(index);
            if (carriage == null)
                continue;

            int finalIndex = index;
            carriage.forEachPresentEntity(cce -> {
                cce.carriageIndex = finalIndex;
                cce.trainId = train.id;
                ((AccessorCarriageContraptionEntity) cce).railways$setCarriage(carriage);
                ((AccessorCarriageContraptionEntity) cce).railways$bindCarriage();
                ((IUpdateCount) cce).railways$markUpdate();
            });
        }
    }
}
