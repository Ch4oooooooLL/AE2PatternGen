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
import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.RecipeFilterFactory;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.I18nUtil;

/**
 * /patterngen 命令
 * <p>
 * 用法:
 * <ul>
 * <li>/patterngen list - 列出所有可用配方表</li>
 * <li>/patterngen rotate &lt;recipeMapId&gt; [outputFilter] [inputFilter]
 * [ncFilter] [blacklistInput] [blacklistOutput] - 生成样板</li>
 * <li>/patterngen count &lt;recipeMapId&gt; [outputFilter] [inputFilter]
 * [ncFilter] [blacklistInput] [blacklistOutput] - 预览匹配数量</li>
 * </ul>
 */
public class CommandPatternGen extends CommandBase {

    @Override
    public String getCommandName() {
        return "patterngen";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/patterngen <list|generate|count> [recipeMapId] [outputFilter] [inputFilter] [ncFilter] [blacklistInput] [blacklistOutput]";
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
        send(sender, EnumChatFormatting.GOLD, "ae2patterngen.command.help.title");
        send(sender, EnumChatFormatting.YELLOW, "ae2patterngen.command.help.list");
        send(sender, EnumChatFormatting.YELLOW, "ae2patterngen.command.help.count");
        send(sender, EnumChatFormatting.YELLOW, "ae2patterngen.command.help.generate");
    }

    private void handleList(ICommandSender sender) {
        Map<String, String> maps = GTRecipeSource.getAvailableRecipeMaps();
        send(sender, EnumChatFormatting.GOLD, "ae2patterngen.command.list.available_maps", maps.size());

        for (Map.Entry<String, String> entry : maps.entrySet()) {
            send(
                sender,
                EnumChatFormatting.GREEN,
                "ae2patterngen.command.list.entry",
                entry.getKey(),
                entry.getValue());
        }
    }

    private void handleGenerate(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, EnumChatFormatting.RED, "ae2patterngen.command.generate.usage");
            return;
        }

        if (!(sender instanceof EntityPlayerMP)) {
            send(sender, EnumChatFormatting.RED, "ae2patterngen.command.only_player");
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        List<RecipeEntry> filtered = collectAndFilter(sender, args);

        if (filtered.isEmpty()) {
            send(sender, EnumChatFormatting.YELLOW, "ae2patterngen.command.no_matching_recipe");
            return;
        }

        // 5. 编码与结算扣费
        List<ItemStack> patterns = PatternEncoder.encodeBatch(filtered);

        int requiredCount = patterns.size();
        ItemStack blankPattern = com.github.ae2patterngen.util.InventoryUtil.getBlankPattern();

        if (!com.github.ae2patterngen.util.InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
            int currentHas = com.github.ae2patterngen.util.InventoryUtil.countItem(player, blankPattern);
            send(sender, EnumChatFormatting.RED, "ae2patterngen.command.generate.insufficient_blank_pattern");
            send(
                sender,
                EnumChatFormatting.RED,
                "ae2patterngen.command.generate.required_vs_owned",
                requiredCount,
                currentHas);
            return;
        }

        send(sender, EnumChatFormatting.GREEN, "ae2patterngen.command.generate.start", patterns.size());

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

        send(
            sender,
            EnumChatFormatting.GREEN,
            "ae2patterngen.command.generate.done",
            givenToInventory,
            droppedOnGround);
    }

    private void handleCount(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, EnumChatFormatting.RED, "ae2patterngen.command.count.usage");
            return;
        }

        List<RecipeEntry> filtered = collectAndFilter(sender, args);

        send(sender, EnumChatFormatting.GREEN, "ae2patterngen.command.count.result", filtered.size());
    }

    private List<RecipeEntry> collectAndFilter(ICommandSender sender, String[] args) {
        String recipeMapIdInput = args[1];
        String outputOreDict = args.length > 2 ? args[2] : null;
        String inputOreDict = args.length > 3 ? args[3] : null;
        String ncItem = args.length > 4 ? args[4] : null;

        // 1. 查找配方表
        List<String> matchedMaps = GTRecipeSource.findMatchingRecipeMaps(recipeMapIdInput);
        if (matchedMaps.isEmpty()) {
            send(sender, EnumChatFormatting.RED, "ae2patterngen.command.map_not_found", recipeMapIdInput);
            return new java.util.ArrayList<>();
        }
        send(sender, EnumChatFormatting.GRAY, "ae2patterngen.command.matched_maps", String.join(", ", matchedMaps));

        // 2. 收集配方
        List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(recipeMapIdInput);
        int totalBefore = recipes.size();

        // 3. 构建过滤器
        CompositeFilter filter = RecipeFilterFactory.build(
            outputOreDict,
            inputOreDict,
            ncItem,
            args.length > 5 ? args[5] : null,
            args.length > 6 ? args[6] : null,
            -1);

        // 4. 应用过滤
        List<RecipeEntry> filtered = new java.util.ArrayList<>();
        for (RecipeEntry recipe : recipes) {
            if (filter.matches(recipe)) {
                filtered.add(recipe);
            }
        }

        send(sender, EnumChatFormatting.GRAY, "ae2patterngen.command.filter_result", totalBefore, filtered.size());

        return filtered;
    }

    private void send(ICommandSender sender, EnumChatFormatting color, String key, Object... args) {
        sender.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }
}
