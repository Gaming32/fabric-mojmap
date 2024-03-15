/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.loader.impl.discovery;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.lib.accesswidener.AccessWidener;
import net.fabricmc.loader.impl.lib.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.loader.impl.lib.accesswidener.AccessWidenerReader;
import net.fabricmc.loader.impl.lib.accesswidener.AccessWidenerRemapper;
import net.fabricmc.loader.impl.lib.accesswidener.AccessWidenerWriter;
import net.fabricmc.loader.impl.lib.tinyremapper.InputTag;
import net.fabricmc.loader.impl.lib.tinyremapper.NonClassCopyMode;
import net.fabricmc.loader.impl.lib.tinyremapper.OutputConsumerPath;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyRemapper;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.MixinExtension;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.mappings.TinyRemapperMappingsHelper;
import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class RuntimeModRemapper {
    private static final String REMAP_TYPE_MANIFEST_KEY = "Fabric-Loom-Mixin-Remap-Type";
    private static final String REMAP_TYPE_STATIC = "static";
    private static final String SOURCE_NAMESPACE = "intermediary";

    public static void remap(Collection<ModCandidate> modCandidates, Path tmpDir, Path outputDir) {
        List<ModCandidate> modsToRemap = new ArrayList<>();
        Set<InputTag> remapMixins = new HashSet<>();

        for (ModCandidate mod : modCandidates) {
            if (mod.getRequiresRemap()) {
                modsToRemap.add(mod);
            }
        }

        if (modsToRemap.isEmpty()) return;

        // Setter is per instance, despite defaultUseCaches being static
        URLConnection cachesSetter = null;
        try {
            cachesSetter = tmpDir.toUri().toURL().openConnection();
        } catch (IOException ignored) {
        }
        final boolean defaultUseCaches;
        if (cachesSetter != null) {
            defaultUseCaches = cachesSetter.getDefaultUseCaches();
            cachesSetter.setDefaultUseCaches(false);
        } else {
            defaultUseCaches = true;
        }

        Map<ModCandidate, RemapInfo> infoMap = new HashMap<>();

        TinyRemapper remapper = null;

        try {
            FabricLauncher launcher = FabricLauncherBase.getLauncher();

            AccessWidener mergedAccessWidener = new AccessWidener();
            mergedAccessWidener.visitHeader(SOURCE_NAMESPACE);

            for (ModCandidate mod : modsToRemap) {
                RemapInfo info = new RemapInfo();
                infoMap.put(mod, info);

                if (mod.hasPath()) {
                    List<Path> paths = mod.getPaths();
                    if (paths.size() != 1) throw new UnsupportedOperationException("multiple path for "+mod);

                    info.inputPath = paths.get(0);
                } else {
                    info.inputPath = mod.copyToDir(tmpDir, true);
                    info.inputIsTemp = true;
                }

                info.outputPath = outputDir.resolve(mod.getDefaultFileName());
                Files.deleteIfExists(info.outputPath);

                String accessWidener = mod.getMetadata().getAccessWidener();

                if (accessWidener != null) {
                    info.accessWidenerPath = accessWidener;

                    try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.inputPath, false)) {
                        FileSystem fs = jarFs.get();
                        info.sourceAccessWidener = Files.readAllBytes(fs.getPath(accessWidener));
                    } catch (Throwable t) {
                        throw new RuntimeException("Error reading access widener for mod '" +mod.getId()+ "'!", t);
                    }

                    new AccessWidenerReader(mergedAccessWidener).read(info.sourceAccessWidener, SOURCE_NAMESPACE);
                }
            }

            remapper = TinyRemapper.newRemapper()
                .withMappings(TinyRemapperMappingsHelper.create(launcher.getMappingConfiguration().getMappings(), SOURCE_NAMESPACE, launcher.getTargetNamespace()))
                .renameInvalidLocals(false)
                .extension(new MixinExtension(remapMixins::contains))
                .extraAnalyzeVisitor((mrjVersion, className, next) ->
                    AccessWidenerClassVisitor.createClassVisitor(FabricLoaderImpl.ASM_VERSION, next, mergedAccessWidener)
                )
                .build();

            try {
                remapper.readClassPathAsync(getRemapClasspath().toArray(new Path[0]));
            } catch (IOException e) {
                throw new RuntimeException("Failed to populate remap classpath", e);
            }

            for (ModCandidate mod : modsToRemap) {
                RemapInfo info = infoMap.get(mod);

                InputTag tag = remapper.createInputTag();
                info.tag = tag;

                if (requiresMixinRemap(info.inputPath)) {
                    remapMixins.add(tag);
                }

                remapper.readInputsAsync(tag, info.inputPath);
            }

            //Done in a 2nd loop as we need to make sure all the inputs are present before remapping
            for (ModCandidate mod : modsToRemap) {
                RemapInfo info = infoMap.get(mod);
                OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(info.outputPath).build();

                info.fileSystemDelegate = FileSystemUtil.getJarFileSystem(info.inputPath, false);

                if (info.fileSystemDelegate.get() == null) {
                    throw new RuntimeException("Could not open JAR file " + info.inputPath.getFileName() + " for NIO reading!");
                }

                Path inputJar = info.fileSystemDelegate.get().getRootDirectories().iterator().next();
                outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper);

                info.outputConsumerPath = outputConsumer;

                remapper.apply(outputConsumer, info.tag);
            }

            //Done in a 3rd loop as this can happen when the remapper is doing its thing.
            for (ModCandidate mod : modsToRemap) {
                RemapInfo info = infoMap.get(mod);

                if (info.sourceAccessWidener != null) {
                    info.accessWidener = remapAccessWidener(info.sourceAccessWidener, remapper.getEnvironment().getRemapper(), launcher.getTargetNamespace());
                }
            }

            remapper.finish();

            for (ModCandidate mod : modsToRemap) {
                RemapInfo info = infoMap.get(mod);

                info.outputConsumerPath.close();

                if (info.accessWidenerPath != null) {
                    try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.outputPath, false)) {
                        FileSystem fs = jarFs.get();

                        Files.delete(fs.getPath(info.accessWidenerPath));
                        Files.write(fs.getPath(info.accessWidenerPath), info.accessWidener);
                    }
                }

                mod.setPaths(Collections.singletonList(info.outputPath));
            }
        } catch (Throwable t) {
            if (remapper != null) {
                remapper.finish();
            }

            for (RemapInfo info : infoMap.values()) {
                if (info.outputPath == null) {
                    continue;
                }

                try {
                    Files.deleteIfExists(info.outputPath);
                } catch (IOException e) {
                    Log.warn(LogCategory.MOD_REMAP, "Error deleting failed output jar %s", info.outputPath, e);
                }
            }

            throw new FormattedException("Failed to remap mods!", t);
        } finally {
            for (RemapInfo info : infoMap.values()) {
                if (info.fileSystemDelegate != null) {
                    try {
                        info.fileSystemDelegate.close();
                    } catch (IOException e) {
                        Log.warn(LogCategory.MOD_REMAP, "Error closing input jar %s", info.inputPath, e);
                    }
                }
                if (info.inputIsTemp) {
                    try {
                        Files.deleteIfExists(info.inputPath);
                    } catch (IOException e) {
                        Log.warn(LogCategory.MOD_REMAP, "Error deleting temporary input jar %s", info.inputPath, e);
                    }
                }
            }

            if (cachesSetter != null) {
                cachesSetter.setDefaultUseCaches(defaultUseCaches);
            }
        }
    }

    private static byte[] remapAccessWidener(byte[] input, Remapper remapper, String targetNamespace) {
        AccessWidenerWriter writer = new AccessWidenerWriter();
        AccessWidenerRemapper remappingDecorator = new AccessWidenerRemapper(writer, remapper, SOURCE_NAMESPACE, targetNamespace);
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(remappingDecorator);
        accessWidenerReader.read(input, SOURCE_NAMESPACE);
        return writer.write();
    }

    private static List<Path> getRemapClasspath() throws IOException {
        String remapClasspathFile = System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE);

        if (remapClasspathFile == null) {
            throw new RuntimeException("No remapClasspathFile provided");
        }

        String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);

        return Arrays.stream(content.split(File.pathSeparator))
            .map(Paths::get)
            .collect(Collectors.toList());
    }

    private static boolean requiresMixinRemap(Path inputPath) throws IOException, URISyntaxException {
        final Manifest manifest = ManifestUtil.readManifest(inputPath.toUri().toURL());
        if (manifest == null) {
            return false;
        }
        final Attributes mainAttributes = manifest.getMainAttributes();
        return REMAP_TYPE_STATIC.equalsIgnoreCase(mainAttributes.getValue(REMAP_TYPE_MANIFEST_KEY));
    }

    private static class RemapInfo {
        InputTag tag;
        Path inputPath;
        Path outputPath;
        FileSystemUtil.FileSystemDelegate fileSystemDelegate;
        boolean inputIsTemp;
        OutputConsumerPath outputConsumerPath;
        String accessWidenerPath;
        byte[] sourceAccessWidener;
        byte[] accessWidener;
    }
}