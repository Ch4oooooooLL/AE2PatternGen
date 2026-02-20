package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
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
        ByteBufUtils.writeUTF8String(buf, productName);
        buf.writeInt(currentIndex);
        buf.writeInt(totalConflicts);
        buf.writeInt(recipes.size());
        for (RecipeEntry re : recipes) {
            writeRecipe(buf, re);
        }
    }

    private void writeRecipe(ByteBuf buf, RecipeEntry re) {
        // 仅写出必要的显示信息
        writeItemStackArray(buf, re.inputs);
        writeItemStackArray(buf, re.outputs);
        writeFluidStackArray(buf, re.fluidInputs);
        writeFluidStackArray(buf, re.fluidOutputs);
        writeItemStackArray(buf, re.specialItems);
    }

    private RecipeEntry readRecipe(ByteBuf buf) {
        ItemStack[] inputs = readItemStackArray(buf);
        ItemStack[] outputs = readItemStackArray(buf);
        FluidStack[] fluidInputs = readFluidStackArray(buf);
        FluidStack[] fluidOutputs = readFluidStackArray(buf);
        ItemStack[] specialItems = readItemStackArray(buf);
        return new RecipeEntry("conflict", "", "", inputs, outputs, fluidInputs, fluidOutputs, specialItems, 0, 0);
    }

    private void writeItemStackArray(ByteBuf buf, ItemStack[] stacks) {
        buf.writeInt(stacks.length);
        for (ItemStack stack : stacks) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    private ItemStack[] readItemStackArray(ByteBuf buf) {
        int len = buf.readInt();
        ItemStack[] stacks = new ItemStack[len];
        for (int i = 0; i < len; i++) {
            stacks[i] = ByteBufUtils.readItemStack(buf);
        }
        return stacks;
    }

    private void writeFluidStackArray(ByteBuf buf, FluidStack[] stacks) {
        buf.writeInt(stacks.length);
        for (FluidStack stack : stacks) {
            if (stack != null) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(
                    buf,
                    stack.getFluid()
                        .getName());
                buf.writeInt(stack.amount);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    private FluidStack[] readFluidStackArray(ByteBuf buf) {
        int len = buf.readInt();
        FluidStack[] stacks = new FluidStack[len];
        for (int i = 0; i < len; i++) {
            if (buf.readBoolean()) {
                String name = ByteBufUtils.readUTF8String(buf);
                int amount = buf.readInt();
                stacks[i] = new FluidStack(net.minecraftforge.fluids.FluidRegistry.getFluid(name), amount);
            }
        }
        return stacks;
    }

    public static class Handler implements IMessageHandler<PacketRecipeConflicts, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketRecipeConflicts message, MessageContext ctx) {
            com.github.ae2patterngen.gui.GuiRecipePicker.open(message);
            return null;
        }
    }
}
