package com.github.ae2patterngen.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import org.junit.Before;
import org.junit.Test;

/**
 * 预览聚合逻辑测试：去重、消耗型输入过滤、采样上限与计数。
 */
public class RecipePreviewSupportTest {

    private static final String MAP_ID = "gt.recipe.metalbender";

    private Item plateItem;
    private Item plateCopyItem;
    private Item ingotItem;
    private Item moldItem;

    private List<RecipeEntry> recipes;

    @Before
    public void setUp() {
        plateItem = new TestItem("Plate");
        plateCopyItem = new TestItem("Plate Copy");
        ingotItem = new TestItem("Ingot");
        moldItem = new TestItem("Mold");
        recipes = new ArrayList<RecipeEntry>();
    }

    @Test
    public void distinctInputsAndOutputsAreCountedOnceAcrossRecipes() {
        recipes.add(recipe(new ItemStack(ingotItem, 2), new ItemStack(plateItem, 1)));
        recipes.add(recipe(new ItemStack(ingotItem, 2), new ItemStack(plateCopyItem, 1)));

        RecipePreviewSupport.Preview preview = RecipePreviewSupport.build(2, recipes);

        assertEquals(2, preview.totalCount);
        assertEquals(2, preview.matchedCount);
        assertEquals(1, preview.distinctInputCount);
        assertEquals(2, preview.distinctOutputCount);
        assertEquals(1, preview.inputSamples.length);
        assertEquals(2, preview.outputSamples.length);
    }

    @Test
    public void nonConsumedInputsAreExcludedFromInputSamples() {
        ItemStack consumed = new ItemStack(ingotItem, 1);
        ItemStack nonConsumed = new ItemStack(moldItem, 1);
        nonConsumed.stackSize = 0;

        recipes.add(recipe(new ItemStack[] { consumed, nonConsumed }, new ItemStack(plateItem, 1)));

        RecipePreviewSupport.Preview preview = RecipePreviewSupport.build(1, recipes);

        assertEquals(1, preview.distinctInputCount);
        assertEquals(ingotItem, preview.inputSamples[0].getItem());
    }

    @Test
    public void nbtMakesStacksDistinct() {
        recipes.add(recipe(nbtStack(ingotItem, 1), new ItemStack(plateItem, 1)));
        recipes.add(recipe(nbtStack(ingotItem, 2), new ItemStack(plateItem, 1)));

        RecipePreviewSupport.Preview preview = RecipePreviewSupport.build(2, recipes);

        assertEquals(2, preview.distinctInputCount);
        assertEquals(1, preview.distinctOutputCount);
    }

    @Test
    public void samplesAreCappedButCountsStayAccurate() {
        List<RecipeEntry> many = new ArrayList<RecipeEntry>();
        for (int i = 0; i < RecipePreviewSupport.MAX_SAMPLE_STACKS + 5; i++) {
            ItemStack input = new ItemStack(ingotItem, 1, i);
            many.add(recipe(input, new ItemStack(plateItem, 1)));
        }

        RecipePreviewSupport.Preview preview = RecipePreviewSupport.build(many.size(), many);

        assertEquals(RecipePreviewSupport.MAX_SAMPLE_STACKS + 5, preview.distinctInputCount);
        assertEquals(RecipePreviewSupport.MAX_SAMPLE_STACKS, preview.inputSamples.length);
    }

    @Test
    public void samplesNeverCarryZeroStackSize() {
        ItemStack output = new ItemStack(plateItem, 1);
        output.stackSize = 0;
        recipes.add(recipe(new ItemStack(ingotItem, 1), output));

        RecipePreviewSupport.Preview preview = RecipePreviewSupport.build(1, recipes);

        assertEquals(1, preview.outputSamples.length);
        assertTrue(preview.outputSamples[0].stackSize > 0);
    }

    @Test
    public void emptyMatchKeepsGroupTotalVisible() {
        RecipePreviewSupport.Preview preview = RecipePreviewSupport.build(7, new ArrayList<RecipeEntry>());

        assertEquals(7, preview.totalCount);
        assertEquals(0, preview.matchedCount);
        assertEquals(0, preview.distinctInputCount);
        assertEquals(0, preview.distinctOutputCount);
        assertEquals(0, preview.inputSamples.length);
        assertEquals(0, preview.outputSamples.length);
    }

    private static RecipeEntry recipe(ItemStack input, ItemStack output) {
        return recipe(new ItemStack[] { input }, output);
    }

    private static RecipeEntry recipe(ItemStack[] inputs, ItemStack output) {
        return new RecipeEntry(
            "gt",
            MAP_ID,
            MAP_ID,
            inputs,
            new ItemStack[] { output },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            100,
            30);
    }

    private static ItemStack nbtStack(Item item, int marker) {
        ItemStack stack = new ItemStack(item, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("marker", marker);
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class TestItem extends Item {

        private final String displayName;

        private TestItem(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return displayName;
        }
    }
}
