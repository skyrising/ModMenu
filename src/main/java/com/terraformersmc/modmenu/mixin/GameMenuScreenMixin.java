package com.terraformersmc.modmenu.mixin;

import com.terraformersmc.modmenu.event.ModMenuEventHandler;
import com.terraformersmc.modmenu.gui.ModsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
	@Inject(method = "init", at = @At("RETURN"))
	private void afterInit(CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = new Window(client);
		ModMenuEventHandler.afterScreenInit(client, this, window.getWidth(), window.getHeight());
	}

	@Inject(method = "buttonClicked", at = @At("RETURN"))
	private void onButtonClick(ButtonWidget button, CallbackInfo ci) {
		if (button.id == ModMenuEventHandler.MOD_MENU_BUTTON_ID) {
			client.openScreen(new ModsScreen(this));
		}
	}
}
