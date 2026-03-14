package com.github.ae2patterngen.gui;

import com.github.ae2patterngen.util.I18nUtil;

/**
 * Client-side status bridge so async packet handlers can update the open pattern generator UI.
 */
public final class GuiPatternGenStatusBridge {

    private static volatile String statusText = "";

    private GuiPatternGenStatusBridge() {}

    public static void setStatus(String status) {
        statusText = status != null ? status : "";
    }

    public static String getStatus() {
        if (statusText == null || statusText.isEmpty()) {
            return I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.ready");
        }
        return statusText;
    }
}
