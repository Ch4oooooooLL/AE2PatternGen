package com.github.ae2patterngen.storage;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.github.ae2patterngen.config.ReplacementConfig;
import com.github.ae2patterngen.recipe.RecipeEntry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/**
 * Computes environment signatures used to validate persisted recipe cache state.
 */
public final class ModVersionHelper {

    private static final String MISSING_HASH = "MISSING";

    private static volatile LoadedModVersionsProvider loadedModVersionsProvider = new LoadedModVersionsProvider() {

        @Override
        public Map<String, String> get() {
            Map<String, String> versions = new LinkedHashMap<String, String>();
            try {
                List<ModContainer> activeMods = Loader.instance()
                    .getActiveModList();
                if (activeMods == null) {
                    return versions;
                }

                for (ModContainer mod : activeMods) {
                    if (mod == null || mod.getModId() == null) {
                        continue;
                    }
                    versions.put(mod.getModId(), mod.getVersion() != null ? mod.getVersion() : "");
                }
            } catch (RuntimeException ignored) {}
            return versions;
        }
    };

    private static volatile ConfigFilesProvider configFilesProvider = new ConfigFilesProvider() {

        @Override
        public List<File> get() {
            List<File> files = new ArrayList<File>();
            File loaderConfigDir = null;
            try {
                loaderConfigDir = Loader.instance()
                    .getConfigDir();
            } catch (RuntimeException ignored) {}

            if (loaderConfigDir != null) {
                files.add(new File(loaderConfigDir, "ae2patterngen.cfg"));
            }
            files.add(ReplacementConfig.getConfigFile());
            return files;
        }
    };

    private ModVersionHelper() {}

    public static Map<String, String> getLoadedModVersions() {
        return new LinkedHashMap<String, String>(loadedModVersionsProvider.get());
    }

    public static Map<String, String> calculateConfigHashes() {
        Map<String, String> hashes = new LinkedHashMap<String, String>();
        for (File file : configFilesProvider.get()) {
            if (file != null) {
                hashes.put(file.getPath(), calculateConfigHash(file));
            }
        }
        return hashes;
    }

    public static String calculateConfigHash(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return MISSING_HASH;
        }

        try (FileInputStream input = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (Exception e) {
            return MISSING_HASH;
        }
    }

    public static String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes) {
        List<String> descriptors = new ArrayList<String>();
        if (recipes != null) {
            for (RecipeEntry recipe : recipes) {
                descriptors.add(describeRecipe(recipe));
            }
        }
        Collections.sort(descriptors);

        StringBuilder payload = new StringBuilder();
        payload.append(mapId != null ? mapId : "")
            .append('\n');
        for (String descriptor : descriptors) {
            payload.append(descriptor)
                .append('\n');
        }
        return sha256(payload.toString());
    }

    public static boolean isModVersionChanged(RecipeCacheMetadata metadata, Map<String, String> currentVersions) {
        if (metadata == null) {
            return true;
        }

        Map<String, String> storedVersions = new LinkedHashMap<String, String>();
        for (RecipeCacheMetadata.ModInfo info : metadata.mods.values()) {
            if (info != null && info.modId != null) {
                storedVersions.put(info.modId, info.version != null ? info.version : "");
            }
        }
        return !storedVersions.equals(currentVersions);
    }

    public static boolean isConfigHashChanged(RecipeCacheMetadata metadata, Map<String, String> currentHashes) {
        if (metadata == null) {
            return true;
        }
        return !metadata.configHashes.equals(currentHashes);
    }

    public static String buildEnvironmentFingerprint(Map<String, String> loadedModVersions,
        Map<String, String> configHashes) {
        StringBuilder payload = new StringBuilder();
        appendSortedMap(payload, loadedModVersions);
        payload.append('|');
        appendSortedMap(payload, configHashes);
        return sha256(payload.toString());
    }

    public static String resolveModId(String mapId) {
        if (mapId == null || mapId.isEmpty()) {
            return "unknown";
        }
        if (mapId.startsWith("gt.recipe.")) {
            return "gregtech";
        }
        int dot = mapId.indexOf('.');
        return dot > 0 ? mapId.substring(0, dot) : mapId;
    }

    static void setLoadedModVersionsProvider(LoadedModVersionsProvider provider) {
        loadedModVersionsProvider = provider != null ? provider : loadedModVersionsProvider;
    }

    static void setConfigFilesProvider(ConfigFilesProvider provider) {
        configFilesProvider = provider != null ? provider : configFilesProvider;
    }

    static void resetProviders() {
        loadedModVersionsProvider = new LoadedModVersionsProvider() {

            @Override
            public Map<String, String> get() {
                Map<String, String> versions = new LinkedHashMap<String, String>();
                try {
                    List<ModContainer> activeMods = Loader.instance()
                        .getActiveModList();
                    if (activeMods == null) {
                        return versions;
                    }
                    for (ModContainer mod : activeMods) {
                        if (mod == null || mod.getModId() == null) {
                            continue;
                        }
                        versions.put(mod.getModId(), mod.getVersion() != null ? mod.getVersion() : "");
                    }
                } catch (RuntimeException ignored) {}
                return versions;
            }
        };

        configFilesProvider = new ConfigFilesProvider() {

            @Override
            public List<File> get() {
                List<File> files = new ArrayList<File>();
                File loaderConfigDir = null;
                try {
                    loaderConfigDir = Loader.instance()
                        .getConfigDir();
                } catch (RuntimeException ignored) {}

                if (loaderConfigDir != null) {
                    files.add(new File(loaderConfigDir, "ae2patterngen.cfg"));
                }
                files.add(ReplacementConfig.getConfigFile());
                return files;
            }
        };
    }

    private static void appendSortedMap(StringBuilder payload, Map<String, String> values) {
        List<String> keys = new ArrayList<String>(values.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            payload.append(key)
                .append('=')
                .append(values.get(key))
                .append(';');
        }
    }

    private static String describeRecipe(RecipeEntry recipe) {
        if (recipe == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(recipe.sourceType)
            .append('|')
            .append(recipe.recipeMapId)
            .append('|')
            .append(recipe.machineDisplayName)
            .append('|')
            .append(recipe.duration)
            .append('|')
            .append(recipe.euPerTick)
            .append('|');
        appendItemStacks(sb, recipe.inputs);
        sb.append('|');
        appendItemStacks(sb, recipe.outputs);
        sb.append('|');
        appendFluidStacks(sb, recipe.fluidInputs);
        sb.append('|');
        appendFluidStacks(sb, recipe.fluidOutputs);
        sb.append('|');
        appendItemStacks(sb, recipe.specialItems);
        return sb.toString();
    }

    private static void appendItemStacks(StringBuilder sb, ItemStack[] stacks) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null) {
                sb.append("null,");
                continue;
            }

            Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
            sb.append(registryName != null ? registryName : Item.getIdFromItem(stack.getItem()))
                .append('@')
                .append(stack.getItemDamage())
                .append('@')
                .append(stack.stackSize);
            if (stack.hasTagCompound()) {
                sb.append('@')
                    .append(
                        stack.getTagCompound()
                            .toString());
            }
            sb.append(',');
        }
    }

    private static void appendFluidStacks(StringBuilder sb, FluidStack[] stacks) {
        if (stacks == null) {
            return;
        }
        for (FluidStack stack : stacks) {
            if (stack == null || stack.getFluid() == null) {
                sb.append("null,");
                continue;
            }
            sb.append(
                stack.getFluid()
                    .getName())
                .append('@')
                .append(stack.amount);
            if (stack.tag != null) {
                sb.append('@')
                    .append(stack.tag.toString());
            }
            sb.append(',');
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }

    interface LoadedModVersionsProvider {

        Map<String, String> get();
    }

    interface ConfigFilesProvider {

        List<File> get();
    }
}
