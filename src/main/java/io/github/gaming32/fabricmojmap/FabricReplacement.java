package io.github.gaming32.fabricmojmap;

import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

public class FabricReplacement implements ClassFileTransformer {
    private static final Set<String> REPLACED_CLASSES = Util.setOf(
        "net/fabricmc/loader/impl/discovery/RuntimeModRemapper",
        "net/fabricmc/loader/impl/discovery/RuntimeModRemapper$1",
        "net/fabricmc/loader/impl/discovery/RuntimeModRemapper$RemapInfo"
    );

    @Override
    public byte[] transform(
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) {
        if (!REPLACED_CLASSES.contains(className)) {
            return null;
        }
        FabricMojmap.info("Replacing " + className);
        try (InputStream is = FabricMojmap.class.getResourceAsStream("embedded/" + className + ".class")) {
            return Util.readAllBytes(is);
        } catch (Throwable t) {
            System.err.println("Failed to replace " + className);
            t.printStackTrace();
            return null;
        }
    }
}
