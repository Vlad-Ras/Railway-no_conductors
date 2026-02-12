package com.railwayteam.railways.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.trains.station.GlobalStation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WrenchItem.class, remap = false)
public abstract class MixinWrenchItem {
    
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void deployerWrenchStationInteraction(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        net.minecraft.world.entity.player.Player player = context.getPlayer();
        if (player == null) return;
        
        // Only handle deployers with stations
        if (!(player instanceof DeployerFakePlayer deployerFakePlayer)) {
            return;
        }
        
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        
        // Check if it's a station block
        if (!(state.getBlock() instanceof StationBlock)) {
            return;
        }
        
        if (level.isClientSide) return;
        
        if (!(level.getBlockEntity(pos) instanceof StationBlockEntity stationBe)) {
            return;
        }
        
        GlobalStation station = stationBe.getStation();
        boolean isAssemblyMode = state.getValue(StationBlock.ASSEMBLING);
        
        if (station != null && station.getPresentTrain() == null) {
            // Assemble
            if (stationBe.isAssembling() || stationBe.tryEnterAssemblyMode()) {
                stationBe.assemble(deployerFakePlayer.getUUID());
                if (isAssemblyMode) {
                    level.setBlock(pos, state.setValue(StationBlock.ASSEMBLING, false), 3);
                    stationBe.refreshBlockState();
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }
        
        // Disassemble
        BlockState newState = null;
        if (!isAssemblyMode) {
            newState = state.setValue(StationBlock.ASSEMBLING, true);
        }
        if (disassembleAndEnterMode(deployerFakePlayer, stationBe)) {
            if (newState != null) {
                level.setBlock(pos, newState, 3);
                stationBe.refreshBlockState();
                stationBe.refreshAssemblyInfo();
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
    
    private boolean disassembleAndEnterMode(ServerPlayer sender, StationBlockEntity te) {
        GlobalStation station = te.getStation();
        if (station != null) {
            com.simibubi.create.content.trains.entity.Train train = station.getPresentTrain();
            BlockPos trackPosition = te.edgePoint.getGlobalPosition();
            net.minecraft.world.item.ItemStack schedule = train == null ? net.minecraft.world.item.ItemStack.EMPTY : train.runtime.returnSchedule(te.getLevel().registryAccess());
            if (train != null && !train.disassemble(te.getAssemblyDirection(), trackPosition.above()))
                return false;
            dropSchedule(sender, te, schedule);
        }
        return te.tryEnterAssemblyMode();
    }
    
    private void dropSchedule(ServerPlayer player, StationBlockEntity te, net.minecraft.world.item.ItemStack schedule) {
        if (schedule.isEmpty())
            return;
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(te.getLevel(),
            te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 1.5, te.getBlockPos().getZ() + 0.5, schedule);
        itemEntity.setDefaultPickUpDelay();
        te.getLevel().addFreshEntity(itemEntity);
    }
}
