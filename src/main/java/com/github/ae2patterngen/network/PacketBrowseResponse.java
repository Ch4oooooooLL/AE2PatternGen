package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * 服务端 -> 客户端：配方浏览器数据。
 * <p>
 * 四种负载：配方池列表、规则组列表、规则组配方明细、状态提示。
 */
public class PacketBrowseResponse implements IMessage {

    public static final int TYPE_POOLS = 0;
    public static final int TYPE_GROUPS = 1;
    public static final int TYPE_RECIPES = 2;
    public static final int TYPE_STATUS = 3;
    /** 三级页面：过滤后的命中统计 + 输入输出清单。 */
    public static final int TYPE_PREVIEW = 4;
    /** 浏览器仓储：一页样板预览。 */
    public static final int TYPE_STORAGE = 5;

    /** 单个规则组在客户端展示所需的摘要信息。 */
    public static final class GroupSummary {

        public String groupId = "";
        public int[] circuitDamages = new int[0];
        public ItemStack circuitDisplay;
        public ItemStack[] ncItems = new ItemStack[0];
        public ItemStack[] outputSamples = new ItemStack[0];
        public int recipeCount;
        public boolean unruled;
    }

    public int type = TYPE_STATUS;
    public String message = "";
    public String mapId = "";
    public String groupId = "";

    public List<String> poolMapIds = new ArrayList<String>();
    public List<String> poolModIds = new ArrayList<String>();
    public List<Integer> poolRecipeCounts = new ArrayList<Integer>();

    public List<GroupSummary> groups = new ArrayList<GroupSummary>();
    public int totalRecipeCount;
    public int hiddenSingletonCount;
    public int hiddenUnruledRecipeCount;
    public boolean showSingletonRules;
    public boolean showUnruledGroup;

    public List<RecipeEntry> recipes = new ArrayList<RecipeEntry>();
    public int recipeTotalCount;

    public int groupTotalCount;
    public int matchedCount;
    public int distinctInputCount;
    public int distinctOutputCount;
    public ItemStack[] inputSamples = new ItemStack[0];
    public ItemStack[] outputSamples = new ItemStack[0];

    public int storageCount;
    public String storageSource = "";
    public int storagePageIndex;
    public int storagePageSize;
    public List<String> storagePreviews = new ArrayList<String>();

    public PacketBrowseResponse() {}

    /** 状态提示负载：message 已是本地化好的最终文本。 */
    public static PacketBrowseResponse statusText(String localizedText) {
        PacketBrowseResponse response = new PacketBrowseResponse();
        response.type = TYPE_STATUS;
        response.message = localizedText != null ? localizedText : "";
        return response;
    }

    /** 三级页面负载：规则组统计 + 命中的输入输出清单。 */
    public static PacketBrowseResponse preview(String mapId, String groupId,
        com.github.ae2patterngen.recipe.RecipePreviewSupport.Preview preview) {
        PacketBrowseResponse response = new PacketBrowseResponse();
        response.type = TYPE_PREVIEW;
        response.mapId = mapId != null ? mapId : "";
        response.groupId = groupId != null ? groupId : "";
        if (preview != null) {
            response.groupTotalCount = preview.totalCount;
            response.matchedCount = preview.matchedCount;
            response.distinctInputCount = preview.distinctInputCount;
            response.distinctOutputCount = preview.distinctOutputCount;
            response.inputSamples = preview.inputSamples;
            response.outputSamples = preview.outputSamples;
        }
        return response;
    }

    /** 浏览器仓储负载：一页样板预览。 */
    public static PacketBrowseResponse storage(int count, String source, int pageIndex, int pageSize,
        List<String> previews) {
        PacketBrowseResponse response = new PacketBrowseResponse();
        response.type = TYPE_STORAGE;
        response.storageCount = Math.max(0, count);
        response.storageSource = source != null ? source : "";
        response.storagePageIndex = Math.max(0, pageIndex);
        response.storagePageSize = Math.max(1, pageSize);
        response.storagePreviews = previews != null ? previews : new ArrayList<String>();
        return response;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readInt();
        switch (type) {
            case TYPE_POOLS:
                readPools(buf);
                break;
            case TYPE_GROUPS:
                readGroups(buf);
                break;
            case TYPE_RECIPES:
                readRecipes(buf);
                break;
            case TYPE_PREVIEW:
                readPreview(buf);
                break;
            case TYPE_STORAGE:
                readStorage(buf);
                break;
            case TYPE_STATUS:
            default:
                message = ByteBufUtils.readUTF8String(buf);
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(type);
        switch (type) {
            case TYPE_POOLS:
                writePools(buf);
                break;
            case TYPE_GROUPS:
                writeGroups(buf);
                break;
            case TYPE_RECIPES:
                writeRecipes(buf);
                break;
            case TYPE_PREVIEW:
                writePreview(buf);
                break;
            case TYPE_STORAGE:
                writeStorage(buf);
                break;
            case TYPE_STATUS:
            default:
                ByteBufUtils.writeUTF8String(buf, message != null ? message : "");
                break;
        }
    }

    private void readPools(ByteBuf buf) {
        int size = buf.readInt();
        poolMapIds = new ArrayList<String>(size);
        poolModIds = new ArrayList<String>(size);
        poolRecipeCounts = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            poolMapIds.add(ByteBufUtils.readUTF8String(buf));
            poolModIds.add(ByteBufUtils.readUTF8String(buf));
            poolRecipeCounts.add(buf.readInt());
        }
    }

    private void writePools(ByteBuf buf) {
        List<String> mapIds = poolMapIds != null ? poolMapIds : Collections.<String>emptyList();
        buf.writeInt(mapIds.size());
        for (int i = 0; i < mapIds.size(); i++) {
            ByteBufUtils.writeUTF8String(buf, safe(mapIds.get(i)));
            ByteBufUtils
                .writeUTF8String(buf, poolModIds != null && i < poolModIds.size() ? safe(poolModIds.get(i)) : "");
            buf.writeInt(poolRecipeCounts != null && i < poolRecipeCounts.size() ? poolRecipeCounts.get(i) : 0);
        }
    }

    private void readGroups(ByteBuf buf) {
        mapId = ByteBufUtils.readUTF8String(buf);
        totalRecipeCount = buf.readInt();
        hiddenSingletonCount = buf.readInt();
        hiddenUnruledRecipeCount = buf.readInt();
        showSingletonRules = buf.readBoolean();
        showUnruledGroup = buf.readBoolean();

        int size = buf.readInt();
        groups = new ArrayList<GroupSummary>(size);
        for (int i = 0; i < size; i++) {
            GroupSummary summary = new GroupSummary();
            summary.groupId = ByteBufUtils.readUTF8String(buf);
            summary.recipeCount = buf.readInt();
            summary.unruled = buf.readBoolean();

            int circuitCount = buf.readInt();
            summary.circuitDamages = new int[circuitCount];
            for (int c = 0; c < circuitCount; c++) {
                summary.circuitDamages[c] = buf.readInt();
            }
            summary.circuitDisplay = ByteBufUtils.readItemStack(buf);
            summary.ncItems = readItemStacks(buf);
            summary.outputSamples = readItemStacks(buf);
            groups.add(summary);
        }
    }

    private void writeGroups(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(mapId));
        buf.writeInt(totalRecipeCount);
        buf.writeInt(hiddenSingletonCount);
        buf.writeInt(hiddenUnruledRecipeCount);
        buf.writeBoolean(showSingletonRules);
        buf.writeBoolean(showUnruledGroup);

        List<GroupSummary> safeGroups = groups != null ? groups : Collections.<GroupSummary>emptyList();
        buf.writeInt(safeGroups.size());
        for (GroupSummary summary : safeGroups) {
            ByteBufUtils.writeUTF8String(buf, safe(summary.groupId));
            buf.writeInt(summary.recipeCount);
            buf.writeBoolean(summary.unruled);

            int[] damages = summary.circuitDamages != null ? summary.circuitDamages : new int[0];
            buf.writeInt(damages.length);
            for (int damage : damages) {
                buf.writeInt(damage);
            }
            ByteBufUtils.writeItemStack(buf, summary.circuitDisplay);
            writeItemStacks(buf, summary.ncItems);
            writeItemStacks(buf, summary.outputSamples);
        }
    }

    private void readRecipes(ByteBuf buf) {
        mapId = ByteBufUtils.readUTF8String(buf);
        groupId = ByteBufUtils.readUTF8String(buf);
        recipeTotalCount = buf.readInt();
        int size = buf.readInt();
        recipes = new ArrayList<RecipeEntry>(size);
        for (int i = 0; i < size; i++) {
            recipes.add(PacketRecipeConflicts.readRecipe(buf));
        }
    }

    private void writeRecipes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(mapId));
        ByteBufUtils.writeUTF8String(buf, safe(groupId));
        buf.writeInt(recipeTotalCount);

        List<RecipeEntry> safeRecipes = recipes != null ? recipes : Collections.<RecipeEntry>emptyList();
        buf.writeInt(safeRecipes.size());
        for (RecipeEntry recipe : safeRecipes) {
            PacketRecipeConflicts.writeRecipe(buf, recipe);
        }
    }

    private void readPreview(ByteBuf buf) {
        mapId = ByteBufUtils.readUTF8String(buf);
        groupId = ByteBufUtils.readUTF8String(buf);
        groupTotalCount = buf.readInt();
        matchedCount = buf.readInt();
        distinctInputCount = buf.readInt();
        distinctOutputCount = buf.readInt();
        inputSamples = readItemStacks(buf);
        outputSamples = readItemStacks(buf);
    }

    private void writePreview(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(mapId));
        ByteBufUtils.writeUTF8String(buf, safe(groupId));
        buf.writeInt(groupTotalCount);
        buf.writeInt(matchedCount);
        buf.writeInt(distinctInputCount);
        buf.writeInt(distinctOutputCount);
        writeItemStacks(buf, inputSamples);
        writeItemStacks(buf, outputSamples);
    }

    private void readStorage(ByteBuf buf) {
        storageCount = buf.readInt();
        storageSource = ByteBufUtils.readUTF8String(buf);
        storagePageIndex = buf.readInt();
        storagePageSize = buf.readInt();
        int size = buf.readInt();
        storagePreviews = new ArrayList<String>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            storagePreviews.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    private void writeStorage(ByteBuf buf) {
        buf.writeInt(storageCount);
        ByteBufUtils.writeUTF8String(buf, safe(storageSource));
        buf.writeInt(storagePageIndex);
        buf.writeInt(storagePageSize);
        List<String> previews = storagePreviews != null ? storagePreviews : Collections.<String>emptyList();
        buf.writeInt(previews.size());
        for (String preview : previews) {
            ByteBufUtils.writeUTF8String(buf, safe(preview));
        }
    }

    static void writeItemStacks(ByteBuf buf, ItemStack[] stacks) {
        List<ItemStack> present = new ArrayList<ItemStack>();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack != null && stack.getItem() != null) {
                    present.add(stack);
                }
            }
        }
        buf.writeInt(present.size());
        for (ItemStack stack : present) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    static ItemStack[] readItemStacks(ByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> stacks = new ArrayList<ItemStack>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            if (stack != null && stack.getItem() != null) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new ItemStack[stacks.size()]);
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    public static class Handler implements IMessageHandler<PacketBrowseResponse, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBrowseResponse message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> com.github.ae2patterngen.gui.GuiRecipeBrowser.open(message));
            return null;
        }
    }
}
