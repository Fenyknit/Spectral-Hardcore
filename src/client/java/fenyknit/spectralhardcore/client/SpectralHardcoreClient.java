package fenyknit.spectralhardcore.client;

// Server Registration Imports
import fenyknit.spectralhardcore.SpectralHardcore;
import fenyknit.spectralhardcore.network.DeathResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
// Respawn Screen Imports
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import net.fabricmc.api.ClientModInitializer;

public class SpectralHardcoreClient implements ClientModInitializer {
	
	private static Boolean canRespawn;

	@Override

	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof DeathScreen) {
				updateRespawnButton(screen);
				ScreenEvents.remove(screen).register(removed -> canRespawn = null);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(DeathResultPayload.TYPE, (payload, context) -> {
			canRespawn = payload.canRespawn();

			SpectralHardcore.LOGGER.info("Client received canRespawn={}", canRespawn);
			updateRespawnButton(context.client().gui.screen());
		});
	}
	
	private static void updateRespawnButton(Screen screen) {
		if (!(screen instanceof DeathScreen) || !Boolean.TRUE.equals(canRespawn)) {
			return;
		}

		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget instanceof Button button
				&& button.getMessage().getContents() instanceof TranslatableContents text
				&& "deathScreen.spectate".equals(text.getKey())) {
				button.setMessage(Component.translatable("deathScreen.respawn"));
				return;
			}
		}
	}
}