package io.github.gaming32.fabricmojmap.transform;

import com.google.common.base.Strings;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Quantifier;

@CTransformer(MemberInfo.class)
public class MemberInfoTransformer {
    @COverride
    public static MemberInfo parse(final String input, final ISelectorContext context) {
        String desc = null;
        String owner = null;
        String name = Strings.nullToEmpty(input).replaceAll("\\s", "");
        String tail = null;

        int arrowPos = name.indexOf("->");
        if (arrowPos > -1) {
            tail = name.substring(arrowPos + 2);
            name = name.substring(0, arrowPos);
        }

        if (context != null) {
            name = context.remap(name);
        }

        int parenPos = name.indexOf('(');
        int colonPos = name.indexOf(':');
        if (parenPos > -1) {
            desc = name.substring(parenPos);
            name = name.substring(0, parenPos);
        } else if (colonPos > -1) {
            desc = name.substring(colonPos + 1);
            name = name.substring(0, colonPos);
        }

        int lastDotPos = name.lastIndexOf('.');
        int semiColonPos = name.indexOf(';');
        if (lastDotPos > -1) {
            owner = name.substring(0, lastDotPos).replace('.', '/');
            name = name.substring(lastDotPos + 1);
        } else if (semiColonPos > -1 && name.startsWith("L")) {
            owner = name.substring(1, semiColonPos).replace('.', '/');
            name = name.substring(semiColonPos + 1);
        }

        if ((name.indexOf('/') > -1 || name.indexOf('.') > -1) && owner == null) {
            owner = name;
            name = "";
        }

        // Use default quantifier with negative max value. Used to indicate that
        // an explicit quantifier was not parsed from the selector string, this
        // allows us to provide backward-compatible behaviour for injection
        // points vs. selecting target members which have different default
        // semantics when omitting the quantifier. This is handled by consumers
        // calling configure() with SELECT_MEMBER or SELECT_INSTRUCTION to
        // promote the default case to a concrete case.
        Quantifier quantifier = Quantifier.DEFAULT;
        if (name.endsWith("*")) {
            quantifier = Quantifier.ANY;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("+")) {
            quantifier = Quantifier.PLUS;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("}")) {
            quantifier = Quantifier.NONE; // Assume invalid until quantifier is parsed
            int bracePos = name.indexOf("{");
            if (bracePos >= 0) {
                try {
                    quantifier = Quantifier.parse(name.substring(bracePos));
                    name = name.substring(0, bracePos);
                } catch (Exception ex) {
                    // Handled later in validate since matchCount will be 0
                }
            }
        } else if (name.contains("{")) {
            quantifier = Quantifier.NONE; // Probably incomplete quantifier
        }

        if (name.isEmpty()) {
            name = null;
        }

        return new MemberInfo(name, owner, desc, quantifier, tail, input);
    }
}
