package com.Ev0sMods.Ev0sChisel.compat;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Compatibility helpers for Ymmersive Statues (when present).
 * <p>
 * Provides detection and candidate statue key derivation based on a
 * block key or its resolved chisel substitutions.
 */
public final class StatuesCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static boolean detected = false;
    private static final Set<String> AVAILABLE_STATUES = new LinkedHashSet<>();
    private static final Map<String, List<String>> STATUES_BY_MATERIAL = new HashMap<>();
    private static final Map<String, String> STATUE_TO_MATERIAL = new HashMap<>();
    // Map chisel-type (e.g. "Rock_Marble", "Rock_Gold_Smooth", "any_wood") -> statue keys
    private static final Map<String, List<String>> CHISEL_TO_STATUES = new HashMap<>();
    private static final Map<String, String> MATERIAL_TO_CHISEL = new HashMap<>();

    private StatuesCompat() {}

    public static void init() {
        try {
            Class.forName("net.conczin.YmmersiveStatues");
            detected = true;

            // Attempt to locate the Ymmersive Statues JAR and enumerate available
            // furniture/statue keys so we can offer exact JSON names.
            try {
                Class<?> cls = Class.forName("net.conczin.YmmersiveStatues");
                URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
                if (loc != null) {
                    Path jarPath = Paths.get(loc.toURI());
                    File f = jarPath.toFile();
                    if (f.exists() && f.isFile()) {
                        // prepare canonical material -> chisel-type mapping
                        MATERIAL_TO_CHISEL.put("gold", "Rock_Gold_Brick_Smooth");
                        MATERIAL_TO_CHISEL.put("marble", "Rock_Marble");
                        MATERIAL_TO_CHISEL.put("shale", "Rock_Shale");
                        MATERIAL_TO_CHISEL.put("ice", "Rock_Ice");
                        MATERIAL_TO_CHISEL.put("limestone", "Rock_Limestone");
                        MATERIAL_TO_CHISEL.put("mossy", "Rock_Stone_Mossy");
                        MATERIAL_TO_CHISEL.put("poisoned", "Rock_Volcanic_Poisoned_Cracked");
                        // wood handled as a special case by returning 'any_wood'
                        MATERIAL_TO_CHISEL.put("wood", "any_wood");

                        try (ZipFile z = new ZipFile(f)) {
                            Enumeration<? extends ZipEntry> en = z.entries();
                            while (en.hasMoreElements()) {
                                ZipEntry e = en.nextElement();
                                String name = e.getName();
                                if ((
                                        name.startsWith("Server/Item/Items/Statue/Ymmersive_Statues_")
                                        )
                                        && name.endsWith(".json")) {
                                    String base = name.substring(name.lastIndexOf('/') + 1, name.length() - 5);
                                    AVAILABLE_STATUES.add(base);
                                    // record reverse mapping for quick lookup
                                    STATUE_TO_MATERIAL.put(base, null);

                                    // Try to read the JSON and infer material from its contents (Tags/Family preferred)
                                    try (InputStream is = z.getInputStream(e);
                                         InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                                         BufferedReader br = new BufferedReader(isr)) {
                                        StringBuilder sb = new StringBuilder();
                                        String line;
                                        while ((line = br.readLine()) != null) sb.append(line).append('\n');
                                        String json = sb.toString();
                                        String material = inferMaterialFromJson(json, base);
                                        if (material != null) {
                                            STATUES_BY_MATERIAL.computeIfAbsent(material, k -> new ArrayList<>()).add(base);
                                            STATUE_TO_MATERIAL.put(base, material);
                                        } else {
                                            // fallback to filename token
                                            String[] parts = base.split("_");
                                            if (parts.length >= 3) {
                                                String materialToken = parts[2].toLowerCase(Locale.ROOT);
                                                STATUES_BY_MATERIAL.computeIfAbsent(materialToken, k -> new ArrayList<>()).add(base);
                                                STATUE_TO_MATERIAL.put(base, materialToken);
                                            }
                                        }
                                    } catch (Throwable readEx) {
                                        // best-effort: fallback to filename token
                                        String[] parts = base.split("_");
                                        if (parts.length >= 3) {
                                            String materialToken = parts[2].toLowerCase(Locale.ROOT);
                                            STATUES_BY_MATERIAL.computeIfAbsent(materialToken, k -> new ArrayList<>()).add(base);
                                            STATUE_TO_MATERIAL.put(base, materialToken);
                                        }
                                    }
                                }
                            }
                        }
                        // Build an index from discovered statue -> chisel type so the UI can
                        // directly ask for statues by chisel-type (e.g. Rock_Marble).
                        for (Map.Entry<String, String> e2 : STATUE_TO_MATERIAL.entrySet()) {
                            String statueKey = e2.getKey();
                            String mat = e2.getValue();
                            String mapped = null;
                            if (mat != null) mapped = MATERIAL_TO_CHISEL.get(mat);
                            if (mapped == null) mapped = getMappedChiselTypeForStatue(statueKey);
                            if (mapped != null) {
                                CHISEL_TO_STATUES.computeIfAbsent(mapped, k -> new ArrayList<>()).add(statueKey);
                                CHISEL_TO_STATUES.computeIfAbsent(mapped.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(statueKey);
                            }
                        }

                        // discovered statue entries (debug logs removed)
                    }
                }
            } catch (Throwable t) {
                LOGGER.atWarning().log("[Chisel] Failed to enumerate Ymmersive Statues JAR: " + t.getMessage());
            }

        } catch (ClassNotFoundException e) {
            detected = false;
        }
    }

    // Infer a simple material token from the statue JSON content.
    // Preferred sources: Tags.Family[0], BlockType.CustomModel filename token, else null.
    private static String inferMaterialFromJson(String json, String filenameBase) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("Tags")) {
                JSONObject tags = obj.optJSONObject("Tags");
                if (tags != null && tags.has("Family")) {
                    JSONArray fam = tags.optJSONArray("Family");
                    if (fam != null && fam.length() > 0) {
                        String fam0 = fam.optString(0, null);
                        if (fam0 != null && !fam0.isEmpty()) return fam0.toLowerCase(Locale.ROOT);
                    }
                }
            }
            if (obj.has("BlockType")) {
                JSONObject bt = obj.optJSONObject("BlockType");
                if (bt != null) {
                    if (bt.has("CustomModel")) {
                        String cm = bt.optString("CustomModel", null);
                        if (cm != null && !cm.isEmpty()) {
                            String f = cm.substring(cm.lastIndexOf('/') + 1);
                            if (f.startsWith("Ymmersive_Statues_")) {
                                String[] p = f.split("_");
                                if (p.length >= 3) return p[2].toLowerCase(Locale.ROOT);
                            }
                        }
                    }
                    if (bt.has("HitboxType")) {
                        String hb = bt.optString("HitboxType", null);
                        if (hb != null && hb.startsWith("Ymmersive_Statues_")) {
                            String[] p = hb.split("_");
                            if (p.length >= 3) return p[2].toLowerCase(Locale.ROOT);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // ignore parse failures
        }
        // fallback: attempt to parse material token from filenameBase
        try {
            String[] parts = filenameBase.split("_");
            if (parts.length >= 3) return parts[2].toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isAvailable() {
        return detected;
    }

    /**
     * Returns statue keys indexed for a canonical chisel-type (e.g. "Rock_Marble").
     * Preserves the same defensive behaviour as other compat APIs (never null).
     */
    public static List<String> getStatuesForChiselType(String chiselType) {
        if (!detected || chiselType == null) return Collections.emptyList();
        List<String> direct = CHISEL_TO_STATUES.get(chiselType);
        if (direct != null) return Collections.unmodifiableList(direct);
        List<String> directLower = CHISEL_TO_STATUES.get(chiselType.toLowerCase(Locale.ROOT));
        if (directLower != null) return Collections.unmodifiableList(directLower);
        return Collections.emptyList();
    }

    /**
     * Returns the chisel-types that we have indexed statuary entries for.
     */
    public static Set<String> getIndexedChiselTypes() {
        if (!detected) return Collections.emptySet();
        return Collections.unmodifiableSet(CHISEL_TO_STATUES.keySet());
    }

    /**
     * Returns statue candidate keys for the given block key. Never returns null.
     * The returned list may be empty if statues mod is not present.
     */
    public static List<String> getCandidatesFor(String candidate, String[] substitutions) {
        if (!detected || candidate == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        String lower = candidate.toLowerCase(Locale.ROOT);

        // If the caller passed a canonical chisel-type (e.g. "Rock_Marble"), prefer
        // the precomputed CHISEL_TO_STATUES index for precise matches.
        if (!CHISEL_TO_STATUES.isEmpty()) {
            // direct match
            List<String> direct = CHISEL_TO_STATUES.get(candidate);
            if (direct != null) return new ArrayList<>(direct);
            // lowercase match
            List<String> directLower = CHISEL_TO_STATUES.get(lower);
            if (directLower != null) return new ArrayList<>(directLower);
            // tolerate rock_/Rock_ prefixes: try adding/removing prefix
            if (candidate.startsWith("Rock_") || candidate.startsWith("rock_")) {
                String stripped = candidate.substring(candidate.indexOf('_') + 1);
                List<String> byStripped = CHISEL_TO_STATUES.get(stripped);
                if (byStripped != null) return new ArrayList<>(byStripped);
                List<String> byStrippedLower = CHISEL_TO_STATUES.get(stripped.toLowerCase(Locale.ROOT));
                if (byStrippedLower != null) return new ArrayList<>(byStrippedLower);
            }
        }

        // If we successfully enumerated the Ymmersive JAR, prefer exact matches
        if (!AVAILABLE_STATUES.isEmpty()) {
            // Wood pillar -> include all Wood statues (if two-block pillar detected upstream)
            if (CarpentryCompat.isAvailable()) {
                String woodType = CarpentryCompat.detectWoodType(candidate, substitutions);
                if (woodType != null) {
                    List<String> list = STATUES_BY_MATERIAL.get("wood");
                    if (list != null) out.addAll(list);
                    return out;
                }
            }

            // Poisoned -> include Poisoned statues (maps to volcanic/poisoned)
            if (lower.contains("poison") || lower.contains("poisoned")) {
                List<String> list = STATUES_BY_MATERIAL.get("poisoned");
                if (list != null) out.addAll(list);
                return out;
            }

            // Determine rock type; treat mossy as 'stone'
            if (lower.contains("mossy")) {
                // Treat mossy as stone
                List<String> stone = STATUES_BY_MATERIAL.get("stone");
                if (stone != null) out.addAll(stone);
                // Also include mossy-specific statues if present
                List<String> mossy = STATUES_BY_MATERIAL.get("mossy");
                if (mossy != null) out.addAll(mossy);
                return out;
            }

            // Try masonry/macaw rock type
            String rockType = null;
            if (MasonryCompat.isAvailable()) rockType = MasonryCompat.detectStoneType(candidate, substitutions);
            if (rockType == null) rockType = MacawCompat.detectRockType(candidate, substitutions);
            if (rockType != null) {
                String key = rockType.toLowerCase(Locale.ROOT);
                // Normalize common prefixes like 'rock_' or 'stone_'
                if (key.startsWith("rock_")) key = key.substring("rock_".length());
                if (key.startsWith("stone_")) key = key.substring("stone_".length());
                List<String> list = STATUES_BY_MATERIAL.get(key);
                if (list != null && !list.isEmpty()) {
                    out.addAll(list);
                    return out;
                }
                // try with original rock_ prefix as fallback
                List<String> list2 = STATUES_BY_MATERIAL.get("rock_" + key);
                if (list2 != null && !list2.isEmpty()) {
                    out.addAll(list2);
                    return out;
                }
            }

            // If no direct rockType found, try to match any material token present in the candidate key
            for (Map.Entry<String, List<String>> e : STATUES_BY_MATERIAL.entrySet()) {
                String mat = e.getKey();
                if (lower.contains(mat)) out.addAll(e.getValue());
            }
            if (!out.isEmpty()) return out;

            // As a last resort include a broad set of rock/wood/poisoned matches
            for (String avail : AVAILABLE_STATUES) {
                String al = avail.toLowerCase(Locale.ROOT);
                if ((al.startsWith("ymmersive_statues_rock_") || al.startsWith("ymmersive_statues_limestone_") || al.startsWith("ymmersive_statues_ice_")
                        || al.startsWith("ymmersive_statues_marble_") || al.startsWith("ymmersive_statues_shale_")
                        || al.startsWith("ymmersive_statues_wood_") || al.startsWith("ymmersive_statues_poisoned_"))
                        && !out.contains(avail)) out.add(avail);
            }
            return out;
        }

        // Fallback behavior if we couldn't enumerate jar entries: create safe rock-like keys
        String safe = candidate.replace(':', '_').replace('-', '_');
        out.add("Ymmersive_Statues_Rock_" + safe);
        out.add("Ymmersive_Statues_Rock_" + safe + "_" + safe);
        return out;
    }

    /**
     * Returns the canonical Chisel type mapped from a discovered statue key, or null if unknown.
     * Examples: "Ymmersive_Statues_Gold_Antelope" -> "Rock_Gold_Smooth",
     * "Ymmersive_Statues_Marble_Bear" -> "Rock_Marble", "...Wood_..." -> "any_wood".
     */
    public static String getMappedChiselTypeForStatue(String statueKey) {
        if (statueKey == null) return null;
        // strip extension if provided
        String key = statueKey;
        if (key.endsWith(".json")) key = key.substring(0, key.length() - 5);
        String mat = STATUE_TO_MATERIAL.get(key);
        if (mat != null) {
            String mapped = MATERIAL_TO_CHISEL.get(mat);
            if (mapped != null) return mapped;
            // try normalized rock token
            if (mat.startsWith("rock_") || mat.startsWith("stone_") ) {
                return mat; // best-effort pass-through
            }
        }
        // fallback: try to infer from statue key tokens
        String[] parts = key.split("_");
        if (parts.length >= 3) {
            String token = parts[2].toLowerCase(Locale.ROOT);
            String mapped = MATERIAL_TO_CHISEL.get(token);
            if (mapped != null) return mapped;
            return token;
        }
        return null;
    }

    /** Map a detected block/material token to the canonical Chisel type used above. */
    public static String mapBlockMaterialToChisel(String detectedMaterial) {
        if (detectedMaterial == null) return null;
        String key = detectedMaterial.toLowerCase(Locale.ROOT);
        if (key.startsWith("rock_")) key = key.substring("rock_".length());
        if (key.startsWith("stone_")) key = key.substring("stone_".length());
        // direct mapping
        String mapped = MATERIAL_TO_CHISEL.get(key);
        if (mapped != null) return mapped;
        // if the detectedMaterial already looks like a chisel type, return as-is
        if (detectedMaterial.startsWith("Rock_") || detectedMaterial.startsWith("rock_")) return detectedMaterial;
        // wood special case
        if (key.contains("wood")) return MATERIAL_TO_CHISEL.get("wood");
        return null;
    }

    /**
     * Inject `Chisel.Data` onto statue BlockTypes so they can be chiseled
     * back into their material variants + the base material block.
     * This mirrors other compat injectors and is safe/ idempotent.
     */
    public static void injectChiselStates() {
        if (!detected) return;
        int injected = 0;
        int failed = 0;

        // injectChiselStates: info logging removed

        for (Map.Entry<String, List<String>> e : CHISEL_TO_STATUES.entrySet()) {
            String chiselType = e.getKey();
            if (chiselType == null) continue;
            List<String> statues = e.getValue();
            if (statues == null || statues.isEmpty()) continue;

            // Determine a sensible source name and base material block key
            String source = "StatuesCompat";
            String baseBlockKey = null;
            // If chiselType looks like Rock_Marble or rock_marble, try to normalise to BlockType id
            String norm = chiselType;
            if (norm.startsWith("rock_") || norm.startsWith("Rock_")) {
                String rest = norm.substring(norm.indexOf('_') + 1);
                baseBlockKey = "Rock_" + capitalizeEach(rest);
            } else if (norm.startsWith("any_wood") || norm.contains("wood")) {
                // wood: we won't attempt to pick a single wood base – leave base null
                baseBlockKey = null;
            } else {
                // pass-through attempt
                baseBlockKey = chiselType;
            }

            // Build substitution list: all statues in this material + optional base material
            LinkedHashSet<String> subs = new LinkedHashSet<>();
            subs.addAll(statues);
            if (baseBlockKey != null) subs.add(baseBlockKey);

            String[] subsArr = subs.toArray(new String[0]);

            // Inject onto each statue BlockType
            for (String statueKey : statues) {
                try {
                    BlockType bt = BlockTypeCache.get(statueKey);
                    if (bt == null) {
                        failed++; continue;
                    }
                    StateData existing = bt.getState();
                    if (existing instanceof Chisel.Data) {
                        continue; // leave existing compat alone
                    }

                    Chisel.Data data = new Chisel.Data();
                    data.source = source;
                    data.substitutions = subsArr;
                    data.stairs = new String[0];
                    data.halfSlabs = new String[0];
                    data.roofing = new String[0];

                    setField(StateData.class, data, "id", "Ev0sChisel");
                    setField(BlockType.class, bt, "state", data);
                    injected++;
                } catch (Throwable t) {
                    LOGGER.atWarning().log("[Chisel] injectChiselStates: failed to inject for " + statueKey + ": " + t.getMessage());
                    failed++;
                }
            }
        }

        // Injected Chisel state summary log removed
    }

        /**
         * Ensure a single statue BlockType is injected with Chisel.Data at runtime.
         * Returns true if injected or already present.
         */
        public static boolean ensureInjectedFor(String statueKey) {
            if (!detected || statueKey == null) return false;
            try {
                // Try several normalized forms of the provided key to handle
                // namespace/path differences between runtime BlockType ids
                String[] candidates = buildStatueKeyCandidates(statueKey);
                BlockType bt = null;
                for (String cand : candidates) {
                    try { bt = BlockTypeCache.get(cand); } catch (Throwable ignored) {}
                    if (bt != null) break;
                }
                if (bt == null) {
                    return false;
                }
                StateData existing = bt.getState();
                if (existing instanceof Chisel.Data) return true;

                // Build a conservative substitutions list: statueKey only
                Chisel.Data data = new Chisel.Data();
                data.source = "StatuesCompat-runtime";
                data.substitutions = new String[]{statueKey};
                data.stairs = new String[0];
                data.halfSlabs = new String[0];
                data.roofing = new String[0];
                    try {
                        setField(StateData.class, data, "id", "Ev0sChisel");
                        setField(BlockType.class, bt, "state", data);
                        return true;
                    } catch (Throwable t) {
                        LOGGER.atWarning().log("[Chisel] ensureInjectedFor: failed to inject for " + statueKey + ": " + t.getMessage());
                        return false;
                    }
            } catch (Throwable t) {
                return false;
            }
        }

    private static String capitalizeEach(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            if (i < parts.length - 1) sb.append("_");
        }
        return sb.toString();
    }

    /** Build a set of candidate statue keys to try when performing runtime injection. */
    private static String[] buildStatueKeyCandidates(String raw) {
        if (raw == null) return new String[0];
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        String s = raw;
        // Strip namespace prefix if present (modid:typename)
        if (s.contains(":")) s = s.substring(s.indexOf(':') + 1);
        // Keep last path segment if a path is provided
        if (s.contains("/")) s = s.substring(s.lastIndexOf('/') + 1);
        // Strip extension
        if (s.endsWith(".json")) s = s.substring(0, s.length() - 5);
        // Add direct candidate(s)
        out.add(s);
        out.add(s.replace('-', '_'));
        out.add(s.replace('.', '_'));
        // Try common Ymmersive prefix variants
        if (!s.toLowerCase(Locale.ROOT).startsWith("ymmersive_statues_")) {
            out.add("Ymmersive_Statues_" + s);
            out.add("ymmersive_statues_" + s);
        }
        // Try matching any discovered available statue names by case-insensitive match
        for (String avail : AVAILABLE_STATUES) {
            if (avail.equalsIgnoreCase(s) || avail.toLowerCase(Locale.ROOT).endsWith(s.toLowerCase(Locale.ROOT))) {
                out.add(avail);
            }
        }
        return out.toArray(new String[0]);
    }

    // Delegate to shared ReflectionCache to avoid repeated lookups
    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) throws Exception {
        ReflectionCache.setField(clazz, target, fieldName, value);
    }
}
