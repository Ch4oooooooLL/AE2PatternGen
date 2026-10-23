package com.github.ae2patterngen.delivery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RuleGroup;

/**
 * 玩家最近一次在配方浏览器里"生成某个规则组"的上下文。
 * <p>
 * 投递时必须知道这批样板属于哪条规则（哪个池、哪些编程电路、哪些 NC 物品）才能给样板输入总成
 * 命名与写电路槽，而这些信息在样板本身里是查不到的（样板按设计不写电路/NC）。
 */
public final class HatchBindingRegistry {

    /** 一次待投递的规则绑定。 */
    public static final class Binding {

        public final String mapId;
        public final int[] circuitDamages;
        public final ItemStack[] ncItems;
        public final int patternCount;
        public final long createdAt;

        Binding(String mapId, int[] circuitDamages, ItemStack[] ncItems, int patternCount) {
            this.mapId = mapId != null ? mapId : "";
            this.circuitDamages = circuitDamages != null ? circuitDamages : new int[0];
            this.ncItems = ncItems != null ? ncItems : new ItemStack[0];
            this.patternCount = Math.max(0, patternCount);
            this.createdAt = System.currentTimeMillis();
        }

        public boolean hasCircuit() {
            return circuitDamages.length > 0;
        }

        /** 主电路号（多电路配方取最小号），无电路时返回 -1。 */
        public int primaryCircuit() {
            return circuitDamages.length > 0 ? circuitDamages[0] : -1;
        }
    }

    private static final Map<UUID, Binding> BINDINGS = new HashMap<UUID, Binding>();

    private HatchBindingRegistry() {}

    public static void remember(UUID player, String mapId, RuleGroup group) {
        if (player == null || group == null) {
            return;
        }
        synchronized (BINDINGS) {
            BINDINGS.put(player, new Binding(mapId, group.circuitDamages, group.ncItems, group.size()));
        }
    }

    public static Binding get(UUID player) {
        if (player == null) {
            return null;
        }
        synchronized (BINDINGS) {
            return BINDINGS.get(player);
        }
    }

    public static void clear(UUID player) {
        if (player == null) {
            return;
        }
        synchronized (BINDINGS) {
            BINDINGS.remove(player);
        }
    }

    static void resetForTest() {
        synchronized (BINDINGS) {
            BINDINGS.clear();
        }
    }
}
