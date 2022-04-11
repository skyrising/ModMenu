package com.terraformersmc.modmenu.util;

import com.terraformersmc.modmenu.util.mod.Mod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class DrawingUtil {
	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

	public static void drawRandomVersionBackground(Mod mod, int x, int y, int width, int height){
		int seed = mod.getName().hashCode() + mod.getVersion().hashCode();
		DrawableHelper.fill(x, y, x + width, y + height, 0xFF000000 + new Random(seed).nextInt(0xFFFFFF));
	}

	public static void drawWrappedString(String string, int x, int y, int wrapWidth, int lines, int color) {
		CLIENT.textRenderer.drawTrimmed(string, x, y, wrapWidth, color);
	}

	public static void drawBadge(int x, int y, int tagWidth, String text, int outlineColor, int fillColor, int textColor) {
		DrawableHelper.fill(x + 1, y - 1, x + tagWidth, y, outlineColor);
		DrawableHelper.fill(x, y, x + 1, y + CLIENT.textRenderer.fontHeight, outlineColor);
		DrawableHelper.fill(x + 1, y + 1 + CLIENT.textRenderer.fontHeight - 1, x + tagWidth, y + CLIENT.textRenderer.fontHeight + 1, outlineColor);
		DrawableHelper.fill(x + tagWidth, y, x + tagWidth + 1, y + CLIENT.textRenderer.fontHeight, outlineColor);
		DrawableHelper.fill(x + 1, y, x + tagWidth, y + CLIENT.textRenderer.fontHeight, fillColor);
		CLIENT.textRenderer.draw(text, (x + 1 + (tagWidth - CLIENT.textRenderer.getStringWidth(text)) / (float) 2), y + 1, textColor, false);
	}
}
