package com.terraformersmc.modmenu.mixin;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.event.ModMenuEventHandler;
import com.terraformersmc.modmenu.gui.ModsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;setScreenBounds(II)V"), index = 1)
	private int adjustRealmsHeight(int height) {
		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.CLASSIC) {
			return height - 51;
		} else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.REPLACE_REALMS || ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.SHRINK) {
			return -99999;
		}
		return height;
	}

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

	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
	private String onRender(String string) {
		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnTitleScreen()) {
			String count = ModMenu.getDisplayedModCount();
			String newString = I18n.translate("modmenu.mods.a", count);
			String countKey = "modmenu.mods." + count;
			if ("69".equals(count) && ModMenuConfig.EASTER_EGGS.getValue()) {
				newString = I18n.translate(countKey + ".nice", count);
			} else if (I18n.method_12500(countKey)) {
				newString = I18n.translate(countKey, count);
			}
			return string.replace(I18n.translate(I18n.translate("menu.modded")), newString);
		}
		return string;
	}
}
