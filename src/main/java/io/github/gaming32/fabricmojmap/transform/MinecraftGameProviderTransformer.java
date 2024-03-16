package io.github.gaming32.fabricmojmap.transform;

import io.github.gaming32.fabricmojmap.rt.IntermediaryRt;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@CTransformer(MinecraftGameProvider.class)
public class MinecraftGameProviderTransformer {
    @CShadow
    List<Path> gameJars;

    @CInject(
        method = "initialize",
        target = @CTarget(
            value = "INVOKE",
            target = "Lnet/fabricmc/loader/impl/game/GameProviderHelper;deobfuscate(Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;Ljava/nio/file/Path;Lnet/fabricmc/loader/impl/launch/FabricLauncher;)Ljava/util/Map;"
        )
    )
    private void remapToIntermediary(FabricLauncher launcher, @CLocalVariable Map<String, Path> obfJars) throws IOException {
        IntermediaryRt.remapToIntermediary(obfJars, (GameProvider)this, launcher, gameJars);
    }
}
