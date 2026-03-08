package com.github.ae2patterngen.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Defensive ItemStack helpers for recipe data that may contain wildcard metadata.
 */
public final class ItemStackUtil {

    private ItemStackUtil() {}

    public static String getSafeDisplayName(ItemStack stack) {
        if (stack == null) return "";

        Item item = stack.getItem();
        if (item == null) return "";

        try {
            String displayName = stack.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        } catch (RuntimeException ignored) {
            // Some wildcard-meta recipe stacks throw when resolving translated display names.
        }

        Object registryName = Item.itemRegistry.getNameForObject(item);
        if (registryName != null) {
            return registryName.toString() + ":" + stack.getItemDamage();
        }

        int itemId = Item.getIdFromItem(item);
        return itemId >= 0 ? "[" + itemId + ":" + stack.getItemDamage() + "]" : "";
    }
}
