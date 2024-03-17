package io.github.gaming32.fabricmojmap.rt;

public class GsonRt {
    public static String toDescriptor(Class<?> clazz) {
        if (clazz == void.class) return "V";
        if (clazz == boolean.class) return "Z";
        if (clazz == byte.class) return "B";
        if (clazz == short.class) return "S";
        if (clazz == char.class) return "C";
        if (clazz == int.class) return "I";
        if (clazz == float.class) return "F";
        if (clazz == long.class) return "J";
        if (clazz == double.class) return "D";
        return 'L' + clazz.getName().replace('.', '/') + ';';
    }

    public static String separateCamelCase(String name, char separator) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0, length = name.length(); i < length; i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }

    public static String upperCaseFirstLetter(String s) {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    return s;
                }

                char uppercased = Character.toUpperCase(c);
                // For leading letter only need one substring
                if (i == 0) {
                    return uppercased + s.substring(1);
                } else {
                    return s.substring(0, i) + uppercased + s.substring(i + 1);
                }
            }
        }

        return s;
    }
}
