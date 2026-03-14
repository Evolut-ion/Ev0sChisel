package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Compat layer for NoCube Neon Blocks (Usermods under AppData). 
 *
 * Detects the NoCube mod directory in the user's Mods folder, attempts
 * to discover block keys, and injects a Paintbrush.Data state onto
 * discovered block types so the Paintbrush UI can operate on them.
 */
public final class NoCubeNeonCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static boolean detected = false;
    private static final List<String> VARIANTS = new ArrayList<>();

    private NoCubeNeonCompat() {}

    public static void init() {
        try {
            String home = System.getProperty("user.home");
            if (home == null) {
                detected = false;
                return;
            }
            File modsDir = new File(home, "AppData\\Roaming\\Hytale\\UserData\\Mods");
            if (!modsDir.exists() || !modsDir.isDirectory()) {
                detected = false;
                LOGGER.atInfo().log("[Paintbrush] No user Mods folder found – NoCube compat disabled");
                return;
            }
            // Accept either a mod directory or a zip archive that contains the NoCube Neon mod
            File[] matches = modsDir.listFiles(f -> (f.isDirectory() || (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".zip")))
                    && f.getName().toLowerCase(Locale.ROOT).contains("nocube"));
            if (matches == null || matches.length == 0) {
                detected = false;
                LOGGER.atInfo().log("[Paintbrush] No NoCube Neon mod folder found – compat disabled");
                return;
            }

            detected = true;
            LOGGER.atInfo().log("[Paintbrush] NoCube Neon mod folder(s) detected – probing for variants");
            buildVariantList(matches);
        } catch (Throwable t) {
            detected = false;
            LOGGER.atWarning().log("[Paintbrush] Failed to init NoCube compat: " + t.getMessage());
        }
    }

    public static boolean isAvailable() { return detected; }

    public static List<String> getVariants() { return Collections.unmodifiableList(VARIANTS); }

    private static void buildVariantList(File[] modDirs) {
        VARIANTS.clear();
        LOGGER.atInfo().log("[Paintbrush] Scanning mod candidates: " + Arrays.toString(modDirs));
        for (File mod : modDirs) {
            // If mod is a directory, iterate files inside it
            if (mod.isDirectory()) {
                File[] children = mod.listFiles();
                if (children == null) continue;
                for (File f : children) {
                    if (f.isDirectory()) continue;
                    String name = f.getName();
                    int dot = name.lastIndexOf('.');
                    String base = dot > 0 ? name.substring(0, dot) : name;
                    List<String> candidates = generateCandidates(mod.getName(), base);
                    for (String cand : candidates) {
                        try {
                            boolean exists = BlockTypeCache.exists(cand);
                            LOGGER.atInfo().log("[Paintbrush] Candidate: " + cand + " -> exists=" + exists);
                            if (exists && !VARIANTS.contains(cand)) {
                                String lower = cand.toLowerCase(Locale.ROOT);
                                if (lower.contains("nocube") || lower.contains("neon") || mod.getName().toLowerCase(Locale.ROOT).contains("nocube")) {
                                    VARIANTS.add(cand);
                                }
                            }
                        } catch (Throwable t) {
                            LOGGER.atWarning().log("[Paintbrush] Error probing candidate " + cand + ": " + t.getMessage());
                        }
                    }
                }
            } else if (mod.isFile() && mod.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                // If mod is a zip archive, inspect entries
                try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(mod)) {
                    Enumeration<? extends java.util.zip.ZipEntry> entries = zf.entries();
                    while (entries.hasMoreElements()) {
                        java.util.zip.ZipEntry entry = entries.nextElement();
                        if (entry.isDirectory()) continue;
                        String path = entry.getName();
                        String name = path.substring(path.lastIndexOf('/') + 1);
                        int dot = name.lastIndexOf('.');
                        String base = dot > 0 ? name.substring(0, dot) : name;
                        List<String> candidates = generateCandidates(mod.getName(), base);
                        for (String cand : candidates) {
                            try {
                                boolean exists = BlockTypeCache.exists(cand);
                                LOGGER.atInfo().log("[Paintbrush] ZIP candidate: " + cand + " -> exists=" + exists + " (entry=" + path + ")");
                                if (exists && !VARIANTS.contains(cand)) {
                                    String lower = cand.toLowerCase(Locale.ROOT);
                                    if (lower.contains("nocube") || lower.contains("neon") || mod.getName().toLowerCase(Locale.ROOT).contains("nocube")) {
                                        VARIANTS.add(cand);
                                    }
                                }
                            } catch (Throwable ignored) { }
                        }
                    }
                } catch (Throwable t) {
                    // ignore unreadable archives
                }
            }
        }

        // If no variants discovered from filenames, probe common numeric suffixes
        if (VARIANTS.isEmpty()) {
            String[] probes = {"NoCube_Neon_", "NoCube_Neon", "nocube_neon_", "nocube_neon"};
            for (String prefix : probes) {
                for (int i = 0; i < 32; i++) {
                    String key = prefix + i;
                    try {
                                if (BlockTypeCache.exists(key) && !VARIANTS.contains(key)) {
                                    VARIANTS.add(key);
                                }
                    } catch (Throwable ignored) { }
                }
            }
        }

        LOGGER.atInfo().log("[Paintbrush] Discovered " + VARIANTS.size() + " NoCube Neon variants: " + VARIANTS);
    }

    /** Generate likely BlockType keys for a given mod and filename. */
    private static List<String> generateCandidates(String modName, String baseName) {
        List<String> out = new ArrayList<>();
        String normMod = modName.replaceAll("[^A-Za-z0-9_]", "_");
        String normBase = baseName.replaceAll("[^A-Za-z0-9_]", "_");
        // Add exact base name (matches block key)
        out.add(baseName);
        out.add(normBase);
        out.add(normMod + "_" + normBase);
        out.add(normMod + ":" + normBase);
        out.add("NoCube_" + normBase);
        out.add("NoCube_Neon_" + normBase);
        out.add("nocube_neon_" + normBase);
        // Add common block key pattern for NoCube Neon blocks
        out.add("NoCube_Neon_Block_" + normBase);
        return out;
    }

    /** Inject a Paintbrush.Data state onto discovered NoCube Neon BlockTypes. */
    public static void injectPaintbrushStates() {
        if (!detected || VARIANTS.isEmpty()) return;

        int injected = 0, failed = 0;
        LOGGER.atInfo().log("[Paintbrush] Beginning injection onto discovered variants: " + VARIANTS);
        for (String key : VARIANTS) {
                try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) { LOGGER.atWarning().log("[Paintbrush] BlockType not found for key: " + key); failed++; continue; }

                StateData existing = bt.getState();
                if (existing instanceof Paintbrush.Data) {
                    LOGGER.atInfo().log("[Paintbrush] Block already has Paintbrush.Data: " + key);
                    continue; // already has paintbrush data
                }

                Paintbrush.Data data = new Paintbrush.Data();
                data.source = "NoCube_Neon";
                data.colorVariants = VARIANTS.toArray(new String[0]);

                setField(StateData.class, data, "id", "Ev0sPaintbrush");
                setField(BlockType.class, bt, "state", data);
                LOGGER.atInfo().log("[Paintbrush] Injected Paintbrush.Data onto: " + key);
                injected++;
            } catch (Throwable t) {
                LOGGER.atWarning().log("[Paintbrush] Failed to inject Paintbrush state for " + key + ": " + t.getMessage());
                failed++;
            }
        }

        LOGGER.atInfo().log("[Paintbrush] Injected Paintbrush state onto " + injected + " NoCube Neon blocks" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
