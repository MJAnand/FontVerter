package org.mabb.fontverter;

import org.apache.fontbox.encoding.Encoding;

import java.util.*;

public class CharsetConverter {
    public static List<GlyphMapping> glyphIdsToNameToEncoding(Map<Integer, String> idToNames, Encoding encoding) {
        List<GlyphMapping> glyphMappings = new ArrayList<GlyphMapping>();
        Map<Integer, Integer> usedCodes = new HashMap<Integer, Integer>();

        for (Map.Entry<Integer, String> nameSetOn : idToNames.entrySet()) {
            String name = nameSetOn.getValue();
            if (symbolCharToWord.containsKey(name))
                name = symbolCharToWord.get(name);

            int charCode = nameToCode(name, encoding, usedCodes);
            int glyphId = nameSetOn.getKey();

            glyphMappings.add(new GlyphMapping(glyphId, charCode, name));
        }

        fixNullCharCodes(glyphMappings, encoding);
        return glyphMappings;
    }

    private static void fixNullCharCodes(List<GlyphMapping> glyphMappings, Encoding encoding) {
        for (GlyphMapping entryOn : glyphMappings) {
            if (entryOn.charCode == 0)
                entryOn.charCode = findNextAvailableCharCode(glyphMappings, encoding);
        }
    }

    public static int findNextAvailableCharCode(List<GlyphMapping> glyphMappings, Encoding encoding) {
        for (int i = 1; i < encoding.getCodeToNameMap().size(); i++) {
            if (findMappingForCharCode(glyphMappings, i) == null && !encoding.getName(i).equals(".notdef"))
                return i;
        }

        return 0;
    }

    private static GlyphMapping findMappingForCharCode(List<GlyphMapping> glyphMappings, int charCode) {
        for (GlyphMapping entryOn : glyphMappings) {
            if (entryOn.charCode == charCode)
                return entryOn;
        }

        return null;
    }

    public static List<GlyphMapping> charCodeToGlyphIdsToEncoding(Map<Integer, Integer> charCodeToGlyphIds, Encoding encoding) {
        List<GlyphMapping> glyphMappings = new ArrayList<GlyphMapping>();
        for (Map.Entry<Integer, Integer> entryOn : charCodeToGlyphIds.entrySet()) {
            Integer charCode = entryOn.getKey();
            Integer glyphId = entryOn.getValue();
            String name = encoding.getName(charCode);

            glyphMappings.add(new GlyphMapping(glyphId, charCode, name));
        }

        return glyphMappings;
    }

    private static int nameToCode(String name, Encoding encoding, Map<Integer, Integer> usedCodes) {
        if (symbolCharToWord.containsKey(name))
            name = symbolCharToWord.get(name);

        int code = 0;
        for (Map.Entry<Integer, String> entryOn : encoding.getCodeToNameMap().entrySet()) {
            int codeOn = entryOn.getKey();

            if (entryOn.getValue().equals(name) && !usedCodes.containsKey(codeOn))
                code = entryOn.getKey();
        }

        // glyph names can map to multiple id's so we have to remove the id from our search after mapping
        // a name to it
        usedCodes.put(code, 0);
        return code;
    }

    public static class GlyphMapping {
        public final Integer glyphId;
        public Integer charCode;
        public final String name;

        public GlyphMapping(Integer glyphId, Integer charCode, String name) {
            this.glyphId = glyphId;
            this.charCode = charCode;
            this.name = name;
        }
    }

    private static Map<String, String> symbolCharToWord = new HashMap<String, String>();

    static {
        symbolCharToWord.put(" ", "space");
        symbolCharToWord.put("!", "exclam");
        symbolCharToWord.put("#", "numbersign");
        symbolCharToWord.put("$", "dollar");
        symbolCharToWord.put("%", "percent");
        symbolCharToWord.put("&", "ampersand");
        symbolCharToWord.put("(", "parenleft");
        symbolCharToWord.put(")", "parenright");
        symbolCharToWord.put("*", "asterisk");
        symbolCharToWord.put("+", "plus");
        symbolCharToWord.put(",", "comma");
        symbolCharToWord.put("-", "hyphen");
        symbolCharToWord.put(".", "period");
        symbolCharToWord.put("/", "slash");
        symbolCharToWord.put("0", "zero");
        symbolCharToWord.put("1", "one");
        symbolCharToWord.put("2", "two");
        symbolCharToWord.put("3", "three");
        symbolCharToWord.put("4", "four");
        symbolCharToWord.put("5", "five");
        symbolCharToWord.put("6", "six");
        symbolCharToWord.put("7", "seven");
        symbolCharToWord.put("8", "eight");
        symbolCharToWord.put("9", "nine");
        symbolCharToWord.put(":", "colon");
        symbolCharToWord.put(";", "semicolon");
        symbolCharToWord.put("<", "less");
        symbolCharToWord.put("=", "equal");
        symbolCharToWord.put(">", "greater");
        symbolCharToWord.put("?", "question");
        symbolCharToWord.put("@", "at");
        symbolCharToWord.put("[", "bracketleft");
        symbolCharToWord.put("\\", "backslash");
        symbolCharToWord.put("]", "bracketright");
        symbolCharToWord.put("_", "underscore");
        symbolCharToWord.put("{", "braceleft");
        symbolCharToWord.put("}", "braceright");
    }
}