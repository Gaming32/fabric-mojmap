package io.github.gaming32.fabricmojmap;

import com.google.gson.stream.JsonReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class Util {
    public static Reader newReader(InputStream is) {
        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }

    public static JsonReader newJsonReader(InputStream is) {
        return new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    public static JsonReader newJsonReaderFromUrl(String url) throws IOException {
        return newJsonReader(new URL(url).openStream());
    }

    public static <T> List<T> immutableAdd(List<T> source, T element) {
        if (source.isEmpty()) {
            return Collections.singletonList(element);
        }
        final List<T> result = new ArrayList<>(source.size() + 1);
        result.addAll(source);
        result.add(element);
        return result;
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        if (elements.length == 0) {
            return Collections.emptyList();
        }
        if (elements.length == 1) {
            return Collections.singletonList(elements[0]);
        }
        return Arrays.asList(elements);
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        if (elements.length == 0) {
            return Collections.emptySet();
        }
        if (elements.length == 1) {
            return Collections.singleton(elements[0]);
        }
        return new HashSet<>(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        if (entries.length == 0) {
            return Collections.emptyMap();
        }
        if (entries.length == 1) {
            return Collections.singletonMap(entries[0].getKey(), entries[0].getValue());
        }
        final Map<K, V> result = new HashMap<>((int)(entries.length / 0.75 + 1));
        for (final Map.Entry<K, V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V> Map.Entry<K, V> mapEntry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    public static byte[] readAllBytes(InputStream is) throws IOException {
        final List<byte[]> bufs = new ArrayList<>();
        int bufSize = 8192;
        int totalRead = 0;
        while (true) {
            final byte[] buf = new byte[bufSize];
            final int read = readFully(is, buf);
            if (read == 0) break;
            totalRead += read;
            bufs.add(buf);
            if (read < buf.length) break;
            bufSize <<= 1;
        }
        final byte[] result = new byte[totalRead];
        totalRead = 0;
        for (final byte[] buf : bufs) {
            System.arraycopy(buf, 0, result, totalRead, Math.min(buf.length, result.length - totalRead));
            totalRead += buf.length;
        }
        return result;
    }

    public static int readFully(InputStream is, byte[] buf) throws IOException {
        int read = 0;
        int n;
        while (read < buf.length && (n = is.read(buf, read, buf.length - read)) != -1) {
            read += n;
        }
        return read;
    }

    public static OutputStream closeGuard(OutputStream output) {
        return new FilterOutputStream(output) {
            @Override
            public void close() {
            }
        };
    }

    public static <T extends AbstractInsnNode> void advancePast(
        Iterator<AbstractInsnNode> iter, Class<T> type, Predicate<T> predicate
    ) {
        while (iter.hasNext()) {
            final AbstractInsnNode insn = iter.next();
            if (type.isInstance(insn) && predicate.test(type.cast(insn))) return;
        }
        throw new IllegalStateException("Could not find matching instruction");
    }

    // Lambdas aren't allowed in CASM, so this is a workaround
    public static void advancePastField(Iterator<AbstractInsnNode> iter, String name) {
        Util.advancePast(iter, FieldInsnNode.class, i -> i.name.equals(name));
    }

    public static void advancePastMethod(Iterator<AbstractInsnNode> iter, String name) {
        Util.advancePast(iter, MethodInsnNode.class, i -> i.name.equals(name));
    }
}
