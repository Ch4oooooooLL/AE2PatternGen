package com.github.ae2patterngen.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
 * 规则分类器行为测试：分类维度、等价判定、排序与默认折叠策略。
 * <p>
 * 测试环境不加载完整游戏，因此使用自定义物品实例：分类器对未注册物品回退到实例标识，
 * 从而保证不同物品不会被错误合并。
 */
public class RuleClassifierTest {

    private static final String MAP_ID = "gt.recipe.metalbender";

    private Item circuitItem;
    private Item appleItem;
    private Item stickItem;
    private Item diamondItem;
    private Item outputItem;

    private List<RecipeEntry> recipes;

    @Before
    public void setUp() {
        initializeMinecraftBootstrap();
        circuitItem = new TestItem("Test Circuit");
        appleItem = new TestItem("Test Apple");
        stickItem = new TestItem("Test Stick");
        diamondItem = new TestItem("Test Diamond");
        outputItem = new TestItem("Test Output");
        recipes = new ArrayList<RecipeEntry>();
    }

    @Test
    public void differentCircuitNumbersGoToDifferentGroups() {
        recipes.add(entry(circuit(1), nc(appleItem)));
        recipes.add(entry(circuit(2), nc(appleItem)));

        PoolRuleIndex index = classify();

        assertEquals(2, index.groups.size());
        assertEquals(1, groupOf(index, 1).circuitDamages[0]);
        assertEquals(2, groupOf(index, 2).circuitDamages[0]);
        assertNull(findGroup(index, 3));
    }

    @Test
    public void sameRuleWithDifferentNcOrderStaysInOneGroup() {
        recipes.add(entry(circuit(1), nc(appleItem), nc(stickItem)));
        recipes.add(entry(circuit(1), nc(stickItem), nc(appleItem)));

        PoolRuleIndex index = classify();

        assertEquals(1, index.groups.size());
        RuleGroup group = index.groups.get(0);
        assertEquals(2, group.size());
        assertEquals(2, group.ncItems.length);
        assertTrue(
            "NC 展示物品应按签名排序",
            RuleClassifier.canonicalNcKey(group.ncItems[0])
                .compareTo(RuleClassifier.canonicalNcKey(group.ncItems[1])) < 0);
    }

    @Test
    public void ncStackSizeDoesNotAffectEquality() {
        ItemStack zero = nc(appleItem);
        ItemStack alsoZero = new ItemStack(appleItem, 1, 0);
        alsoZero.stackSize = 0;

        recipes.add(entry(circuit(1), zero));
        recipes.add(entry(circuit(1), alsoZero));

        assertEquals(1, classify().groups.size());
    }

    @Test
    public void differentNcItemSetSplitsGroups() {
        recipes.add(entry(circuit(1), nc(appleItem)));
        recipes.add(entry(circuit(1), nc(stickItem)));

        PoolRuleIndex index = classify();

        assertEquals(2, index.groups.size());
        for (RuleGroup group : index.groups) {
            assertEquals(1, group.ncItems.length);
        }
    }

    @Test
    public void ordinaryInputsAreNeverTreatedAsNcItems() {
        recipes.add(entry(circuit(1), normal(appleItem), normal(stickItem)));

        RuleGroup group = classify().groups.get(0);

        assertEquals(0, group.ncItems.length);
        assertEquals(1, group.circuitDamages.length);
        assertEquals(1, group.circuitDamages[0]);
    }

    @Test
    public void specialItemsCountAsNcItems() {
        recipes.add(
            entryWithSpecials(
                new ItemStack[] { circuit(3), normal(appleItem) },
                new ItemStack[] { ncStack(diamondItem, 7) }));

        RuleGroup group = classify().groups.get(0);

        assertEquals(1, group.ncItems.length);
        assertEquals(diamondItem, group.ncItems[0].getItem());
        assertEquals(7, group.ncItems[0].getItemDamage());
    }

    @Test
    public void nbtDifferenceSplitsNcGroups() {
        recipes.add(entry(circuit(1), nbtStack(diamondItem, 1)));
        recipes.add(entry(circuit(1), nbtStack(diamondItem, 2)));

        assertEquals(2, classify().groups.size());
    }

    @Test
    public void zeroMetaCircuitMeansNoCircuitRequirement() {
        recipes.add(entry(circuit(0)));

        RuleGroup group = classify().groups.get(0);

        assertEquals(0, group.circuitDamages.length);
        assertEquals(0, group.ncItems.length);
        assertTrue(group.unruled);
    }

    @Test
    public void multipleCircuitsInOneRecipeKeepEveryNumber() {
        recipes.add(entry(circuit(5), circuit(2)));

        RuleGroup group = classify().groups.get(0);

        assertEquals(2, group.circuitDamages.length);
        assertEquals(2, group.circuitDamages[0]);
        assertEquals(5, group.circuitDamages[1]);
    }

    @Test
    public void groupsAreSortedByRecipeCountDescending() {
        recipes.add(entry(circuit(9), nc(appleItem)));
        for (int i = 0; i < 3; i++) {
            recipes.add(entry(circuit(1), nc(appleItem)));
        }

        PoolRuleIndex index = classify();

        assertEquals(4, index.totalRecipeCount);
        assertEquals(2, index.groups.size());
        assertEquals(
            3,
            index.groups.get(0)
                .size());
        assertEquals(
            1,
            index.groups.get(1)
                .size());
        assertEquals(1, index.groups.get(0).circuitDamages[0]);
    }

    @Test
    public void defaultViewHidesSingletonRulesAndUnruledRecipes() {
        recipes.add(entry(circuit(1), nc(appleItem)));
        recipes.add(entry(circuit(1), nc(appleItem)));
        recipes.add(entry(circuit(7), nc(appleItem)));
        recipes.add(entry(circuit(0)));
        recipes.add(entry(circuit(0)));

        PoolRuleIndex index = classify();

        assertEquals(3, index.groups.size());
        assertEquals(1, index.getSingletonRuleCount());
        assertEquals(2, index.getUnruledRecipeCount());

        List<RuleGroup> defaultView = index.visibleGroups(false, false);
        assertEquals(1, defaultView.size());
        assertEquals(
            2,
            defaultView.get(0)
                .size());

        assertEquals(
            3,
            index.visibleGroups(true, true)
                .size());
        assertEquals(
            2,
            index.visibleGroups(true, false)
                .size());
    }

    @Test
    public void unruledRecipesLandInTheirOwnGroupUnderCircuitGroups() {
        recipes.add(entry(normal(appleItem), normal(stickItem)));
        recipes.add(entry(normal(appleItem), normal(diamondItem)));
        recipes.add(entry(circuit(1), nc(appleItem)));

        PoolRuleIndex index = classify();
        RuleGroup unruled = null;
        for (RuleGroup group : index.groups) {
            if (group.unruled) {
                unruled = group;
            }
        }

        assertNotNull("应存在无电路无 NC 的默认组", unruled);
        assertEquals(2, unruled.size());
        assertEquals(2, index.getUnruledRecipeCount());
        assertEquals(
            2,
            index.visibleGroups(true, true)
                .size());
    }

    @Test
    public void sameRuleAlwaysProducesSameGroupId() {
        recipes.add(entry(circuit(2), nc(appleItem), nc(stickItem)));
        recipes.add(entry(circuit(2), nc(stickItem), nc(appleItem)));
        String firstId = classify().groups.get(0).groupId;

        recipes.clear();
        recipes.add(entry(circuit(2), nc(appleItem), nc(stickItem)));
        recipes.add(entry(circuit(2), nc(stickItem), nc(appleItem)));

        PoolRuleIndex rebuilt = classify();
        assertEquals(firstId, rebuilt.groups.get(0).groupId);
        assertNotNull(rebuilt.findById(firstId));
    }

    private PoolRuleIndex classify() {
        return RuleClassifier.classify(MAP_ID, recipes, stack -> stack.getItem() == circuitItem);
    }

    private static RuleGroup findGroup(PoolRuleIndex index, int circuitNumber) {
        for (RuleGroup group : index.groups) {
            for (int damage : group.circuitDamages) {
                if (damage == circuitNumber) {
                    return group;
                }
            }
        }
        return null;
    }

    private static RuleGroup groupOf(PoolRuleIndex index, int circuitNumber) {
        RuleGroup group = findGroup(index, circuitNumber);
        assertNotNull("缺少电路 " + circuitNumber + " 的分组", group);
        return group;
    }

    private RecipeEntry entry(ItemStack... inputs) {
        return entryWithSpecials(inputs, new ItemStack[0]);
    }

    private RecipeEntry entryWithSpecials(ItemStack[] inputs, ItemStack[] specials) {
        return new RecipeEntry(
            "gt",
            MAP_ID,
            MAP_ID,
            inputs,
            new ItemStack[] { new ItemStack(outputItem) },
            new FluidStack[0],
            new FluidStack[0],
            specials,
            100,
            30);
    }

    private ItemStack circuit(int number) {
        return ncStack(circuitItem, number);
    }

    private static ItemStack ncStack(Item item, int damage) {
        ItemStack stack = new ItemStack(item, 1, damage);
        stack.stackSize = 0;
        return stack;
    }

    private static ItemStack nc(Item item) {
        return ncStack(item, 0);
    }

    private static ItemStack normal(Item item) {
        return new ItemStack(item, 4, 0);
    }

    private static ItemStack nbtStack(Item item, int marker) {
        ItemStack stack = ncStack(item, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("marker", marker);
        stack.setTagCompound(tag);
        return stack;
    }

    private static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}
            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
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
