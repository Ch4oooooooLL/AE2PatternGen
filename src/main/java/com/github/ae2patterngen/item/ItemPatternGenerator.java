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
public class ItemPatternGenerator extends Item {

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
            list.add(EnumChatFormatting.YELLOW + "[\u529F\u80FD]"); // [功能]
            list.add(
                EnumChatFormatting.GRAY + "1. "
                    + EnumChatFormatting.WHITE
                    + "\u6279\u91CF\u751F\u6210"
                    + EnumChatFormatting.GRAY
                    + ": \u8F93\u5165 ID \u5339\u914D\u5E76\u6279\u91CF\u751F\u6210\u914D\u65B9\u6837\u677F");
            list.add(
                EnumChatFormatting.GRAY + "2. "
                    + EnumChatFormatting.WHITE
                    + "\u667A\u80FD\u8FC7\u6EE4"
                    + EnumChatFormatting.GRAY
                    + ": \u652F\u6301\u6B63\u5219\u7B5B\u9009\u4E0E\u9ED1\u540D\u5355");
            list.add(
                EnumChatFormatting.GRAY + "3. "
                    + EnumChatFormatting.WHITE
                    + "\u865A\u62DF\u4ED3\u50A8"
                    + EnumChatFormatting.GRAY
                    + ": \u7ED3\u679C\u5B58\u5165\u5185\u7F6E\u786C\u76D8\uFF0C\u4E0D\u5360\u80CC\u5305");
            list.add(
                EnumChatFormatting.GRAY + "4. "
                    + EnumChatFormatting.WHITE
                    + "\u81EA\u52A8\u66FF\u6362"
                    + EnumChatFormatting.GRAY
                    + ": \u652F\u6301\u914D\u7F6E\u77FF\u8F9E\u66FF\u6362\u89C4\u5219");
            list.add(
                EnumChatFormatting.GRAY + "5. "
                    + EnumChatFormatting.WHITE
                    + "\u7B49\u4EF7\u6D88\u8017"
                    + EnumChatFormatting.GRAY
                    + ": \u751F\u6210\u65F6\u9700\u6D88\u8017\u80CC\u5305\u5185\u7684\u7A7A\u767D\u6837\u677F");
            list.add("");
            list.add(EnumChatFormatting.YELLOW + "[\u64CD\u4F5C]"); // [操作]
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "\u53F3\u952E (\u5BF9\u7740\u7A7A\u6C14)"
                    + EnumChatFormatting.GRAY
                    + ": \u6253\u5F00\u751F\u6210\u914D\u7F6E\u754C\u9762");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "Shift+\u53F3\u952E (\u5BF9\u7740\u7A7A\u6C14)"
                    + EnumChatFormatting.GRAY
                    + ": \u6253\u5F00\u6837\u677F\u4ED3\u50A8\u754C\u9762");
            list.add(
                EnumChatFormatting.GRAY + "- "
                    + EnumChatFormatting.WHITE
                    + "Shift+\u53F3\u952E (\u5BF9\u7740\u5BB9\u5668)"
                    + EnumChatFormatting.GRAY
                    + ": \u5BFC\u51FA\u6837\u677F\u5230\u8BE5\u5BB9\u5668");
        } else {
            list.add(EnumChatFormatting.GRAY + "\u53F3\u952E\u6253\u5F00\u751F\u6210\u5668 GUI");
            list.add(
                EnumChatFormatting.GRAY + "\u6309\u4F4F "
                    + EnumChatFormatting.AQUA
                    + "Shift"
                    + EnumChatFormatting.GRAY
                    + " \u67E5\u770B\u8BE6\u7EC6\u529F\u80FD");
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
}
