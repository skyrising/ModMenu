package com.terraformersmc.modmenu.gui.widget;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.GlStateManager;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DescriptionListWidget extends EntryListWidget {
	private final List<DescriptionEntry> entries = new ArrayList<>();

	private final ModsScreen parent;
	private final TextRenderer textRenderer;
	private ModListEntry lastSelected = null;

	public DescriptionListWidget(MinecraftClient client, int width, int height, int top, int bottom, int entryHeight, ModsScreen parent) {
		super(client, width, height, top, bottom, entryHeight);
		this.parent = parent;
		this.textRenderer = client.textRenderer;
	}

	@Override
	public int getRowWidth() {
		return this.width - 10;
	}

	public int getRowLeft() {
		return this.xStart + this.width / 2 - this.getRowWidth() / 2 + 2;
	}

	@Override
	protected int getScrollbarPosition() {
		return this.width - 6 + xStart;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseY >= this.yStart && mouseY <= this.yEnd && getRowLeft() <= mouseX && getRowLeft() + getRowWidth() > mouseX;
	}

	@Override
	protected int getEntryCount() {
		return entries.size();
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		this.capYPosition();
		ModListEntry selectedEntry = parent.getSelectedEntry();
		if (selectedEntry != lastSelected) {
			lastSelected = selectedEntry;
			entries.clear();
			scrollAmount = 0;
			if (lastSelected != null) {
				Mod mod = lastSelected.getMod();
				String description = mod.getDescription();
				String translatableDescriptionKey = "modmenu.descriptionTranslation." + mod.getId();
				if (I18n.method_12500(translatableDescriptionKey)) {
					description = I18n.translate(translatableDescriptionKey);
				}
				if (!description.isEmpty()) {
					for (String line : textRenderer.wrapLines(description.replaceAll("\n", "\n\n"), getRowWidth() - 5)) {
						entries.add(new DescriptionEntry(line, this));
					}
				}

				Map<String, String> links = mod.getLinks();
				String sourceLink = mod.getSource();
				if ((!links.isEmpty() || sourceLink != null) && !ModMenuConfig.HIDE_MOD_LINKS.getValue()) {
					entries.add(new DescriptionEntry("", this));
					entries.add(new DescriptionEntry(I18n.translate("modmenu.links"), this));

					if (sourceLink != null) {
						entries.add(new LinkEntry(Formatting.RESET + "  " + Formatting.BLUE + Formatting.UNDERLINE + I18n.translate("modmenu.source"), sourceLink, this));
					}

					links.forEach((key, value) -> {
						entries.add(new LinkEntry(Formatting.RESET + "  " + Formatting.BLUE + Formatting.UNDERLINE + I18n.translate(key), value, this));
					});
				}

				Set<String> licenses = mod.getLicense();
				if (!ModMenuConfig.HIDE_MOD_LICENSE.getValue() && !licenses.isEmpty()) {
					entries.add(new DescriptionEntry("", this));
					entries.add(new DescriptionEntry(I18n.translate("modmenu.license"), this));

					for (String license : licenses) {
						entries.add(new DescriptionEntry("  " + license, this));
					}
				}

				if (!ModMenuConfig.HIDE_MOD_CREDITS.getValue()) {
					if ("minecraft".equals(mod.getId())) {
						entries.add(new DescriptionEntry("", this));
						entries.add(new MojangCreditsEntry(Formatting.BLUE.toString() + Formatting.UNDERLINE + I18n.translate("modmenu.viewCredits"), this));
					} else {
						List<String> authors = mod.getAuthors();
						List<String> contributors = mod.getContributors();
						if (!authors.isEmpty() || !contributors.isEmpty()) {
							entries.add(new DescriptionEntry("", this));
							entries.add(new DescriptionEntry(I18n.translate("modmenu.credits"), this));
							for (String author : authors) {
								entries.add(new DescriptionEntry("  " + author, this));
							}
							for (String contributor : contributors) {
								entries.add(new DescriptionEntry("  " + contributor, this));
							}
						}
					}
				}
			}
		}

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();

		GlStateManager.depthFunc(GL11.GL_LEQUAL);
		GlStateManager.disableDepthTest();
		GlStateManager.enableBlend();
		GlStateManager.method_12288(GlStateManager.class_2870.field_13525, GlStateManager.class_2866.field_13480, GlStateManager.class_2870.field_13528, GlStateManager.class_2866.field_13475);
		GlStateManager.disableAlphaTest();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(this.xStart, (this.yStart + 4), 0.0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.xEnd, (this.yStart + 4), 0.0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.xEnd, this.yStart, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.xStart, this.yStart, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.xStart, this.yEnd, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.xEnd, this.yEnd, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.xEnd, (this.yEnd - 4), 0.0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.xStart, (this.yEnd - 4), 0.0D).color(0, 0, 0, 0).next();
		tessellator.draw();

		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(this.xStart, this.yEnd, 0.0D).color(0, 0, 0, 128).next();
		bufferBuilder.vertex(this.xEnd, this.yEnd, 0.0D).color(0, 0, 0, 128).next();
		bufferBuilder.vertex(this.xEnd, this.yStart, 0.0D).color(0, 0, 0, 128).next();
		bufferBuilder.vertex(this.xStart, this.yStart, 0.0D).color(0, 0, 0, 128).next();
		tessellator.draw();

		int k = this.getRowLeft();
		int l = this.yStart + 4 - this.getScrollAmount();
		GlStateManager.enableTexture();
		this.method_6704(k, l, mouseX, mouseY, delta);
		this.renderScrollBar(bufferBuilder, tessellator);

		GlStateManager.enableTexture();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableAlphaTest();
		GlStateManager.disableBlend();
	}

	public void renderScrollBar(BufferBuilder bufferBuilder, Tessellator tessellator) {
		int scrollbarStartX = this.getScrollbarPosition();
		int scrollbarEndX = scrollbarStartX + 6;
		int maxScroll = this.getMaxScroll();
		if (maxScroll > 0) {
			int p = (this.yEnd - this.yStart) * (this.yEnd - this.yStart) / this.getMaxPosition();
			p = MathHelper.clamp(p, 32, this.yEnd - this.yStart - 8);
			int q = (int)this.scrollAmount * (this.yEnd - this.yStart - p) / maxScroll + this.yStart;
			if (q < this.yStart) {
				q = this.yStart;
			}

			bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
			bufferBuilder.vertex(scrollbarStartX, this.yEnd, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarEndX, this.yEnd, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarEndX, this.yStart, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarStartX, this.yStart, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarStartX, q + p, 0.0).color(128, 128, 128, 255).next();
			bufferBuilder.vertex(scrollbarEndX, q + p, 0.0).color(128, 128, 128, 255).next();
			bufferBuilder.vertex(scrollbarEndX, q, 0.0).color(128, 128, 128, 255).next();
			bufferBuilder.vertex(scrollbarStartX, q, 0.0).color(128, 128, 128, 255).next();
			bufferBuilder.vertex(scrollbarStartX, q + p - 1, 0.0D).color(192, 192, 192, 255).next();
			bufferBuilder.vertex(scrollbarEndX - 1, q + p - 1, 0.0D).color(192, 192, 192, 255).next();
			bufferBuilder.vertex(scrollbarEndX - 1, q, 0.0D).color(192, 192, 192, 255).next();
			bufferBuilder.vertex(scrollbarStartX, q, 0.0D).color(192, 192, 192, 255).next();
			tessellator.draw();
		}
	}

	@Override
	public Entry getEntry(int index) {
		return entries.get(index);
	}

	protected class DescriptionEntry implements Entry {
		private final DescriptionListWidget widget;
		protected String text;

		public DescriptionEntry(String text, DescriptionListWidget widget) {
			this.text = text;
			this.widget = widget;
		}

		@Override
		public void method_9473(int i, int j, int y, float delta) {

		}

		@Override
		public void method_6700(int index, int x, int y, int itemWidth, int itemHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
			if (widget.yStart > y || widget.yStart - textRenderer.fontHeight < y) {
				return;
			}
			textRenderer.drawWithShadow(text, x, y, 0xAAAAAA);
		}

		public boolean isMouseOver(int mouseX, int mouseY) {
			return widget.isMouseOver(mouseX, mouseY);
		}

		@Override
		public boolean mouseClicked(int i, int mouseX, int mouseY, int button, int j, int k) {
			return false;
		}

		@Override
		public void mouseReleased(int i, int mouseX, int mouseY, int button, int j, int k) {

		}
	}

	protected class MojangCreditsEntry extends DescriptionEntry {
		public MojangCreditsEntry(String text, DescriptionListWidget widget) {
			super(text, widget);
		}

		@Override
		public boolean mouseClicked(int i, int mouseX, int mouseY, int button, int j, int k) {
			if (isMouseOver(mouseX, mouseY)) {
				client.openScreen(new CreditsScreen(false, Runnables.doNothing()));
			}
			return super.mouseClicked(i, mouseX, mouseY, button, j, k);
		}
	}

	protected class LinkEntry extends DescriptionEntry {
		private final String link;

		public LinkEntry(String text, String link, DescriptionListWidget widget) {
			super(text, widget);
			this.link = link;
		}

		@Override
		public boolean mouseClicked(int i, int mouseX, int mouseY, int button, int j, int k) {
			if (isMouseOver(mouseX, mouseY)) {
				Style style = new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
				parent.handleTextClick(new LiteralText("").setStyle(style));
			}
			return super.mouseClicked(i, mouseX, mouseY, button, j, k);
		}
	}

}
