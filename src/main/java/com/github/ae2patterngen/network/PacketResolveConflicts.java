package com.github.ae2patterngen.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.I18nUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server: 提交冲突决策
 */
public class PacketResolveConflicts implements IMessage {

    public int recipeIndex;
    public boolean cancel;
    public int expectedConflictIndex;

    public PacketResolveConflicts() {}

    public PacketResolveConflicts(int recipeIndex, boolean cancel) {
        this(recipeIndex, cancel, -1);
    }

    public PacketResolveConflicts(int recipeIndex, boolean cancel, int expectedConflictIndex) {
        this.recipeIndex = recipeIndex;
        this.cancel = cancel;
        this.expectedConflictIndex = expectedConflictIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeIndex = buf.readInt();
        cancel = buf.readBoolean();
        expectedConflictIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(recipeIndex);
        buf.writeBoolean(cancel);
        buf.writeInt(expectedConflictIndex);
    }

    public static class Handler implements IMessageHandler<PacketResolveConflicts, IMessage> {

        @Override
        public IMessage onMessage(PacketResolveConflicts message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();
            ConflictSession session = ConflictSession.get(uuid);

            if (session == null) return null;

            if (message.cancel) {
                ConflictSession.stop(uuid);
                send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.conflict.cancelled");
                return null;
            }

            int serverConflictIndex = ConflictResolutionService.currentServerStartIndex(session);
            if (message.expectedConflictIndex > 0 && message.expectedConflictIndex != serverConflictIndex) {
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.conflict.session_changed");
                ConflictResolutionService.sendCurrentBatch(player, session);
                return null;
            }

            List<RecipeEntry> currentRecipes = session.getCurrentRecipes();
            if (currentRecipes == null || currentRecipes.isEmpty()) {
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.conflict.session_empty");
                ConflictSession.stop(uuid);
                return null;
            }

            if (message.recipeIndex < 0 || message.recipeIndex >= currentRecipes.size()) {
                send(
                    player,
                    EnumChatFormatting.RED,
                    "ae2patterngen.msg.conflict.invalid_selection",
                    message.recipeIndex);
                ConflictResolutionService.sendCurrentBatch(player, session);
                return null;
            }

            session.select(message.recipeIndex);

            if (session.isComplete()) {
                ConflictResolutionService.finalizeSession(player, session);
                ConflictSession.stop(uuid);
            } else {
                ConflictResolutionService.sendCurrentBatch(player, session);
            }

            return null;
        }

        private void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
            player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
        }
    }
}
