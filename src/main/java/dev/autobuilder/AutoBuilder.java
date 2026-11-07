package dev.autobuilder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoBuilder implements ModInitializer {
    public static final String MOD_ID = "autobuilder";
    public static final String MOD_NAME = "AutoBuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Initializing AutoBuilder mod...", MOD_NAME);
    }
}
