package io.github.gaming32.fabricmojmap.transform;

import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;

@CTransformer({Knot.class, MappingConfiguration.class})
public class TargetNamespaceTransformer {
    @COverride
    public String getTargetNamespace() {
        return "named";
    }
}
