package com.github.ae2patterngen.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.Loader;

/**
 * 矿辞替换规则的配置文件管理
 * <p>
 * 文件路径: {@code config/ae2patterngen_replacements.cfg}
 * <p>
 * 格式: 每行一条 {@code 源矿辞=目标矿辞}，{@code #} 开头为注释
 */
public class ReplacementConfig {

    private static final String FILE_NAME = "ae2patterngen_replacements.cfg";

    private static List<String> loadedRules = new ArrayList<>();
    private static int ruleCount = 0;

    /**
     * 加载规则文件
     *
     * @return 加载的规则条数
     */
    public static int load() {
        loadedRules.clear();
        ruleCount = 0;

        File file = getConfigFile();

        // 首次运行: 生成带注释的模板
        if (!file.exists()) {
            generateTemplate(file);
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) continue;
                // 验证格式
                int eq = line.indexOf('=');
                if (eq > 0 && eq < line.length() - 1) {
                    loadedRules.add(line);
                    ruleCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to load replacement config: " + e.getMessage());
        }

        return ruleCount;
    }

    /**
     * @return 规则拼接为 ";" 分隔的字符串 (供 OreDictReplacer 解析)
     */
    public static String getRulesString() {
        if (loadedRules.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < loadedRules.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(loadedRules.get(i));
        }
        return sb.toString();
    }

    /**
     * @return 已加载的规则条数
     */
    public static int getRuleCount() {
        return ruleCount;
    }

    /**
     * @return 配置文件对象
     */
    public static File getConfigFile() {
        File configDir = Loader.instance()
            .getConfigDir();
        return new File(configDir, FILE_NAME);
    }

    /**
     * 生成带注释的模板文件
     */
    private static void generateTemplate(File file) {
        try {
            file.getParentFile()
                .mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("# AE2 Pattern Generator - 矿辞替换规则");
                writer.newLine();
                writer.write("# Ore Dictionary Replacement Rules");
                writer.newLine();
                writer.write("#");
                writer.newLine();
                writer.write("# 格式: 源矿辞=目标矿辞 (每行一条)");
                writer.newLine();
                writer.write("# Format: sourceOreDict=targetOreDict (one per line)");
                writer.newLine();
                writer.write("#");
                writer.newLine();
                writer.write("# 示例 / Examples:");
                writer.newLine();
                writer.write("# ingotCopper=dustCopper");
                writer.newLine();
                writer.write("# ingotTin=dustTin");
                writer.newLine();
                writer.write("# plateIron=plateSteel");
                writer.newLine();
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to generate replacement config template: " + e.getMessage());
        }
    }
}
