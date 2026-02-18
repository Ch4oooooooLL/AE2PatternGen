package com.github.ae2patterngen.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 样本生成器物品 — 右键打开配置 GUI
 * <p>
 * 材质: 使用 AE2 的空白样板材质
 * NBT: 保存 GUI 输入的配置信息 (recipeMap, outputOre, inputOre, ncItem)
 */
public class ItemPatternGenerator extends Item {

    public static final int GUI_ID = 101;

    // NBT 键名
    public static final String NBT_RECIPE_MAP = "recipeMap";
    public static final String NBT_OUTPUT_ORE = "outputOre";
    public static final String NBT_INPUT_ORE = "inputOre";
    public static final String NBT_NC_ITEM = "ncItem";
    public static final String NBT_BLACKLIST_INPUT = "blacklistInput";
    public static final String NBT_BLACKLIST_OUTPUT = "blacklistOutput";

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
        // 使用 AE2 的已编码样板材质 (作为生成器图标)
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
            cpw.mods.fml.common.network.internal.FMLNetworkHandler.openGui(
                player,
                com.github.ae2patterngen.AE2PatternGen.instance,
                GUI_ID,
                world,
                (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
        }
        return stack;
    }

    // ======== NBT 持久化工具方法 ========

    /**
     * 获取物品上保存的配置信息
     */
    public static String getSavedField(ItemStack stack, String key) {
        if (stack == null || !stack.hasTagCompound()) return "";
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey(key) ? tag.getString(key) : "";
    }

    /**
     * 保存配置信息到物品 NBT
     */
    public static void saveField(ItemStack stack, String key, String value) {
        if (stack == null) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setString(key, value != null ? value : "");
    }

    /**
     * 批量保存所有配置字段
     */
    public static void saveAllFields(ItemStack stack, String recipeMap, String outputOre, String inputOre,
        String ncItem, String blacklistInput, String blacklistOutput) {
        saveField(stack, NBT_RECIPE_MAP, recipeMap);
        saveField(stack, NBT_OUTPUT_ORE, outputOre);
        saveField(stack, NBT_INPUT_ORE, inputOre);
        saveField(stack, NBT_NC_ITEM, ncItem);
        saveField(stack, NBT_BLACKLIST_INPUT, blacklistInput);
        saveField(stack, NBT_BLACKLIST_OUTPUT, blacklistOutput);
    }
}
