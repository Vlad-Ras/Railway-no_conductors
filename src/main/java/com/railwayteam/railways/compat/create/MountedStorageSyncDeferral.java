package com.railwayteam.railways.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MountedStorageSyncDeferral {
	private MountedStorageSyncDeferral() {
	}

	private static final int TTL_TICKS = 200;

	private static final Map<Integer, Pending> PENDING_BY_ENTITY = new LinkedHashMap<>();

	public static void defer(MountedStorageSyncPacket packet) {
		PENDING_BY_ENTITY.put(packet.contraptionId(), new Pending(packet, TTL_TICKS));
	}

	public static void clientTick(Minecraft mc) {
		if (PENDING_BY_ENTITY.isEmpty())
			return;
		if (mc.level == null)
			return;

		Iterator<Map.Entry<Integer, Pending>> iterator = PENDING_BY_ENTITY.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Pending> entry = iterator.next();
			Pending pending = entry.getValue();

			Entity entity = mc.level.getEntity(entry.getKey());
			if (entity instanceof AbstractContraptionEntity ace && ace.getContraption() != null) {
				ace.getContraption().getStorage().handleSync(pending.packet, ace);
				iterator.remove();
				continue;
			}

			pending.ticksRemaining--;
			if (pending.ticksRemaining <= 0) {
				iterator.remove();
			}
		}
	}

	private static final class Pending {
		private final MountedStorageSyncPacket packet;
		private int ticksRemaining;

		private Pending(MountedStorageSyncPacket packet, int ticksRemaining) {
			this.packet = packet;
			this.ticksRemaining = ticksRemaining;
		}
	}
}
