package com.github.ae2patterngen.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * 统一配方数据结构，可容纳 GT 配方和其他类型配方
 */
public class RecipeEntry {

    /** 配方来源类型: "gt", "vanilla", "nei" */
    public final String sourceType;

    /** 配方表 ID / 机器名称 (如 "gt.recipe.assembler") */
    public final String recipeMapId;

    /** 机器显示名称 */
    public final String machineDisplayName;

    /** 输入物品 */
    public final ItemStack[] inputs;

    /** 输出物品 */
    public final ItemStack[] outputs;

    /** 流体输入 */
    public final FluidStack[] fluidInputs;

    /** 流体输出 */
    public final FluidStack[] fluidOutputs;

    /** 不消耗的物品 (NC 物品) */
    public final ItemStack[] specialItems;

    /** 处理时间 (ticks) */
    public final int duration;

    /** EU/t */
    public final int euPerTick;

    public RecipeEntry(String sourceType, String recipeMapId, String machineDisplayName, ItemStack[] inputs,
        ItemStack[] outputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs, ItemStack[] specialItems,
        int duration, int euPerTick) {
        this.sourceType = sourceType;
        this.recipeMapId = recipeMapId;
        this.machineDisplayName = machineDisplayName;
        this.inputs = inputs != null ? inputs : new ItemStack[0];
        this.outputs = outputs != null ? outputs : new ItemStack[0];
        this.fluidInputs = fluidInputs != null ? fluidInputs : new FluidStack[0];
        this.fluidOutputs = fluidOutputs != null ? fluidOutputs : new FluidStack[0];
        this.specialItems = specialItems != null ? specialItems : new ItemStack[0];
        this.duration = duration;
        this.euPerTick = euPerTick;
    }
}
