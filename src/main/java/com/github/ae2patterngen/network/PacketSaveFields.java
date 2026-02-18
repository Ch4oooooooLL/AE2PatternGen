package com.github.ae2patterngen.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.item.ItemPatternGenerator;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端: 保存 GUI 输入字段到手持物品的 NBT
 */
public class PacketSaveFields implements IMessage {

    private String recipeMap;
    private String outputOre;
    private String inputOre;
    private String ncItem;
    private String blacklistInput;
    private String blacklistOutput;

    public PacketSaveFields() {}

    public PacketSaveFields(String recipeMap, String outputOre, String inputOre, String ncItem, String blacklistInput,
        String blacklistOutput) {
        this.recipeMap = recipeMap;
        this.outputOre = outputOre;
        this.inputOre = inputOre;
        this.ncItem = ncItem;
        this.blacklistInput = blacklistInput;
        this.blacklistOutput = blacklistOutput;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMap = ByteBufUtils.readUTF8String(buf);
        outputOre = ByteBufUtils.readUTF8String(buf);
        inputOre = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
        blacklistInput = ByteBufUtils.readUTF8String(buf);
        blacklistOutput = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMap != null ? recipeMap : "");
        ByteBufUtils.writeUTF8String(buf, outputOre != null ? outputOre : "");
        ByteBufUtils.writeUTF8String(buf, inputOre != null ? inputOre : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
        ByteBufUtils.writeUTF8String(buf, blacklistInput != null ? blacklistInput : "");
        ByteBufUtils.writeUTF8String(buf, blacklistOutput != null ? blacklistOutput : "");
    }

    public static class Handler implements IMessageHandler<PacketSaveFields, IMessage> {

        @Override
        public IMessage onMessage(PacketSaveFields message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;
            ItemStack held = player.getCurrentEquippedItem();

            if (held != null && held.getItem() instanceof ItemPatternGenerator) {
                ItemPatternGenerator.saveAllFields(
                    held,
                    message.recipeMap,
                    message.outputOre,
                    message.inputOre,
                    message.ncItem,
                    message.blacklistInput,
                    message.blacklistOutput);
            }

            return null;
        }
    }
}
