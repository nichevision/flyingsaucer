package com.github.danfickle.flyingsaucer.swing.test;

import com.github.danfickle.flyingsaucer.swing.Graphics2DRenderer;
import org.xhtmlrenderer.util.Uu;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;

public class AllPageTest {

    public static void main(final String[] args) throws Exception {
        new AllPageTest().run();
    }

    public void run() {
        try {
            final String demosDir = "d:/java/javanet/xhtmlrenderer/demos/browser/xhtml/new";
            final File[] files = new File(demosDir).listFiles(new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return name.endsWith("xhtml");
                }
            });
            for (final File file : files) {
                try {
                    render(file);
                } catch ( final Exception ex ) {
                    ex.printStackTrace();
                }
            }
        } catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    private void render(final File file) throws Exception {
        System.out.println("\n\n*** Rendering page " + file.getName() + " ***\n\n");
        long total = 0;
        final int cnt = 1;
        final String page = file.toURL().toExternalForm();
        System.out.println("Testing with page " + page);
        for (int i = 0; i < cnt; i++) {
            final Date start = new Date();
            Graphics2DRenderer.renderToImage(page, 700, 700);
            final Date end = new Date();
            final long diff = (end.getTime() - start.getTime());
            Uu.p("ms = " + diff);
            if (i > 4) total += diff;
        }
        final long avg = total / cnt;
        System.out.println("average : " + avg);
    }
}
