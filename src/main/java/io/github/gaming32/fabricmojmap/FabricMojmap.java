package io.github.gaming32.fabricmojmap;

import com.google.gson.stream.JsonReader;
import net.lenni0451.reflect.ClassLoaders;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FabricMojmap {
    public static final Path CACHE_DIR = Paths.get(".fabric/runtime-mojmap");

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        Files.createDirectories(CACHE_DIR);
        final String minecraftVersion = getMinecraftVersion();
        final Path mappingsJar = CACHE_DIR.resolve("mappings-" + minecraftVersion + ".jar");
        info("Using mappings jar " + mappingsJar);
        if (!Files.isRegularFile(mappingsJar)) {
            try {
                MappingsSetup.prepareMappings(minecraftVersion, mappingsJar);
            } catch (Throwable t) {
                try {
                    Files.deleteIfExists(mappingsJar);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
        }
        ClassLoaders.loadToFront(mappingsJar.toUri().toURL());
        inst.addTransformer(new FabricReplacement());
        inst.addTransformer(new FabricInstrumentation());
//        Files.write(CLASSPATH_FILE, System.getProperty("java.class.path").getBytes(StandardCharsets.UTF_8));
//        System.setProperty(SystemProperties.REMAP_CLASSPATH_FILE, CLASSPATH_FILE.toString()); // Inlined, so reference is safe
    }

    public static String getMinecraftVersion() throws IOException {
        try (JsonReader reader = Util.newJsonReader(FabricMojmap.class.getResourceAsStream("/version.json"))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("id")) {
                    reader.skipValue();
                    continue;
                }
                return reader.nextString();
            }
            reader.endObject();
        }
        throw new IllegalStateException("Minecraft version.json missing version ID");
    }

    public static void info(String message) {
        System.out.println("[fabric-mojmap] " + message);
    }
}