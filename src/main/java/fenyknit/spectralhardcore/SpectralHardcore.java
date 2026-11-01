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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;


// Data Attachement Imports
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.GlobalAttachments;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
// Server Registration Imports
import fenyknit.spectralhardcore.network.DeathResultPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpectralHardcore implements ModInitializer {
	public static final String MOD_ID = "spectral-hardcore";
	
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	public static final GameRule<Integer> MAX_LIVES = GameRuleBuilder
		.forInteger(1)
		.minValue(1)
		.category(GameRuleCategory.MISC)
		.buildAndRegister(id("max_lives"));
	
	public static final GameRule<Boolean> SHARED_LIVES = GameRuleBuilder
    .forBoolean(false)
    .category(GameRuleCategory.MISC)
    .buildAndRegister(id("shared_lives"));

	public static final GameRule<HardcoreMode> HARDCORE_MODE = GameRuleBuilder
	.forEnum(HardcoreMode.MULTIPLE_LIVES)
	.category(GameRuleCategory.MISC)
	.buildAndRegister(id("hardcore_mode"));
	
	public static final GameRule<Integer> HEARTS_LOST_PER_DEATH = GameRuleBuilder
	.forInteger(1)
	.minValue(1)
	.category(GameRuleCategory.MISC)
	.buildAndRegister(id("hearts_lost_per_death"));

	public static final AttachmentType<Integer> DEATH_COUNT = AttachmentRegistry.create(
    	id("death_count"),
    	builder -> builder
    	    .initializer(() -> 0) // Starts the player with zero deaths
    	    .persistent(Codec.INT) // Saves the player death count when exiting the world
    	    .copyOnDeath() // Preservers the current death count after player death
	);

	public static final AttachmentType<Double> MAX_HEALTH_LOST = AttachmentRegistry.create(
		id("max_health_lost"),
		builder -> builder
		.initializer(() -> 0.0 )
		.persistent(Codec.DOUBLE)
		.copyOnDeath()
	);

	public static final AttachmentType<Boolean> CAN_RESPAWN_AFTER_DEATH = AttachmentRegistry.create(
    	id("can_respawn_after_death"),
    	builder -> builder
		.persistent(Codec.BOOL)
		.copyOnDeath()
	);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
    		if (entity instanceof ServerPlayer player && player.level().getServer().isHardcore()) {
    			int individualDeaths = player.getAttachedOrCreate(DEATH_COUNT) + 1;
    			player.setAttached(DEATH_COUNT, individualDeaths);

				GlobalAttachments worldData = player.level().getServer().globalAttachments();
				int sharedDeaths = worldData.getAttachedOrCreate(DEATH_COUNT) + 1;
    			worldData.setAttached(DEATH_COUNT, sharedDeaths);

				// Calculating the health / live lost and its total
				HardcoreMode mode = player.level().getGameRules().get(HARDCORE_MODE);
				boolean canRespawn;

				if (mode == HardcoreMode.DECAYING_HARDCORE) {
					int heartsLost = player.level().getGameRules().get(HEARTS_LOST_PER_DEATH);
					double healthLostThisDeath = heartsLost * 2.0;
					double totalHealthLost = player.getAttachedOrCreate(MAX_HEALTH_LOST) + healthLostThisDeath;
				
				player.setAttached(MAX_HEALTH_LOST, totalHealthLost);

				double baseMaxHealth = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
				double remainingMaxHealth = baseMaxHealth - totalHealthLost;

				canRespawn = remainingMaxHealth > 0.0;
				} else {
					boolean sharedLives = player.level().getGameRules().get(SHARED_LIVES);
					int deaths = sharedLives ? sharedDeaths : individualDeaths;
					int maxLives = player.level().getGameRules().get(MAX_LIVES);

					canRespawn = deaths < maxLives;
				}
				// Save the decision of respawn so shared deaths after don't conflict with it
				player.setAttached(CAN_RESPAWN_AFTER_DEATH, canRespawn);

			if (ServerPlayNetworking.canSend(player, DeathResultPayload.TYPE)) {
				ServerPlayNetworking.send(player, new DeathResultPayload(canRespawn));
			}

			LOGGER.info("{}: mode {}; individual deaths{}; can respawn {}",
				player.getName().getString(), mode, individualDeaths, canRespawn);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!newPlayer.level().getServer().isHardcore()
				|| newPlayer.level().getGameRules().get(HARDCORE_MODE)
					!= HardcoreMode.DECAYING_HARDCORE) {
						return;
			}

			boolean canRespawn = oldPlayer.getAttachedOrElse(CAN_RESPAWN_AFTER_DEATH, false);

			if (!alive && !canRespawn) {
				return;
			}

			double healthLost = oldPlayer.getAttachedOrElse(MAX_HEALTH_LOST, 0.0);
			AttributeInstance maxHealth = newPlayer.getAttribute(Attributes.MAX_HEALTH);

			if (maxHealth == null) {
				return;
			}

			maxHealth.addOrReplacePermanentModifier(new AttributeModifier(
				id("decaying_health"), -healthLost, AttributeModifier.Operation.ADD_VALUE
			));
			
			if (!alive) {
				newPlayer.setHealth(newPlayer.getMaxHealth());
			}
		});

		PayloadTypeRegistry.clientboundPlay().register(
			DeathResultPayload.TYPE,
			DeathResultPayload.CODEC
		);
		// Sends the saved respawn decision when the player reconnects while still dead
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;

			if (server.isHardcore() && player.getHealth() <= 0.0F) {
				boolean canRespawn = player.getAttachedOrElse(CAN_RESPAWN_AFTER_DEATH, false);

				if (ServerPlayNetworking.canSend(player, DeathResultPayload.TYPE)) {
					ServerPlayNetworking.send(player, new DeathResultPayload(canRespawn));
				}
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
