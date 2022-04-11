package com.terraformersmc.modmenu.util;

import com.google.gson.*;
import net.minecraft.client.resource.ResourceMetadataProvider;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.MetadataSerializer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModMenuResourcePack implements ResourcePack {
	private final Path root;

	public ModMenuResourcePack(Path root) {
		this.root = root;
	}

	private Path getPath(Identifier id) {
		return root.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
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
	public Set<String> getNamespaces() {
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
	public <T extends ResourceMetadataProvider> T parseMetadata(MetadataSerializer parser, String section) throws IOException {
		try {
			return parseJson(parser, this.openFile("pack.mcmeta"), section);
		} catch (NoSuchFileException e) {
			return null;
		}
	}

	static <T extends ResourceMetadataProvider> T parseJson(MetadataSerializer parser, InputStream inputStream, String section) {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			JsonObject jsonObject = (new JsonParser()).parse(bufferedReader).getAsJsonObject();
			return parser.fromJson(section, jsonObject);
		} catch (RuntimeException var9) {
			throw new JsonParseException(var9);
		} finally {
			IOUtils.closeQuietly(bufferedReader);
		}
	}

	@Override
	public BufferedImage getIcon() throws IOException {
		return TextureUtil.create(this.openFile("pack.png"));
	}

	@Override
	public String getName() {
		return "ModMenu";
	}
}
