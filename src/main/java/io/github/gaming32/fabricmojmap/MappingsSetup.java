package io.github.gaming32.fabricmojmap;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MappingTree;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class MappingsSetup {
    public static void prepareMappings(String version, Path mappingsJar) throws IOException {
        FabricMojmap.info("Preparing mappings...");
        final URL versionUrl = MappingsUtils.getVersionJsonUrl(version);
        FabricMojmap.info("Version URL: " + versionUrl);
        final URL mappingsUrl = MappingsUtils.getMappingsUrl(versionUrl);
        FabricMojmap.info("Mappings URL: " + mappingsUrl);
        final MappingTree mojmap = MappingsUtils.downloadMappings(mappingsUrl);
        FabricMojmap.info("Loaded " + mojmap.getClasses().size() + " Mojmap classes");

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Game-Id", "minecraft");
        manifest.getMainAttributes().putValue("Game-Version", version);

        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(mappingsJar), manifest)) {
            output.putNextEntry(new JarEntry("mappings/mappings.tiny"));
            try (Reader intermediaryInput = Util.newReader(FabricMojmap.class.getResourceAsStream("/mappings/mappings.tiny"))) {
                final MappingWriter writer = new Tiny2FileWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), false);
                final MappingNsCompleter completer = new MappingNsCompleter(writer, Util.mapOf(
                    Util.mapEntry("intermediary", "official"),
                    Util.mapEntry("named", "intermediary")
                ));
                MappingReader.read(intermediaryInput, new ForwardingMappingVisitor(completer) {
                    MappingTree.ClassMapping classMapping;
                    MappingTree.MemberMapping memberMapping;

                    @Override
                    public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
                        if (!srcNamespace.equals("official")) {
                            throw new IllegalArgumentException("Source namespace is not official");
                        }
                        if (!dstNamespaces.equals(Util.listOf("intermediary"))) {
                            throw new IllegalArgumentException("Target namespace is not intermediary");
                        }
                        super.visitNamespaces(srcNamespace, Util.listOf("intermediary", "named"));
                    }

                    @Override
                    public boolean visitClass(String srcName) throws IOException {
                        classMapping = mojmap.getClass(srcName, 0);
                        if (classMapping == null) {
                            throw new IllegalStateException("Missing class " + srcName + " in Mojmap");
                        }
                        return super.visitClass(srcName);
                    }

                    @Override
                    public boolean visitField(String srcName, String srcDesc) throws IOException {
                        memberMapping = classMapping.getField(srcName, srcDesc, 0);
                        if (memberMapping == null) {
                            throw new IllegalStateException("Missing field " + srcName + ":" + srcDesc + " in Mojmap");
                        }
                        return super.visitField(srcName, srcDesc);
                    }

                    @Override
                    public boolean visitMethod(String srcName, String srcDesc) throws IOException {
                        memberMapping = classMapping.getMethod(srcName, srcDesc, 0);
                        if (memberMapping == null) {
                            throw new IllegalStateException("Missing method " + srcName + srcDesc + " in Mojmap");
                        }
                        return super.visitMethod(srcName, srcDesc);
                    }

                    @Override
                    public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
                        super.visitDstName(targetKind, namespace, name);
                        if (namespace != 0) return;
                        if (targetKind.level > 1) {
                            super.visitDstName(targetKind, 1, name);
                            return;
                        }
                        if (targetKind == MappedElementKind.CLASS) {
                            super.visitDstName(targetKind, 1, classMapping.getSrcName());
                        } else {
                            super.visitDstName(targetKind, 1, memberMapping.getSrcName());
                        }
                    }

                    @Override
                    public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
                        super.visitDstDesc(targetKind, namespace, desc);
                        if (namespace != 0) return;
                        if (targetKind.level != 1) {
                            super.visitDstDesc(targetKind, 1, desc);
                            return;
                        }
                        super.visitDstDesc(targetKind, 1, memberMapping.getSrcDesc());
                    }
                });
            }
        }
        FabricMojmap.info("Prepared mappings");
    }
}
