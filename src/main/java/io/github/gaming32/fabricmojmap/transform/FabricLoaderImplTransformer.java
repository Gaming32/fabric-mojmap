package io.github.gaming32.fabricmojmap.transform;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CRedirect;

@CTransformer(FabricLoaderImpl.class)
public class FabricLoaderImplTransformer {
    @CRedirect(
        method = "setup",
        target = @CTarget(
            value = "INVOKE",
            target = "Lnet/fabricmc/loader/impl/FabricLoaderImpl;isDevelopmentEnvironment()Z"
        )
    )
    private boolean alwaysRemapMods(FabricLoaderImpl instance) {
        return true;
    }
}
