package fenyknit.spectralhardcore;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

// Entity and Server Player Imports
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
// Gamerule Imports
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
// Data Attachement Imports
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
// Server Registration Imports
import fenyknit.spectralhardcore.network.DeathResultPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpectralHardcore implements ModInitializer {
	public static final String MOD_ID = "spectral-hardcore";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	public static final GameRule<Integer> MAX_LIVES = GameRuleBuilder
		.forInteger(1)
		.minValue(1)
		.category(GameRuleCategory.MISC)
		.buildAndRegister(id("max_lives"));
	
	public static final AttachmentType<Integer> DEATH_COUNT = AttachmentRegistry.create(
    	id("death_count"),
    	builder -> builder
    	    .initializer(() -> 0) // Starts the player with zero deaths
    	    .persistent(Codec.INT) // Saves the player death count when exiting the world
    	    .copyOnDeath() // Preservers the current death count after player death
	);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
    		if (entity instanceof ServerPlayer player && player.level().getServer().isHardcore()) {
    			int deaths = player.getAttachedOrCreate(DEATH_COUNT) + 1;
    			player.setAttached(DEATH_COUNT, deaths);

    			int maxLives = player.level().getGameRules().get(MAX_LIVES);
				boolean canRespawn = deaths < maxLives;

			if (ServerPlayNetworking.canSend(player, DeathResultPayload.TYPE)) {
				ServerPlayNetworking.send(player, new DeathResultPayload(canRespawn));
			}
			// Death Log Info Countdown
			LOGGER.info("{}: death {}/{}; can respawn: {}",
				player.getName().getString(), deaths, maxLives, canRespawn);
			}
		});

		PayloadTypeRegistry.clientboundPlay().register(
			DeathResultPayload.TYPE,
			DeathResultPayload.CODEC
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
