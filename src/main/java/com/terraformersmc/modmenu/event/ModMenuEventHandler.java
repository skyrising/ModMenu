package com.terraformersmc.modmenu.event;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.ModMenuButtonWidget;
import com.terraformersmc.modmenu.gui.widget.ModMenuTexturedButtonWidget;
import com.terraformersmc.modmenu.mixin.ButtonWidgetAccessor;
import com.terraformersmc.modmenu.mixin.ScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModMenuEventHandler {
	public static final int MOD_MENU_BUTTON_ID = "modmenu".hashCode();
	private static final Identifier FABRIC_ICON_BUTTON_LOCATION = new Identifier(ModMenu.MOD_ID, "textures/gui/mods_button.png");

	public static void register() {
		//ScreenEvents.AFTER_INIT.register(ModMenuEventHandler::afterScreenInit);
	}

	public static void afterScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof TitleScreen) {
			afterTitleScreenInit(screen);
		} else if (screen instanceof GameMenuScreen) {
			afterGameMenuScreenInit(screen);
		}
	}

	private static void afterTitleScreenInit(Screen screen) {
		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue()) {
			final List<ButtonWidget> buttons = ((ScreenAccessor) screen).getButtons();
			int modsButtonIndex = -1;
			final int spacing = 24;
			final int buttonsY = screen.height / 4 + 48;
			for (int i = 0; i < buttons.size(); i++) {
				ButtonWidget button = buttons.get(i);
				if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.CLASSIC) {
					shiftButtons(button, modsButtonIndex == -1, spacing);
				}
				if (buttonHasText(button, "menu.online")) {
					if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.REPLACE_REALMS) {
						buttons.set(i, new ModMenuButtonWidget(MOD_MENU_BUTTON_ID, button.x, button.y, button.getWidth(), ((ButtonWidgetAccessor) button).getHeight(), ModMenuApi.createModsButtonText()));
					} else {
						if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.SHRINK) {
							button.setWidth(98);
						}
						modsButtonIndex = i + 1;
					}
				}
			}
			if (modsButtonIndex != -1) {
				if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.CLASSIC) {
					buttons.add(modsButtonIndex, new ModMenuButtonWidget(MOD_MENU_BUTTON_ID, screen.width / 2 - 100, buttonsY + spacing * 3 - (spacing / 2), 200, 20, ModMenuApi.createModsButtonText()));
				} else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.SHRINK) {
					buttons.add(modsButtonIndex, new ModMenuButtonWidget(MOD_MENU_BUTTON_ID, screen.width / 2 + 2, buttonsY + spacing * 2, 98, 20, ModMenuApi.createModsButtonText()));
				} else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.ICON) {
					buttons.add(modsButtonIndex, new ModMenuTexturedButtonWidget(MOD_MENU_BUTTON_ID, screen.width / 2 + 104, buttonsY + spacing * 2, 20, 20, 0, 0, FABRIC_ICON_BUTTON_LOCATION, 32, 64, ModMenuApi.createModsButtonText().asFormattedString()));
				}
			}
		}
	}

	private static void afterGameMenuScreenInit(Screen screen) {
		if (ModMenuConfig.MODIFY_GAME_MENU.getValue()) {
			final List<ButtonWidget> buttons = ((ScreenAccessor) screen).getButtons();
			int modsButtonIndex = -1;
			final int spacing = 24;
			final int buttonsY = screen.height / 4 + 8;
			ModMenuConfig.ModsButtonStyle style = ModMenuConfig.MODS_BUTTON_STYLE.getValue().forGameMenu();
			for (int i = 0; i < buttons.size(); i++) {
				ButtonWidget button = buttons.get(i);
				if (style == ModMenuConfig.ModsButtonStyle.CLASSIC) {
					shiftButtons(button, modsButtonIndex == -1, spacing);
				}
				if (buttonHasText(button, "menu.reportBugs")) {
					modsButtonIndex = i + 1;
					if (style == ModMenuConfig.ModsButtonStyle.SHRINK) {
						buttons.set(i, new ModMenuButtonWidget(MOD_MENU_BUTTON_ID, button.x, button.y, button.getWidth(), ((ButtonWidgetAccessor) button).getHeight(), ModMenuApi.createModsButtonText()));
					} else {
						modsButtonIndex = i + 1;
					}
				}
			}
			if (modsButtonIndex != -1) {
				if (style == ModMenuConfig.ModsButtonStyle.CLASSIC) {
					buttons.add(modsButtonIndex, new ModMenuButtonWidget(MOD_MENU_BUTTON_ID, screen.width / 2 - 102, buttonsY + spacing * 3 - (spacing / 2), 204, 20, ModMenuApi.createModsButtonText()));
				} else if (style == ModMenuConfig.ModsButtonStyle.ICON) {
					buttons.add(modsButtonIndex, new ModMenuTexturedButtonWidget(MOD_MENU_BUTTON_ID, screen.width / 2 + 4 + 100 + 2, screen.height / 4 + 72 + -16, 20, 20, 0, 0, FABRIC_ICON_BUTTON_LOCATION, 32, 64, ModMenuApi.createModsButtonText().asFormattedString()));
				}
			}
		}
	}

	private static boolean buttonHasText(ButtonWidget button, String translationKey) {
		return button.message.equals(I18n.translate(translationKey));
	}

	private static void shiftButtons(ButtonWidget button, boolean shiftUp, int spacing) {
		if (shiftUp) {
			button.y -= spacing / 2;
		} else {
			button.y += spacing - (spacing / 2);
		}
	}
}
