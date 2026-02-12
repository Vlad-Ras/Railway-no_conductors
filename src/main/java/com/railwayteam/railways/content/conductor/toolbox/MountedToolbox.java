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

package com.railwayteam.railways.content.conductor.toolbox;

import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.mixin.AccessorBlockEntity;
import com.railwayteam.railways.mixin.AccessorToolboxBlockEntity;
import com.railwayteam.railways.util.packet.PacketSender;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MountedToolbox extends ToolboxBlockEntity {
  protected final ConductorEntity parent;

  public MountedToolbox(ConductorEntity parent, DyeColor dyeColor) {
    super(AllBlockEntityTypes.TOOLBOX.get(), parent.blockPosition(), AllBlocks.TOOLBOXES.get(dyeColor).getDefaultState());
    this.parent = parent;
    setLevel(parent.level());
    setLazyTickRate(10);
  }

  public void readFromItem(ItemStack stack) {
    // Read from Create's data components (1.21+)
    ToolboxInventory inv = stack.get(AllDataComponents.TOOLBOX_INVENTORY);
    if (inv != null) {
      readInventory(inv);
    }
    UUID uuid = stack.get(AllDataComponents.TOOLBOX_UUID);
    if (uuid != null) {
      setUniqueId(uuid);
    }
    if (stack.has(DataComponents.CUSTOM_NAME)) {
      setCustomName(stack.get(DataComponents.CUSTOM_NAME));
    }
  }

  public ConductorEntity getParent() {
    return parent;
  }

  @Override
  public void tick() {
    // keep saved block pos updated for updateOpenCount and tickAudio
    ((AccessorBlockEntity) this).setWorldPosition(parent.blockPosition());
    super.tick();
  }

  @Override
  public void read(CompoundTag compound, HolderLookup.Provider lookupProvider, boolean clientPacket) {
    super.read(compound, lookupProvider, clientPacket);
    if (compound.contains("Color", CompoundTag.TAG_INT)) {
      DyeColor color = DyeColor.byId(compound.getInt("Color"));
      // change the color by setting the stored state and updating the color provider
      BlockState state = AllBlocks.TOOLBOXES.get(color).get().defaultBlockState();
      setBlockState(state);
    }
  }

  @Override
  public void write(CompoundTag compound, HolderLookup.Provider lookupProvider, boolean clientPacket) {
    super.write(compound, lookupProvider, clientPacket);
    compound.putInt("Color", getColor().getId());
  }

  public List<Player> getConnectedPlayers() {
    Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers = ((AccessorToolboxBlockEntity) this).getConnectedPlayers();
    Set<Player> players = new HashSet<>();
    for (Map.Entry<Integer, WeakHashMap<Player, Integer>> entry : connectedPlayers.entrySet()) {
       players.addAll(entry.getValue().keySet());
    }
    return players.stream().toList();
  }

  @Override
  public void sendData() {
    if (level == null || level.isClientSide)
      return;
    CompoundTag nbt = new CompoundTag();
    this.write(nbt, parent.level().registryAccess(), true);
    PacketSender.syncMountedToolboxNBT(this.parent, nbt);
  }

  @Override
  public void setChanged() {
    // override and do nothing, this isn't in-world
  }

  public static MountedToolbox read(ConductorEntity parent, CompoundTag compound) {
    MountedToolbox holder = new MountedToolbox(parent, DyeColor.BROWN);
    holder.read(compound, parent.level().registryAccess(), false);
    return holder;
  }

  @Override
  public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
    return MountedToolboxContainer.create(id, inv, this);
  }

  public ItemStack getDisplayStack() {
    ItemStack stack = new ItemStack(AllBlocks.TOOLBOXES.get(getColor()).get());
    if (hasCustomName())
      stack.set(DataComponents.CUSTOM_NAME, getCustomName());
    return stack;
  }

  public ItemStack getCloneItemStack() {
    ItemStack stack = getDisplayStack();
    // Use Create's data components (1.21+)
    stack.set(AllDataComponents.TOOLBOX_INVENTORY, ((AccessorToolboxBlockEntity) this).getInventory());
    stack.set(AllDataComponents.TOOLBOX_UUID, getUniqueId());
    return stack;
  }

  @Override
  public void sendToMenu(RegistryFriendlyByteBuf buffer) {
    buffer.writeVarInt(parent.getId());
    buffer.writeNbt(getUpdateTag(parent.level().registryAccess()));
  }

  public static void openMenu(ServerPlayer player, MountedToolbox toolbox) {
    MenuProvider provider = new MenuProvider() {
      @Override
      public AbstractContainerMenu createMenu(int id, Inventory inv, Player ply) {
        return MountedToolboxContainer.create(id, inv, toolbox);
      }

      @Override
      public net.minecraft.network.chat.Component getDisplayName() {
        return toolbox.getDisplayName();
      }
    };
    player.openMenu(provider, (buffer) -> toolbox.sendToMenu(buffer));
  }
}
