package com.kmgacdby.quickdash;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (main) entrypoint. This mod is client-only, but Fabric still wants
 * a "main" entrypoint declared, so this just sets up logging.
 */
public class QuickDashMod implements ModInitializer {
	public static final String MOD_ID = "quickdash";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[QuickDash] common init");
	}
}
