package com.terraformersmc.modmenu.util;

import com.google.gson.*;
import net.fabricmc.loader.util.FileSystemUtil;
import net.minecraft.class_6055;
import net.minecraft.class_6057;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class ModMenuResourcePack implements ResourcePack {
	private final Path root;

	public ModMenuResourcePack(Path root) {
		this.root = root;
	}

	private Path getPath(Identifier id) {
		return root.resolve(Paths.get("assets", id.getNamespace(), id.getPath()));
	}

	private Path getPath(String file) {
		return root.resolve(file);
	}

	private Path translationFilePath(Identifier id) {
		String path = id.getPath();
		if (path.endsWith(".lang")) {
			Path filePath = getPath(new Identifier(id.getNamespace(), path.substring(0, path.length() - 5) + ".json"));
			if (Files.exists(filePath)) return filePath;
		}
		return null;
	}

	@Override
	public InputStream open(Identifier id) throws IOException {
		Path translationFile = translationFilePath(id);
		if (translationFile != null) {
			InputStream jsonInputStream = Files.newInputStream(translationFile);
			JsonObject obj = new Gson().fromJson(new InputStreamReader(jsonInputStream, StandardCharsets.UTF_8), JsonObject.class);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter printer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
			for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
				printer.print(e.getKey());
				printer.print('=');
				printer.println(e.getValue().getAsString());
			}
			printer.close();
			return new ByteArrayInputStream(baos.toByteArray());
		}
		return Files.newInputStream(getPath(id));
	}

	private InputStream openFile(String file) throws IOException {
		return Files.newInputStream(getPath(file));
	}

	@Override
	public boolean contains(Identifier id) {
		return Files.exists(getPath(id)) || translationFilePath(id) != null;
	}

	@Override
	public Set<String> method_31465() {
		try {
			return Files.list(getPath("assets")).filter(Files::isDirectory)
					.map(Path::getFileName)
					.map(Path::toString)
					.map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
					.collect(Collectors.toSet());
		} catch (IOException e) {
			return Collections.emptySet();
		}
	}

	@Nullable
	@Override
	public <T extends class_6055> T method_31461(class_6057 parser, String section) throws IOException {
		try {
			return parseJson(parser, this.openFile("pack.mcmeta"), section);
		} catch (NoSuchFileException e) {
			return null;
		}
	}

	static <T extends class_6055> T parseJson(class_6057 parser, InputStream inputStream, String section) {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			JsonObject jsonObject = (new JsonParser()).parse(bufferedReader).getAsJsonObject();
			return parser.method_31530(section, jsonObject);
		} catch (RuntimeException var9) {
			throw new JsonParseException(var9);
		} finally {
			IOUtils.closeQuietly(bufferedReader);
		}
	}

	@Override
	public BufferedImage method_31460() throws IOException {
		return TextureUtil.readImage(this.openFile("pack.png"));
	}

	@Override
	public String getName() {
		return "ModMenu";
	}
}
