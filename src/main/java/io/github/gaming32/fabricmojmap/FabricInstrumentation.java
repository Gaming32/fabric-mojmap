package io.github.gaming32.fabricmojmap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;

public class FabricInstrumentation implements ClassFileTransformer {
    private static final String FLI_NAME = "net/fabricmc/loader/impl/FabricLoaderImpl";
    private static final String KNOT_NAME = "net/fabricmc/loader/impl/launch/knot/Knot";
    private static final String MC_NAME = "net/fabricmc/loader/impl/launch/MappingConfiguration";
    private static final String FMB_NAME = "net/fabricmc/loader/impl/launch/FabricMixinBootstrap";
    private static final String MGP_NAME = "net/fabricmc/loader/impl/game/minecraft/MinecraftGameProvider";
    private static final String GPH_NAME = "net/fabricmc/loader/impl/game/GameProviderHelper";

    private final Map<String, Function<ClassNode, @Nullable TransformResult>> transformers = Util.mapOf(
        Util.mapEntry(FLI_NAME, this::transformFabricLoaderImpl),
        Util.mapEntry(KNOT_NAME, this::transformKnot),
        Util.mapEntry(MC_NAME, this::transformMappingConfiguration),
        Util.mapEntry(FMB_NAME, this::transformFabricMixinBootstrap),
        Util.mapEntry(MGP_NAME, this::transformMinecraftGameProvider),
        Util.mapEntry(GPH_NAME, this::transformGameProviderHelper)
    );

    @Override
    public byte[] transform(
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) {
        final Function<ClassNode, TransformResult> transformer = transformers.get(className);
        if (transformer == null) {
            return null;
        }
        FabricMojmap.info("Transforming " + className);
        try {
            final ClassReader reader = new ClassReader(classfileBuffer);
            final ClassNode node = new ClassNode();
            reader.accept(node, 0);
            final TransformResult transformResult = transformer.apply(node);
            if (transformResult == null) {
                return null;
            }
            final ClassWriter writer = new ClassWriter(transformResult.light ? reader : null, transformResult.writeFlags);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            System.err.println("Failed to transform " + className);
            t.printStackTrace();
            return null;
        }
    }

    private TransformResult transformFabricLoaderImpl(ClassNode clazz) {
        clazz.methods.stream().filter(m -> m.name.equals("setup")).forEach(method -> {
            final int targetVar = findVar(method, "remapRegularMods");
            if (targetVar == -1) return;
            final ListIterator<AbstractInsnNode> iter = method.instructions.iterator();
            while (iter.hasNext()) {
                final AbstractInsnNode insn = iter.next();
                if (!(insn instanceof VarInsnNode)) continue;
                final VarInsnNode varInsn = (VarInsnNode)insn;
                if (varInsn.getOpcode() == Opcodes.ISTORE && varInsn.var == targetVar) {
                    iter.add(new InsnNode(Opcodes.ICONST_1));
                    iter.add(new VarInsnNode(Opcodes.ISTORE, targetVar));
                    break;
                }
            }
        });
        return new TransformResult();
    }

    private TransformResult transformKnot(ClassNode clazz) {
        modifyTargetNamespace(clazz);
        return new TransformResult();
    }

    private TransformResult transformMappingConfiguration(ClassNode clazz) {
        modifyTargetNamespace(clazz);
        return new TransformResult();
    }

    private TransformResult transformFabricMixinBootstrap(ClassNode clazz) {
        clazz.methods.stream().filter(m -> m.name.equals("init")).forEach(method -> {
            final ListIterator<AbstractInsnNode> iter = method.instructions.iterator();
            while (iter.hasNext()) {
                final AbstractInsnNode insn = iter.next();
                if (!(insn instanceof MethodInsnNode)) continue;
                final MethodInsnNode methodInsn = (MethodInsnNode)insn;
                if (methodInsn.name.equals("isDevelopment")) {
                    iter.remove();
                    iter.add(new InsnNode(Opcodes.POP));
                    iter.add(new InsnNode(Opcodes.ICONST_1));
                }
            }
        });
        return new TransformResult();
    }

    private TransformResult transformMinecraftGameProvider(ClassNode clazz) {
        final TransformResult result = new TransformResult();
        clazz.methods.stream().filter(m -> m.name.equals("initialize")).forEach(method -> {
            final int obfJars = findVar(method, "obfJars");
            if (obfJars == -1) return;
            final ListIterator<AbstractInsnNode> iter = method.instructions.iterator();
            while (iter.hasNext()) {
                final AbstractInsnNode insn = iter.next();
                if (!(insn instanceof MethodInsnNode)) continue;
                final MethodInsnNode methodInsn = (MethodInsnNode)insn;
                if (methodInsn.name.equals("deobfuscate")) {
                    iter.add(new VarInsnNode(Opcodes.ALOAD, obfJars));
                    iter.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    iter.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    iter.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    iter.add(new FieldInsnNode(Opcodes.GETFIELD, MGP_NAME, "gameJars", "Ljava/util/List;"));
                    iter.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "io/github/gaming32/fabricmojmap/rt/IntermediaryRt",
                        "remapToIntermediary",
                        "(Ljava/util/Map;Lnet/fabricmc/loader/impl/game/GameProvider;Lnet/fabricmc/loader/impl/launch/FabricLauncher;Ljava/util/List;)V"
                    ));
                    result.writeFlags |= ClassWriter.COMPUTE_MAXS;
                    break;
                }
            }
        });
        return result;
    }

    private TransformResult transformGameProviderHelper(ClassNode clazz) {
        clazz.methods.stream().filter(m -> m.name.equals("deobfuscate")).forEach(method -> {
            final ListIterator<AbstractInsnNode> iter = method.instructions.iterator();
            while (iter.hasNext()) {
                final AbstractInsnNode insn = iter.next();
                if (!(insn instanceof MethodInsnNode)) continue;
                final MethodInsnNode methodInsn = (MethodInsnNode)insn;
                if (methodInsn.name.equals("getTargetNamespace")) {
                    iter.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "io/github/gaming32/fabricmojmap/rt/IntermediaryRt",
                        "getTargetNamespace",
                        "(Ljava/lang/String;)Ljava/lang/String;"
                    ));
                }
            }
        });
        return new TransformResult();
    }

    private static void modifyTargetNamespace(ClassNode clazz) {
        clazz.methods.stream().filter(m -> m.name.equals("getTargetNamespace")).forEach(method -> {
            LineNumberNode lineNode = null;
            for (final AbstractInsnNode insn : method.instructions) {
                if (insn instanceof LineNumberNode) {
                    lineNode = (LineNumberNode)insn;
                    break;
                }
            }
            method.instructions.clear();
            if (lineNode != null) {
                // Preserve line number
                method.instructions.add(lineNode);
            }
            method.instructions.add(new LdcInsnNode("named"));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
        });
    }

    private static int findVar(MethodNode method, String name) {
        return method.localVariables.stream()
            .filter(v -> v.name.equals(name))
            .mapToInt(v -> v.index)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing local variable " + name + " in " + method.name + method.desc));
    }

    private static class TransformResult {
        boolean light = true;
        int writeFlags = 0;

        TransformResult() {
        }

        TransformResult(boolean light, int writeFlags) {
            this.light = light;
            this.writeFlags = writeFlags;
        }
    }
}
