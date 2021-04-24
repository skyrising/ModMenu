package com.terraformersmc.modmenu.config.option;

import com.terraformersmc.modmenu.util.TranslationUtil;
import net.minecraft.client.resource.language.I18n;

public class BooleanConfigOption implements ConfigOption {
	private final String key, translationKey;
	private final boolean defaultValue;

	public BooleanConfigOption(String key, boolean defaultValue) {
		ConfigOptionStorage.setBoolean(key, defaultValue);
		this.key = key;
		this.translationKey = TranslationUtil.translationKeyOf("option", key);
		this.defaultValue = defaultValue;
	}

	@Override
	public String getKey() {
		return key;
	}

	public boolean getValue() {
		return ConfigOptionStorage.getBoolean(key);
	}

	public void setValue(boolean value) {
		ConfigOptionStorage.setBoolean(key, value);
	}

	public void toggleValue() {
		ConfigOptionStorage.toggleBoolean(key);
	}

	@Override
	public String getDisplayString() {
		return I18n.translate(translationKey, I18n.translate(translationKey + "." + ConfigOptionStorage.getBoolean(key)));
	}

	public boolean getDefaultValue() {
		return defaultValue;
	}
}
