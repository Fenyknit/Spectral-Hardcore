package fenyknit.spectralhardcore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fenyknit.spectralhardcore.SpectralHardcore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @ModifyExpressionValue(
        method = "handleClientCommand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;isHardcore()Z"
        )
    )
    private boolean spectralHardcore$shouldBecomeSpectator(boolean hardcore) {
        if (!hardcore) {
            return false;
        }

        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        boolean canRespawn = player.getAttachedOrElse(
            SpectralHardcore.CAN_RESPAWN_AFTER_DEATH, false
        );
        
        return !canRespawn;
    }
}