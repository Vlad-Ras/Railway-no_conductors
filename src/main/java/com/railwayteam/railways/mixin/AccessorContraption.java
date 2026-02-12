package com.railwayteam.railways.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.Nullable;

@Mixin(Contraption.class)
public interface AccessorContraption {
	@Accessor("entity")
	@Nullable
	AbstractContraptionEntity railways$getEntity();
}
