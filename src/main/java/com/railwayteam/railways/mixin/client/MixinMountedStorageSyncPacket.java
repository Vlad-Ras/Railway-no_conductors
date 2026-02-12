package com.railwayteam.railways.mixin.client;

import com.railwayteam.railways.compat.create.MountedStorageSyncDeferral;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MountedStorageSyncPacket.class)
public abstract class MixinMountedStorageSyncPacket {
	@Shadow
	public abstract int contraptionId();

	@Inject(method = "handle", at = @At("HEAD"), cancellable = true)
	private void railways$deferUntilContraptionReady(LocalPlayer player, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			ci.cancel();
			return;
		}

		Entity entity = level.getEntity(contraptionId());
		if (!(entity instanceof AbstractContraptionEntity ace)) {
			ci.cancel();
			return;
		}

		if (ace.getContraption() == null) {
			MountedStorageSyncDeferral.defer((MountedStorageSyncPacket) (Object) this);
			ci.cancel();
			return;
		}

		ace.getContraption().getStorage().handleSync((MountedStorageSyncPacket) (Object) this, ace);
		ci.cancel();
	}
}
