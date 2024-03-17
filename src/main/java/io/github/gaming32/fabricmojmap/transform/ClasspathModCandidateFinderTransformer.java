package io.github.gaming32.fabricmojmap.transform;

import io.github.gaming32.fabricmojmap.Util;
import net.fabricmc.loader.impl.discovery.ClasspathModCandidateFinder;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CASM;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ListIterator;

@CTransformer(ClasspathModCandidateFinder.class)
public class ClasspathModCandidateFinderTransformer {
    @CASM("findCandidates")
    private static void injectMod(MethodNode node) {
        final ListIterator<AbstractInsnNode> iter = node.instructions.iterator();
        Util.advancePastField(iter, "LOADER_CODE_SOURCE");
        Util.advancePastMethod(iter, "accept");
        iter.add(new VarInsnNode(Opcodes.ALOAD, 1));
        iter.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "io/github/gaming32/fabricmojmap/rt/ModInjectRt",
            "getModCodeSource",
            "()Ljava/nio/file/Path;"
        ));
        iter.add(new InsnNode(Opcodes.ICONST_0));
        iter.add(new MethodInsnNode(
            Opcodes.INVOKEINTERFACE,
            "net/fabricmc/loader/impl/discovery/ModCandidateFinder$ModCandidateConsumer",
            "accept",
            "(Ljava/nio/file/Path;Z)V"
        ));
    }
}
