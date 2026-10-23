package com.github.ae2patterngen.delivery;

import java.util.List;

/**
 * 样板输入总成/接口的自定义名格式化。
 * <p>
 * 名称格式与"参考实现"的搜索关键词同源：{@code <基础名>_<电路>[_NC_<NC物品...>]}，
 * 例如 {@code 卷板机_电路1_NC_模具A+模具B}。基础名、电路 token 与 NC 前缀都由调用方传入
 * （便于本地化与单元测试），本类只负责拼接、去空与长度收口。
 */
public final class HatchNameFormatter {

    /** GT/AE2 的名称显示长度有限，超长只会被截断，这里主动收口避免半个词。 */
    public static final int MAX_NAME_LENGTH = 64;

    /** NC 物品名最多列举几个，其余折成"等 N 种"。 */
    public static final int MAX_NC_NAMES = 3;

    private HatchNameFormatter() {}

    /**
     * @param baseName     基础名（通常是配方池/机器显示名）
     * @param circuitToken 电路 token（如 {@code 电路1} 或 {@code 无电路}），可为空
     * @param ncNames      NC（不消耗）物品显示名，可为空
     */
    public static String format(String baseName, String circuitToken, List<String> ncNames) {
        StringBuilder builder = new StringBuilder(safe(baseName));
        String circuit = safe(circuitToken);
        if (!circuit.isEmpty()) {
            appendToken(builder, circuit);
        }
        String nc = ncToken(ncNames);
        if (!nc.isEmpty()) {
            appendToken(builder, nc);
        }
        if (builder.length() == 0) {
            return "";
        }
        return builder.length() <= MAX_NAME_LENGTH ? builder.toString() : builder.substring(0, MAX_NAME_LENGTH);
    }

    /** NC token，例如 {@code NC_模具A+模具B}；没有 NC 时返回空串。 */
    static String ncToken(List<String> ncNames) {
        if (ncNames == null || ncNames.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("NC_");
        int shown = 0;
        for (String name : ncNames) {
            String safeName = safe(name);
            if (safeName.isEmpty()) {
                continue;
            }
            if (shown >= MAX_NC_NAMES) {
                builder.append("+")
                    .append(ncNames.size() - shown)
                    .append("more");
                break;
            }
            if (shown > 0) {
                builder.append('+');
            }
            builder.append(safeName);
            shown++;
        }
        return builder.length() <= 3 ? "" : builder.toString();
    }

    private static void appendToken(StringBuilder builder, String token) {
        if (builder.length() > 0) {
            builder.append('_');
        }
        builder.append(token);
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
