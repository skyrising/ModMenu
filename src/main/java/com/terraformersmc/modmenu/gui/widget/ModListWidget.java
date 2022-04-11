package com.terraformersmc.modmenu.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.entries.ChildEntry;
import com.terraformersmc.modmenu.gui.widget.entries.IndependentEntry;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.gui.widget.entries.ParentEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModIconHandler;
import com.terraformersmc.modmenu.util.mod.ModSearch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ModListWidget extends EntryListWidget {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final boolean DEBUG = Boolean.getBoolean("modmenu.debug");

	private final ModsScreen parent;
	private final List<ModListEntry> entries = new ArrayList<>();
	private ModListEntry selected;
	private List<Mod> mods = null;
	private final Set<Mod> addedMods = new HashSet<>();
	private String selectedModId = null;
	private boolean scrolling;
	private final ModIconHandler iconHandler = new ModIconHandler();

	public ModListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, String searchTerm, ModListWidget list, ModsScreen parent) {
		super(client, width, height, y1, y2, entryHeight);
		this.parent = parent;
		if (list != null) {
			this.mods = list.mods;
		}
		this.filter(searchTerm, false);
		setScrollAmount(parent.getScrollPercent() * Math.max(0, this.getMaxPosition() - (this.yEnd - this.yStart - 4)));
	}

	public void setScrollAmount(double amount) {
		scrollAmount = (float) amount;
		capYPosition();
		int denominator = Math.max(0, this.getMaxPosition() - (this.yEnd - this.yStart - 4));
		if (denominator <= 0) {
			parent.updateScrollPercent(0);
		} else {
			parent.updateScrollPercent(getScrollAmount() / Math.max(0, this.getMaxPosition() - (this.yEnd - this.yStart - 4)));
		}
	}

	protected boolean isFocused() {
		return false; //return parent.getFocused() == this;
	}

	public void select(ModListEntry entry) {
		this.setSelected(entry);
	}

	public void setSelected(ModListEntry entry) {
		selected = entry;
		selectedModId = entry.getMod().getId();
		parent.updateSelectedEntry(entry);
	}

	protected boolean isSelectedItem(int index) {
		return selected != null && selected.getMod().getId().equals(entries.get(index).getMod().getId());
	}

	public void addEntry(ModListEntry entry) {
		if (addedMods.contains(entry.mod)) return;
		addedMods.add(entry.mod);
		entries.add(entry);
		if (entry.getMod().getId().equals(selectedModId)) {
			setSelected(entry);
		}
	}

	protected boolean removeEntry(ModListEntry entry) {
		addedMods.remove(entry.mod);
		return entries.remove(entry);
	}

	protected ModListEntry remove(int index) {
		addedMods.remove(entries.get(index).mod);
		return entries.remove(index);
	}

	public void reloadFilters() {
		filter(parent.getSearchInput(), true, false);
	}


	public void filter(String searchTerm, boolean refresh) {
		filter(searchTerm, refresh, true);
	}

	private void filter(String searchTerm, boolean refresh, boolean search) {
		entries.clear();
		addedMods.clear();
		Collection<Mod> mods = ModMenu.MODS.values().stream().filter(mod -> !ModMenuConfig.HIDDEN_MODS.getValue().contains(mod.getId())).collect(Collectors.toSet());

		if (DEBUG) {
			mods = new ArrayList<>(mods);
//			mods.addAll(TestModContainer.getTestModContainers());
		}

		if (this.mods == null || refresh) {
			this.mods = new ArrayList<>();
			this.mods.addAll(mods);
			this.mods.sort(ModMenuConfig.SORTING.getValue().getComparator());
		}

		List<Mod> matched = ModSearch.search(parent, searchTerm, this.mods);

		for (Mod mod : matched) {
			String modId = mod.getId();

			//Hide parent lib mods when the config is set to hide
			if (mod.getBadges().contains(Mod.Badge.LIBRARY) && !ModMenuConfig.SHOW_LIBRARIES.getValue()) {
				continue;
			}

			if (!ModMenu.PARENT_MAP.values().contains(mod)) {
				if (ModMenu.PARENT_MAP.keySet().contains(mod)) {
					//Add parent mods when not searching
					List<Mod> children = ModMenu.PARENT_MAP.get(mod);
					children.sort(ModMenuConfig.SORTING.getValue().getComparator());
					ParentEntry parent = new ParentEntry(mod, children, this);
					this.addEntry(parent);
					//Add children if they are meant to be shown
					if (this.parent.showModChildren.contains(modId)) {
						List<Mod> validChildren = ModSearch.search(this.parent, searchTerm, children);
						for (Mod child : validChildren) {
							this.addEntry(new ChildEntry(child, parent, this, validChildren.indexOf(child) == validChildren.size() - 1));
						}
					}
				} else {
					//A mod with no children
					this.addEntry(new IndependentEntry(mod, this));
				}
			}
		}

		if (parent.getSelectedEntry() != null && !entries.isEmpty() || this.selected != null && selected.getMod() != parent.getSelectedEntry().getMod()) {
			for (ModListEntry entry : entries) {
				if (entry.getMod().equals(parent.getSelectedEntry().getMod())) {
					setSelected(entry);
				}
			}
		} else {
			if (selected == null && !entries.isEmpty() && getEntry(0) != null) {
				setSelected(entries.get(0));
			}
		}

		if (getScrollAmount() > Math.max(0, this.getMaxPosition() - (this.yEnd - this.yStart - 4))) {
			setScrollAmount(Math.max(0, this.getMaxPosition() - (this.yEnd - this.yStart - 4)));
		}
	}


	@Override
	protected void method_6704(int x, int y, int mouseX, int mouseY, float delta) {
		int itemCount = this.getEntryCount();
		Tessellator tessellator_1 = Tessellator.getInstance();
		BufferBuilder buffer = tessellator_1.getBuffer();

		for (int index = 0; index < itemCount; ++index) {
			int entryTop = this.getRowTop(index) + 2;
			int entryBottom = this.getRowTop(index) + this.entryHeight;
			if (entryBottom >= this.yStart && entryTop <= this.yEnd) {
				int entryHeight = this.entryHeight - 4;
				ModListEntry entry = entries.get(index);
				int rowWidth = this.getRowWidth();
				int entryLeft;
				if (this.isSelectedItem(index)) {
					entryLeft = getRowLeft() - 2 + entry.getXOffset();
					int selectionRight = x + rowWidth + 2;
					GlStateManager.disableTexture();
					float float_2 = this.isFocused() ? 1.0F : 0.5F;
					GlStateManager.color4f(float_2, float_2, float_2, 1.0F);
					buffer.begin(7, VertexFormats.POSITION);
					buffer.vertex(entryLeft, entryTop + entryHeight + 2, 0.0F).next();
					buffer.vertex(selectionRight, entryTop + entryHeight + 2, 0.0F).next();
					buffer.vertex(selectionRight, entryTop - 2, 0.0F).next();
					buffer.vertex(entryLeft, entryTop - 2, 0.0F).next();
					tessellator_1.draw();
					GlStateManager.color4f(0.0F, 0.0F, 0.0F, 1.0F);
					buffer.begin(7, VertexFormats.POSITION);
					buffer.vertex(entryLeft + 1, entryTop + entryHeight + 1, 0.0F).next();
					buffer.vertex(selectionRight - 1, entryTop + entryHeight + 1, 0.0F).next();
					buffer.vertex(selectionRight - 1, entryTop - 1, 0.0F).next();
					buffer.vertex(entryLeft + 1, entryTop - 1, 0.0F).next();
					tessellator_1.draw();
					GlStateManager.enableTexture();
				}

				entryLeft = this.getRowLeft();
				entry.method_6700(index, entryTop, entryLeft, rowWidth, entryHeight, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPos(mouseX, mouseY), entry), delta);
			}
		}

	}

	protected void updateScrollingState(int mouseX, int mouseY, int button) {
		this.scrolling = button == 0 && mouseX >= (double) this.getScrollbarPosition() && mouseX < (double) (this.getScrollbarPosition() + 6);
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		this.updateScrollingState(mouseX, mouseY, button);
		if (!this.isMouseOver(mouseX, mouseY)) return false;
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		return this.scrolling;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseY >= this.yStart && mouseY <= this.yEnd && getRowLeft() <= mouseX && getRowLeft() + getRowWidth() > mouseX;
	}

	@Override
	public Entry getEntry(int index) {
		return entries.get(index);
	}

	public final ModListEntry getEntryAtPos(int x, int y) {
		int int_5 = MathHelper.floor(y - (double) this.yStart) - this.headerHeight + this.getScrollAmount() - 4;
		int index = int_5 / this.entryHeight;
		return x < this.getScrollbarPosition() && x >= getRowLeft() && x <= (getRowLeft() + getRowWidth()) && index >= 0 && int_5 >= 0 && index < this.getEntryCount() ? entries.get(index) : null;
	}

	@Override
	protected int getScrollbarPosition() {
		return this.width - 6;
	}

	@Override
	public int getRowWidth() {
		return this.width - (Math.max(0, this.getMaxPosition() - (this.yEnd - this.yStart - 4)) > 0 ? 18 : 12);
	}

	public int getRowLeft() {
		return xStart + 6;
	}

	protected int getRowTop(int index) {
		return this.yStart + 4 - this.getScrollAmount() + index * this.entryHeight + this.headerHeight;
	}

	public int getWidth() {
		return width;
	}

	public int getTop() {
		return this.yStart;
	}

	public ModsScreen getParent() {
		return parent;
	}

	@Override
	protected int getEntryCount() {
		return entries.size();
	}

	@Override
	protected int getMaxPosition() {
		return super.getMaxPosition() + 4;
	}

	public int getDisplayedCountFor(Set<String> set) {
		int count = 0;
		for (ModListEntry c : entries) {
			if (set.contains(c.getMod().getId())) {
				count++;
			}
		}
		return count;
	}

	public ModIconHandler getIconHandler() {
		return iconHandler;
	}
}
