package com.meteoritecraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteoriteCraft implements ModInitializer {
	public static final String MOD_ID = "meteoritecraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();
		ModItemGroups.initialize();
		ModExtraItems.initialize();
		ModEvents.initialize();

		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> ModCommands.register(dispatcher));
		// Never let a tick exception take the whole game down: log it and keep playing.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			try {
				MeteorManager.tick(server);
			} catch (Throwable error) {
				LOGGER.error("[MeteoriteCraft] meteor tick failed", error);
			}
			try {
				BossManager.tick(server);
			} catch (Throwable error) {
				LOGGER.error("[MeteoriteCraft] boss tick failed", error);
			}
		});

		LOGGER.info("[MeteoriteCraft] loaded");
	}
}
