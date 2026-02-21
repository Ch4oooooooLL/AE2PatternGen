package com.github.ae2patterngen.item;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.github.ae2patterngen.storage.PatternStorage;

import appeng.api.features.INetworkEncodable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 样板生成器物品 — 三种交互模式:
 * <ul>
 * <li>正常右键 (空气) → 打开配置 GUI</li>
 * <li>蹲下右键 (空气) → 打开仓储 GUI</li>
 * <li>蹲下右键 (方块) → 导出样板到方块容器</li>
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
     * 蹲下右键方块 → 导出样板到 IInventory
     */
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) return false;
        if (!player.isSneaking()) return false;

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IInventory)) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 该方块不支持存储"));
            return true;
        }

        UUID uuid = player.getUniqueID();
        if (PatternStorage.isEmpty(uuid)) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 仓储为空，无可导出的样板"));
            return true;
        }

        IInventory inv = (IInventory) te;
        List<ItemStack> patterns = PatternStorage.load(uuid);
        int transferred = 0;

        java.util.Iterator<ItemStack> it = patterns.iterator();
        while (it.hasNext()) {
            ItemStack pattern = it.next();
            boolean inserted = false;

            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                if (inv.getStackInSlot(slot) == null && inv.isItemValidForSlot(slot, pattern)) {
                    inv.setInventorySlotContents(slot, pattern);
                    it.remove();
                    transferred++;
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                // 容器已满
                break;
            }
        }

        // 更新存储
        if (patterns.isEmpty()) {
            PatternStorage.clear(uuid);
        } else {
            PatternStorage.StorageSummary summary = PatternStorage.getSummary(uuid);
            PatternStorage.save(uuid, patterns, summary.source);
        }

        inv.markDirty();

        String msg = EnumChatFormatting.GREEN + "[AE2PatternGen] 已导出 " + transferred + " 个样板到容器";
        if (!patterns.isEmpty()) {
            msg += EnumChatFormatting.GRAY + " (剩余 " + patterns.size() + " 个)";
        }
        player.addChatMessage(new ChatComponentText(msg));

        return true; // 消费事件，不打开方块自身 GUI
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
                    + "Shift+右键 (容器)"
                    + EnumChatFormatting.GRAY
                    + ": 将虚拟样板批量注入目标物理容器");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "网络绑定"
                    + EnumChatFormatting.GRAY
                    + ": 通过安全终端与ME网络进行绑定");
        } else {
            list.add(EnumChatFormatting.GRAY + "右键打开生成器 GUI");
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
