/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.pdf;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.sheet.FontFaceRule;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.FSDerivedValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

import java.io.*;
import java.util.*;

public class ITextFontResolver implements FontResolver {
    private Map<String, FontFamily> _fontFamilies = createInitialFontMap();
    private Map<String, FontDescription> _fontCache = new HashMap<String, FontDescription>();

    private final SharedContext _sharedContext;

    public ITextFontResolver(final SharedContext sharedContext) {
        _sharedContext = sharedContext;
    }

    /**
     * Utility method which uses iText libraries to determine the family name(s) for the font at the given path.
     * The iText APIs seem to indicate there can be more than one name, but this method will return a set of them.
     * Use a name from this list when referencing the font in CSS for PDF output. Note that family names as reported
     * by iText may vary from those reported by the AWT Font class, e.g. "Arial Unicode MS" for iText and
     * "ArialUnicodeMS" for AWT.
     *
     * @param path local path to the font file
     * @param encoding same as what you would use for {@link #addFont(String, String, boolean)}
     * @param embedded same as what you would use for {@link #addFont(String, String, boolean)}
     * @return set of all family names for the font file, as reported by iText libraries
     */
    public static Set<String> getDistinctFontFamilyNames(final String path, final String encoding, final boolean embedded) {
        BaseFont font = null;
        try {
            font = BaseFont.createFont(path, encoding, embedded);
            final String[] fontFamilyNames = TrueTypeUtil.getFamilyNames(font);
            final Set<String> distinct = new HashSet<String>();
            for (final String fontFamilyName : fontFamilyNames) {
                distinct.add(fontFamilyName);
            }
            return distinct;
        } catch (final DocumentException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FSFont resolveFont(final SharedContext renderingContext, final FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }

    public void flushCache() {
        _fontFamilies = createInitialFontMap();
        _fontCache = new HashMap<String, FontDescription>();
    }

    public void flushFontFaceFonts() {
        _fontCache = new HashMap<String, FontDescription>();

        for (final Iterator<FontFamily> i = _fontFamilies.values().iterator(); i.hasNext(); ) {
            final FontFamily family = i.next();
            for (final Iterator<FontDescription> j = family.getFontDescriptions().iterator(); j.hasNext(); ) {
                final FontDescription d = j.next();
                if (d.isFromFontFace()) {
                    j.remove();
                }
            }
            if (family.getFontDescriptions().size() == 0) {
                i.remove();
            }
        }
    }

    public void importFontFaces(final List<FontFaceRule> fontFaces) {
        for (final FontFaceRule rule : fontFaces) {
            final CalculatedStyle style = rule.getCalculatedStyle();

            final FSDerivedValue src = style.valueByName(CSSName.SRC);
            if (src == IdentValue.NONE) {
                continue;
            }

            byte[] font1 = _sharedContext.getUac().getBinaryResource(src.asString());
            if (font1 == null) {
                XRLog.exception("Could not load font " + src.asString());
                continue;
            }

            byte[] font2 = null;
            final FSDerivedValue metricsSrc = style.valueByName(CSSName.FS_FONT_METRIC_SRC);
            if (metricsSrc != IdentValue.NONE) {
                font2 = _sharedContext.getUac().getBinaryResource(metricsSrc.asString());
                if (font2 == null) {
                    XRLog.exception("Could not load font metric data " + src.asString());
                    continue;
                }
            }

            if (font2 != null) {
                final byte[] t = font1;
                font1 = font2;
                font2 = t;
            }

            final boolean embedded = style.isIdent(CSSName.FS_PDF_FONT_EMBED, IdentValue.EMBED);

            final String encoding = style.getStringProperty(CSSName.FS_PDF_FONT_ENCODING);

            String fontFamily = null;
            if (rule.hasFontFamily()) {
                fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
            }
            try {
                addFontFaceFont(fontFamily, src.asString(), encoding, embedded, font1, font2);
            } catch (final DocumentException e) {
                XRLog.exception("Could not load font " + src.asString(), e);
                continue;
            } catch (final IOException e) {
                XRLog.exception("Could not load font " + src.asString(), e);
            }
        }
    }

    public void addFontDirectory(final String dir, final boolean embedded)
            throws DocumentException, IOException {
        final File f = new File(dir);
        if (f.isDirectory()) {
            final File[] files = f.listFiles(new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    final String lower = name.toLowerCase();
                    return lower.endsWith(".otf") || lower.endsWith(".ttf");
                }
            });
            for (final File file : files) {
                addFont(file.getAbsolutePath(), embedded);
            }
        }
    }

    public void addFont(final String path, final boolean embedded)
            throws DocumentException, IOException {
        addFont(path, BaseFont.CP1252, embedded);
    }

    public void addFont(final String path, final String encoding, final boolean embedded)
            throws DocumentException, IOException {
        addFont(path, encoding, embedded, null);
    }

    public void addFont(final String path, final String encoding, final boolean embedded, final String pathToPFB)
            throws DocumentException, IOException {
        addFont(path, null, encoding, embedded, pathToPFB);
    }

    public void addFont(final String path, final String fontFamilyNameOverride,
                        final String encoding, final boolean embedded, final String pathToPFB)
            throws DocumentException, IOException {
        final String lower = path.toLowerCase();
        if (lower.endsWith(".otf") || lower.endsWith(".ttf") || lower.indexOf(".ttc,") != -1) {
            final BaseFont font = BaseFont.createFont(path, encoding, embedded);

            String[] fontFamilyNames;
            if (fontFamilyNameOverride != null) {
                fontFamilyNames = new String[] { fontFamilyNameOverride };
            } else {
                fontFamilyNames = TrueTypeUtil.getFamilyNames(font);
            }

            for (final String fontFamilyName : fontFamilyNames) {
                final FontFamily fontFamily = getFontFamily(fontFamilyName);

                final FontDescription descr = new FontDescription(font);
                try {
                    TrueTypeUtil.populateDescription(path, font, descr);
                } catch (final Exception e) {
                    throw new XRRuntimeException(e.getMessage(), e);
                }

                fontFamily.addFontDescription(descr);
            }
        } else if (lower.endsWith(".ttc")) {
            final String[] names = BaseFont.enumerateTTCNames(path);
            for (int i = 0; i < names.length; i++) {
                addFont(path + "," + i, fontFamilyNameOverride, encoding, embedded, null);
            }
        } else if (lower.endsWith(".afm") || lower.endsWith(".pfm")) {
            if (embedded && pathToPFB == null) {
                throw new IOException("When embedding a font, path to PFB/PFA file must be specified");
            }

            final BaseFont font = BaseFont.createFont(
                    path, encoding, embedded, false, null, readFile(pathToPFB));

            String fontFamilyName;
            if (fontFamilyNameOverride != null) {
                fontFamilyName = fontFamilyNameOverride;
            } else {
                fontFamilyName = font.getFamilyFontName()[0][3];
            }

            final FontFamily fontFamily = getFontFamily(fontFamilyName);

            final FontDescription descr = new FontDescription(font);
            // XXX Need to set weight, underline position, etc.  This information
            // is contained in the AFM file (and even parsed by Type1Font), but
            // unfortunately it isn't exposed to the caller.
            fontFamily.addFontDescription(descr);
        } else {
            throw new IOException("Unsupported font type");
        }
    }

    private void addFontFaceFont(
            final String fontFamilyNameOverride, final String uri, final String encoding, final boolean embedded, final byte[] afmttf, final byte[] pfb)
            throws DocumentException, IOException {
        final String lower = uri.toLowerCase();
        if (lower.endsWith(".otf") || lower.endsWith(".ttf") || lower.indexOf(".ttc,") != -1) {
            final BaseFont font = BaseFont.createFont(uri, encoding, embedded, false, afmttf, pfb);

            String[] fontFamilyNames;
            if (fontFamilyNameOverride != null) {
                fontFamilyNames = new String[] { fontFamilyNameOverride };
            } else {
                fontFamilyNames = TrueTypeUtil.getFamilyNames(font);
            }

            for (final String fontFamilyName : fontFamilyNames) {
                final FontFamily fontFamily = getFontFamily(fontFamilyName);

                final FontDescription descr = new FontDescription(font);
                try {
                    TrueTypeUtil.populateDescription(uri, afmttf, font, descr);
                } catch (final Exception e) {
                    throw new XRRuntimeException(e.getMessage(), e);
                }

                descr.setFromFontFace(true);

                fontFamily.addFontDescription(descr);
            }
        } else if (lower.endsWith(".afm") || lower.endsWith(".pfm") || lower.endsWith(".pfb") || lower.endsWith(".pfa")) {
            if (embedded && pfb == null) {
                throw new IOException("When embedding a font, path to PFB/PFA file must be specified");
            }

            final String name = uri.substring(0, uri.length()-4) + ".afm";
            final BaseFont font = BaseFont.createFont(
                    name, encoding, embedded, false, afmttf, pfb);

            final String fontFamilyName = font.getFamilyFontName()[0][3];
            final FontFamily fontFamily = getFontFamily(fontFamilyName);

            final FontDescription descr = new FontDescription(font);
            descr.setFromFontFace(true);
            // XXX Need to set weight, underline position, etc.  This information
            // is contained in the AFM file (and even parsed by Type1Font), but
            // unfortunately it isn't exposed to the caller.
            fontFamily.addFontDescription(descr);
        } else {
            throw new IOException("Unsupported font type");
        }
    }

    private byte[] readFile(final String path) throws IOException {
        final File f = new File(path);
        if (f.exists()) {
            final ByteArrayOutputStream result = new ByteArrayOutputStream((int)f.length());
            InputStream is = null;
            try {
                is = new FileInputStream(path);
                final byte[] buf = new byte[10240];
                int i;
                while ( (i = is.read(buf)) != -1) {
                    result.write(buf, 0, i);
                }
                is.close();
                is = null;

                return result.toByteArray();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        } else {
            throw new IOException("File " + path + " does not exist or is not accessible");
        }
    }

    public FontFamily getFontFamily(final String fontFamilyName) {
        FontFamily fontFamily = _fontFamilies.get(fontFamilyName);
        if (fontFamily == null) {
            fontFamily = new FontFamily();
            fontFamily.setName(fontFamilyName);
            _fontFamilies.put(fontFamilyName, fontFamily);
        }
        return fontFamily;
    }

    private FSFont resolveFont(final SharedContext ctx, final String[] families, final float size, final IdentValue weight, IdentValue style, final IdentValue variant) {
        if (! (style == IdentValue.NORMAL || style == IdentValue.OBLIQUE
                || style == IdentValue.ITALIC)) {
            style = IdentValue.NORMAL;
        }
        if (families != null) {
            for (final String family : families) {
                final FSFont font = resolveFont(ctx, family, size, weight, style, variant);
                if (font != null) {
                    return font;
                }
            }
        }

        return resolveFont(ctx, "Serif", size, weight, style, variant);
    }

    private String normalizeFontFamily(final String fontFamily) {
        String result = fontFamily;
        // strip off the "s if they are there
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }

        // normalize the font name
        if (result.equalsIgnoreCase("serif")) {
            result = "Serif";
        }
        else if (result.equalsIgnoreCase("sans-serif")) {
            result = "SansSerif";
        }
        else if (result.equalsIgnoreCase("monospace")) {
            result = "Monospaced";
        }

        return result;
    }

    private FSFont resolveFont(final SharedContext ctx, final String fontFamily, final float size, final IdentValue weight, final IdentValue style, final IdentValue variant) {
        final String normalizedFontFamily = normalizeFontFamily(fontFamily);

        final String cacheKey = getHashName(normalizedFontFamily, weight, style);
        FontDescription result = _fontCache.get(cacheKey);
        if (result != null) {
            return new ITextFSFont(result, size);
        }

        final FontFamily family = _fontFamilies.get(normalizedFontFamily);
        if (family != null) {
            result = family.match(convertWeightToInt(weight), style);
            if (result != null) {
                _fontCache.put(cacheKey, result);
                return new ITextFSFont(result, size);
            }
        }

        return null;
    }

    public static int convertWeightToInt(final IdentValue weight) {
        if (weight == IdentValue.NORMAL) {
            return 400;
        } else if (weight == IdentValue.BOLD) {
            return 700;
        } else if (weight == IdentValue.FONT_WEIGHT_100) {
            return 100;
        } else if (weight == IdentValue.FONT_WEIGHT_200) {
            return 200;
        } else if (weight == IdentValue.FONT_WEIGHT_300) {
            return 300;
        } else if (weight == IdentValue.FONT_WEIGHT_400) {
            return 400;
        } else if (weight == IdentValue.FONT_WEIGHT_500) {
            return 500;
        } else if (weight == IdentValue.FONT_WEIGHT_600) {
            return 600;
        } else if (weight == IdentValue.FONT_WEIGHT_700) {
            return 700;
        } else if (weight == IdentValue.FONT_WEIGHT_800) {
            return 800;
        } else if (weight == IdentValue.FONT_WEIGHT_900) {
            return 900;
        } else if (weight == IdentValue.LIGHTER) {
            // FIXME
            return 400;
        } else if (weight == IdentValue.BOLDER) {
            // FIXME
            return 700;
        }
        throw new IllegalArgumentException();
    }

    protected static String getHashName(
            final String name, final IdentValue weight, final IdentValue style) {
        return name + "-" + weight + "-" + style;
    }

    private static Map<String, FontFamily> createInitialFontMap() {
        final HashMap<String, FontFamily> result = new HashMap<String, FontFamily>();

        try {
            addCourier(result);
            addTimes(result);
            addHelvetica(result);
            addSymbol(result);
            addZapfDingbats(result);

            // Try and load the iTextAsian fonts
            if(ITextFontResolver.class.getClassLoader().getResource("com/lowagie/text/pdf/fonts/cjkfonts.properties") != null) {
                addCJKFonts(result);
            }
        } catch (final DocumentException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return result;
    }

    private static BaseFont createFont(final String name) throws DocumentException, IOException {
        return ITextFontResolver.createFont(name, "winansi", true);
    }

    private static BaseFont createFont(final String name, final String encoding, final boolean embedded) throws DocumentException, IOException {
        return BaseFont.createFont(name, encoding, embedded);
    }

    private static void addCourier(final HashMap<String, FontFamily> result) throws DocumentException, IOException {
        final FontFamily courier = new FontFamily();
        courier.setName("Courier");

        courier.addFontDescription(new FontDescription(
                createFont(BaseFont.COURIER_BOLDOBLIQUE), IdentValue.OBLIQUE, 700));
        courier.addFontDescription(new FontDescription(
                createFont(BaseFont.COURIER_OBLIQUE), IdentValue.OBLIQUE, 400));
        courier.addFontDescription(new FontDescription(
                createFont(BaseFont.COURIER_BOLD), IdentValue.NORMAL, 700));
        courier.addFontDescription(new FontDescription(
                createFont(BaseFont.COURIER), IdentValue.NORMAL, 400));

        result.put("DialogInput", courier);
        result.put("Monospaced", courier);
        result.put("Courier", courier);
    }

    private static void addTimes(final HashMap<String, FontFamily> result) throws DocumentException, IOException {
        final FontFamily times = new FontFamily();
        times.setName("Times");

        times.addFontDescription(new FontDescription(
                createFont(BaseFont.TIMES_BOLDITALIC), IdentValue.ITALIC, 700));
        times.addFontDescription(new FontDescription(
                createFont(BaseFont.TIMES_ITALIC), IdentValue.ITALIC, 400));
        times.addFontDescription(new FontDescription(
                createFont(BaseFont.TIMES_BOLD), IdentValue.NORMAL, 700));
        times.addFontDescription(new FontDescription(
                createFont(BaseFont.TIMES_ROMAN), IdentValue.NORMAL, 400));

        result.put("Serif", times);
        result.put("TimesRoman", times);
    }

    private static void addHelvetica(final HashMap<String, FontFamily> result) throws DocumentException, IOException {
        final FontFamily helvetica = new FontFamily();
        helvetica.setName("Helvetica");

        helvetica.addFontDescription(new FontDescription(
                createFont(BaseFont.HELVETICA_BOLDOBLIQUE), IdentValue.OBLIQUE, 700));
        helvetica.addFontDescription(new FontDescription(
                createFont(BaseFont.HELVETICA_OBLIQUE), IdentValue.OBLIQUE, 400));
        helvetica.addFontDescription(new FontDescription(
                createFont(BaseFont.HELVETICA_BOLD), IdentValue.NORMAL, 700));
        helvetica.addFontDescription(new FontDescription(
                createFont(BaseFont.HELVETICA), IdentValue.NORMAL, 400));

        result.put("Dialog", helvetica);
        result.put("SansSerif", helvetica);
        result.put("Helvetica", helvetica);
    }

    private static void addSymbol(final Map<String, FontFamily> result) throws DocumentException, IOException {
        final FontFamily fontFamily = new FontFamily();
        fontFamily.setName("Symbol");

        fontFamily.addFontDescription(new FontDescription(createFont(BaseFont.SYMBOL, BaseFont.CP1252, false), IdentValue.NORMAL, 400));

        result.put("Symbol", fontFamily);
    }

    private static void addZapfDingbats(final Map<String, FontFamily> result) throws DocumentException, IOException {
        final FontFamily fontFamily = new FontFamily();
        fontFamily.setName("ZapfDingbats");

        fontFamily.addFontDescription(new FontDescription(createFont(BaseFont.ZAPFDINGBATS, BaseFont.CP1252, false), IdentValue.NORMAL, 400));

        result.put("ZapfDingbats", fontFamily);
    }

    // fontFamilyName, fontName, encoding
    private static final String[][] cjkFonts = {
        {"STSong-Light-H", "STSong-Light", "UniGB-UCS2-H"},
        {"STSong-Light-V", "STSong-Light", "UniGB-UCS2-V"},
        {"STSongStd-Light-H", "STSongStd-Light", "UniGB-UCS2-H"},
        {"STSongStd-Light-V", "STSongStd-Light", "UniGB-UCS2-V"},
        {"MHei-Medium-H", "MHei-Medium", "UniCNS-UCS2-H"},
        {"MHei-Medium-V", "MHei-Medium", "UniCNS-UCS2-V"},
        {"MSung-Light-H", "MSung-Light", "UniCNS-UCS2-H"},
        {"MSung-Light-V", "MSung-Light", "UniCNS-UCS2-V"},
        {"MSungStd-Light-H", "MSungStd-Light", "UniCNS-UCS2-H"},
        {"MSungStd-Light-V", "MSungStd-Light", "UniCNS-UCS2-V"},
        {"HeiseiMin-W3-H", "HeiseiMin-W3", "UniJIS-UCS2-H"},
        {"HeiseiMin-W3-V", "HeiseiMin-W3", "UniJIS-UCS2-V"},
        {"HeiseiKakuGo-W5-H", "HeiseiKakuGo-W5", "UniJIS-UCS2-H"},
        {"HeiseiKakuGo-W5-V", "HeiseiKakuGo-W5", "UniJIS-UCS2-V"},
        {"KozMinPro-Regular-H", "KozMinPro-Regular", "UniJIS-UCS2-HW-H"},
        {"KozMinPro-Regular-V", "KozMinPro-Regular", "UniJIS-UCS2-HW-V"},
        {"HYGoThic-Medium-H", "HYGoThic-Medium", "UniKS-UCS2-H"},
        {"HYGoThic-Medium-V", "HYGoThic-Medium", "UniKS-UCS2-V"},
        {"HYSMyeongJo-Medium-H", "HYSMyeongJo-Medium", "UniKS-UCS2-H"},
        {"HYSMyeongJo-Medium-V", "HYSMyeongJo-Medium", "UniKS-UCS2-V"},
        {"HYSMyeongJoStd-Medium-H", "HYSMyeongJoStd-Medium", "UniKS-UCS2-H"},
        {"HYSMyeongJoStd-Medium-V", "HYSMyeongJoStd-Medium", "UniKS-UCS2-V"}
    };

    private static void addCJKFonts(final Map<String, FontFamily> fontFamilyMap) throws DocumentException, IOException {
        for (final String[] cjkFont : cjkFonts) {
            final String fontFamilyName = cjkFont[0];
            final String fontName = cjkFont[1];
            final String encoding = cjkFont[2];

            addCJKFont(fontFamilyName, fontName, encoding, fontFamilyMap);
        }
    }

    private static void addCJKFont(final String fontFamilyName, final String fontName, final String encoding, final Map<String, FontFamily> fontFamilyMap) throws DocumentException, IOException {
        final FontFamily fontFamily = new FontFamily();
        fontFamily.setName(fontFamilyName);

        fontFamily.addFontDescription(new FontDescription(createFont(fontName+",BoldItalic", encoding, false), IdentValue.OBLIQUE, 700));
        fontFamily.addFontDescription(new FontDescription(createFont(fontName+",Italic", encoding, false), IdentValue.OBLIQUE, 400));
        fontFamily.addFontDescription(new FontDescription(createFont(fontName+",Bold", encoding, false), IdentValue.NORMAL, 700));
        fontFamily.addFontDescription(new FontDescription(createFont(fontName, encoding, false), IdentValue.NORMAL, 400));

        fontFamilyMap.put(fontFamilyName, fontFamily);
    }

    private static class FontFamily {
        private String _name;
        private List<FontDescription> _fontDescriptions;

        public FontFamily() {
        }

        public List<FontDescription> getFontDescriptions() {
            return _fontDescriptions;
        }

        public void addFontDescription(final FontDescription descr) {
            if (_fontDescriptions == null) {
                _fontDescriptions = new ArrayList<FontDescription>();
            }
            _fontDescriptions.add(descr);
            Collections.sort(_fontDescriptions,
                    new Comparator() {
                        public int compare(final Object o1, final Object o2) {
                            final FontDescription f1 = (FontDescription)o1;
                            final FontDescription f2 = (FontDescription)o2;
                            return f1.getWeight() - f2.getWeight();
                        }
            });
        }

        public String getName() {
            return _name;
        }

        public void setName(final String name) {
            _name = name;
        }

        public FontDescription match(final int desiredWeight, final IdentValue style) {
            if (_fontDescriptions == null) {
                throw new RuntimeException("fontDescriptions is null");
            }

            final List<FontDescription> candidates = new ArrayList<FontDescription>();

            for (final FontDescription description : _fontDescriptions) {
                if (description.getStyle() == style) {
                    candidates.add(description);
                }
            }

            if (candidates.size() == 0) {
                if (style == IdentValue.ITALIC) {
                    return match(desiredWeight, IdentValue.OBLIQUE);
                } else if (style == IdentValue.OBLIQUE) {
                    return match(desiredWeight, IdentValue.NORMAL);
                } else {
                    candidates.addAll(_fontDescriptions);
                }
            }

            final FontDescription[] matches = candidates.toArray(new FontDescription[candidates.size()]);
            FontDescription result;

            result = findByWeight(matches, desiredWeight, SM_EXACT);

            if (result != null) {
                return result;
            } else {
                if (desiredWeight <= 500) {
                    return findByWeight(matches, desiredWeight, SM_LIGHTER_OR_DARKER);
                } else {
                    return findByWeight(matches, desiredWeight, SM_DARKER_OR_LIGHTER);
                }
            }
        }

        private static final int SM_EXACT = 1;
        private static final int SM_LIGHTER_OR_DARKER = 2;
        private static final int SM_DARKER_OR_LIGHTER = 3;

        private FontDescription findByWeight(final FontDescription[] matches,
                final int desiredWeight, final int searchMode) {
            if (searchMode == SM_EXACT) {
                for (final FontDescription descr : matches) {
                    if (descr.getWeight() == desiredWeight) {
                        return descr;
                    }
                }
                return null;
            } else if (searchMode == SM_LIGHTER_OR_DARKER){
                int offset = 0;
                FontDescription descr = null;
                for (offset = 0; offset < matches.length; offset++) {
                    descr = matches[offset];
                    if (descr.getWeight() > desiredWeight) {
                        break;
                    }
                }

                if (offset > 0 && descr.getWeight() > desiredWeight) {
                    return matches[offset-1];
                } else {
                    return descr;
                }

            } else if (searchMode == SM_DARKER_OR_LIGHTER) {
                int offset = 0;
                FontDescription descr = null;
                for (offset = matches.length - 1; offset >= 0; offset--) {
                    descr = matches[offset];
                    if (descr.getWeight() < desiredWeight) {
                        break;
                    }
                }

                if (offset != matches.length - 1 && descr.getWeight() < desiredWeight) {
                    return matches[offset+1];
                } else {
                    return descr;
                }
            }

            return null;
        }
    }

    public static class FontDescription {
        private IdentValue _style;
        private int _weight;

        private BaseFont _font;

        private float _underlinePosition;
        private float _underlineThickness;

        private float _yStrikeoutSize;
        private float _yStrikeoutPosition;

        private boolean _isFromFontFace;

        public FontDescription() {
        }

        public FontDescription(final BaseFont font) {
            this(font, IdentValue.NORMAL, 400);
        }

        public FontDescription(final BaseFont font, final IdentValue style, final int weight) {
            _font = font;
            _style = style;
            _weight = weight;
            setMetricDefaults();
        }

        public BaseFont getFont() {
            return _font;
        }

        public void setFont(final BaseFont font) {
            _font = font;
        }

        public int getWeight() {
            return _weight;
        }

        public void setWeight(final int weight) {
            _weight = weight;
        }

        public IdentValue getStyle() {
            return _style;
        }

        public void setStyle(final IdentValue style) {
            _style = style;
        }

        /**
         * @see #getUnderlinePosition()
         */
        public float getUnderlinePosition() {
            return _underlinePosition;
        }

        /**
         * This refers to the top of the underline stroke
         */
        public void setUnderlinePosition(final float underlinePosition) {
            _underlinePosition = underlinePosition;
        }

        public float getUnderlineThickness() {
            return _underlineThickness;
        }

        public void setUnderlineThickness(final float underlineThickness) {
            _underlineThickness = underlineThickness;
        }

        public float getYStrikeoutPosition() {
            return _yStrikeoutPosition;
        }

        public void setYStrikeoutPosition(final float strikeoutPosition) {
            _yStrikeoutPosition = strikeoutPosition;
        }

        public float getYStrikeoutSize() {
            return _yStrikeoutSize;
        }

        public void setYStrikeoutSize(final float strikeoutSize) {
            _yStrikeoutSize = strikeoutSize;
        }

        private void setMetricDefaults() {
            _underlinePosition = -50;
            _underlineThickness = 50;

            final int[] box = _font.getCharBBox('x');
            if (box != null) {
                _yStrikeoutPosition = box[3] / 2 + 50;
                _yStrikeoutSize = 100;
            } else {
                // Do what the JDK does, size will be calculated by ITextTextRenderer
                _yStrikeoutPosition = _font.getFontDescriptor(BaseFont.BBOXURY, 1000f) / 3.0f;
            }
        }

        public boolean isFromFontFace() {
            return _isFromFontFace;
        }

        public void setFromFontFace(final boolean isFromFontFace) {
            _isFromFontFace = isFromFontFace;
        }
    }
}
