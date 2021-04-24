package com.terraformersmc.modmenu.gui;

import com.google.common.base.Joiner;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.config.ModMenuConfigManager;
import com.terraformersmc.modmenu.gui.widget.DescriptionListWidget;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.gui.widget.ModMenuTexturedButtonWidget;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.DrawingUtil;
import com.terraformersmc.modmenu.util.TranslationUtil;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModBadgeRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ModsScreen extends Screen {
	private static final int CONFIGURE_BUTTON = 1;
	private static final int WEBSITE_BUTTON = 2;
	private static final int ISSUES_BUTTON = 3;
	private static final int TOGGLE_FILTER_OPTIONS_BUTTON = 4;
	private static final int SORTING_BUTTON = 5;
	private static final int SHOW_LIBRARIES_BUTTON = 6;
	private static final int MODS_FOLDER_BUTTON = 7;
	private static final int DONE_BUTTON = 8;
	private static final int CONFIRM_DROP_CODE = 1000;

	private static final Identifier FILTERS_BUTTON_LOCATION = new Identifier(ModMenu.MOD_ID, "textures/gui/filters_button.png");
	private static final Identifier CONFIGURE_BUTTON_LOCATION = new Identifier(ModMenu.MOD_ID, "textures/gui/configure_button.png");

	private static final Logger LOGGER = LogManager.getLogger();

	private TextFieldWidget searchBox;
	private DescriptionListWidget descriptionListWidget;
	private final Screen previousScreen;
	private ModListWidget modList;
	private String tooltip;
	private ModListEntry selected;
	private ModBadgeRenderer modBadgeRenderer;
	private double scrollPercent = 0;
	private boolean init = false;
	private boolean filterOptionsShown = false;
	private int paneY;
	private int paneWidth;
	private int rightPaneX;
	private int searchBoxX;
	private int filtersX;
	private int filtersWidth;
	private int searchRowWidth;
	private int lastMouseX;
	private int lastMouseY;
	private List<Path> draggedPaths = Collections.emptyList();
	public final Set<String> showModChildren = new HashSet<>();

	public final Map<String, Screen> configScreenCache = new HashMap<>();
	private String title;
	private String toggleFilterOptionsText;
	private String configureText;

	public ModsScreen(Screen previousScreen) {
		this.previousScreen = previousScreen;
	}

	@Override
	public void handleMouseEvents() {
		super.handleMouseEvents();
		if (modList.isMouseOver(lastMouseX, lastMouseY)) {
			modList.handleMouseEvents();
		} else if (descriptionListWidget.isMouseOver(lastMouseX, lastMouseY)) {
			descriptionListWidget.handleMouseEvents();
		}
	}

	@Override
	public void tick() {
		this.searchBox.tick();
	}

	@Override
	public void init() {
		this.title = I18n.translate("modmenu.title");
		this.toggleFilterOptionsText = I18n.translate("modmenu.toggleFilterOptions");
		this.configureText = I18n.translate("modmenu.configure");
		Keyboard.enableRepeatEvents(true);
		paneY = 48;
		paneWidth = this.width / 2 - 8;
		rightPaneX = width - paneWidth;

		int searchBoxWidth = paneWidth - 32 - 22;
		searchBoxX = paneWidth / 2 - searchBoxWidth / 2 - 22 / 2;
		String oldText = Optional.ofNullable(searchBox).map(TextFieldWidget::getText).orElse("");
		this.searchBox = new TextFieldWidget(420, this.textRenderer, searchBoxX, 22, searchBoxWidth, 20);
		this.searchBox.setText(oldText);
		this.modList = new ModListWidget(this.client, paneWidth, this.height, paneY + 19, this.height - 36, ModMenuConfig.COMPACT_LIST.getValue() ? 23 : 36, this.searchBox.getText(), this.modList, this);
		this.modList.setLeftPos(0);
		modList.reloadFilters();

		for (Mod mod : ModMenu.MODS.values()) {
			if (!configScreenCache.containsKey(mod.getId())) {
				try {
					Screen configScreen = ModMenu.getConfigScreen(mod.getId(), this);
					configScreenCache.put(mod.getId(), configScreen);
				} catch (Throwable e) {
					LOGGER.error("Error from mod '" + mod.getId() + "'", e);
				}
			}
		}

		this.descriptionListWidget = new DescriptionListWidget(this.client, paneWidth, this.height, paneY + 60, this.height - 36, textRenderer.lineHeight + 1, this);
		this.descriptionListWidget.setLeftPos(rightPaneX);
		ButtonWidget configureButton = new ModMenuTexturedButtonWidget(CONFIGURE_BUTTON, width - 24, paneY, 20, 20, 0, 0, CONFIGURE_BUTTON_LOCATION, 32, 64, configureText) {
			@Override
			public void render(MinecraftClient client, int mouseX, int mouseY, float delta) {
				if (selected != null) {
					String modid = selected.getMod().getId();
					active = configScreenCache.get(modid) != null;
				} else {
					active = false;
				}
				visible = active;
				super.render(client, mouseX, mouseY, delta);
			}

			@Override
			public void renderToolTip(int mouseX, int mouseY) {
				if (this.isHovered()) {
					ModsScreen.this.renderTooltip(configureText, mouseX, mouseY);
				}
			}

			@Override
			public void renderBg(MinecraftClient client, int mouseX, int mouseY) {
				GlStateManager.color4f(1, 1, 1, 1f);
				super.renderBg(client, mouseX, mouseY);
			}
		};
		int urlButtonWidths = paneWidth / 2 - 2;
		int cappedButtonWidth = Math.min(urlButtonWidths, 200);
		ButtonWidget websiteButton = new ButtonWidget(WEBSITE_BUTTON, rightPaneX + (urlButtonWidths / 2) - (cappedButtonWidth / 2), paneY + 36, Math.min(urlButtonWidths, 200), 20,
				I18n.translate("modmenu.website")) {
			@Override
			public void render(MinecraftClient client, int mouseX, int mouseY, float delta) {
				visible = selected != null;
				active = visible && selected.getMod().getWebsite() != null;
				super.render(client, mouseX, mouseY, delta);
			}
		};
		ButtonWidget issuesButton = new ButtonWidget(ISSUES_BUTTON, rightPaneX + urlButtonWidths + 4 + (urlButtonWidths / 2) - (cappedButtonWidth / 2), paneY + 36, Math.min(urlButtonWidths, 200), 20,
				I18n.translate("modmenu.issues")) {
			@Override
			public void render(MinecraftClient client, int mouseX, int mouseY, float delta) {
				visible = selected != null;
				active = visible && selected.getMod().getIssueTracker() != null;
				super.render(client, mouseX, mouseY, delta);
			}
		};
		this.addButton(new ModMenuTexturedButtonWidget(TOGGLE_FILTER_OPTIONS_BUTTON, paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 2, 22, 20, 20, 0, 0, FILTERS_BUTTON_LOCATION, 32, 64, toggleFilterOptionsText) {
			@Override
			public void renderToolTip(int mouseX, int mouseY) {
				if (this.isHovered()) {
					ModsScreen.this.renderTooltip(toggleFilterOptionsText, mouseX, mouseY);
				}
			}
		});
		String showLibrariesText = ModMenuConfig.SHOW_LIBRARIES.getDisplayString();
		String sortingText = ModMenuConfig.SORTING.getDisplayString();
		int showLibrariesWidth = textRenderer.getWidth(showLibrariesText) + 20;
		int sortingWidth = textRenderer.getWidth(sortingText) + 20;
		filtersWidth = showLibrariesWidth + sortingWidth + 2;
		searchRowWidth = searchBoxX + searchBoxWidth + 22;
		updateFiltersX();
		this.addButton(new ButtonWidget(SORTING_BUTTON, filtersX, 45, sortingWidth, 20, sortingText) {
			@Override
			public void render(MinecraftClient client, int mouseX, int mouseY, float delta) {
				visible = filterOptionsShown;
				message = ModMenuConfig.SORTING.getDisplayString();
				super.render(client, mouseX, mouseY, delta);
			}
		});
		this.addButton(new ButtonWidget(SHOW_LIBRARIES_BUTTON, filtersX + sortingWidth + 2, 45, showLibrariesWidth, 20, showLibrariesText) {
			@Override
			public void render(MinecraftClient client, int mouseX, int mouseY, float delta) {
				visible = filterOptionsShown;
				message = ModMenuConfig.SHOW_LIBRARIES.getDisplayString();
				super.render(client, mouseX, mouseY, delta);
			}
		});
		if (!ModMenuConfig.HIDE_CONFIG_BUTTONS.getValue()) {
			this.addButton(configureButton);
		}
		this.addButton(websiteButton);
		this.addButton(issuesButton);
		this.addButton(new ButtonWidget(MODS_FOLDER_BUTTON, this.width / 2 - 154, this.height - 28, 150, 20, I18n.translate("modmenu.modsFolder")));
		this.addButton(new ButtonWidget(DONE_BUTTON, this.width / 2 + 4, this.height - 28, 150, 20, I18n.translate("gui.done")));
		this.searchBox.onFocusChanged(true);

		init = true;
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		switch (button.id) {
			case CONFIGURE_BUTTON: {
				final String modid = Objects.requireNonNull(selected).getMod().getId();
				final Screen screen = configScreenCache.get(modid);
				if (screen != null) {
					client.openScreen(screen);
				} else {
					button.active = false;
				}
				break;
			}
			case WEBSITE_BUTTON: {
				final Mod mod = Objects.requireNonNull(selected).getMod();
				Style style = new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, mod.getWebsite()));
				this.handleTextClick(new LiteralText("").setStyle(style));
				break;
			}
			case ISSUES_BUTTON: {
				final Mod mod = Objects.requireNonNull(selected).getMod();
				Style style = new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, mod.getIssueTracker()));
				this.handleTextClick(new LiteralText("").setStyle(style));
				break;
			}
			case TOGGLE_FILTER_OPTIONS_BUTTON: {
				filterOptionsShown = !filterOptionsShown;
				break;
			}
			case SORTING_BUTTON: {
				ModMenuConfig.SORTING.cycleValue();
				ModMenuConfigManager.save();
				modList.reloadFilters();
				break;
			}
			case SHOW_LIBRARIES_BUTTON: {
				ModMenuConfig.SHOW_LIBRARIES.toggleValue();
				ModMenuConfigManager.save();
				modList.reloadFilters();
				break;
			}
			case MODS_FOLDER_BUTTON: {
				GLX.openFile(new File(FabricLoader.getInstance().getGameDirectory(), "mods"));
				break;
			}
			case DONE_BUTTON: {
				client.openScreen(previousScreen);
				break;
			}
		}
	}

	@Override
	public void confirm(boolean open, int code) {
		if (code == CONFIRM_DROP_CODE) {
			if (open) {
				Path modsDirectory = FabricLoader.getInstance().getGameDir().resolve("mods");

				// Filter out none mods
				List<Path> mods = draggedPaths.stream()
						.filter(ModsScreen::isFabricMod)
						.collect(Collectors.toList());

				if (mods.isEmpty()) {
					return;
				}

				String modList = mods.stream()
						.map(Path::getFileName)
						.map(Path::toString)
						.collect(Collectors.joining(", "));
				boolean allSuccessful = true;

				for (Path path : mods) {
					try {
						Files.copy(path, modsDirectory.resolve(path.getFileName()));
					} catch (IOException e) {
						LOGGER.warn("Failed to copy mod from {} to {}", path, modsDirectory.resolve(path.getFileName()));
						//SystemToast.addPackCopyFailure(client, path.toString());
						allSuccessful = false;
						break;
					}
				}

				if (allSuccessful) {
					SystemToast.show(client.getToastManager(), SystemToast.Type.TUTORIAL_HINT, new TranslatableText("modmenu.dropSuccessful.line1"), new TranslatableText("modmenu.dropSuccessful.line2"));
				}
			}
			this.draggedPaths = Collections.emptyList();
			this.client.openScreen(this);
		} else {
			super.confirm(open, code);
		}
	}

	@Override
	public void charTyped(char char_1, int int_1) {
		this.searchBox.charTyped(char_1, int_1);
		this.modList.reloadFilters();
	}

	@Override
	protected void mouseClicked(int x, int y, int mouseButton) {
		super.mouseClicked(x, y, mouseButton);
		if (modList.isMouseOver(x, y)) modList.mouseClicked(x, y, mouseButton);
		else if (descriptionListWidget.isMouseOver(x, y)) descriptionListWidget.mouseClicked(x, y, mouseButton);
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		this.lastMouseX = mouseX;
		this.lastMouseY = mouseY;
		this.renderBackground();
		this.tooltip = null;
		ModListEntry selectedEntry = selected;
		if (selectedEntry != null) {
			this.descriptionListWidget.render(mouseX, mouseY, delta);
		}
		this.modList.render(mouseX, mouseY, delta);
		this.searchBox.render();
		GlStateManager.disableBlend();
		drawCenteredString(this.textRenderer, this.title, this.modList.getWidth() / 2, 8, 16777215);
		drawCenteredString(this.textRenderer, Formatting.GRAY + I18n.translate("modmenu.dropInfo.1"), this.width - this.modList.getWidth() / 2, paneY / 2 - client.textRenderer.lineHeight - 1, 16777215);
		drawCenteredString(this.textRenderer, Formatting.GRAY + I18n.translate("modmenu.dropInfo.2"), this.width - this.modList.getWidth() / 2, paneY / 2 + 1, 16777215);
		String fullModCount = computeModCountText(true);
		if (updateFiltersX()) {
			if (filterOptionsShown) {
				if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || textRenderer.getWidth(fullModCount) <= filtersX - 5) {
					textRenderer.draw(fullModCount, searchBoxX, 52, 0xFFFFFF);
				} else {
					textRenderer.draw(computeModCountText(false), searchBoxX, 46, 0xFFFFFF);
					textRenderer.draw(computeLibraryCountText(), searchBoxX, 57, 0xFFFFFF);
				}
			} else {
				if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || textRenderer.getWidth(fullModCount) <= modList.getWidth() - 5) {
					textRenderer.draw(fullModCount, searchBoxX, 52, 0xFFFFFF);
				} else {
					textRenderer.draw(computeModCountText(false), searchBoxX, 46, 0xFFFFFF);
					textRenderer.draw(computeLibraryCountText(), searchBoxX, 57, 0xFFFFFF);
				}
			}
		}
		if (selectedEntry != null) {
			Mod mod = selectedEntry.getMod();
			int x = rightPaneX;
			if ("java".equals(mod.getId())) {
				DrawingUtil.drawRandomVersionBackground(mod, x, paneY, 32, 32);
			}
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.selected.bindIconTexture();
			GlStateManager.enableBlend();
			drawTexture(x, paneY, 0.0F, 0.0F, 32, 32, 32, 32);
			GlStateManager.disableBlend();
			int lineSpacing = textRenderer.lineHeight + 1;
			int imageOffset = 36;
			String trimmedName = mod.getName();
			int maxNameWidth = this.width - (x + imageOffset);
			if (textRenderer.getWidth(trimmedName) > maxNameWidth) {
				String ellipsis = "...";
				trimmedName = textRenderer.trimToWidth(trimmedName, maxNameWidth - textRenderer.getWidth(ellipsis)) + ellipsis;
			}
			textRenderer.draw(trimmedName, x + imageOffset, paneY + 1, 0xFFFFFF);
			if (mouseX > x + imageOffset && mouseY > paneY + 1 && mouseY < paneY + 1 + textRenderer.lineHeight && mouseX < x + imageOffset + textRenderer.getWidth(trimmedName)) {
				setTooltip(I18n.translate("modmenu.modIdToolTip", mod.getId()));
			}
			if (init || modBadgeRenderer == null || modBadgeRenderer.getMod() != mod) {
				modBadgeRenderer = new ModBadgeRenderer(x + imageOffset + Objects.requireNonNull(this.client).textRenderer.getWidth(trimmedName) + 2, paneY, width - 28, selectedEntry.mod, this);
				init = false;
			}
			if (!ModMenuConfig.HIDE_BADGES.getValue()) {
				modBadgeRenderer.draw(mouseX, mouseY);
			}
			if (mod.isReal()) {
				textRenderer.draw("v" + mod.getVersion(), x + imageOffset, paneY + 2 + lineSpacing, 0x808080);
			}
			String authors;
			List<String> names = mod.getAuthors();

			if (!names.isEmpty()) {
				if (names.size() > 1) {
					authors = Joiner.on(", ").join(names);
				} else {
					authors = names.get(0);
				}
				DrawingUtil.drawWrappedString(I18n.translate("modmenu.authorPrefix", authors), x + imageOffset, paneY + 2 + lineSpacing * 2, paneWidth - imageOffset - 4, 1, 0x808080);
			}
		}
		super.render(mouseX, mouseY, delta);
		if (this.tooltip != null) {
			this.renderOrderedTooltip(textRenderer.wrapStringToWidthAsList(this.tooltip, Integer.MAX_VALUE), mouseX, mouseY);
		}
	}

	private String computeModCountText(boolean includeLibs) {
		int[] rootMods = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> !mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));

		if (includeLibs && ModMenuConfig.SHOW_LIBRARIES.getValue()) {
			int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));
			return TranslationUtil.translateNumeric("modmenu.showingModsLibraries", rootMods, rootLibs).toFormattedString();
		} else {
			return TranslationUtil.translateNumeric("modmenu.showingMods", rootMods).toFormattedString();
		}
	}

	private String computeLibraryCountText() {
		if (ModMenuConfig.SHOW_LIBRARIES.getValue()) {
			int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));
			return TranslationUtil.translateNumeric("modmenu.showingLibraries", rootLibs).toFormattedString();
		} else {
			return null;
		}
	}

	private int[] formatModCount(Set<String> set) {
		int visible = modList.getDisplayedCountFor(set);
		int total = set.size();
		if (visible == total) {
			return new int[]{total};
		}
		return new int[]{visible, total};
	}

	private void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public ModListEntry getSelectedEntry() {
		return selected;
	}

	public void updateSelectedEntry(ModListEntry entry) {
		if (entry != null) {
			this.selected = entry;
		}
	}

	public double getScrollPercent() {
		return scrollPercent;
	}

	public void updateScrollPercent(double scrollPercent) {
		this.scrollPercent = scrollPercent;
	}

	public String getSearchInput() {
		return searchBox.getText();
	}

	private boolean updateFiltersX() {
		if ((filtersWidth + textRenderer.getWidth(computeModCountText(true)) + 20) >= searchRowWidth && ((filtersWidth + textRenderer.getWidth(computeModCountText(false)) + 20) >= searchRowWidth || (filtersWidth + textRenderer.getWidth(computeLibraryCountText()) + 20) >= searchRowWidth)) {
			filtersX = paneWidth / 2 - filtersWidth / 2;
			return !filterOptionsShown;
		} else {
			filtersX = searchRowWidth - filtersWidth + 1;
			return true;
		}
	}

	public void filesDragged(List<Path> paths) {
		this.draggedPaths = paths;
		this.client.openScreen(new ConfirmScreen(this, I18n.translate("modmenu.dropConfirm"), String.valueOf(modList), CONFIRM_DROP_CODE));
	}

	private static boolean isFabricMod(Path mod) {
		try (JarFile jarFile = new JarFile(mod.toFile())) {
			return jarFile.getEntry("fabric.mod.json") != null;
		} catch (IOException e) {
			return false;
		}
	}

	public Map<String, Screen> getConfigScreenCache() {
		return configScreenCache;
	}
}
