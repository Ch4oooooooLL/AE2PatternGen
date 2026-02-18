package com.github.ae2patterngen.network;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.encoder.PatternEncoder;
import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.InputOreDictFilter;
import com.github.ae2patterngen.filter.NCItemFilter;
import com.github.ae2patterngen.filter.OutputOreDictFilter;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端: 请求生成样板
 */
public class PacketGeneratePatterns implements IMessage {

    private String recipeMapId;
    private String outputOreDict;
    private String inputOreDict;
    private String ncItem;

    public PacketGeneratePatterns() {}

    public PacketGeneratePatterns(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem) {
        this.recipeMapId = recipeMapId;
        this.outputOreDict = outputOreDict;
        this.inputOreDict = inputOreDict;
        this.ncItem = ncItem;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMapId = ByteBufUtils.readUTF8String(buf);
        outputOreDict = ByteBufUtils.readUTF8String(buf);
        inputOreDict = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMapId != null ? recipeMapId : "");
        ByteBufUtils.writeUTF8String(buf, outputOreDict != null ? outputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, inputOreDict != null ? inputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
    }

    public static class Handler implements IMessageHandler<PacketGeneratePatterns, IMessage> {

        @Override
        public IMessage onMessage(PacketGeneratePatterns message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            // 收集配方
            List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(message.recipeMapId);

            // 构建过滤器
            CompositeFilter filter = new CompositeFilter();
            if (message.outputOreDict != null && !message.outputOreDict.isEmpty()) {
                filter.addFilter(new OutputOreDictFilter(message.outputOreDict));
            }
            if (message.inputOreDict != null && !message.inputOreDict.isEmpty()) {
                filter.addFilter(new InputOreDictFilter(message.inputOreDict));
            }
            if (message.ncItem != null && !message.ncItem.isEmpty()) {
                filter.addFilter(new NCItemFilter(message.ncItem));
            }

            // 应用过滤
            java.util.List<RecipeEntry> filtered = new java.util.ArrayList<>();
            for (RecipeEntry recipe : recipes) {
                if (filter.matches(recipe)) {
                    filtered.add(recipe);
                }
            }

            if (filtered.isEmpty()) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 没有找到匹配的配方"));
                return null;
            }

            // 编码样板
            List<ItemStack> patterns = PatternEncoder.encodeBatch(filtered);

            int givenToInventory = 0;
            int droppedOnGround = 0;

            for (ItemStack pattern : patterns) {
                if (!player.inventory.addItemStackToInventory(pattern)) {
                    EntityItem entityItem = player.entityDropItem(pattern, 0.5F);
                    if (entityItem != null) {
                        entityItem.delayBeforeCanPickup = 0;
                    }
                    droppedOnGround++;
                } else {
                    givenToInventory++;
                }
            }

            player.inventoryContainer.detectAndSendChanges();

            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "[AE2PatternGen] 生成完成! 放入背包: "
                        + givenToInventory
                        + ", 掉落地面: "
                        + droppedOnGround));

            return null;
        }
    }
}
