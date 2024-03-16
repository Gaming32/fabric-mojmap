package io.github.gaming32.fabricmojmap.transform;

import io.github.gaming32.fabricmojmap.rt.IntermediaryRt;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CRedirect;

@CTransformer(GameProviderHelper.class)
public class GameProviderHelperTransformer {
    @CRedirect(
        method = "deobfuscate",
        target = @CTarget(
            value = "INVOKE",
            target = "Lnet/fabricmc/loader/impl/launch/MappingConfiguration;getTargetNamespace()Ljava/lang/String;"
        )
    )
    private static String updateTargetNamespace(MappingConfiguration instance) {
        return IntermediaryRt.getTargetNamespace(instance.getTargetNamespace());
    }
}
