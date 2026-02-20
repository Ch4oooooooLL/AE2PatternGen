package com.github.ae2patterngen.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;

/**
 * 样板虚拟仓储 — 基于本地文件的持久化存储
 * <p>
 * 存储路径: {@code <世界存档>/ae2patterngen/<玩家UUID>.dat}
 * <p>
 * 文件格式: GZip 压缩的 NBT，包含样板列表和元数据。
 */
public class PatternStorage {

    private static final String DIR_NAME = "ae2patterngen";

    // NBT 键名
    private static final String KEY_PATTERNS = "Patterns";
    private static final String KEY_COUNT = "Count";
    private static final String KEY_SOURCE = "Source";
    private static final String KEY_TIMESTAMP = "Timestamp";

    /**
     * 保存样板列表到文件
     */
    public static void save(UUID playerUUID, List<ItemStack> patterns, String source) {
        try {
            File file = getStorageFile(playerUUID);
            file.getParentFile()
                    .mkdirs();

            NBTTagCompound root = new NBTTagCompound();
            NBTTagList list = new NBTTagList();

            for (ItemStack stack : patterns) {
                if (stack == null)
                    continue;
                NBTTagCompound tag = new NBTTagCompound();
                stack.writeToNBT(tag);
                list.appendTag(tag);
            }

            root.setTag(KEY_PATTERNS, list);
            root.setInteger(KEY_COUNT, list.tagCount());
            root.setString(KEY_SOURCE, source != null ? source : "");
            root.setLong(KEY_TIMESTAMP, System.currentTimeMillis());

            CompressedStreamTools.writeCompressed(root, new FileOutputStream(file));
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to save pattern storage: " + e.getMessage());
        }
    }

    /**
     * 从文件加载全部样板
     */
    public static List<ItemStack> load(UUID playerUUID) {
        List<ItemStack> patterns = new ArrayList<>();
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists())
                return patterns;

            NBTTagCompound root = CompressedStreamTools.readCompressed(new FileInputStream(file));
            NBTTagList list = root.getTagList(KEY_PATTERNS, 10); // 10 = NBTTagCompound

            for (int i = 0; i < list.tagCount(); i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null) {
                    patterns.add(stack);
                }
            }
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to load pattern storage: " + e.getMessage());
        }
        return patterns;
    }

    /**
     * 获取存储摘要 (不加载全部数据)
     */
    public static StorageSummary getSummary(UUID playerUUID) {
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists())
                return StorageSummary.EMPTY;

            NBTTagCompound root = CompressedStreamTools.readCompressed(new FileInputStream(file));
            int count = root.getInteger(KEY_COUNT);
            String source = root.getString(KEY_SOURCE);
            long timestamp = root.getLong(KEY_TIMESTAMP);

            if (count == 0)
                return StorageSummary.EMPTY;

            // 加载前几条作为预览 (解析产物名)
            List<String> previews = new ArrayList<>();
            NBTTagList list = root.getTagList(KEY_PATTERNS, 10);
            int previewCount = list.tagCount();
            for (int i = 0; i < previewCount; i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null) {
                    previews.add(extractOutputSummary(stack));
                }
            }

            return new StorageSummary(count, source, timestamp, previews);
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to read storage summary: " + e.getMessage());
            return StorageSummary.EMPTY;
        }
    }

    /**
     * 检查存储是否为空
     */
    public static boolean isEmpty(UUID playerUUID) {
        File file = getStorageFile(playerUUID);
        return !file.exists() || file.length() == 0 || getSummary(playerUUID).count == 0;
    }

    /**
     * 清空存储
     */
    public static void clear(UUID playerUUID) {
        File file = getStorageFile(playerUUID);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 从存储中取出指定数量的样板，更新文件
     *
     * @return 实际取出的样板列表
     */
    public static List<ItemStack> extract(UUID playerUUID, int maxCount) {
        List<ItemStack> all = load(playerUUID);
        List<ItemStack> extracted = new ArrayList<>();

        int toExtract = Math.min(maxCount, all.size());
        for (int i = 0; i < toExtract; i++) {
            extracted.add(all.remove(0));
        }

        if (all.isEmpty()) {
            clear(playerUUID);
        } else {
            // 重新保存剩余的
            StorageSummary summary = getSummary(playerUUID);
            save(playerUUID, all, summary.source);
        }

        return extracted;
    }

    /**
     * 删除存储中指定索引的样板
     *
     * @return 被删除的样板，或 null（索引越界）
     */
    public static ItemStack delete(UUID playerUUID, int index) {
        List<ItemStack> all = load(playerUUID);
        if (index < 0 || index >= all.size())
            return null;

        ItemStack removed = all.remove(index);

        if (all.isEmpty()) {
            clear(playerUUID);
        } else {
            StorageSummary summary = getSummary(playerUUID);
            save(playerUUID, all, summary.source);
        }

        return removed;
    }

    /**
     * 获取指定页的样板摘要预览
     *
     * @param page     页码 (0-indexed)
     * @param pageSize 每页数量
     * @return 该页的 StorageSummary（previews 仅包含该页的样板名称）
     */
    public static StorageSummary getPage(UUID playerUUID, int page, int pageSize) {
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists())
                return StorageSummary.EMPTY;

            NBTTagCompound root = CompressedStreamTools.readCompressed(new FileInputStream(file));
            int count = root.getInteger(KEY_COUNT);
            String source = root.getString(KEY_SOURCE);
            long timestamp = root.getLong(KEY_TIMESTAMP);

            if (count == 0)
                return StorageSummary.EMPTY;

            NBTTagList list = root.getTagList(KEY_PATTERNS, 10);
            int start = page * pageSize;
            int end = Math.min(start + pageSize, list.tagCount());

            List<String> previews = new ArrayList<>();
            for (int i = start; i < end; i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null) {
                    previews.add(extractOutputSummary(stack));
                }
            }

            return new StorageSummary(count, source, timestamp, previews);
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to read storage page: " + e.getMessage());
            return StorageSummary.EMPTY;
        }
    }

    /**
     * 获取指定索引的样板详情 (输入/输出物品名列表)
     */
    public static PatternDetail getPatternDetail(UUID playerUUID, int index) {
        List<ItemStack> all = load(playerUUID);
        if (index < 0 || index >= all.size())
            return null;

        ItemStack pattern = all.get(index);
        List<String> inputs = new ArrayList<>();
        List<String> outputs = new ArrayList<>();

        if (pattern.hasTagCompound()) {
            NBTTagCompound tag = pattern.getTagCompound();

            NBTTagList inList = tag.getTagList("in", 10);
            for (int i = 0; i < inList.tagCount(); i++) {
                ItemStack item = ItemStack.loadItemStackFromNBT(inList.getCompoundTagAt(i));
                if (item != null) {
                    long cnt = inList.getCompoundTagAt(i)
                            .hasKey("Cnt")
                                    ? inList.getCompoundTagAt(i)
                                            .getLong("Cnt")
                                    : item.stackSize;
                    inputs.add(item.getDisplayName() + " x" + cnt);
                }
            }

            NBTTagList outList = tag.getTagList("out", 10);
            for (int i = 0; i < outList.tagCount(); i++) {
                ItemStack item = ItemStack.loadItemStackFromNBT(outList.getCompoundTagAt(i));
                if (item != null) {
                    long cnt = outList.getCompoundTagAt(i)
                            .hasKey("Cnt")
                                    ? outList.getCompoundTagAt(i)
                                            .getLong("Cnt")
                                    : item.stackSize;
                    outputs.add(item.getDisplayName() + " x" + cnt);
                }
            }
        }

        return new PatternDetail(inputs, outputs);
    }

    /**
     * 从样板 ItemStack 的 NBT "out" 标签提取产物摘要
     */
    private static String extractOutputSummary(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) {
            return pattern != null ? pattern.getDisplayName() : "?";
        }

        NBTTagCompound tag = pattern.getTagCompound();
        NBTTagList outList = tag.getTagList("out", 10);

        if (outList.tagCount() == 0) {
            return pattern.getDisplayName();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outList.tagCount(); i++) {
            ItemStack item = ItemStack.loadItemStackFromNBT(outList.getCompoundTagAt(i));
            if (item != null) {
                if (sb.length() > 0)
                    sb.append(", ");
                long cnt = outList.getCompoundTagAt(i)
                        .hasKey("Cnt")
                                ? outList.getCompoundTagAt(i)
                                        .getLong("Cnt")
                                : item.stackSize;
                sb.append(item.getDisplayName());
                if (cnt > 1)
                    sb.append(" x")
                            .append(cnt);
            }
        }

        return sb.length() > 0 ? sb.toString() : pattern.getDisplayName();
    }

    private static File getStorageFile(UUID playerUUID) {
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        File storageDir = new File(worldDir, DIR_NAME);
        return new File(storageDir, playerUUID.toString() + ".dat");
    }

    /**
     * 存储摘要 — 轻量级信息
     */
    public static class StorageSummary {

        public static final StorageSummary EMPTY = new StorageSummary(0, "", 0, new ArrayList<String>());

        public final int count;
        public final String source;
        public final long timestamp;
        public final List<String> previews;

        public StorageSummary(int count, String source, long timestamp, List<String> previews) {
            this.count = count;
            this.source = source;
            this.timestamp = timestamp;
            this.previews = previews;
        }
    }

    /**
     * 样板详情 — 输入/输出物品名列表
     */
    public static class PatternDetail {

        public final List<String> inputs;
        public final List<String> outputs;

        public PatternDetail(List<String> inputs, List<String> outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }
}
