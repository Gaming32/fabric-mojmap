package io.github.gaming32.fabricmojmap.rt;

import net.fabricmc.tinyremapper.IMappingProvider;

public class UtilRt {
    public static IMappingProvider adaptMappingProvider(net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider loader) {
        return out -> loader.load(new net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider.MappingAcceptor() {
            @Override
            public void acceptClass(String s, String s1) {
                out.acceptClass(s, s1);
            }

            @Override
            public void acceptMethod(net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider.Member member, String s) {
                out.acceptMethod(new IMappingProvider.Member(member.owner, member.name, member.desc), s);
            }

            @Override
            public void acceptField(net.fabricmc.loader.impl.lib.tinyremapper.IMappingProvider.Member member, String s) {
                out.acceptField(new IMappingProvider.Member(member.owner, member.name, member.desc), s);
            }
        });
    }
}
