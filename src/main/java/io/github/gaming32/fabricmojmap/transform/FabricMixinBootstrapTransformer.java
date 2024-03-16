package io.github.gaming32.fabricmojmap.transform;

import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricMixinBootstrap;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CRedirect;

@CTransformer(FabricMixinBootstrap.class)
public class FabricMixinBootstrapTransformer {
    @CRedirect(
        method = "init",
        target = @CTarget(
            value = "INVOKE",
            target = "Lnet/fabricmc/loader/impl/launch/FabricLauncher;isDevelopment()Z"
        )
    )
    private static boolean alwaysRemap(FabricLauncher instance) {
        return true;
    }
}
