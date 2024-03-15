package io.github.gaming32.fabricmojmap.rt;

import net.fabricmc.tinyremapper.IMappingProvider;

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
}
