package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.util.NeiBookmarkHelper;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * 服务端 -> 客户端：把规则组里的 NC（不消耗）物品加入 NEI 书签栏。
 * <p>
 * NEI 是纯客户端模组，服务端只能把物品清单送过来由客户端处理。
 */
public class PacketNeiBookmarks implements IMessage {

    private List<ItemStack> items = new ArrayList<ItemStack>();

    public PacketNeiBookmarks() {}

    public PacketNeiBookmarks(List<ItemStack> items) {
        this.items = items != null ? items : new ArrayList<ItemStack>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        items = new ArrayList<ItemStack>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            if (stack != null && stack.getItem() != null) {
                items.add(stack);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        List<ItemStack> safe = items != null ? items : new ArrayList<ItemStack>();
        int size = 0;
        for (ItemStack stack : safe) {
            if (stack != null && stack.getItem() != null) {
                size++;
            }
        }
        buf.writeInt(size);
        for (ItemStack stack : safe) {
            if (stack != null && stack.getItem() != null) {
                ByteBufUtils.writeItemStack(buf, stack);
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketNeiBookmarks, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketNeiBookmarks message, MessageContext ctx) {
            if (message == null || message.items.isEmpty()) {
                return null;
            }
            Minecraft.getMinecraft()
                .func_152344_a(() -> NeiBookmarkHelper.addAll(message.items));
            return null;
        }
    }
}
