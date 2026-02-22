package com.github.ae2patterngen.item;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.github.ae2patterngen.storage.PatternStorage;

import appeng.api.features.INetworkEncodable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.recipe.RecipeMap;

/**
 * 样板生成器物品 — 三种交互模式:
 * <ul>
 * <li>正常右键 (空气) → 打开配置 GUI</li>
 * <li>蹲下右键 (空气) → 打开仓储 GUI</li>
 * <li>蹲下右键 (方块) → 检测方块属性，并尝试导出样板到容器</li>
 * </ul>
 */
public class ItemPatternGenerator extends Item implements INetworkEncodable, IWirelessTermHandler {

    public static final int GUI_ID = 101;
    public static final int GUI_ID_STORAGE = 102;

    // NBT 键名
    public static final String NBT_RECIPE_MAP = "recipeMap";
    public static final String NBT_OUTPUT_ORE = "outputOre";
    public static final String NBT_INPUT_ORE = "inputOre";
    public static final String NBT_NC_ITEM = "ncItem";
    public static final String NBT_BLACKLIST_INPUT = "blacklistInput";
    public static final String NBT_BLACKLIST_OUTPUT = "blacklistOutput";
    public static final String NBT_REPLACEMENTS = "replacements";
    public static final String NBT_TARGET_TIER = "targetTier";

    @SideOnly(Side.CLIENT)
    private IIcon blankPatternIcon;

    public ItemPatternGenerator() {
        super();
        setUnlocalizedName("ae2patterngen.pattern_generator");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        blankPatternIcon = register.registerIcon("appliedenergistics2:ItemEncodedPattern");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return blankPatternIcon;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            // Sneak + right click on block should be handled by onItemUseFirst (detect/export), not storage GUI.
            MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, false);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return stack;
            }
        }

        if (!world.isRemote) {
            int guiId = player.isSneaking() ? GUI_ID_STORAGE : GUI_ID;
            cpw.mods.fml.common.FMLLog.info(
                "[AE2PatternGen] SERVER SIDE: Requesting to open GUI %d for player %s with instance %s",
                guiId,
                player.getCommandSenderName(),
                com.github.ae2patterngen.AE2PatternGen.instance);
            player.openGui(
                com.github.ae2patterngen.AE2PatternGen.instance,
                guiId,
                world,
                (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
        }
        return stack;
    }

    /**
     * 蹲下右键方块 → 探测机器配方或导出样板到容器
     */
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return false;
        if (world.isRemote) return false;

        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 该方块不支持探测属性"));
            return true;
        }

        // 1. 尝试探测 GT 机器配方表 (优先级: 主方块/控制器)
        if (te instanceof IGregTechTileEntity) {
            IGregTechTileEntity gte = (IGregTechTileEntity) te;
            IMetaTileEntity mte = gte.getMetaTileEntity();
            RecipeMap<?> recipeMap = resolveRecipeMap(mte);
            if (recipeMap != null) {
                saveField(stack, NBT_RECIPE_MAP, recipeMap.unlocalizedName);
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.GREEN + "[AE2PatternGen] 已探测并记录配方表: "
                            + EnumChatFormatting.WHITE
                            + recipeMap.unlocalizedName));
                return true;
            }
        }

        // 2. 如果不是 GT 主方块或探测失败，尝试作为普通容器导出样板
        if (!(te instanceof IInventory)) {
            // 如果既不是可读取的 GT 机器也不是容器，提示错误
            if (te instanceof IGregTechTileEntity) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 该机器部件不支持探测配方或导出样板"));
            } else {
                player
                    .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 该方块不支持提取数据或样板导出"));
            }
            return true;
        }

        // 执行原有导出逻辑
        UUID uuid = player.getUniqueID();
        if (PatternStorage.isEmpty(uuid)) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 仓储为空，无可导出的样板"));
            return true;
        }

        IInventory inv = (IInventory) te;
        PatternStorage.StorageSummary storageSummary = PatternStorage.getSummary(uuid);
        List<ItemStack> patterns = PatternStorage.load(uuid);
        List<ItemStack> remainingPatterns = new ArrayList<>(patterns.size());
        int transferred = 0;

        for (int i = 0; i < patterns.size(); i++) {
            ItemStack pattern = patterns.get(i);
            if (tryInsertPattern(inv, pattern, side)) {
                transferred++;
            } else {
                remainingPatterns.add(pattern);
                // No slot can accept this pattern now; keep all remaining entries untouched.
                for (int j = i + 1; j < patterns.size(); j++) {
                    remainingPatterns.add(patterns.get(j));
                }
                break;
            }
        }

        // 更新存储
        if (remainingPatterns.isEmpty()) {
            PatternStorage.clear(uuid);
        } else {
            if (!PatternStorage.save(uuid, remainingPatterns, storageSummary.source)) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 仓储更新失败，请稍后重试"));
                return true;
            }
        }

        inv.markDirty();

        String msg = EnumChatFormatting.GREEN + "[AE2PatternGen] 已导出 " + transferred + " 个样板到容器";
        if (!remainingPatterns.isEmpty()) {
            msg += EnumChatFormatting.GRAY + " (剩余 " + remainingPatterns.size() + " 个)";
        }
        player.addChatMessage(new ChatComponentText(msg));

        return true; // 消费事件
    }

    private static RecipeMap<?> resolveRecipeMap(IMetaTileEntity mte) {
        if (mte == null) return null;

        if (mte instanceof RecipeMapWorkable) {
            return ((RecipeMapWorkable) mte).getRecipeMap();
        }

        // Some GT multi-block controllers expose getRecipeMap() but do not implement RecipeMapWorkable.
        try {
            Method method = mte.getClass()
                .getMethod("getRecipeMap");
            Object result = method.invoke(mte);
            if (result instanceof RecipeMap<?>) {
                return (RecipeMap<?>) result;
            }
        } catch (Throwable ignored) {
            // Fall through to null when the machine does not expose a recipe map.
        }

        return null;
    }

    private static boolean tryInsertPattern(IInventory inv, ItemStack pattern, int side) {
        if (inv == null || pattern == null) return false;

        int[] candidateSlots = resolveInsertSlots(inv, side);
        for (int slot : candidateSlots) {
            if (slot < 0 || slot >= inv.getSizeInventory()) {
                continue;
            }

            ItemStack existing;
            try {
                existing = inv.getStackInSlot(slot);
            } catch (Throwable ignored) {
                continue;
            }
            if (existing != null) {
                continue;
            }

            if (!canInsertIntoSlot(inv, slot, pattern, side)) {
                continue;
            }

            ItemStack inserted = pattern.copy();
            if (inserted.stackSize <= 0) {
                inserted.stackSize = 1;
            }
            int limit = Math.max(1, Math.min(inv.getInventoryStackLimit(), inserted.getMaxStackSize()));
            inserted.stackSize = Math.min(inserted.stackSize, limit);

            try {
                inv.setInventorySlotContents(slot, inserted);
            } catch (Throwable ignored) {
                continue;
            }

            ItemStack after;
            try {
                after = inv.getStackInSlot(slot);
            } catch (Throwable ignored) {
                after = null;
            }
            if (isInsertedAsExpected(after, inserted)) {
                return true;
            }
        }

        return false;
    }

    private static boolean canInsertIntoSlot(IInventory inv, int slot, ItemStack pattern, int side) {
        boolean validForSlot;
        try {
            validForSlot = inv.isItemValidForSlot(slot, pattern);
        } catch (Throwable ignored) {
            validForSlot = false;
        }
        if (!validForSlot) {
            return false;
        }

        if (inv instanceof ISidedInventory) {
            try {
                return ((ISidedInventory) inv).canInsertItem(slot, pattern, normalizeSide(side));
            } catch (Throwable ignored) {
                return false;
            }
        }
        return true;
    }

    private static int[] resolveInsertSlots(IInventory inv, int side) {
        if (inv instanceof ISidedInventory) {
            try {
                int[] sidedSlots = ((ISidedInventory) inv).getAccessibleSlotsFromSide(normalizeSide(side));
                if (sidedSlots != null && sidedSlots.length > 0) {
                    return sidedSlots;
                }
            } catch (Throwable ignored) {}
        }

        int size = Math.max(0, inv.getSizeInventory());
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }

    private static int normalizeSide(int side) {
        return side >= 0 && side <= 5 ? side : 0;
    }

    private static boolean isInsertedAsExpected(ItemStack actual, ItemStack expected) {
        if (actual == null || expected == null) {
            return false;
        }
        if (actual.getItem() != expected.getItem()) {
            return false;
        }
        if (actual.getItemDamage() != expected.getItemDamage()) {
            return false;
        }
        if (!ItemStack.areItemStackTagsEqual(actual, expected)) {
            return false;
        }
        return actual.stackSize >= expected.stackSize;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            list.add(EnumChatFormatting.YELLOW + "[功能特性]");
            list.add(
                EnumChatFormatting.GRAY + "(1) "
                    + EnumChatFormatting.WHITE
                    + "批量编码"
                    + EnumChatFormatting.GRAY
                    + ": 将 GregTech 机器配方批量导出为 AE2 处理模式样板");
            list.add(
                EnumChatFormatting.GRAY + "(2) "
                    + EnumChatFormatting.WHITE
                    + "智能过滤"
                    + EnumChatFormatting.GRAY
                    + ": 支持正则匹配、材料替换与电压等级匹配限制");
            list.add(
                EnumChatFormatting.GRAY + "(3) "
                    + EnumChatFormatting.WHITE
                    + "配方冲突"
                    + EnumChatFormatting.GRAY
                    + ": 遇到多个匹配配方时支持通过手动 GUI 进行挑选");
            list.add(
                EnumChatFormatting.GRAY + "(4) "
                    + EnumChatFormatting.WHITE
                    + "虚拟仓储"
                    + EnumChatFormatting.GRAY
                    + ": 样板生成后存于内部虚拟硬盘，不占用玩家背包");
            list.add(
                EnumChatFormatting.GRAY + "(5) "
                    + EnumChatFormatting.WHITE
                    + "等价消耗"
                    + EnumChatFormatting.GRAY
                    + ": 自动从绑定的 ME 网络或背包中扣除空白样板");
            list.add("");
            list.add(EnumChatFormatting.YELLOW + "[操作方式]");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "右键 (空气)"
                    + EnumChatFormatting.GRAY
                    + ": 打开 生成配置界面");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "Shift+右键 (空气)"
                    + EnumChatFormatting.GRAY
                    + ": 打开 虚拟样板管理器");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "Shift+右键 (方块)"
                    + EnumChatFormatting.GRAY
                    + ": 检测方块属性；若为容器则批量导出虚拟样板");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "网络绑定"
                    + EnumChatFormatting.GRAY
                    + ": 通过安全终端与ME网络进行绑定");
        } else {
            list.add(EnumChatFormatting.GRAY + "右键打开生成器 GUI");
            list.add(EnumChatFormatting.GRAY + "Shift+右键空气打开仓储界面");
            list.add(EnumChatFormatting.GRAY + "Shift+右键方块检测属性/导出样板");
            list.add(
                EnumChatFormatting.GRAY + "按住 "
                    + EnumChatFormatting.AQUA
                    + "Shift"
                    + EnumChatFormatting.GRAY
                    + " 查看详细功能");
        }
    }

    public static String getSavedField(ItemStack stack, String key) {
        if (stack == null || !stack.hasTagCompound()) return "";
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey(key) ? tag.getString(key) : "";
    }

    public static void saveField(ItemStack stack, String key, String value) {
        if (stack == null) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setString(key, value != null ? value : "");
    }

    public static int getSavedInt(ItemStack stack, String key, int def) {
        if (stack == null || !stack.hasTagCompound()) return def;
        return stack.getTagCompound()
            .hasKey(key)
                ? stack.getTagCompound()
                    .getInteger(key)
                : def;
    }

    public static void saveInt(ItemStack stack, String key, int value) {
        if (stack == null) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setInteger(key, value);
    }

    public static void saveAllFields(ItemStack stack, String recipeMap, String outputOre, String inputOre,
        String ncItem, String blacklistInput, String blacklistOutput, String replacements, int targetTier) {
        saveField(stack, NBT_RECIPE_MAP, recipeMap);
        saveField(stack, NBT_OUTPUT_ORE, outputOre);
        saveField(stack, NBT_INPUT_ORE, inputOre);
        saveField(stack, NBT_NC_ITEM, ncItem);
        saveField(stack, NBT_BLACKLIST_INPUT, blacklistInput);
        saveField(stack, NBT_BLACKLIST_OUTPUT, blacklistOutput);
        saveField(stack, NBT_REPLACEMENTS, replacements);
        saveInt(stack, NBT_TARGET_TIER, targetTier);
    }

    // ---- INetworkEncodable ----

    @Override
    public String getEncryptionKey(ItemStack item) {
        if (item != null && item.hasTagCompound()) {
            return item.getTagCompound()
                .getString("encryptionKey");
        }
        return "";
    }

    @Override
    public void setEncryptionKey(ItemStack item, String encKey, String name) {
        if (item != null) {
            if (!item.hasTagCompound()) {
                item.setTagCompound(new NBTTagCompound());
            }
            item.getTagCompound()
                .setString("encryptionKey", encKey);
        }
    }

    // ---- IWirelessTermHandler ----

    @Override
    public boolean canHandle(ItemStack is) {
        return is != null && is.getItem() == this;
    }

    @Override
    public boolean usePower(EntityPlayer player, double amount, ItemStack is) {
        // 生成器目前不消耗 AE2 能量
        return true;
    }

    @Override
    public boolean hasPower(EntityPlayer player, double amount, ItemStack is) {
        return true;
    }

    @Override
    public IConfigManager getConfigManager(ItemStack is) {
        return null;
    }
}
