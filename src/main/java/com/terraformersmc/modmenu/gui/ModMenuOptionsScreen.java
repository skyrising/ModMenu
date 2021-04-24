package com.terraformersmc.modmenu.gui;

import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.config.ModMenuConfigManager;
import com.terraformersmc.modmenu.config.option.BooleanConfigOption;
import com.terraformersmc.modmenu.config.option.ConfigOption;
import com.terraformersmc.modmenu.config.option.ConfigOptionStorage;
import com.terraformersmc.modmenu.config.option.EnumConfigOption;
import net.minecraft.class_4247;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.resource.language.I18n;

import java.util.List;

public class ModMenuOptionsScreen extends Screen {
	private Screen previous;
	private String title;

	public ModMenuOptionsScreen(Screen previous) {
		this.previous = previous;
	}

	public void init() {
		this.title = I18n.translate("modmenu.options");
		ConfigOption[] options = ModMenuConfig.OPTIONS;
		for (int i = 0; i < options.length; i++) {
			ConfigOption option = options[i];
			this.buttons.add(new OptionButtonWidget(i, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), option.getDisplayString()));
		}
		this.buttons.add(new ButtonWidget(200, this.width / 2 - 100, this.height / 6 + 24 * ((options.length + 1) >> 1), I18n.translate("gui.done")));
	}

	@Override
	protected void charTyped(char c, int i) {
		if (i == 1) {
			ModMenuConfigManager.save();
		}

		super.charTyped(c, i);
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.active) {
			ConfigOption[] options = ModMenuConfig.OPTIONS;
			if (button.id < options.length && button instanceof OptionButtonWidget) {
				ConfigOption option = options[button.id];
				if (option instanceof EnumConfigOption) {
					((EnumConfigOption) option).cycleValue();
				} else if (option instanceof BooleanConfigOption) {
					((BooleanConfigOption) option).toggleValue();
				}
				button.message = option.getDisplayString();
			}

			if (button.id == 200) {
				ModMenuConfigManager.save();
				this.client.openScreen(this.previous);
			}
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		this.renderBackground();
		this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 20, 0xffffff);
		super.render(mouseX, mouseY, delta);
	}
}
