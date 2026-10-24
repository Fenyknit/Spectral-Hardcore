package fenyknit.spectralhardcore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fenyknit.spectralhardcore.client.screen.SpectralHardcoreSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$MoreTab")
public abstract class CreateWorldMoreTabMixin {
    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/GridLayout;createRowHelper(I)Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;"
        )
    )
    private GridLayout.RowHelper spectralHardcore$addSettingsButton(
        GridLayout.RowHelper rows,
        CreateWorldScreen parent
    ) {
        rows.addChild(
            Button.builder(
                Component.literal("Spectral Hardcore"),
                button -> Minecraft.getInstance().gui.setScreen(
                    new SpectralHardcoreSettingsScreen(parent)
                )
            )
            .width(210)
            .build()
        );

        return rows;
    }
}