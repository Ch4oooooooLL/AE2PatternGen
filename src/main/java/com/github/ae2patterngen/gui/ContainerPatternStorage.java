package com.github.ae2patterngen.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import com.github.ae2patterngen.storage.PatternStorage;

/**
 * 仓储 GUI 的 Container — 无槽位，仅传递摘要信息
 */
public class ContainerPatternStorage extends Container {

    public final int patternCount;
    public final String source;
    public final long timestamp;
    public final List<String> previews;

    public ContainerPatternStorage(EntityPlayer player) {
        PatternStorage.StorageSummary summary = PatternStorage.getSummary(player.getUniqueID());
        this.patternCount = summary.count;
        this.source = summary.source;
        this.timestamp = summary.timestamp;
        this.previews = summary.previews;
    }

    /** 客户端用构造 */
    public ContainerPatternStorage(int patternCount, String source, long timestamp, List<String> previews) {
        this.patternCount = patternCount;
        this.source = source;
        this.timestamp = timestamp;
        this.previews = previews != null ? previews : new ArrayList<String>();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
