package org.xhtmlrenderer.simple.extend;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;

public class NoReplacedElementFactory implements ReplacedElementFactory {

    public ReplacedElement createReplacedElement(final LayoutContext c, final BlockBox box,
            final UserAgentCallback uac, final int cssWidth, final int cssHeight) {
        return null;
    }

    public void remove(final Element e) {

    }

    public void setFormSubmissionListener(final FormSubmissionListener listener) {
        //TODO
    }

    public void reset() {
    }

}
