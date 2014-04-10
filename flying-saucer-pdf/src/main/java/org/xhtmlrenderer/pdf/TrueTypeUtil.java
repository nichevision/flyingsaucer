package org.xhtmlrenderer.pdf;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.pdf.ITextFontResolver.FontDescription;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.RandomAccessFileOrArray;

/**
 * Uses code from iText's DefaultFontMapper and TrueTypeFont classes.  See
 * <a href="http://sourceforge.net/projects/itext/">http://sourceforge.net/projects/itext/</a> for license information.
 */
public class TrueTypeUtil {
    private static IdentValue guessStyle(final BaseFont font) {
        final String[][] names = font.getFullFontName();
        for (final String[] name : names) {
            final String lower = name[3].toLowerCase();
            if (lower.indexOf("italic") != -1) {
                return IdentValue.ITALIC;
            } else if (lower.indexOf("oblique") != -1) {
                return IdentValue.OBLIQUE;
            }
        }

        return IdentValue.NORMAL;
    }

    public static String[] getFamilyNames(final BaseFont font) {
        final String names[][] = font.getFamilyFontName();
        if (names.length == 1) {
            return new String[] { names[0][3] };
        }

        final List<String> result = new ArrayList<String>();
        for (final String[] name : names) {
            if ((name[0].equals("1") && name[1].equals("0")) ||
                    name[2].equals("1033")) {
                result.add(name[3]);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    // HACK No accessor
    private static Map<?, ?> extractTables(final BaseFont font)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException,
                    IllegalAccessException {
        Class<?> current = font.getClass();
        while (current != null) {
            if (current.getName().endsWith(".TrueTypeFont")) {
                final Field f = current.getDeclaredField("tables");
                f.setAccessible(true);
                return (Map<?, ?>)f.get(font);
            }

            current = current.getSuperclass();
        }

        throw new NoSuchFieldException("Could not find tables field");
    }

    private static String getTTCName(final String name) {
        final int idx = name.toLowerCase().indexOf(".ttc,");
        if (idx < 0) {
            return name;
        } else {
            return name.substring(0, idx + 4);
        }
    }

    public static void populateDescription(final String path, final BaseFont font, final FontDescription descr)
            throws IOException, NoSuchFieldException, IllegalAccessException, DocumentException {
        RandomAccessFileOrArray rf = null;
        try {
            rf = new RandomAccessFileOrArray(getTTCName(path));

            rf = populateDescription0(path, font, descr, rf);
        } finally {
            if (rf != null) {
                try {
                    rf.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    public static void populateDescription(final String path, final byte[] contents, final BaseFont font, final FontDescription descr)
            throws IOException, NoSuchFieldException, IllegalAccessException, DocumentException {
        RandomAccessFileOrArray rf = null;
        try {
            rf = new RandomAccessFileOrArray(contents);

            rf = populateDescription0(path, font, descr, rf);
        } finally {
            if (rf != null) {
                try {
                    rf.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    private static RandomAccessFileOrArray populateDescription0(final String path,
            final BaseFont font, final FontDescription descr, RandomAccessFileOrArray rf)
               throws NoSuchFieldException, IllegalAccessException, DocumentException, IOException {
        final Map<?, ?> tables = extractTables(font);

        descr.setStyle(guessStyle(font));

        int[] location = (int[])tables.get("OS/2");
        if (location == null) {
            throw new DocumentException("Table 'OS/2' does not exist in " + path);
        }
        rf.seek(location[0]);
        int want = 4;
        long got = rf.skip(want);
        if (got < want) {
            throw new DocumentException("Skip TT font weight, expect read " + want + " bytes, but only got " + got);
        }
        descr.setWeight(rf.readUnsignedShort());
        want = 20;
        got = rf.skip(want);
        if (got < want) {
            throw new DocumentException("Skip TT font strikeout, expect read " + want + " bytes, but only got " + got);
        }
        descr.setYStrikeoutSize(rf.readShort());
        descr.setYStrikeoutPosition(rf.readShort());

        location = (int[])tables.get("post");

        if (location != null) {
            rf.seek(location[0]);
            want = 8;
            got = rf.skip(want);
            if (got < want) {
                throw new DocumentException("Skip TT font underline, expect read " + want + " bytes, but only got " + got);
            }
            descr.setUnderlinePosition(rf.readShort());
            descr.setUnderlineThickness(rf.readShort());
        }

        rf.close();
        rf = null;
        return rf;
    }
}
