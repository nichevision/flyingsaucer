package org.xhtmlrenderer.pdf;

/**
 * No-op implementation of a {@link org.xhtmlrenderer.pdf.PDFCreationListener}. Override methods as needed.
 */
public class DefaultPDFCreationListener implements PDFCreationListener {
    /**
     * {@inheritDoc}
     */
    public void preOpen(final ITextRenderer iTextRenderer) { }

    /**
     * {@inheritDoc}
     */
    public void preWrite(final ITextRenderer iTextRenderer, final int pageCount) {}

    /**
     * {@inheritDoc}
     */
    public void onClose(final ITextRenderer renderer) { }
}
