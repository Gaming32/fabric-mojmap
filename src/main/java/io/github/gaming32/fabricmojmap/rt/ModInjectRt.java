package io.github.gaming32.fabricmojmap.rt;

import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.UrlUtil;

import java.nio.file.Path;

public class ModInjectRt {
    public static Path getModCodeSource() {
        return LoaderUtil.normalizeExistingPath(UrlUtil.getCodeSource(ModInjectRt.class));
    }
}
