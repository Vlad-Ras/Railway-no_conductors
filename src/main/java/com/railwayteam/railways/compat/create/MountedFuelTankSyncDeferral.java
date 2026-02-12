package com.railwayteam.railways.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side deferral for applying mounted fuel tank fluid state.
 * <p>
 * Create's mounted storage sync can arrive before the client contraption has finished
 * building its {@link BlockEntity} instances. When that happens, {@link com.railwayteam.railways.content.fuel.tank.FuelTankMountedStorage#afterSync}
 * can't find the {@code FuelTankBlockEntity} to apply the fluid to.
 */
public final class MountedFuelTankSyncDeferral {
	private MountedFuelTankSyncDeferral() {
	}

	private static final int TTL_TICKS = 200;

	private static final Map<Key, Pending> PENDING = new LinkedHashMap<>();

	public static void defer(int entityId, BlockPos localPos, FluidStack fluid) {
		if (fluid == null)
			return;
		PENDING.put(new Key(entityId, localPos), new Pending(fluid.copy(), TTL_TICKS));
	}

	public static void clientTick(Minecraft mc, FuelTankApplier applier) {
		if (PENDING.isEmpty())
			return;
		if (mc.level == null)
			return;

		Iterator<Map.Entry<Key, Pending>> iterator = PENDING.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Key, Pending> entry = iterator.next();
			Key key = entry.getKey();
			Pending pending = entry.getValue();

			Entity entity = mc.level.getEntity(key.entityId);
			if (entity instanceof AbstractContraptionEntity ace) {
				Contraption contraption = ace.getContraption();
				if (contraption != null) {
					BlockEntity be = contraption.getOrCreateClientContraptionLazy().getBlockEntity(key.localPos);
					if (be != null && applier.tryApply(be, pending.fluid)) {
						iterator.remove();
						continue;
					}
				}
			}

			pending.ticksRemaining--;
			if (pending.ticksRemaining <= 0) {
				iterator.remove();
			}
		}
	}

	@FunctionalInterface
	public interface FuelTankApplier {
		boolean tryApply(BlockEntity be, FluidStack fluid);
	}

	private record Key(int entityId, BlockPos localPos) {
		private Key {
			if (localPos == null)
				throw new IllegalArgumentException("localPos cannot be null");
		}
	}

	private static final class Pending {
		private final FluidStack fluid;
		private int ticksRemaining;

		private Pending(FluidStack fluid, int ticksRemaining) {
			this.fluid = fluid;
			this.ticksRemaining = ticksRemaining;
		}
	}
}
