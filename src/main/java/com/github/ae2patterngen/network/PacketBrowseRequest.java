package com.github.ae2patterngen.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.ae2patterngen.util.I18nUtil;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端：配方浏览器请求。
 * <p>
 * 同一个入口承担"打开池列表 / 展开规则组 / 展开配方明细 / 预览过滤 / 生成样板"五件事，
 * 服务端始终基于持久化配方缓存重新解析，不信任客户端送来的配方内容。
 */
public class PacketBrowseRequest implements IMessage {

    public static final int ACTION_OPEN = 0;
    public static final int ACTION_GROUPS = 1;
    public static final int ACTION_RECIPES = 2;
    public static final int ACTION_GENERATE = 3;
    /** 三级页面的"预览"：按样板生成器同一套过滤语法统计命中配方与输入输出清单。 */
    public static final int ACTION_PREVIEW = 4;
    /** 三级页面的"生成"：只生成过滤后命中的配方。 */
    public static final int ACTION_GENERATE_FILTERED = 5;
    /** 右键样板接收方：把浏览器仓储里的样板投递进去（含改名与写电路槽）。 */
    public static final int ACTION_DELIVER = 6;
    /** 浏览器仓储：列一页。 */
    public static final int ACTION_STORAGE_LIST = 7;
    /** 浏览器仓储：删除指定索引的样板（索引用 {@link #recipeIndex} 承载）。 */
    public static final int ACTION_STORAGE_DELETE = 8;
    /** 浏览器仓储：清空。 */
    public static final int ACTION_STORAGE_CLEAR = 9;

    /** 与样板生成器一致的一组过滤条件。 */
    public static final class Filters {

        public final String outputOre;
        public final String inputOre;
        public final String ncItem;
        public final String blacklistInput;
        public final String blacklistOutput;
        public final int targetTier;

        public Filters(String outputOre, String inputOre, String ncItem, String blacklistInput, String blacklistOutput,
            int targetTier) {
            this.outputOre = safe(outputOre);
            this.inputOre = safe(inputOre);
            this.ncItem = safe(ncItem);
            this.blacklistInput = safe(blacklistInput);
            this.blacklistOutput = safe(blacklistOutput);
            this.targetTier = targetTier;
        }

        public static Filters none() {
            return new Filters("", "", "", "", "", -1);
        }
    }

    int action = ACTION_OPEN;
    String mapId = "";
    String groupId = "";
    boolean showSingletonRules;
    boolean showUnruledGroup;
    int recipeIndex = -1;
    Filters filters = Filters.none();
    int targetX;
    int targetY;
    int targetZ;
    int pageIndex;

    public PacketBrowseRequest() {}

    public static PacketBrowseRequest open() {
        return new PacketBrowseRequest();
    }

    public static PacketBrowseRequest groups(String mapId, boolean showSingletonRules, boolean showUnruledGroup) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_GROUPS;
        request.mapId = safe(mapId);
        request.showSingletonRules = showSingletonRules;
        request.showUnruledGroup = showUnruledGroup;
        return request;
    }

    public static PacketBrowseRequest recipes(String mapId, String groupId) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_RECIPES;
        request.mapId = safe(mapId);
        request.groupId = safe(groupId);
        return request;
    }

    public static PacketBrowseRequest generate(String mapId, String groupId, int recipeIndex) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_GENERATE;
        request.mapId = safe(mapId);
        request.groupId = safe(groupId);
        request.recipeIndex = recipeIndex;
        return request;
    }

    public static PacketBrowseRequest preview(String mapId, String groupId, Filters filters) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_PREVIEW;
        request.mapId = safe(mapId);
        request.groupId = safe(groupId);
        request.filters = filters != null ? filters : Filters.none();
        return request;
    }

    public static PacketBrowseRequest generateFiltered(String mapId, String groupId, Filters filters) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_GENERATE_FILTERED;
        request.mapId = safe(mapId);
        request.groupId = safe(groupId);
        request.filters = filters != null ? filters : Filters.none();
        return request;
    }

    public static PacketBrowseRequest deliver(int x, int y, int z) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_DELIVER;
        request.targetX = x;
        request.targetY = y;
        request.targetZ = z;
        return request;
    }

    public static PacketBrowseRequest storageList(int pageIndex) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_STORAGE_LIST;
        request.pageIndex = Math.max(0, pageIndex);
        return request;
    }

    public static PacketBrowseRequest storageDelete(int itemIndex) {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_STORAGE_DELETE;
        request.recipeIndex = itemIndex;
        return request;
    }

    public static PacketBrowseRequest storageClear() {
        PacketBrowseRequest request = new PacketBrowseRequest();
        request.action = ACTION_STORAGE_CLEAR;
        return request;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        mapId = ByteBufUtils.readUTF8String(buf);
        groupId = ByteBufUtils.readUTF8String(buf);
        showSingletonRules = buf.readBoolean();
        showUnruledGroup = buf.readBoolean();
        recipeIndex = buf.readInt();
        filters = new Filters(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readInt());
        targetX = buf.readInt();
        targetY = buf.readInt();
        targetZ = buf.readInt();
        pageIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        ByteBufUtils.writeUTF8String(buf, mapId);
        ByteBufUtils.writeUTF8String(buf, groupId);
        buf.writeBoolean(showSingletonRules);
        buf.writeBoolean(showUnruledGroup);
        buf.writeInt(recipeIndex);
        Filters safeFilters = filters != null ? filters : Filters.none();
        ByteBufUtils.writeUTF8String(buf, safeFilters.outputOre);
        ByteBufUtils.writeUTF8String(buf, safeFilters.inputOre);
        ByteBufUtils.writeUTF8String(buf, safeFilters.ncItem);
        ByteBufUtils.writeUTF8String(buf, safeFilters.blacklistInput);
        ByteBufUtils.writeUTF8String(buf, safeFilters.blacklistOutput);
        buf.writeInt(safeFilters.targetTier);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        buf.writeInt(pageIndex);
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    public static class Handler implements IMessageHandler<PacketBrowseRequest, IMessage> {

        @Override
        public IMessage onMessage(PacketBrowseRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || message == null) {
                return null;
            }

            try {
                PacketBrowseResponse response = PacketBrowseResponder.handle(player, message);
                if (response != null) {
                    NetworkHandler.INSTANCE.sendTo(response, player);
                }
            } catch (RuntimeException e) {
                cpw.mods.fml.common.FMLLog.severe(
                    "[AE2PatternGen] Recipe browser request failed for player %s: %s",
                    player.getCommandSenderName(),
                    e.getMessage());
                NetworkHandler.INSTANCE.sendTo(
                    PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.browser.internal_error")),
                    player);
            }
            return null;
        }
    }
}
