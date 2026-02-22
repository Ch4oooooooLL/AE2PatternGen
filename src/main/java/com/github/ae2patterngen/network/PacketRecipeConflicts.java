package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: 发送冲突配方
 */
public class PacketRecipeConflicts implements IMessage {

    public String productName;
    public int currentIndex;
    public int totalConflicts;
    public List<RecipeEntry> recipes;

    public PacketRecipeConflicts() {}

    public PacketRecipeConflicts(String productName, int currentIndex, int totalConflicts, List<RecipeEntry> recipes) {
        this.productName = productName;
        this.currentIndex = currentIndex;
        this.totalConflicts = totalConflicts;
        this.recipes = recipes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        productName = ByteBufUtils.readUTF8String(buf);
        currentIndex = buf.readInt();
        totalConflicts = buf.readInt();
        int recipeSize = buf.readInt();
        recipes = new ArrayList<>(recipeSize);
        for (int i = 0; i < recipeSize; i++) {
            recipes.add(readRecipe(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, productName != null ? productName : "");
        buf.writeInt(currentIndex);
        buf.writeInt(totalConflicts);
        List<RecipeEntry> safeRecipes = recipes != null ? recipes : new ArrayList<RecipeEntry>();
        buf.writeInt(safeRecipes.size());
        for (RecipeEntry re : safeRecipes) {
            writeRecipe(buf, re);
        }
    }

    static void writeRecipe(ByteBuf buf, RecipeEntry re) {
        ByteBufUtils.writeUTF8String(buf, re.recipeMapId != null ? re.recipeMapId : "");
        ByteBufUtils.writeUTF8String(buf, re.machineDisplayName != null ? re.machineDisplayName : "");
        buf.writeInt(re.duration);
        buf.writeInt(re.euPerTick);

        // 仅写出必要的显示信息
        writeItemStackArray(buf, re.inputs);
        writeItemStackArray(buf, re.outputs);
        writeFluidStackArray(buf, re.fluidInputs);
        writeFluidStackArray(buf, re.fluidOutputs);
        writeItemStackArray(buf, re.specialItems);
    }

    static RecipeEntry readRecipe(ByteBuf buf) {
        String recipeMapId = ByteBufUtils.readUTF8String(buf);
        String machineDisplayName = ByteBufUtils.readUTF8String(buf);
        int duration = buf.readInt();
        int euPerTick = buf.readInt();
        ItemStack[] inputs = readItemStackArray(buf);
        ItemStack[] outputs = readItemStackArray(buf);
        FluidStack[] fluidInputs = readFluidStackArray(buf);
        FluidStack[] fluidOutputs = readFluidStackArray(buf);
        ItemStack[] specialItems = readItemStackArray(buf);
        return new RecipeEntry(
            "conflict",
            recipeMapId,
            machineDisplayName,
            inputs,
            outputs,
            fluidInputs,
            fluidOutputs,
            specialItems,
            duration,
            euPerTick);
    }

    static void writeItemStackArray(ByteBuf buf, ItemStack[] stacks) {
        ItemStack[] safeStacks = stacks != null ? stacks : new ItemStack[0];
        buf.writeInt(safeStacks.length);
        for (ItemStack stack : safeStacks) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    static ItemStack[] readItemStackArray(ByteBuf buf) {
        int len = buf.readInt();
        ItemStack[] stacks = new ItemStack[len];
        for (int i = 0; i < len; i++) {
            stacks[i] = ByteBufUtils.readItemStack(buf);
        }
        return stacks;
    }

    static void writeFluidStackArray(ByteBuf buf, FluidStack[] stacks) {
        FluidStack[] safeStacks = stacks != null ? stacks : new FluidStack[0];
        buf.writeInt(safeStacks.length);
        for (FluidStack stack : safeStacks) {
            if (stack != null && stack.getFluid() != null && stack.amount > 0) {
                buf.writeBoolean(true);
                NBTTagCompound fluidTag = new NBTTagCompound();
                stack.writeToNBT(fluidTag);
                ByteBufUtils.writeTag(buf, fluidTag);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    static FluidStack[] readFluidStackArray(ByteBuf buf) {
        int len = buf.readInt();
        FluidStack[] stacks = new FluidStack[len];
        for (int i = 0; i < len; i++) {
            if (buf.readBoolean()) {
                NBTTagCompound fluidTag = ByteBufUtils.readTag(buf);
                if (fluidTag != null) {
                    FluidStack fs = FluidStack.loadFluidStackFromNBT(fluidTag);
                    if (fs != null && fs.getFluid() != null && fs.amount > 0) {
                        stacks[i] = fs;
                    }
                }
            }
        }
        return stacks;
    }

    public static class Handler implements IMessageHandler<PacketRecipeConflicts, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketRecipeConflicts message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> com.github.ae2patterngen.gui.GuiRecipePicker.open(message));
            return null;
        }
    }
}
