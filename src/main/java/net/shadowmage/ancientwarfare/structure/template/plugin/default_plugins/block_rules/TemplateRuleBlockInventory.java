/*
 Copyright 2012-2013 John Cummens (aka Shadowmage, Shadowmage4513)
 This software is distributed under the terms of the GNU General Public License.
 Please see COPYING for precise license information.

 This file is part of Ancient Warfare.

 Ancient Warfare is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Ancient Warfare is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Ancient Warfare.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.shadowmage.ancientwarfare.structure.template.plugin.default_plugins.block_rules;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.util.Constants;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;
import net.shadowmage.ancientwarfare.structure.block.BlockDataManager;

import javax.annotation.Nonnull;
import java.util.Random;

import static net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST;

public class TemplateRuleBlockInventory extends TemplateRuleVanillaBlocks {

    public int randomLootLevel = 0;
    public NBTTagCompound tag = new NBTTagCompound();
    ItemStack[] inventoryStacks;

    public TemplateRuleBlockInventory(World world, int x, int y, int z, Block block, int meta, int turns) {
        super(world, x, y, z, block, meta, turns);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IInventory) {
            IInventory inventory = (IInventory) te;
            if (inventory.getSizeInventory() <= 0) {
                return;
            }
            @Nonnull ItemStack keyStack = inventory.getStackInSlot(0);
            boolean useKey = keyStack != null && (keyStack.getItem() == Items.GOLD_INGOT || keyStack.getItem() == Items.DIAMOND || keyStack.getItem() == Items.EMERALD);
            if (useKey) {
                for (int i = 1; i < inventory.getSizeInventory(); i++) {
                    if (inventory.getStackInSlot(i) != null) {
                        useKey = false;
                        break;
                    }
                }
            }
            this.randomLootLevel = useKey ? keyStack.getItem() == Items.GOLD_INGOT ? 1 : keyStack.getItem() == Items.DIAMOND ? 2 : 3 : 0;
            inventoryStacks = new ItemStack[inventory.getSizeInventory()];
            @Nonnull ItemStack stack;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                stack = inventory.getStackInSlot(i);
                inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                inventoryStacks[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            te.writeToNBT(tag);
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                inventory.setInventorySlotContents(i, inventoryStacks[i]);
            }
            //actual items were already removed from tag in previous for loop blocks prior to tile writing to nbt
            tag.removeTag("Items");//remove vanilla inventory tag from tile-entities (need to custom handle AW inventoried blocks still)
        }
    }

    public TemplateRuleBlockInventory() {

    }

    @Override
    public void handlePlacement(World world, int turns, BlockPos pos, IStructureBuilder builder) {
        super.handlePlacement(world, turns, x, y, z, builder);
        int localMeta = BlockDataManager.INSTANCE.getRotatedMeta(block, this.meta, turns);
        world.setBlockMetadataWithNotify(x, y, z, localMeta, 3);
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof IInventory)) {
            return;
        }
        IInventory inventory = (IInventory) te;
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        te.readFromNBT(tag);
        if (randomLootLevel > 0) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            }//clear the inventory in prep for random loot stuff
            generateLootFor(inventory, world.rand);
        } else if (inventoryStacks != null) {
            @Nonnull ItemStack stack;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                stack = i < inventoryStacks.length ? inventoryStacks[i] : null;
                inventory.setInventorySlotContents(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
        }
        BlockTools.notifyBlockUpdate(world, pos);
    }

    public void generateLootFor(IInventory inventory, Random rng) {
        for (int i = 0; i < randomLootLevel; i++) {
            WeightedRandomChestContent.generateChestContents(rng, ChestGenHooks.getItems(DUNGEON_CHEST, rng), inventory, ChestGenHooks.getCount(DUNGEON_CHEST, rng));
        }
    }

    @Override
    public boolean shouldReuseRule(World world, Block block, int meta, int turns, int x, int y, int z) {
        return false;
    }

    @Override
    public void writeRuleData(NBTTagCompound tag) {
        super.writeRuleData(tag);
        tag.setInteger("lootLevel", randomLootLevel);
        tag.setTag("teData", this.tag);

        NBTTagCompound invData = new NBTTagCompound();
        invData.setInteger("length", inventoryStacks.length);
        NBTTagCompound itemTag;
        NBTTagList list = new NBTTagList();
        @Nonnull ItemStack stack;
        for (int i = 0; i < inventoryStacks.length; i++) {
            stack = inventoryStacks[i];
            if (stack.isEmpty()) {
                continue;
            }
            itemTag = stack.writeToNBT(new NBTTagCompound());
            itemTag.setInteger("slot", i);
            list.appendTag(itemTag);
        }
        invData.setTag("inventoryContents", list);
        tag.setTag("inventoryData", invData);
    }

    @Override
    public void parseRuleData(NBTTagCompound tag) {
        super.parseRuleData(tag);
        if (tag.hasKey("inventoryData")) {
            NBTTagCompound inventoryTag = tag.getCompoundTag("inventoryData");
            int length = inventoryTag.getInteger("length");
            inventoryStacks = new ItemStack[length];
            NBTTagCompound itemTag;
            NBTTagList list = inventoryTag.getTagList("inventoryContents", Constants.NBT.TAG_COMPOUND);
            int slot;
            @Nonnull ItemStack stack;
            for (int i = 0; i < list.tagCount(); i++) {
                itemTag = list.getCompoundTagAt(i);
                stack = new ItemStack(itemTag);
                if (!stack.isEmpty()) {
                    slot = itemTag.getInteger("slot");
                    inventoryStacks[slot] = stack;
                }
            }
        }
        randomLootLevel = tag.getInteger("lootLevel");
        this.tag = tag.getCompoundTag("teData");
    }
}
