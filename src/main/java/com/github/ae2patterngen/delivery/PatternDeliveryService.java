package com.github.ae2patterngen.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.github.ae2patterngen.storage.PatternStorage;
import com.github.ae2patterngen.util.I18nUtil;

import appeng.api.util.IInterfaceViewable;
import appeng.helpers.ICustomNameObject;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTUtility;

/**
 * 把样板浏览器仓储里的样板投递进"样板接收方"（GT 样板输入总成/总线、AE2 面板接口）。
 * <p>
 * 只使用公开 API，不引入 mixin：
 * <ul>
 * <li>识别与槽位：AE2 的 {@link IInterfaceViewable}（拿到 {@code getPatterns()} 与槽数）+ GT 的
 * {@link IConfigurationCircuitSupport#getCircuitSlot()}（总成里"样板槽 + 电路槽 + 手动槽"同处一个库存，
 * 样板槽上限必须是电路槽下标，绝不能拿 {@code getSizeInventory()} 当上限）；</li>
 * <li>改名：AE2 的 {@link ICustomNameObject}；</li>
 * <li>写电路：GT 公开的 {@code getCircuitSlot()} + {@code GTUtility.getIntegratedCircuit(n)}
 * （GT 自己就用 size=0 的电路栈表示幽灵电路）。</li>
 * </ul>
 */
public final class PatternDeliveryService {

    public enum Status {
        /** 有样板被写入。 */
        DELIVERED,
        /** 浏览器仓储里没有样板。 */
        NO_PATTERNS,
        /** 目标不是样板接收方。 */
        NOT_SUPPORTED,
        /** 样板槽已满。 */
        NO_SPACE
    }

    /** 一次投递的结果。 */
    public static final class Result {

        public final Status status;
        public final int inserted;
        public final int capacity;
        public final boolean renamed;
        public final boolean circuitWritten;
        public final boolean circuitCleared;
        public final int circuit;
        public final String appliedName;
        public final ItemStack[] ncItems;
        public final boolean bindingKnown;

        Result(Status status, int inserted, int capacity, boolean renamed, boolean circuitWritten,
            boolean circuitCleared, int circuit, String appliedName, ItemStack[] ncItems, boolean bindingKnown) {
            this.status = status;
            this.inserted = inserted;
            this.capacity = capacity;
            this.renamed = renamed;
            this.circuitWritten = circuitWritten;
            this.circuitCleared = circuitCleared;
            this.circuit = circuit;
            this.appliedName = appliedName != null ? appliedName : "";
            this.ncItems = ncItems != null ? ncItems : new ItemStack[0];
            this.bindingKnown = bindingKnown;
        }
    }

    private PatternDeliveryService() {}

    /** 目标是不是能接收编码样板的对象（GT 总成 / AE2 面板接口）。 */
    public static boolean isPatternTarget(Object metaTileEntity) {
        return metaTileEntity instanceof IInterfaceViewable && patternsOf(metaTileEntity) != null;
    }

    /**
     * 解析方块背后的样板接收方：GT 机器优先看 MTE，其次接受 AE2 面板接口本身。
     */
    public static Object resolveTarget(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity == null) {
            return null;
        }

        if (tileEntity instanceof IGregTechTileEntity) {
            try {
                IMetaTileEntity metaTileEntity = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
                if (metaTileEntity != null && isPatternTarget(metaTileEntity)) {
                    return metaTileEntity;
                }
            } catch (Throwable ignored) {}
        }
        return isPatternTarget(tileEntity) ? tileEntity : null;
    }

    public static Result deliver(UUID player, Object metaTileEntity) {
        if (player == null || !isPatternTarget(metaTileEntity)) {
            return new Result(Status.NOT_SUPPORTED, 0, 0, false, false, false, -1, "", new ItemStack[0], false);
        }

        HatchBindingRegistry.Binding binding = HatchBindingRegistry.get(player);
        ItemStack[] ncItems = binding != null ? binding.ncItems : new ItemStack[0];

        List<ItemStack> patterns = PatternStorage.load(player, PatternStorage.CHANNEL_BROWSER);
        if (patterns.isEmpty()) {
            return new Result(
                Status.NO_PATTERNS,
                0,
                patternSlotLimit(metaTileEntity),
                false,
                false,
                false,
                -1,
                "",
                ncItems,
                binding != null);
        }

        int capacity = patternSlotLimit(metaTileEntity);
        int inserted = 0;
        if (capacity > 0) {
            for (ItemStack pattern : patterns) {
                if (!insertOne(metaTileEntity, capacity, pattern)) {
                    break;
                }
                inserted++;
            }
        }

        if (inserted > 0) {
            List<ItemStack> remaining = new ArrayList<ItemStack>(patterns.subList(inserted, patterns.size()));
            PatternStorage.save(
                player,
                remaining,
                PatternStorage.getSummary(player, PatternStorage.CHANNEL_BROWSER).source,
                PatternStorage.CHANNEL_BROWSER);
        }

        boolean renamed = false;
        boolean circuitWritten = false;
        boolean circuitCleared = false;
        int appliedCircuit = -1;
        String appliedName = "";
        if (binding != null) {
            appliedName = buildName(binding);
            renamed = applyName(metaTileEntity, appliedName);
            int circuit = binding.primaryCircuit();
            if (circuit > 0 && applyCircuit(metaTileEntity, circuit)) {
                circuitWritten = true;
                appliedCircuit = circuit;
            } else if (circuit <= 0) {
                circuitCleared = clearCircuit(metaTileEntity);
            }
        }

        Status status = inserted > 0 ? Status.DELIVERED : (capacity <= 0 ? Status.NOT_SUPPORTED : Status.NO_SPACE);
        return new Result(
            status,
            inserted,
            capacity,
            renamed,
            circuitWritten,
            circuitCleared,
            appliedCircuit,
            appliedName,
            ncItems,
            binding != null);
    }

    /** 样板槽上限：[0, limit) 才是样板槽，0 表示不接受样板。 */
    static int patternSlotLimit(Object metaTileEntity) {
        IInventory patterns = patternsOf(metaTileEntity);
        if (patterns == null) {
            return 0;
        }

        int declared = 0;
        if (metaTileEntity instanceof IConfigurationCircuitSupport) {
            // GT 的样板输入总成把电路槽放在样板槽之后，因此电路槽下标就是样板槽上限。
            try {
                declared = ((IConfigurationCircuitSupport) metaTileEntity).getCircuitSlot();
            } catch (Throwable ignored) {
                declared = 0;
            }
        }
        if (declared <= 0 && metaTileEntity instanceof IInterfaceViewable) {
            IInterfaceViewable viewable = (IInterfaceViewable) metaTileEntity;
            try {
                declared = viewable.numSlots();
                if (declared <= 0) {
                    declared = Math.max(0, viewable.rows()) * Math.max(0, viewable.rowSize());
                }
            } catch (Throwable ignored) {
                declared = 0;
            }
        }
        if (declared <= 0) {
            return 0;
        }
        return Math.min(declared, patterns.getSizeInventory());
    }

    private static IInventory patternsOf(Object metaTileEntity) {
        if (metaTileEntity instanceof IInterfaceViewable) {
            try {
                IInventory patterns = ((IInterfaceViewable) metaTileEntity).getPatterns();
                if (patterns != null) {
                    return patterns;
                }
            } catch (Throwable ignored) {}
        }
        return metaTileEntity instanceof IInventory ? (IInventory) metaTileEntity : null;
    }

    /**
     * 只写空槽；先尊重槽位自己的接受判定，再退回"样板区间里的空槽一律可用"，
     * 避免 GT 侧判定口径变化导致"界面有空位却投不进去"。
     */
    private static boolean insertOne(Object metaTileEntity, int limit, ItemStack pattern) {
        IInventory inventory = patternsOf(metaTileEntity);
        if (inventory == null || pattern == null) {
            return false;
        }

        boolean[] validityPasses = { true, false };
        for (boolean checkValidity : validityPasses) {
            for (int slot = 0; slot < limit; slot++) {
                ItemStack existing = inventory.getStackInSlot(slot);
                if (existing != null && existing.stackSize > 0) {
                    continue;
                }
                if (checkValidity && !accepts(inventory, slot, pattern)) {
                    continue;
                }

                ItemStack copy = pattern.copy();
                copy.stackSize = 1;
                inventory.setInventorySlotContents(slot, copy);
                try {
                    inventory.markDirty();
                } catch (Throwable ignored) {}
                return true;
            }
        }
        return false;
    }

    private static boolean accepts(IInventory inventory, int slot, ItemStack pattern) {
        try {
            return inventory.isItemValidForSlot(slot, pattern);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean applyName(Object metaTileEntity, String name) {
        if (name.isEmpty() || !(metaTileEntity instanceof ICustomNameObject)) {
            return false;
        }
        try {
            ((ICustomNameObject) metaTileEntity).setCustomName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean applyCircuit(Object metaTileEntity, int circuit) {
        return writeCircuit(metaTileEntity, GTUtility.getIntegratedCircuit(circuit));
    }

    private static boolean clearCircuit(Object metaTileEntity) {
        return writeCircuit(metaTileEntity, null);
    }

    private static boolean writeCircuit(Object metaTileEntity, ItemStack ghostCircuit) {
        if (!(metaTileEntity instanceof IConfigurationCircuitSupport) || !(metaTileEntity instanceof IInventory)) {
            return false;
        }
        try {
            IConfigurationCircuitSupport support = (IConfigurationCircuitSupport) metaTileEntity;
            int slot = support.getCircuitSlot();
            IInventory inventory = (IInventory) metaTileEntity;
            if (slot < 0 || slot >= inventory.getSizeInventory()) {
                return false;
            }
            inventory.setInventorySlotContents(slot, ghostCircuit);
            inventory.markDirty();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 名称：{@code <池名>_<电路>[_NC_物品...]}，与参考实现的搜索关键词同源。 */
    static String buildName(HatchBindingRegistry.Binding binding) {
        if (binding == null) {
            return "";
        }

        String base = binding.mapId;
        try {
            String translated = StatCollector.translateToLocal(binding.mapId);
            if (translated != null && !translated.isEmpty()) {
                base = translated;
            }
        } catch (Throwable ignored) {}

        String circuitToken = binding.hasCircuit()
            ? I18nUtil.tr("ae2patterngen.delivery.name.circuit", joinCircuits(binding.circuitDamages))
            : I18nUtil.tr("ae2patterngen.delivery.name.no_circuit");

        List<String> ncNames = new ArrayList<String>();
        for (ItemStack stack : binding.ncItems) {
            if (stack == null || stack.getItem() == null) {
                continue;
            }
            ncNames.add(com.github.ae2patterngen.util.ItemStackUtil.getSafeDisplayName(stack));
        }

        return HatchNameFormatter.format(base, circuitToken, ncNames);
    }

    private static String joinCircuits(int[] damages) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < damages.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(damages[i]);
        }
        return builder.toString();
    }
}
