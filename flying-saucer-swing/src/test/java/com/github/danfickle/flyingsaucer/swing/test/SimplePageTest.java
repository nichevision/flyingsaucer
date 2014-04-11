package com.github.danfickle.flyingsaucer.swing.test;

import java.io.File;
import java.util.Date;

import com.github.danfickle.flyingsaucer.swing.Graphics2DRenderer;
import org.xhtmlrenderer.util.Uu;

public class SimplePageTest {

    public static void main(final String[] args) throws Exception {
        long total = 0;
        final int cnt = 1;
        final String demosDir = "d:/java/javanet/xhtmlrenderer/demos/browser/xhtml/new";
        String page = demosDir + "/dead-simple-page.xhtml";
        if ( args.length == 1 ) {
            page = demosDir + "/" + args[0];
        }
        //String page = demosDir + "/browser/xhtml/hamlet.xhtml";
        //String page = demosDir + "/splash/splash.html";
        System.out.println("Testing with page " + page);
        for (int i = 0; i < cnt; i++) {
            final Date start = new Date();
            Graphics2DRenderer.renderToImage(new File(page).toURL().toExternalForm(),
                    700, 700);
            final Date end = new Date();
            final long diff = (end.getTime() - start.getTime());
            Uu.p("ms = " + diff);
            if (i > 4) total += diff;
        }
        final long avg = total / cnt;
        System.out.println("average : " + avg);
    }
}
