package io.github.gaming32.fabricmojmap.rt;

import io.github.gaming32.fabricmojmap.FabricMojmap;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntermediaryRt {
    public static final Path CLASSPATH_FILE = FabricMojmap.CACHE_DIR.resolve("remap-classpath.txt");

    private static String forcedNamespace;

    public static void remapToIntermediary(
        Map<String, Path> obfJars, GameProvider gameProvider, FabricLauncher launcher, List<Path> gameJars
    ) throws IOException {
        forcedNamespace = "intermediary";
        final Map<String, Path> intermediaryJars = GameProviderHelper.deobfuscate(
            obfJars, gameProvider.getGameId(), gameProvider.getNormalizedGameVersion(), gameProvider.getLaunchDirectory(), launcher
        );
        forcedNamespace = null;
        final String classpath = Stream.concat(
            Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Paths::get)
                .filter(p -> !gameJars.contains(p)),
            intermediaryJars.values().stream()
        ).distinct()
            .map(Path::toAbsolutePath)
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
        Files.write(CLASSPATH_FILE, classpath.getBytes(StandardCharsets.UTF_8));
        System.setProperty(SystemProperties.REMAP_CLASSPATH_FILE, CLASSPATH_FILE.toString());
    }

    public static String getTargetNamespace(String namespace) {
        return forcedNamespace != null ? forcedNamespace : namespace;
    }
}
