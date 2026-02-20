package com.github.ae2patterngen.command;

import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.encoder.PatternEncoder;
import com.github.ae2patterngen.filter.BlacklistFilter;
import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.InputOreDictFilter;
import com.github.ae2patterngen.filter.NCItemFilter;
import com.github.ae2patterngen.filter.OutputOreDictFilter;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * /patterngen 命令
 * <p>
 * 用法:
 * <ul>
 * <li>/patterngen list - 列出所有可用配方表</li>
 * <li>/patterngen rotate &lt;recipeMapId&gt; [outputOreDict] [inputOreDict]
 * [ncItem] [blacklistInput] [blacklistOutput] - 生成样板</li>
 * <li>/patterngen count &lt;recipeMapId&gt; [outputOreDict] [inputOreDict]
 * [ncItem] [blacklistInput] [blacklistOutput] - 预览匹配数量</li>
 * </ul>
 */
public class CommandPatternGen extends CommandBase {

    @Override
    public String getCommandName() {
        return "patterngen";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/patterngen <list|generate|count> [recipeMapId] [outputOreDict] [inputOreDict] [ncItem] [blacklistInput] [blacklistOutput]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 所有玩家可用
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleList(sender);
                break;
            case "generate":
                handleGenerate(sender, args);
                break;
            case "count":
                handleCount(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
    }

    private void sendHelp(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== AE2 Pattern Generator ==="));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.YELLOW + "/patterngen list" + EnumChatFormatting.WHITE + " - 列出所有配方表"));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.YELLOW + "/patterngen count <配方表ID> [输出矿辞] [输入矿辞] [NC物品] [输入黑名单] [输出黑名单]"
                    + EnumChatFormatting.WHITE
                    + " - 预览匹配数量"));
        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.YELLOW + "/patterngen generate <配方表ID> [输出矿辞] [输入矿辞] [NC物品] [输入黑名单] [输出黑名单]"
                    + EnumChatFormatting.WHITE
                    + " - 生成样板"));
    }

    private void handleList(ICommandSender sender) {
        Map<String, String> maps = GTRecipeSource.getAvailableRecipeMaps();
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "可用的配方表 (" + maps.size() + " 个):"));

        for (Map.Entry<String, String> entry : maps.entrySet()) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "  "
                        + entry.getKey()
                        + EnumChatFormatting.GRAY
                        + " ("
                        + entry.getValue()
                        + ")"));
        }
    }

    private void handleGenerate(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "用法: /patterngen generate <配方表ID> [输出矿辞] [输入矿辞] [NC物品]"));
            return;
        }

        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "此命令只能由玩家执行"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        List<RecipeEntry> filtered = collectAndFilter(sender, args);

        if (filtered.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "没有找到匹配的配方"));
            return;
        }

        // 5. 编码与结算扣费
        List<ItemStack> patterns = PatternEncoder.encodeBatch(filtered);

        int requiredCount = patterns.size();
        ItemStack blankPattern = com.github.ae2patterngen.util.InventoryUtil.getBlankPattern();

        if (!com.github.ae2patterngen.util.InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
            int currentHas = com.github.ae2patterngen.util.InventoryUtil.countItem(player, blankPattern);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "生成失败: 空白样板不足。"));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "需要 " + requiredCount + " 个，但你只有 " + currentHas + " 个。"));
            return;
        }

        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GREEN + "扣除空样板并存入成品 " + patterns.size() + " 个..."));

        int givenToInventory = 0;
        int droppedOnGround = 0;

        for (ItemStack pattern : patterns) {
            if (!player.inventory.addItemStackToInventory(pattern)) {
                // 背包满了，掉落到地上
                EntityItem entityItem = player.entityDropItem(pattern, 0.5F);
                if (entityItem != null) {
                    entityItem.delayBeforeCanPickup = 0;
                }
                droppedOnGround++;
            } else {
                givenToInventory++;
            }
        }

        player.inventoryContainer.detectAndSendChanges();

        sender.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GREEN + "完成! 放入背包: " + givenToInventory + ", 掉落地面: " + droppedOnGround));
    }

    private void handleCount(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "用法: /patterngen count <配方表ID> [输出矿辞] [输入矿辞] [NC物品]"));
            return;
        }

        List<RecipeEntry> filtered = collectAndFilter(sender, args);

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "匹配到 " + filtered.size() + " 个配方"));
    }

    private List<RecipeEntry> collectAndFilter(ICommandSender sender, String[] args) {
        String recipeMapIdInput = args[1];
        String outputOreDict = args.length > 2 ? args[2] : null;
        String inputOreDict = args.length > 3 ? args[3] : null;
        String ncItem = args.length > 4 ? args[4] : null;

        // 1. 查找配方表
        List<String> matchedMaps = GTRecipeSource.findMatchingRecipeMaps(recipeMapIdInput);
        if (matchedMaps.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "未找到匹配的配方表: " + recipeMapIdInput));
            return new java.util.ArrayList<>();
        }
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GRAY + "匹配到配方表: " + String.join(", ", matchedMaps)));

        // 2. 收集配方
        List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(recipeMapIdInput);
        int totalBefore = recipes.size();

        // 3. 构建过滤器
        CompositeFilter filter = new CompositeFilter();
        if (outputOreDict != null && !outputOreDict.isEmpty() && !outputOreDict.equals("*")) {
            filter.addFilter(new OutputOreDictFilter(outputOreDict));
        }
        if (inputOreDict != null && !inputOreDict.isEmpty() && !inputOreDict.equals("*")) {
            filter.addFilter(new InputOreDictFilter(inputOreDict));
        }
        if (ncItem != null && !ncItem.isEmpty() && !ncItem.equals("*")) {
            filter.addFilter(new NCItemFilter(ncItem));
        }
        if (args.length > 5 && !args[5].isEmpty() && !args[5].equals("*")) {
            filter.addFilter(new BlacklistFilter(args[5], true, false));
        }
        if (args.length > 6 && !args[6].isEmpty() && !args[6].equals("*")) {
            filter.addFilter(new BlacklistFilter(args[6], false, true));
        }

        // 预留: 如果将来命令行增加 Tier 参数，可在此处添加 TierFilter
        // if (targetTier >= 0) {
        // filter.addFilter(new TierFilter(targetTier));
        // }

        // 4. 应用过滤
        List<RecipeEntry> filtered = new java.util.ArrayList<>();
        for (RecipeEntry recipe : recipes) {
            if (filter.matches(recipe)) {
                filtered.add(recipe);
            }
        }

        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GRAY + "原始配方: " + totalBefore + ", 过滤后: " + filtered.size()));

        return filtered;
    }
}
