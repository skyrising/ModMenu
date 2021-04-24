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
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
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

public class DescriptionListWidget extends AlwaysSelectedEntryListWidget {
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
		return this.left + this.width / 2 - this.getRowWidth() / 2 + 2;
	}

	@Override
	protected int getScrollbarPositionX() {
		return this.width - 6 + left;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseY >= this.top && mouseY <= this.bottom && getRowLeft() <= mouseX && getRowLeft() + getRowWidth() > mouseX;
	}

	@Override
	protected int getItemCount() {
		return entries.size();
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		this.clampScrollAmount();
		ModListEntry selectedEntry = parent.getSelectedEntry();
		if (selectedEntry != lastSelected) {
			lastSelected = selectedEntry;
			entries.clear();
			scrollAmount = 0;
			if (lastSelected != null) {
				Mod mod = lastSelected.getMod();
				String description = mod.getDescription();
				String translatableDescriptionKey = "modmenu.descriptionTranslation." + mod.getId();
				if (I18n.hasTranslation(translatableDescriptionKey)) {
					description = I18n.translate(translatableDescriptionKey);
				}
				if (!description.isEmpty()) {
					for (String line : textRenderer.wrapStringToWidthAsList(description.replaceAll("\n", "\n\n"), getRowWidth() - 5)) {
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
		GlStateManager.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
		GlStateManager.disableAlphaTest();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(this.left, (this.top + 4), 0.0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.right, (this.top + 4), 0.0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.right, this.top, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.left, this.top, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.left, this.bottom, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.right, this.bottom, 0.0D).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.right, (this.bottom - 4), 0.0D).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.left, (this.bottom - 4), 0.0D).color(0, 0, 0, 0).next();
		tessellator.draw();

		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(this.left, this.bottom, 0.0D).color(0, 0, 0, 128).next();
		bufferBuilder.vertex(this.right, this.bottom, 0.0D).color(0, 0, 0, 128).next();
		bufferBuilder.vertex(this.right, this.top, 0.0D).color(0, 0, 0, 128).next();
		bufferBuilder.vertex(this.left, this.top, 0.0D).color(0, 0, 0, 128).next();
		tessellator.draw();

		int k = this.getRowLeft();
		int l = this.top + 4 - this.getScrollAmount();
		GlStateManager.enableTexture();
		this.renderList(k, l, mouseX, mouseY, delta);
		this.renderScrollBar(bufferBuilder, tessellator);

		GlStateManager.enableTexture();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableAlphaTest();
		GlStateManager.disableBlend();
	}

	public void renderScrollBar(BufferBuilder bufferBuilder, Tessellator tessellator) {
		int scrollbarStartX = this.getScrollbarPositionX();
		int scrollbarEndX = scrollbarStartX + 6;
		int maxScroll = this.getMaxScroll();
		if (maxScroll > 0) {
			int p = (this.bottom - this.top) * (this.bottom - this.top) / this.getMaxPosition();
			p = MathHelper.clamp(p, 32, this.bottom - this.top - 8);
			int q = (int)this.scrollAmount * (this.bottom - this.top - p) / maxScroll + this.top;
			if (q < this.top) {
				q = this.top;
			}

			bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
			bufferBuilder.vertex(scrollbarStartX, this.bottom, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarEndX, this.bottom, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarEndX, this.top, 0.0).color(0, 0, 0, 255).next();
			bufferBuilder.vertex(scrollbarStartX, this.top, 0.0).color(0, 0, 0, 255).next();
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
		public void method_29159(int i, int j, int y, float delta) {

		}

		@Override
		public void render(int index, int x, int y, int itemWidth, int itemHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
			if (widget.top > y || widget.bottom - textRenderer.lineHeight < y) {
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
