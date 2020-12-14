package io.github.prospector.modmenu.event;

import io.github.prospector.modmenu.ModMenu;
import io.github.prospector.modmenu.gui.ModMenuButtonWidget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.List;

public class ModMenuEventHandler {

	public static void register() {
		ScreenEvents.AFTER_INIT.register(ModMenuEventHandler::afterScreenInit);
	}

	public static void afterScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof TitleScreen) {
			afterTitleScreenInit(screen);
		} else if (screen instanceof GameMenuScreen) {
			afterGameMenuScreenInit(screen);
		}
	}

	private static void afterTitleScreenInit(Screen screen) {
		final List<AbstractButtonWidget> buttons = Screens.getButtons(screen);
		int modsButtonIndex = -1;
		final int spacing = 24;
		final int buttonsY = screen.height / 4 + 48;
		for (int i = 0; i < buttons.size(); i++) {
			AbstractButtonWidget button = buttons.get(i);
			shiftButtons(button, modsButtonIndex == -1, spacing);
			if (buttonHasText(button, "menu.online")) {
				modsButtonIndex = i + 1;
			}
		}
		if (modsButtonIndex != -1) {
			buttons.add(modsButtonIndex, new ModMenuButtonWidget(screen.width / 2 - 100, buttonsY + spacing * 3 - (spacing / 2), 200, 20, new TranslatableText("modmenu.title").append(new LiteralText(" ")).append(new TranslatableText("modmenu.loaded", ModMenu.getDisplayedModCount())), screen));
		}
	}

	private static void afterGameMenuScreenInit(Screen screen) {
		final List<AbstractButtonWidget> buttons = Screens.getButtons(screen);
		int modsButtonIndex = -1;
		final int spacing = 24;
		final int buttonsY = screen.height / 4 + 8;
		for (int i = 0; i < buttons.size(); i++) {
			AbstractButtonWidget button = buttons.get(i);
			shiftButtons(button, modsButtonIndex == -1, spacing);
			if (buttonHasText(button, "menu.reportBugs")) {
				modsButtonIndex = i + 1;
			}
		}
		if (modsButtonIndex != -1) {
			buttons.add(modsButtonIndex, new ModMenuButtonWidget(screen.width / 2 - 102, buttonsY + spacing * 3 - (spacing / 2), 204, 20, new TranslatableText("modmenu.title").append(new LiteralText(" ")).append(new TranslatableText("modmenu.loaded", ModMenu.getDisplayedModCount())), screen));
		}
	}

	private static boolean buttonHasText(AbstractButtonWidget button, String translationKey) {
		Text text = button.getMessage();
		return text instanceof TranslatableText && ((TranslatableText) text).getKey().equals(translationKey);
	}

	private static void shiftButtons(AbstractButtonWidget button, boolean shiftUp, int spacing) {
		if (shiftUp) {
			button.y -= spacing / 2;
		} else {
			button.y += spacing - (spacing / 2);
		}
	}
}
