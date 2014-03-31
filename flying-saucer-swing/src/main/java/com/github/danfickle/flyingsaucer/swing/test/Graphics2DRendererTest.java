package com.github.danfickle.flyingsaucer.swing.test;

import com.github.danfickle.flyingsaucer.swing.Graphics2DRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Graphics2DRendererTest {

    public static void main(String[] args) throws Exception {
        BufferedImage img = Graphics2DRenderer.renderToImageAutoSize(new File("demos/splash/splash.html").toURL().toExternalForm(),
                700, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(img, "png", new File("test.png"));
    }
}
