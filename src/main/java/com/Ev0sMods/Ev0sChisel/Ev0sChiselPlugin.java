package com.Ev0sMods.Ev0sChisel;

import com.Ev0sMods.Ev0sChisel.Interactions.ChiselInteraction;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateRegistry;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Main plugin class.
 *
 * TODO: Implement your plugin logic here.
 *
 * @author YourName
 * @version 1.0.0
 */
public class Ev0sChiselPlugin extends JavaPlugin {

    private static Ev0sChiselPlugin instance;

    /**
     * Constructor - Called when plugin is loaded.
     */
    public Ev0sChiselPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin loaded!");
    }

    /**
     * Get plugin instance.
     */
    public static Ev0sChiselPlugin getInstance() {
        return instance;
    }

    /**
     * Called when plugin is set up.
     */
    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin setup!");

        // TODO: Initialize your plugin here
        // - Load configuration
        // - Register event listeners
        // - Register commands
        // - Start services
        registerEvents();
        registerCommands();
    }

    /**
     * Called when plugin is enabled.
     */
    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin enabled!");
    }

    /**
     * Called when plugin is disabled.
     */
    @Override
    public void shutdown() {
        getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin disabled!");

        // TODO: Cleanup your plugin here
        // - Save data
        // - Stop services
        // - Close connections
    }

    /**
     * Register your commands here.
     */
    private void registerEvents() {
        final BlockStateRegistry blockStateRegistry = this.getBlockStateRegistry();
        blockStateRegistry.registerBlockState(Chisel.class, "Ev0sChisel", Chisel.CODEC, Chisel.Data.class, Chisel.Data.CHISELCODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ChiselInteraction", ChiselInteraction.class, ChiselInteraction.CODEC);

    }

    /**
     * Register your commands here.
     */
    private void registerCommands() {

    }

}
