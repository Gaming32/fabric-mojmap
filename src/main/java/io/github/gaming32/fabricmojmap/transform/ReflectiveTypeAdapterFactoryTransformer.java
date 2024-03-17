package io.github.gaming32.fabricmojmap.transform;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import io.github.gaming32.fabricmojmap.rt.GsonRt;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CRedirect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CTransformer(ReflectiveTypeAdapterFactory.class)
public class ReflectiveTypeAdapterFactoryTransformer {
    @CShadow
    private FieldNamingStrategy fieldNamingPolicy;

    @SuppressWarnings("unchecked")
    @CRedirect(
        method = "getFieldNames",
        target = @CTarget(
            value = "INVOKE",
            target = "Ljava/util/Collections;singletonList(Ljava/lang/Object;)Ljava/util/List;",
            ordinal = 0
        )
    )
    private <T> List<T> includeIntermediaryNames(T o, @CLocalVariable Field f) {
        final MappingTree.FieldMapping mapping = FabricLauncherBase.getLauncher()
            .getMappingConfiguration()
            .getMappings()
            .getField(
                f.getDeclaringClass().getName().replace('.', '/'),
                f.getName(), GsonRt.toDescriptor(f.getType()),
                1
            );
        if (mapping == null) {
            return Collections.singletonList(o);
        }

        final List<T> result = new ArrayList<>();
        result.add(o);

        String intermediaryName = mapping.getName(0);
        assert intermediaryName != null;
        if (fieldNamingPolicy instanceof FieldNamingPolicy) {
            switch ((FieldNamingPolicy)fieldNamingPolicy) {
                case IDENTITY:
                    break;
                case UPPER_CAMEL_CASE:
                    intermediaryName = GsonRt.upperCaseFirstLetter(intermediaryName);
                    break;
                case UPPER_CAMEL_CASE_WITH_SPACES:
                    intermediaryName = GsonRt.upperCaseFirstLetter(GsonRt.separateCamelCase(intermediaryName, ' '));
                    break;
                case UPPER_CASE_WITH_UNDERSCORES:
                    intermediaryName = GsonRt.separateCamelCase(intermediaryName, '_').toUpperCase(Locale.ENGLISH);
                    break;
                case LOWER_CASE_WITH_UNDERSCORES:
                    intermediaryName = GsonRt.separateCamelCase(intermediaryName, '_').toLowerCase(Locale.ENGLISH);
                    break;
                case LOWER_CASE_WITH_DASHES:
                    intermediaryName = GsonRt.separateCamelCase(intermediaryName, '-').toLowerCase(Locale.ENGLISH);
                    break;
                case LOWER_CASE_WITH_DOTS:
                    intermediaryName = GsonRt.separateCamelCase(intermediaryName, '.').toLowerCase(Locale.ENGLISH);
                    break;
            }
        }
        result.add((T)intermediaryName);

        return result;
    }
}
