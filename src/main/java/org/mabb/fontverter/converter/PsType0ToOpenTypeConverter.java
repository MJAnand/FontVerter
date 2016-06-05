/*
 * Copyright (C) Matthew Abboud 2016
 *
 * FontVerter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FontVerter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FontVerter. If not, see <http://www.gnu.org/licenses/>.
 */

package org.mabb.fontverter.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.fontbox.cff.CFFStandardEncoding;
import org.apache.fontbox.cmap.CMap;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.font.encoding.StandardEncoding;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.mabb.fontverter.CharsetConverter;
import org.mabb.fontverter.CharsetConverter.GlyphMapping;
import org.mabb.fontverter.FVFont;
import org.mabb.fontverter.FontVerterUtils;
import org.mabb.fontverter.opentype.*;
import org.mabb.fontverter.opentype.OpenTypeFont;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PsType0ToOpenTypeConverter {
    private OpenTypeFont otfFont;
    private PDType0Font type0Font;
    private int originalNumGlyphs;

    public FVFont convert(PDType0Font type0Font) throws IOException, IllegalAccessException, InstantiationException {
        this.type0Font = type0Font;
        PDCIDFont descendantFont = type0Font.getDescendantFont();

        if (!(descendantFont instanceof PDCIDFontType2))
            throw new IOException("Can only convert type 0 fonts with ttf type descendant.");

        byte[] ttfData = type0Font.getFontDescriptor().getFontFile2().toByteArray();
        OpenTypeParser otfParser = new OpenTypeParser();
        otfFont = otfParser.parse(ttfData);
        originalNumGlyphs = otfFont.getMxap().getNumGlyphs();

        // so the descendant ttf font will usually have some important tables missing from it
        // that we need to create ourselves from data in the parent type 0 font.

        // always build cmap from type0 parent ourselves, cmaps existing in the ttf child tend to have some
        // issues in certain browsers and apps.
        convertCmap();
        if (otfFont.getNameTable() == null)
            convertNameRecords();

        otfFont.finalizeFont();

        return otfFont;
    }

    private void convertCmap() throws IllegalAccessException, IOException {
        List<GlyphMapping> glyphMappings = new ArrayList<GlyphMapping>();

        Map<Integer, String> charToUnicode = getType0CharToUnicode();
        for (Map.Entry<Integer, String> nameSetOn : charToUnicode.entrySet()) {
            String name = nameSetOn.getValue();
            int charCode = name.charAt(0);
            int glyphId = nameSetOn.getKey();

            if(name.length() > 2 || charCode > 0xFFFF)
                throw new IOException("Multi byte glyph name not supported.");

            if (charCode != 0)
                glyphMappings.add(new GlyphMapping(glyphId, charCode, name));
        }

        // todo different platform/encode/langauge handeling?
        CmapTable cmapTable = CmapTable.createDefaultTable();
        cmapTable.addGlyphMapping(glyphMappings);

        otfFont.setCmap(cmapTable);
    }

    private void convertNameRecords() {
        NameTable names = NameTable.createDefaultTable();
        String fullName = type0Font.getName();
        String family = type0Font.getName();
        String subFamily = "Normal";

        Matcher matcher = Pattern.compile("([^-^+]*)(\\+|-)([^-]*)-?([^-]*)?").matcher(type0Font.getName());
        if (matcher.find()) {
            family = matcher.group(3);

            String subMatch = matcher.group(4);
            if (!StringUtils.isEmpty(subMatch) && !subMatch.equals("Identity"))
                subFamily = subMatch;
        }

        names.setFontFullName(fullName);
        names.setPostScriptName(family);
        names.setFontFamily(family);
        names.setFontSubFamily(subFamily);

        otfFont.setName(names);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> getType0CharToUnicode() throws IllegalAccessException {
        CMap cmap = (CMap) FontVerterUtils.findPrivateField("toUnicodeCMap", PDFont.class).get(type0Font);
        if (cmap == null)
            return new HashMap<Integer, String>();

        return getCmapUnicodeMap(cmap);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> getCmapUnicodeMap(CMap cmap) throws IllegalAccessException {
        Object mappings = FontVerterUtils.findPrivateField("charToUnicode", cmap.getClass()).get(cmap);
        if (mappings == null)
            return new HashMap<Integer, String>();

        return (Map<Integer, String>) mappings;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getType0CodeToCid(CMap cmap) throws IllegalAccessException {
        Object mappings = (Map<Integer, Integer>) FontVerterUtils.findPrivateField("codeToCid", cmap.getClass()).get(cmap);
        if (mappings == null)
            return new HashMap<Integer, Integer>();

        return (Map<Integer, Integer>) mappings;
    }

}
