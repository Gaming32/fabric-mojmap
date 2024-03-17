package io.github.gaming32.fabricmojmap;

import io.github.gaming32.fabricmojmap.libs.gson.JsonObject;
import io.github.gaming32.fabricmojmap.libs.gson.internal.Streams;
import io.github.gaming32.fabricmojmap.libs.gson.stream.JsonReader;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MappingsUtils {
    public static URL getVersionJsonUrl(String version) throws IOException {
        try (JsonReader reader = Util.newJsonReaderFromUrl("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("versions")) {
                    reader.skipValue();
                    continue;
                }
                reader.beginArray();
                while (reader.hasNext()) {
                    final JsonObject versionInfo = Streams.parse(reader).getAsJsonObject();
                    if (versionInfo.get("id").getAsString().equals(version)) {
                        return new URL(versionInfo.get("url").getAsString());
                    }
                }
                reader.endArray();
            }
            reader.endObject();
        }
        throw new IllegalStateException("Missing version URL for version " + version);
    }

    public static URL getMappingsUrl(URL versionJson) throws IOException {
        String version = null;
        try (JsonReader reader = Util.newJsonReader(versionJson.openStream())) {
            reader.beginObject();
            while (reader.hasNext()) {
                final String key = reader.nextName();
                if (key.equals("id")) {
                    version = reader.nextString();
                    continue;
                }
                if (!key.equals("downloads")) {
                    reader.skipValue();
                    continue;
                }
                reader.beginObject();
                while (reader.hasNext()) {
                    if (!reader.nextName().equals("client_mappings")) {
                        reader.skipValue();
                        continue;
                    }
                    reader.beginObject();
                    while (reader.hasNext()) {
                        if (!reader.nextName().equals("url")) {
                            reader.skipValue();
                            continue;
                        }
                        return new URL(reader.nextString());
                    }
                    reader.endObject();
                }
                reader.endObject();
            }
            reader.endObject();
        }
        throw new IllegalStateException("Missing mappings URL for version " + version);
    }

    public static MappingTree downloadMappings(URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            return readMappings(is);
        }
    }

    public static MappingTree readMappings(InputStream is) throws IOException {
        final VisitableMappingTree result = new MemoryMappingTree(true);
        MappingReader.read(new InputStreamReader(is, StandardCharsets.UTF_8), result);
        return result;
    }
}
