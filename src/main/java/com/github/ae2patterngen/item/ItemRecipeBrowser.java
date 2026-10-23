package com.github.ae2patterngen.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.github.ae2patterngen.delivery.PatternDeliveryService;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketBrowseRequest;
import com.github.ae2patterngen.util.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 配方浏览器物品 — 右键打开"配方池 -> 规则组 -> 配方明细"三层界面。
 * <p>
 * 界面完全由客户端打开，数据由服务端按配方缓存派发；服务端不承载任何 GUI 类。
 */
public class ItemRecipeBrowser extends Item {

    @SideOnly(Side.CLIENT)
    private IIcon browserIcon;

    public ItemRecipeBrowser() {
        super();
        setUnlocalizedName("ae2patterngen.recipe_browser");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        browserIcon = register.registerIcon("appliedenergistics2:ItemMaterial.BlankPattern");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return browserIcon;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            // 蹲下右键空气 = 打开浏览器仓储页；普通右键空气 = 打开配方池浏览页。
            NetworkHandler.INSTANCE
                .sendToServer(player.isSneaking() ? PacketBrowseRequest.storageList(0) : PacketBrowseRequest.open());
        }
        return stack;
    }

    /**
     * 右键"样板接收方"（GT 样板输入总成/总线、AE2 面板接口）时投递浏览器仓储里的样板。
     * <p>
     * 判定在两侧都做：客户端一旦确认目标是接收方，就自己发投递请求并吞掉这次右键——
     * 否则会同时弹出浏览器界面（onItemRightClick 仍会执行）。服务端同样吃掉这次交互，
     * 真正的写入只由客户端送来的投递请求驱动，避免两边各跑一遍。
     */
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (player == null) {
            return false;
        }

        Object target = PatternDeliveryService.resolveTarget(world, x, y, z);
        if (target == null) {
            return false;
        }

        if (world.isRemote) {
            NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.deliver(x, y, z));
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            list.add(EnumChatFormatting.YELLOW + I18nUtil.tr("ae2patterngen.tooltip.browser.title"));
            list.add(EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.tooltip.browser.line1"));
            list.add(EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.tooltip.browser.line2"));
            list.add(EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.tooltip.browser.line3"));
            list.add(EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.tooltip.browser.line4"));
        } else {
            list.add(EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.tooltip.hint.hold_shift"));
        }
    }
}
