package com.github.ae2patterngen.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * OreDictionary helper methods with defensive bounds checks.
 */
public final class OreDictUtil {

    private OreDictUtil() {}

    public static String[] getOreNamesSafe(ItemStack stack) {
        if (stack == null) return new String[0];
        int[] oreIds = OreDictionary.getOreIDs(stack);
        return getOreNamesSafe(oreIds, OreDictionary.getOreNames());
    }

    static String[] getOreNamesSafe(int[] oreIds, String[] oreNamesById) {
        if (oreIds == null || oreIds.length == 0 || oreNamesById == null || oreNamesById.length == 0) {
            return new String[0];
        }

        List<String> names = new ArrayList<>(oreIds.length);
        for (int oreId : oreIds) {
            if (oreId < 0 || oreId >= oreNamesById.length) {
                continue;
            }

            String oreName = oreNamesById[oreId];
            if (oreName != null && !oreName.isEmpty()) {
                names.add(oreName);
            }
        }

        return names.toArray(new String[0]);
    }
}
