package fenyknit.spectralhardcore.network;

import fenyknit.spectralhardcore.SpectralHardcore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeathResultPayload(boolean canRespawn) implements CustomPacketPayload {
    public static final Type<DeathResultPayload> TYPE =
        new Type<>(SpectralHardcore.id("death_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathResultPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL,
            DeathResultPayload::canRespawn,
            DeathResultPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}