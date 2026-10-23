package com.github.ae2patterngen.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;

import com.github.ae2patterngen.config.ForgeConfig;

/**
 * 样板虚拟仓储 — 基于本地文件的持久化存储
 * <p>
 * 存储路径: {@code <世界存档>/<配置目录名>/<玩家UUID>.dat}
 * <p>
 * 文件格式: GZip 压缩的 NBT，按"来源通道"分桶：
 * <ul>
 * <li>默认通道沿用旧格式的顶层键（{@code Patterns/Count/Source/Timestamp}），旧存档无需迁移；</li>
 * <li>其它通道放在 {@code Channels/<通道名>/} 下，例如配方浏览器生成的样板。</li>
 * </ul>
 * 分桶的目的是让"按过滤条件生成"与"配方浏览器生成"互不阻塞、互不覆盖，各自在自己的仓储界面里管理。
 */
public class PatternStorage {

    /** 默认通道：样板生成器（也是历史数据所在通道）。 */
    public static final String CHANNEL_DEFAULT = "";
    /** 配方浏览器通道。 */
    public static final String CHANNEL_BROWSER = "browser";

    // NBT 键名
    private static final String KEY_PATTERNS = "Patterns";
    private static final String KEY_COUNT = "Count";
    private static final String KEY_SOURCE = "Source";
    private static final String KEY_TIMESTAMP = "Timestamp";
    private static final String KEY_CHANNELS = "Channels";

    private static volatile StorageDirectoryResolver directoryResolver = new StorageDirectoryResolver() {

        @Override
        public File getWorldSaveRoot() {
            return DimensionManager.getCurrentSaveRootDirectory();
        }
    };

    /**
     * 保存样板列表到默认通道
     */
    public static boolean save(UUID playerUUID, List<ItemStack> patterns, String source) {
        return save(playerUUID, patterns, source, CHANNEL_DEFAULT);
    }

    /**
     * 保存样板列表到指定通道（只覆盖该通道，其它通道原样保留）
     */
    public static boolean save(UUID playerUUID, List<ItemStack> patterns, String source, String channel) {
        File tmpFile = null;
        try {
            File file = getStorageFile(playerUUID);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                System.err
                    .println("[AE2PatternGen] Failed to create pattern storage directory: " + parent.getAbsolutePath());
                return false;
            }

            NBTTagCompound root = readRoot(file);
            NBTTagCompound bucket = bucketOf(root, channel, true);

            NBTTagList list = new NBTTagList();
            if (patterns != null) {
                for (ItemStack stack : patterns) {
                    if (stack == null) continue;
                    NBTTagCompound tag = new NBTTagCompound();
                    stack.writeToNBT(tag);
                    list.appendTag(tag);
                }
            }

            bucket.setTag(KEY_PATTERNS, list);
            bucket.setInteger(KEY_COUNT, list.tagCount());
            bucket.setString(KEY_SOURCE, source != null ? source : "");
            bucket.setLong(KEY_TIMESTAMP, System.currentTimeMillis());

            if (isEmptyBuckets(root)) {
                file.delete();
                return true;
            }

            tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }

            try {
                Files.move(
                    tmpFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to save pattern storage: " + e.getMessage());
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
            return false;
        }
    }

    /**
     * 从默认通道加载全部样板
     */
    public static List<ItemStack> load(UUID playerUUID) {
        return load(playerUUID, CHANNEL_DEFAULT);
    }

    /**
     * 从指定通道加载全部样板
     */
    public static List<ItemStack> load(UUID playerUUID, String channel) {
        List<ItemStack> patterns = new ArrayList<ItemStack>();
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists()) return patterns;

            NBTTagCompound root;
            try (FileInputStream fis = new FileInputStream(file)) {
                root = CompressedStreamTools.readCompressed(fis);
            }
            NBTTagCompound bucket = bucketOf(root, channel, false);
            if (bucket == null) {
                return patterns;
            }

            String source = bucket.getString(KEY_SOURCE);
            NBTTagList list = bucket.getTagList(KEY_PATTERNS, 10); // 10 = NBTTagCompound
            boolean repaired = false;

            for (int i = 0; i < list.tagCount(); i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null) {
                    if (stack.stackSize <= 0) {
                        stack.stackSize = 1;
                        repaired = true;
                    }
                    if (normalizePatternCounts(stack)) {
                        repaired = true;
                    }
                    patterns.add(stack);
                }
            }

            // 自动修复历史坏样板（负数/1L/Cnt-Count 不一致）并回写
            if (repaired) {
                save(playerUUID, patterns, source, channel);
            }
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to load pattern storage: " + e.getMessage());
        }
        return patterns;
    }

    /**
     * 获取默认通道的存储摘要 (不加载全部数据)
     */
    public static StorageSummary getSummary(UUID playerUUID) {
        return getSummary(playerUUID, CHANNEL_DEFAULT);
    }

    /**
     * 获取指定通道的存储摘要 (不加载全部数据)
     */
    public static StorageSummary getSummary(UUID playerUUID, String channel) {
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists()) return StorageSummary.EMPTY;

            NBTTagCompound root;
            try (FileInputStream fis = new FileInputStream(file)) {
                root = CompressedStreamTools.readCompressed(fis);
            }
            NBTTagCompound bucket = bucketOf(root, channel, false);
            if (bucket == null) {
                return StorageSummary.EMPTY;
            }

            int count = bucket.getInteger(KEY_COUNT);
            String source = bucket.getString(KEY_SOURCE);
            long timestamp = bucket.getLong(KEY_TIMESTAMP);

            if (count == 0) return StorageSummary.EMPTY;

            // 加载前几条作为预览 (解析产物名)
            List<String> previews = new ArrayList<String>();
            NBTTagList list = bucket.getTagList(KEY_PATTERNS, 10);
            for (int i = 0; i < list.tagCount(); i++) {
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
     * 检查默认通道是否为空
     */
    public static boolean isEmpty(UUID playerUUID) {
        return isEmpty(playerUUID, CHANNEL_DEFAULT);
    }

    /**
     * 检查指定通道是否为空
     */
    public static boolean isEmpty(UUID playerUUID, String channel) {
        return getSummary(playerUUID, channel).count == 0;
    }

    /**
     * 清空默认通道
     */
    public static void clear(UUID playerUUID) {
        clear(playerUUID, CHANNEL_DEFAULT);
    }

    /**
     * 清空指定通道（其它通道保留；全部为空时删除文件）
     */
    public static void clear(UUID playerUUID, String channel) {
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists()) return;

            NBTTagCompound root;
            try (FileInputStream fis = new FileInputStream(file)) {
                root = CompressedStreamTools.readCompressed(fis);
            }

            if (CHANNEL_DEFAULT.equals(normalizeChannel(channel))) {
                root.removeTag(KEY_PATTERNS);
                root.removeTag(KEY_COUNT);
                root.removeTag(KEY_SOURCE);
                root.removeTag(KEY_TIMESTAMP);
            } else {
                NBTTagCompound channels = root.getCompoundTag(KEY_CHANNELS);
                channels.removeTag(normalizeChannel(channel));
                if (channels.hasNoTags()) {
                    root.removeTag(KEY_CHANNELS);
                }
            }

            if (isEmptyBuckets(root)) {
                file.delete();
                return;
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to clear pattern storage: " + e.getMessage());
        }
    }

    /**
     * 从指定通道取出指定数量的样板并更新文件
     */
    public static List<ItemStack> extract(UUID playerUUID, int maxCount) {
        return extract(playerUUID, maxCount, CHANNEL_DEFAULT);
    }

    public static List<ItemStack> extract(UUID playerUUID, int maxCount, String channel) {
        List<ItemStack> all = load(playerUUID, channel);
        List<ItemStack> extracted = new ArrayList<ItemStack>();

        int toExtract = Math.min(maxCount, all.size());
        for (int i = 0; i < toExtract; i++) {
            extracted.add(all.remove(0));
        }

        if (all.isEmpty()) {
            clear(playerUUID, channel);
        } else {
            StorageSummary summary = getSummary(playerUUID, channel);
            if (!save(playerUUID, all, summary.source, channel)) {
                System.err.println("[AE2PatternGen] Failed to persist remaining patterns after extract.");
            }
        }

        return extracted;
    }

    /**
     * 删除指定通道中指定索引的样板
     */
    public static ItemStack delete(UUID playerUUID, int index) {
        return delete(playerUUID, index, CHANNEL_DEFAULT);
    }

    public static ItemStack delete(UUID playerUUID, int index, String channel) {
        List<ItemStack> all = load(playerUUID, channel);
        if (index < 0 || index >= all.size()) return null;

        ItemStack removed = all.remove(index);

        if (all.isEmpty()) {
            clear(playerUUID, channel);
        } else {
            StorageSummary summary = getSummary(playerUUID, channel);
            if (!save(playerUUID, all, summary.source, channel)) {
                System.err.println("[AE2PatternGen] Failed to persist remaining patterns after delete.");
            }
        }

        return removed;
    }

    /**
     * 获取指定通道、指定页的样板摘要预览
     */
    public static StorageSummary getPage(UUID playerUUID, int page, int pageSize) {
        return getPage(playerUUID, page, pageSize, CHANNEL_DEFAULT);
    }

    public static StorageSummary getPage(UUID playerUUID, int page, int pageSize, String channel) {
        try {
            File file = getStorageFile(playerUUID);
            if (!file.exists()) return StorageSummary.EMPTY;

            NBTTagCompound root;
            try (FileInputStream fis = new FileInputStream(file)) {
                root = CompressedStreamTools.readCompressed(fis);
            }
            NBTTagCompound bucket = bucketOf(root, channel, false);
            if (bucket == null) {
                return StorageSummary.EMPTY;
            }

            int count = bucket.getInteger(KEY_COUNT);
            String source = bucket.getString(KEY_SOURCE);
            long timestamp = bucket.getLong(KEY_TIMESTAMP);

            if (count == 0) return StorageSummary.EMPTY;

            NBTTagList list = bucket.getTagList(KEY_PATTERNS, 10);
            int start = Math.max(0, page * pageSize);
            int end = Math.min(start + pageSize, list.tagCount());

            List<String> previews = new ArrayList<String>();
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
     * 获取指定通道中指定索引的样板详情 (输入/输出物品名列表)
     */
    public static PatternDetail getPatternDetail(UUID playerUUID, int index) {
        return getPatternDetail(playerUUID, index, CHANNEL_DEFAULT);
    }

    public static PatternDetail getPatternDetail(UUID playerUUID, int index, String channel) {
        List<ItemStack> all = load(playerUUID, channel);
        if (index < 0 || index >= all.size()) return null;

        ItemStack pattern = all.get(index);
        List<String> inputs = new ArrayList<String>();
        List<String> outputs = new ArrayList<String>();

        if (pattern.hasTagCompound()) {
            NBTTagCompound tag = pattern.getTagCompound();

            NBTTagList inList = tag.getTagList("in", 10);
            for (int i = 0; i < inList.tagCount(); i++) {
                ItemStack item = ItemStack.loadItemStackFromNBT(inList.getCompoundTagAt(i));
                if (item != null) {
                    inputs.add(item.getDisplayName() + " x" + countOf(inList.getCompoundTagAt(i), item));
                }
            }

            NBTTagList outList = tag.getTagList("out", 10);
            for (int i = 0; i < outList.tagCount(); i++) {
                ItemStack item = ItemStack.loadItemStackFromNBT(outList.getCompoundTagAt(i));
                if (item != null) {
                    outputs.add(item.getDisplayName() + " x" + countOf(outList.getCompoundTagAt(i), item));
                }
            }
        }

        return new PatternDetail(inputs, outputs);
    }

    private static long countOf(NBTTagCompound itemTag, ItemStack item) {
        long count = itemTag.hasKey("Cnt") ? itemTag.getLong("Cnt") : item.stackSize;
        return count <= 0 ? 1 : count;
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
                if (sb.length() > 0) sb.append(", ");
                long cnt = countOf(outList.getCompoundTagAt(i), item);
                sb.append(item.getDisplayName());
                if (cnt > 1) sb.append(" x")
                    .append(cnt);
            }
        }

        return sb.length() > 0 ? sb.toString() : pattern.getDisplayName();
    }

    /**
     * 修复旧版本样板中可能出现的计数字段异常（负数、Count/Cnt 不一致）。
     */
    private static boolean normalizePatternCounts(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) {
            return false;
        }
        NBTTagCompound root = pattern.getTagCompound();
        boolean changed = false;
        changed |= normalizePatternSide(root.getTagList("in", 10));
        changed |= normalizePatternSide(root.getTagList("out", 10));
        return changed;
    }

    private static boolean normalizePatternSide(NBTTagList list) {
        boolean changed = false;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound itemTag = list.getCompoundTagAt(i);
            int count = itemTag.getInteger("Count");
            long cnt = itemTag.hasKey("Cnt") ? itemTag.getLong("Cnt") : Long.MIN_VALUE;

            int target = (cnt > 0) ? clampToPositiveInt(cnt) : count;
            if (target <= 0) {
                target = 1;
            }

            if (!itemTag.hasKey("Count") || count != target) {
                itemTag.setInteger("Count", target);
                changed = true;
            }
            if (!itemTag.hasKey("Cnt") || cnt != (long) target) {
                itemTag.setLong("Cnt", target);
                changed = true;
            }
        }
        return changed;
    }

    private static int clampToPositiveInt(long value) {
        if (value <= 0) {
            return 1;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /** 通道名为空即默认通道（旧格式的顶层键）。 */
    private static String normalizeChannel(String channel) {
        return channel != null ? channel : CHANNEL_DEFAULT;
    }

    private static NBTTagCompound bucketOf(NBTTagCompound root, String channel, boolean create) {
        if (root == null) {
            return null;
        }
        String key = normalizeChannel(channel);
        if (CHANNEL_DEFAULT.equals(key)) {
            return root;
        }

        NBTTagCompound channels = root.getCompoundTag(KEY_CHANNELS);
        if (!channels.hasKey(key)) {
            if (!create) {
                return null;
            }
            channels.setTag(key, new NBTTagCompound());
            root.setTag(KEY_CHANNELS, channels);
        }
        return channels.getCompoundTag(key);
    }

    /** 文件里是否还有任何通道存在（含空列表的通道）。 */
    private static boolean isEmptyBuckets(NBTTagCompound root) {
        if (root.hasKey(KEY_PATTERNS)) {
            return false;
        }
        return root.getCompoundTag(KEY_CHANNELS)
            .hasNoTags();
    }

    private static NBTTagCompound readRoot(File file) {
        if (!file.exists()) {
            return new NBTTagCompound();
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            return root != null ? root : new NBTTagCompound();
        } catch (Exception e) {
            return new NBTTagCompound();
        }
    }

    private static File getStorageFile(UUID playerUUID) {
        File worldDir = directoryResolver.getWorldSaveRoot();
        String dirName = ForgeConfig.getStorageDirectoryName();
        File storageDir = worldDir != null ? new File(worldDir, dirName) : new File(dirName);
        return new File(storageDir, playerUUID.toString() + ".dat");
    }

    static void setDirectoryResolver(StorageDirectoryResolver resolver) {
        directoryResolver = resolver != null ? resolver : directoryResolver;
    }

    static void resetDirectoryResolver() {
        directoryResolver = new StorageDirectoryResolver() {

            @Override
            public File getWorldSaveRoot() {
                return DimensionManager.getCurrentSaveRootDirectory();
            }
        };
    }

    /** 世界存档根目录解析钩子（单元测试注入临时目录）。 */
    interface StorageDirectoryResolver {

        File getWorldSaveRoot();
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
