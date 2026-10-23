package com.github.ae2patterngen.recipe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.util.GTUtility;

/**
 * 配方池规则分类器。
 * <p>
 * 分类维度：
 * <ul>
 * <li><b>编程电路</b>：配方物品输入里 stackSize = 0 的 GT 编程电路（{@code gt.integrated_circuit}），
 * 取物品 meta 作为电路号。meta 0 按 GT 幽灵电路的语义视为"不要求电路"。</li>
 * <li><b>NC 物品</b>：配方里"不消耗"的其它物品，即 stackSize = 0 的非电路输入，以及配方自带的
 * specialItems（如模具、催化剂）。</li>
 * </ul>
 * 同一个分组内，编程电路集合与 NC 物品集合必须完全一致。NC 物品按
 * (registryName, meta, NBT) 作为集合比较，不区分顺序、不比较数量。
 */
public final class RuleClassifier {

    /** 判定某个物品是否为 GT 编程电路的钩子，便于脱离 GT 运行环境做单元测试。 */
    public interface CircuitDetector {

        boolean isCircuit(ItemStack stack);
    }

    private static final char GROUP_KEY_SEPARATOR = '\u0001';

    private RuleClassifier() {}

    /**
     * 默认电路判定器。GT 类只在真正执行分类时才加载，避免单元测试环境触发 GT 静态初始化。
     */
    public static CircuitDetector defaultCircuitDetector() {
        return new GTCircuitDetector();
    }

    public static PoolRuleIndex classify(String mapId, List<RecipeEntry> recipes) {
        return classify(mapId, recipes, defaultCircuitDetector());
    }

    public static PoolRuleIndex classify(String mapId, List<RecipeEntry> recipes, CircuitDetector detector) {
        CircuitDetector circuitDetector = detector != null ? detector : defaultCircuitDetector();

        Map<String, Bucket> buckets = new LinkedHashMap<String, Bucket>();
        int total = 0;
        if (recipes != null) {
            for (RecipeEntry recipe : recipes) {
                if (recipe == null) {
                    continue;
                }
                total++;

                Bucket sample = describe(recipe, circuitDetector);
                Bucket bucket = buckets.get(sample.groupId);
                if (bucket == null) {
                    bucket = sample;
                    buckets.put(bucket.groupId, bucket);
                }
                bucket.recipes.add(recipe);
            }
        }

        List<RuleGroup> groups = new ArrayList<RuleGroup>(buckets.size());
        for (Bucket bucket : buckets.values()) {
            groups.add(bucket.toGroup());
        }
        Collections.sort(groups, GROUP_ORDER);
        return new PoolRuleIndex(mapId, total, groups);
    }

    /**
     * 计算单个配方的规则描述（电路编号、NC 物品签名与展示用物品）。
     */
    static Bucket describe(RecipeEntry recipe, CircuitDetector detector) {
        TreeMap<Integer, ItemStack> circuits = new TreeMap<Integer, ItemStack>();
        Map<String, ItemStack> ncItems = new LinkedHashMap<String, ItemStack>();

        collect(recipe.inputs, true, circuits, ncItems, detector);
        collect(recipe.specialItems, false, circuits, ncItems, detector);

        int[] circuitDamages = new int[circuits.size()];
        int index = 0;
        for (Integer damage : circuits.keySet()) {
            circuitDamages[index++] = damage != null ? damage.intValue() : 0;
        }

        List<String> ncKeys = new ArrayList<String>(ncItems.keySet());
        Collections.sort(ncKeys);

        ItemStack circuitDisplay = null;
        if (!circuits.isEmpty()) {
            ItemStack source = circuits.get(circuits.firstKey());
            circuitDisplay = displayCopy(source);
        }

        ItemStack[] ncDisplays = new ItemStack[ncKeys.size()];
        for (int i = 0; i < ncKeys.size(); i++) {
            ncDisplays[i] = displayCopy(ncItems.get(ncKeys.get(i)));
        }

        return new Bucket(
            buildGroupId(circuitDamages, ncKeys),
            circuitDamages,
            circuitDisplay,
            ncKeys.toArray(new String[ncKeys.size()]),
            ncDisplays);
    }

    private static void collect(ItemStack[] stacks, boolean zeroSizeMeansNonConsumed,
        TreeMap<Integer, ItemStack> circuits, Map<String, ItemStack> ncItems, CircuitDetector detector) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null) {
                continue;
            }
            if (detector.isCircuit(stack)) {
                // meta 0 等价于 GT 幽灵电路槽的"未设置电路"，不计入规则。
                if (stack.getItemDamage() > 0 && !circuits.containsKey(stack.getItemDamage())) {
                    circuits.put(stack.getItemDamage(), stack);
                }
                continue;
            }
            if (zeroSizeMeansNonConsumed && stack.stackSize != 0) {
                continue;
            }
            String key = canonicalNcKey(stack);
            if (!ncItems.containsKey(key)) {
                ncItems.put(key, stack);
            }
        }
    }

    /** NC 物品的内部签名：(registryName, meta, NBT)，不包含数量。 */
    static String canonicalNcKey(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        builder.append(registryNameOf(stack))
            .append('@')
            .append(stack.getItemDamage());
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            builder.append('#')
                .append(tag.toString());
        }
        return builder.toString();
    }

    static String registryNameOf(ItemStack stack) {
        try {
            Object name = Item.itemRegistry.getNameForObject(stack.getItem());
            if (name != null) {
                return name.toString();
            }
        } catch (Throwable ignored) {}

        try {
            int id = Item.getIdFromItem(stack.getItem());
            if (id > 0) {
                return "id:" + id;
            }
        } catch (Throwable ignored) {}

        // 仅在物品未注册时才会走到这里（例如未加载完整游戏环境的单元测试）。
        return "unregistered:" + System.identityHashCode(stack.getItem());
    }

    static String buildGroupId(int[] circuitDamages, List<String> ncKeys) {
        StringBuilder builder = new StringBuilder("c");
        if (circuitDamages != null) {
            for (int damage : circuitDamages) {
                builder.append(damage)
                    .append('+');
            }
        }
        builder.append("|n");
        if (ncKeys == null || ncKeys.isEmpty()) {
            return builder.append('0')
                .toString();
        }

        StringBuilder joined = new StringBuilder();
        for (String key : ncKeys) {
            joined.append(key)
                .append(GROUP_KEY_SEPARATOR);
        }
        return builder.append(shortHash(joined.toString()))
            .toString();
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < bytes.length && hex.length() < 12; i++) {
                hex.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
                if (hex.length() < 12) {
                    hex.append(Character.forDigit(bytes[i] & 0xF, 16));
                }
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static ItemStack displayCopy(ItemStack source) {
        if (source == null || source.getItem() == null) {
            return null;
        }
        ItemStack copy = source.copy();
        copy.stackSize = 1;
        return copy;
    }

    private static final Comparator<RuleGroup> GROUP_ORDER = new Comparator<RuleGroup>() {

        @Override
        public int compare(RuleGroup left, RuleGroup right) {
            int byCount = Integer.compare(right.size(), left.size());
            if (byCount != 0) {
                return byCount;
            }
            int byCircuit = compareCircuits(left.circuitDamages, right.circuitDamages);
            if (byCircuit != 0) {
                return byCircuit;
            }
            return left.groupId.compareTo(right.groupId);
        }
    };

    private static int compareCircuits(int[] left, int[] right) {
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int compared = Integer.compare(left[i], right[i]);
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    static final class Bucket {

        final String groupId;
        final int[] circuitDamages;
        final ItemStack circuitDisplay;
        final String[] ncKeys;
        final ItemStack[] ncDisplays;
        final List<RecipeEntry> recipes = new ArrayList<RecipeEntry>();

        Bucket(String groupId, int[] circuitDamages, ItemStack circuitDisplay, String[] ncKeys,
            ItemStack[] ncDisplays) {
            this.groupId = groupId;
            this.circuitDamages = circuitDamages;
            this.circuitDisplay = circuitDisplay;
            this.ncKeys = ncKeys;
            this.ncDisplays = ncDisplays;
        }

        RuleGroup toGroup() {
            return new RuleGroup(groupId, circuitDamages, circuitDisplay, ncDisplays, recipes);
        }
    }

    private static final class GTCircuitDetector implements CircuitDetector {

        @Override
        public boolean isCircuit(ItemStack stack) {
            try {
                return GTUtility.isAnyIntegratedCircuit(stack);
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
