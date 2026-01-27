//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel;

import com.Ev0sMods.Ev0sChisel.Chisel.Data;
import com.Ev0sMods.Ev0sChisel.Interactions.ChiselInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateRegistry;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class Ev0sChiselPlugin extends JavaPlugin {
    private static Ev0sChiselPlugin instance;

    public Ev0sChiselPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin loaded!");
    }

    public static Ev0sChiselPlugin getInstance() {
        return instance;
    }

    protected void setup() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin setup!");
        this.registerEvents();
        this.registerCommands();
    }

    protected void start() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin enabled!");
    }

    public void shutdown() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin disabled!");
    }

    private void registerEvents() {
        BlockStateRegistry blockStateRegistry = this.getBlockStateRegistry();
        blockStateRegistry.registerBlockState(Chisel.class, "Ev0sChisel", Chisel.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ChiselInteraction", ChiselInteraction.class, ChiselInteraction.CODEC);
    }

    private void registerCommands() {
    }
}
