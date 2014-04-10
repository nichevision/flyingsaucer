import com.lowagie.text.pdf.BaseFont;

import org.xhtmlrenderer.pdf.ITextFontResolver;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 */
public class ListFontFamilyNames {
    public static void main(final String[] args) {
        if (args.length == 0) {
            System.err.println("Need path to font files (directory or file name)");
            System.exit(1);
        }
        final File fod = new File(args[0]);
        final List<File> fontFiles = new ArrayList<File>();
        if (fod.isDirectory()) {
            fontFiles.addAll(Arrays.asList(fod.listFiles(new FilenameFilter() {
                public boolean accept(final File file, final String s) {
                    return s.endsWith(".ttf");
                }
            })));
        } else {
            fontFiles.add(fod);
        }
        //System.out.println("font files " + fontFiles);
        final List<String> errors = new ArrayList<String>();
        for (final File file : fontFiles) {
            final File f = (File) file;
            Font awtf = null;
            try {
                awtf = Font.createFont(Font.TRUETYPE_FONT, f);
            } catch (final FontFormatException e) {
                System.err.println("Trying to load font via AWT: " + e.getMessage());
                System.exit(1);
            } catch (final IOException e) {
                System.err.println("Trying to load font via AWT: " + e.getMessage());
                System.exit(1);
            }
            Set set;
            try {
                set = ITextFontResolver.getDistinctFontFamilyNames(f.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                System.out.println(
                        "Font located at " + f.getPath() + "\n" +
                                "  family name (reported by AWT): " + awtf.getFamily() + "\n" +
                                "  family name (reported by iText): " + set.iterator().next() + "\n"
                );
            } catch (final RuntimeException e) {
                if (e.getMessage().contains("not a valid TTF or OTF file.")) {
                    errors.add(e.getMessage());
                } else if (e.getMessage().contains("Table 'OS/2' does not exist")) {
                    errors.add(e.getMessage());
                } else if (e.getMessage().contains("licensing restrictions.")) {
                    errors.add(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
        if (errors.size() > 0) {
            if (args.length == 2 && args[1].equals("-e")) {
                System.err.println("Errors were reported on reading some font files.");
                for (final String string : errors) {
                    System.err.println((String) string);
                }
            } else {
                System.err.println("Errors were reported on reading some font files. Pass -e as argument to show them, and re-run.");
            }
        }
    }
}
