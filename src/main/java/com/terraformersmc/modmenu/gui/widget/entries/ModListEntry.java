package com.terraformersmc.modmenu.gui.widget.entries;

import com.mojang.blaze3d.platform.GlStateManager;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.util.DrawingUtil;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModBadgeRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModListEntry implements AlwaysSelectedEntryListWidget.Entry {
	public static final Identifier UNKNOWN_ICON = new Identifier("textures/misc/unknown_pack.png");
	private static final Logger LOGGER = LogManager.getLogger();

	protected final MinecraftClient client;
	public final Mod mod;
	protected final ModListWidget list;
	protected Identifier iconLocation;
	protected static final int FULL_ICON_SIZE = 32;
	protected static final int COMPACT_ICON_SIZE = 19;

	public ModListEntry(Mod mod, ModListWidget list) {
		this.mod = mod;
		this.list = list;
		this.client = MinecraftClient.getInstance();
	}

	@Override
	public void method_29159(int i, int j, int y, float delta) {

	}

	@Override
	public void render(int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		x += getXOffset();
		rowWidth -= getXOffset();
		int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
		if ("java".equals(mod.getId())) {
			DrawingUtil.drawRandomVersionBackground(mod, x, y, iconSize, iconSize);
		}
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.bindIconTexture();
		GlStateManager.enableBlend();
		DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
		GlStateManager.disableBlend();
		String trimmedName = mod.getName();
		int maxNameWidth = rowWidth - iconSize - 3;
		TextRenderer font = this.client.textRenderer;
		if (font.getWidth(trimmedName) > maxNameWidth) {
			String ellipsis = "...";
			trimmedName = font.trimToWidth(trimmedName, maxNameWidth - font.getWidth(ellipsis)) + ellipsis;
		}
		font.draw(trimmedName, x + iconSize + 3, y + 1, 0xFFFFFF);
		if (!ModMenuConfig.HIDE_BADGES.getValue()) {
			new ModBadgeRenderer(x + iconSize + 3 + font.getWidth(trimmedName) + 2, y, x + rowWidth, mod, list.getParent()).draw(mouseX, mouseY);
		}
		if (!ModMenuConfig.COMPACT_LIST.getValue()) {
			String summary = mod.getSummary();
			String translatableSummaryKey = "modmenu.summaryTranslation." + mod.getId();
			String translatableDescriptionKey = "modmenu.descriptionTranslation." + mod.getId();
			if (I18n.hasTranslation(translatableSummaryKey)) {
				summary = I18n.translate(translatableSummaryKey);
			} else if (I18n.hasTranslation(translatableDescriptionKey)) {
				summary = I18n.translate(translatableDescriptionKey);
			}
			DrawingUtil.drawWrappedString(summary, (x + iconSize + 3 + 4), (y + client.textRenderer.lineHeight + 2), rowWidth - iconSize - 7, 2, 0x808080);
		} else {
			DrawingUtil.drawWrappedString("v" + mod.getVersion(), (x + iconSize + 3), (y + client.textRenderer.lineHeight + 2), rowWidth - iconSize - 7, 2, 0x808080);
		}
	}

	@Override
	public boolean mouseClicked(int i, int mouseX, int mouseY, int button, int j, int k) {
		list.select(this);
		return true;
	}

	@Override
	public void mouseReleased(int i, int mouseX, int mouseY, int button, int j, int k) {

	}

	public Mod getMod() {
		return mod;
	}

	public void bindIconTexture() {
		if (this.iconLocation == null) {
			this.iconLocation = new Identifier(ModMenu.MOD_ID, mod.getId() + "_icon");
			NativeImageBackedTexture icon = mod.getIcon(list.getIconHandler(), 64 * MinecraftClient.getInstance().options.guiScale);
			if (icon != null) {
				this.client.getTextureManager().registerTexture(this.iconLocation, icon);
			} else {
				this.iconLocation = UNKNOWN_ICON;
			}
		}
		this.client.getTextureManager().bindTexture(this.iconLocation);
	}

	public int getXOffset() {
		return 0;
	}
}
