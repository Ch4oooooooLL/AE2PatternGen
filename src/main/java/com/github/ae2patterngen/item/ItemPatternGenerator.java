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
            cpw.mods.fml.common.network.internal.FMLNetworkHandler.openGui(
                player,
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

    // ======== NBT 持久化工具方法 ========

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

    public static void saveAllFields(ItemStack stack, String recipeMap, String outputOre, String inputOre,
        String ncItem, String blacklistInput, String blacklistOutput, String replacements) {
        saveField(stack, NBT_RECIPE_MAP, recipeMap);
        saveField(stack, NBT_OUTPUT_ORE, outputOre);
        saveField(stack, NBT_INPUT_ORE, inputOre);
        saveField(stack, NBT_NC_ITEM, ncItem);
        saveField(stack, NBT_BLACKLIST_INPUT, blacklistInput);
        saveField(stack, NBT_BLACKLIST_OUTPUT, blacklistOutput);
        saveField(stack, NBT_REPLACEMENTS, replacements);
    }
}
