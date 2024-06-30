package com.brov3r.help;

import com.avrix.commands.CommandsManager;
import com.avrix.plugin.Metadata;
import com.avrix.plugin.Plugin;

/**
 * Main entry point of the example plugin
 */
public class Main extends Plugin {
    private static Main instance;

    /**
     * Constructs a new {@link Plugin} with the specified metadata.
     * Metadata is transferred when the plugin is loaded into the game context.
     *
     * @param metadata The {@link Metadata} associated with this plugin.
     */
    public Main(Metadata metadata) {
        super(metadata);
    }

    /**
     * Called when the plugin is initialized.
     * <p>
     * Implementing classes should override this method to provide the initialization logic.
     */
    @Override
    public void onInitialize() {
        instance = this;
        loadDefaultConfig();

        CommandsManager.addCommand(new HelpCommand());
    }

    /**
     * Getting a plugin instance
     * @return plugin instance
     */
    public static Main getInstance(){
        return instance;
    }
}