package io.github.gaming32.fabricmojmap.rt;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RuntimeRemapperRt {
    public static IMappingProvider adaptMappingProvider(net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider loader) {
        return out -> loader.load(new net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider.MappingAcceptor() {
            @Override
            public void acceptClass(String srcName, String dstName) {
                out.acceptClass(srcName, dstName);
            }

            @Override
            public void acceptMethod(net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider.Member method, String dstName) {
                out.acceptMethod(new IMappingProvider.Member(method.owner, method.name, method.desc), dstName);
            }

            @Override
            public void acceptField(net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider.Member field, String dstName) {
                out.acceptField(new IMappingProvider.Member(field.owner, field.name, field.desc), dstName);
            }
        });
    }

    public static IMappingProvider createCompatMappings() {
        return out -> {
            //noinspection DataFlowIssue
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        RuntimeRemapperRt.class.getResourceAsStream("/fabric-mojmap-compat.tiny")
                    )
                )
            ) {
                TinyUtils.createTinyMappingProvider(reader, "intermediary", "named").load(out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
