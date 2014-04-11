package com.github.danfickle.flyingsaucer.swing.test;

import com.github.danfickle.flyingsaucer.swing.Graphics2DRenderer;
import org.xhtmlrenderer.util.Uu;

import java.io.File;
import java.util.Date;

public class HamletSpeedTest {

    public static void main(final String[] args) throws Exception {
        long total = 0;
        for (int i = 0; i < 10; i++) {
            final Date start = new Date();
            Graphics2DRenderer.renderToImage(
                    new File("demos/browser/xhtml/old/hamlet.xhtml").toURL().toExternalForm(),
                    700, 700);
            final Date end = new Date();
            final long diff = (end.getTime() - start.getTime());
            Uu.p("ms = " + diff);
            total += diff;
        }
        final long avg = total / 10;
        Uu.p("average : " + avg);
    }
}
