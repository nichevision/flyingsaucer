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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.pdf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSCMYKColor;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextFontResolver.FontDescription;
import org.xhtmlrenderer.render.AbstractOutputDevice;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.BorderPainter;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.InlineLayoutBox;
import org.xhtmlrenderer.render.InlineText;
import org.xhtmlrenderer.render.JustificationInfo;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.JsoupUtil;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.CMYKColor;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfBorderArray;
import com.lowagie.text.pdf.PdfBorderDictionary;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfIndirectReference;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfShading;
import com.lowagie.text.pdf.PdfShadingPattern;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfTextArray;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ShadingColor;

/**
 * This class is largely based on {@link com.lowagie.text.pdf.PdfGraphics2D}.
 * See <a href="http://sourceforge.net/projects/itext/">http://sourceforge.net/
 * projects/itext/</a> for license information.
 */
public class ITextOutputDevice extends AbstractOutputDevice implements OutputDevice {
    private static final int FILL = 1;
    private static final int STROKE = 2;
    private static final int CLIP = 3;

    private static AffineTransform IDENTITY = new AffineTransform();

    private static final BasicStroke STROKE_ONE = new BasicStroke(1);

    private static final boolean ROUND_RECT_DIMENSIONS_DOWN = Configuration.isTrue("xr.pdf.round.rect.dimensions.down", false);

    private PdfContentByte _currentPage;
    private float _pageHeight;

    private ITextFSFont _font;

    private AffineTransform _transform = new AffineTransform();

    private Color _color = Color.BLACK;

    private Color _fillColor;
    private Color _strokeColor;

    private Stroke _stroke = null;
    private Stroke _originalStroke = null;
    private Stroke _oldStroke = null;

    private Area _clip;

    private SharedContext _sharedContext;
    private final float _dotsPerPoint;

    private PdfWriter _writer;

    private final Map<URI, PdfReader> _readerCache = new HashMap<URI, PdfReader>();

    private PdfDestination _defaultDestination;

    private final List<Bookmark> _bookmarks = new ArrayList<Bookmark>();

    private final List<Metadata> _metadata = new ArrayList<Metadata>();

    private Box _root;

    private int _startPageNo;

    private int _nextFormFieldIndex;

    private Set<String> _linkTargetAreas;
    
    private boolean haveOpacity = false;

    public ITextOutputDevice(final float dotsPerPoint) {
        _dotsPerPoint = dotsPerPoint;
    }

    public void setWriter(final PdfWriter writer) {
        _writer = writer;
    }

    public PdfWriter getWriter() {
        return _writer;
    }

    public int getNextFormFieldIndex() {
        return ++_nextFormFieldIndex;
    }

    public void initializePage(final PdfContentByte currentPage, final float height) {
        _currentPage = currentPage;
        _pageHeight = height;

        _currentPage.saveState();

        _transform = new AffineTransform();
        _transform.scale(1.0d / _dotsPerPoint, 1.0d / _dotsPerPoint);

        _stroke = transformStroke(STROKE_ONE);
        _originalStroke = _stroke;
        _oldStroke = _stroke;

        setStrokeDiff(_stroke, null);

        if (_defaultDestination == null) {
            _defaultDestination = new PdfDestination(PdfDestination.FITH, height);
            _defaultDestination.addPage(_writer.getPageReference(1));
        }

        _linkTargetAreas = new HashSet<String>();
    }

    public void finishPage() {
        _currentPage.restoreState();
    }

    public void paintReplacedElement(final RenderingContext c, final BlockBox box) {
        final ITextReplacedElement element = (ITextReplacedElement) box.getReplacedElement();
        element.paint(c, this, box);
    }

    public void paintBackground(final RenderingContext c, final Box box) {
        super.paintBackground(c, box);

        processLink(c, box);
    }

    private com.lowagie.text.Rectangle calcTotalLinkArea(final RenderingContext c, final Box box) {
        Box current = box;
        while (true) {
            final Box prev = current.getPreviousSibling();
            if (prev == null || prev.getElement() != box.getElement()) {
                break;
            }

            current = prev;
        }

        com.lowagie.text.Rectangle result = createLocalTargetArea(c, current, true);

        current = current.getNextSibling();
        while (current != null && current.getElement() == box.getElement()) {
            result = add(result, createLocalTargetArea(c, current, true));

            current = current.getNextSibling();
        }

        return result;
    }

    private com.lowagie.text.Rectangle add(final com.lowagie.text.Rectangle r1, final com.lowagie.text.Rectangle r2) {
        final float llx = Math.min(r1.getLeft(), r2.getLeft());
        final float urx = Math.max(r1.getRight(), r2.getRight());
        final float lly = Math.min(r1.getBottom(), r2.getBottom());
        final float ury = Math.max(r1.getTop(), r2.getTop());

        return new com.lowagie.text.Rectangle(llx, lly, urx, ury);
    }

    private String createRectKey(final com.lowagie.text.Rectangle rect) {
        return rect.getLeft() + ":" + rect.getBottom() + ":" + rect.getRight() + ":" + rect.getTop();
    }

    private com.lowagie.text.Rectangle checkLinkArea(final RenderingContext c, final Box box) {
        final com.lowagie.text.Rectangle targetArea = calcTotalLinkArea(c, box);
        final String key = createRectKey(targetArea);
        if (_linkTargetAreas.contains(key)) {
            return null;
        }
        _linkTargetAreas.add(key);
        return targetArea;
    }

    private void processLink(final RenderingContext c, final Box box) {
        final Element elem = box.getElement();
        if (elem != null) {
            final NamespaceHandler handler = _sharedContext.getNamespaceHandler();
            final String uri = handler.getLinkUri(elem);
            if (uri != null) {
                if (uri.length() > 1 && uri.charAt(0) == '#') {
                    final String anchor = uri.substring(1);
                    final Box target = _sharedContext.getBoxById(anchor);
                    if (target != null) {
                        final PdfDestination dest = createDestination(c, target);

                        if (dest != null) {
                            PdfAction action = new PdfAction();
                            if (!"".equals(handler.getAttributeValue(elem, "onclick"))) {
                                action = PdfAction.javaScript(handler.getAttributeValue(elem, "onclick"), _writer);
                            } else {
                                action.put(PdfName.S, PdfName.GOTO);
                                action.put(PdfName.D, dest);
                            }

                            final com.lowagie.text.Rectangle targetArea = checkLinkArea(c, box);
                            if (targetArea == null) {
                                return;
                            }

                            targetArea.setBorder(0);
                            targetArea.setBorderWidth(0);

                            final PdfAnnotation annot = new PdfAnnotation(_writer, targetArea.getLeft(), targetArea.getBottom(),
                                    targetArea.getRight(), targetArea.getTop(), action);
                            annot.put(PdfName.SUBTYPE, PdfName.LINK);
                            annot.setBorderStyle(new PdfBorderDictionary(0.0f, 0));
                            annot.setBorder(new PdfBorderArray(0.0f, 0.0f, 0));
                            _writer.addAnnotation(annot);
                        }
                    }
                } else if (uri.indexOf("://") != -1) {
                    final PdfAction action = new PdfAction(uri);

                    final com.lowagie.text.Rectangle targetArea = checkLinkArea(c, box);
                    if (targetArea == null) {
                        return;
                    }
                    final PdfAnnotation annot = new PdfAnnotation(_writer, targetArea.getLeft(), targetArea.getBottom(), targetArea.getRight(),
                            targetArea.getTop(), action);
                    annot.put(PdfName.SUBTYPE, PdfName.LINK);

                    annot.setBorderStyle(new PdfBorderDictionary(0.0f, 0));
                    annot.setBorder(new PdfBorderArray(0.0f, 0.0f, 0));
                    _writer.addAnnotation(annot);
                }
            }
        }
    }

    public com.lowagie.text.Rectangle createLocalTargetArea(final RenderingContext c, final Box box) {
        return createLocalTargetArea(c, box, false);
    }

    private com.lowagie.text.Rectangle createLocalTargetArea(final RenderingContext c, final Box box, final boolean useAggregateBounds) {
        Rectangle bounds;
        if (useAggregateBounds && box.getPaintingInfo() != null) {
            bounds = box.getPaintingInfo().getAggregateBounds();
        } else {
            bounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
        }

        final Point2D docCorner = new Point2D.Double(bounds.x, bounds.y + bounds.height);
        final Point2D pdfCorner = new Point.Double();
        _transform.transform(docCorner, pdfCorner);
        pdfCorner.setLocation(pdfCorner.getX(), normalizeY((float) pdfCorner.getY()));

        final com.lowagie.text.Rectangle result = new com.lowagie.text.Rectangle((float) pdfCorner.getX(), (float) pdfCorner.getY(),
                (float) pdfCorner.getX() + getDeviceLength(bounds.width), (float) pdfCorner.getY() + getDeviceLength(bounds.height));
        return result;
    }

    public com.lowagie.text.Rectangle createTargetArea(final RenderingContext c, final Box box) {
        final PageBox current = c.getPage();
        final boolean inCurrentPage = box.getAbsY() > current.getTop() && box.getAbsY() < current.getBottom();

        if (inCurrentPage || box.isContainedInMarginBox()) {
            return createLocalTargetArea(c, box);
        } else {
            final Rectangle bounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
            final PageBox page = _root.getLayer().getPage(c, bounds.y);

            final float bottom = getDeviceLength(page.getBottom() - (bounds.y + bounds.height)
                    + page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM));
            final float left = getDeviceLength(page.getMarginBorderPadding(c, CalculatedStyle.LEFT) + bounds.x);

            final com.lowagie.text.Rectangle result = new com.lowagie.text.Rectangle(left, bottom, left + getDeviceLength(bounds.width), bottom
                    + getDeviceLength(bounds.height));
            return result;
        }
    }

    public float getDeviceLength(final float length) {
        return length / _dotsPerPoint;
    }

    private PdfDestination createDestination(final RenderingContext c, final Box box) {
        PdfDestination result = null;

        final PageBox page = _root.getLayer().getPage(c, getPageRefY(box));
        if (page != null) {
            int distanceFromTop = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
            distanceFromTop += box.getAbsY() + box.getMargin(c).top() - page.getTop();
            result = new PdfDestination(PdfDestination.XYZ, 0, page.getHeight(c) / _dotsPerPoint - distanceFromTop / _dotsPerPoint, 0);
            result.addPage(_writer.getPageReference(_startPageNo + page.getPageNo() + 1));
        }

        return result;
    }

    public void drawBorderLine(final Rectangle bounds, final int side, final int lineWidth, final boolean solid) {
        final float x = bounds.x;
        final float y = bounds.y;
        final float w = bounds.width;
        final float h = bounds.height;

        final float adj = solid ? (float) lineWidth / 2 : 0;
        final float adj2 = lineWidth % 2 != 0 ? 0.5f : 0f;

        Line2D.Float line = null;

        // FIXME: findbugs reports possible loss of precision, compare with
        // width / (float)2
        if (side == BorderPainter.TOP) {
            line = new Line2D.Float(x + adj, y + lineWidth / 2 + adj2, x + w - adj, y + lineWidth / 2 + adj2);
        } else if (side == BorderPainter.LEFT) {
            line = new Line2D.Float(x + lineWidth / 2 + adj2, y + adj, x + lineWidth / 2 + adj2, y + h - adj);
        } else if (side == BorderPainter.RIGHT) {
            float offset = lineWidth / 2;
            if (lineWidth % 2 != 0) {
                offset += 1;
            }
            line = new Line2D.Float(x + w - offset + adj2, y + adj, x + w - offset + adj2, y + h - adj);
        } else if (side == BorderPainter.BOTTOM) {
            float offset = lineWidth / 2;
            if (lineWidth % 2 != 0) {
                offset += 1;
            }
            line = new Line2D.Float(x + adj, y + h - offset + adj2, x + w - adj, y + h - offset + adj2);
        }

        draw(line);
    }

    public void setColor(final FSColor color) {
        if (color instanceof FSRGBColor) {
            final FSRGBColor rgb = (FSRGBColor) color;
            _color = new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), (int) (rgb.getAlpha() * 255));
        } else if (color instanceof FSCMYKColor) {
            final FSCMYKColor cmyk = (FSCMYKColor) color;
            _color = new CMYKColor(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
        } else {
            throw new RuntimeException("internal error: unsupported color class " + color.getClass().getName());
        }
    }

    protected void drawLine(final int x1, final int y1, final int x2, final int y2) {
        final Line2D line = new Line2D.Double(x1, y1, x2, y2);
        draw(line);
    }

    public void drawRect(final int x, final int y, final int width, final int height) {
        draw(new Rectangle(x, y, width, height));
    }

    public void drawOval(final int x, final int y, final int width, final int height) {
        final Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        draw(oval);
    }

    public void fill(final Shape s) {
        followPath(s, FILL);
    }

    public void fillRect(final int x, final int y, final int width, final int height) {
        if (ROUND_RECT_DIMENSIONS_DOWN) {
            fill(new Rectangle(x, y, width - 1, height - 1));
        } else {
            fill(new Rectangle(x, y, width, height));
        }
    }

    public void fillOval(final int x, final int y, final int width, final int height) {
        final Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        fill(oval);
    }

    public void translate(final double tx, final double ty) {
        _transform.translate(tx, ty);
    }

    public Object getRenderingHint(final Key key) {
        return null;
    }

    public void setRenderingHint(final Key key, final Object value) {
    }

    public void setFont(final FSFont font) {
        _font = ((ITextFSFont) font);
    }

    private AffineTransform normalizeMatrix(final AffineTransform current) {
        final double[] mx = new double[6];
        AffineTransform result = new AffineTransform();
        result.getMatrix(mx);
        mx[3] = -1;
        mx[5] = _pageHeight;
        result = new AffineTransform(mx);
        result.concatenate(current);
        return result;
    }

    public void drawString(String s, final float x, final float y, final JustificationInfo info) {
        if (Configuration.isTrue("xr.renderer.replace-missing-characters", false)) {
            s = replaceMissingCharacters(s);
        }
        if (s.length() == 0)
            return;
        final PdfContentByte cb = _currentPage;
        ensureFillColor();
        final AffineTransform at = (AffineTransform) getTransform().clone();
        at.translate(x, y);
        final AffineTransform inverse = normalizeMatrix(at);
        final AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
        inverse.concatenate(flipper);
        inverse.scale(_dotsPerPoint, _dotsPerPoint);
        final double[] mx = new double[6];
        inverse.getMatrix(mx);
        cb.beginText();
        // Check if bold or italic need to be emulated
        boolean resetMode = false;
        final FontDescription desc = _font.getFontDescription();
        final float fontSize = _font.getSize2D() / _dotsPerPoint;
        cb.setFontAndSize(desc.getFont(), fontSize);
        float b = (float) mx[1];
        float c = (float) mx[2];
        final FontSpecification fontSpec = getFontSpecification();
        if (fontSpec != null) {
            final int need = ITextFontResolver.convertWeightToInt(fontSpec.fontWeight);
            final int have = desc.getWeight();
            if (need > have) {
                cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL_STROKE);
                final float lineWidth = fontSize * 0.04f; // 4% of font size
                cb.setLineWidth(lineWidth);
                resetMode = true;
            }
            if ((fontSpec.fontStyle == IdentValue.ITALIC) && (desc.getStyle() != IdentValue.ITALIC)) {
                b = 0f;
                c = 0.21256f;
            }
        }
        cb.setTextMatrix((float) mx[0], b, c, (float) mx[3], (float) mx[4], (float) mx[5]);
        if (info == null) {
            cb.showText(s);
        } else {
            final PdfTextArray array = makeJustificationArray(s, info);
            cb.showText(array);
        }
        if (resetMode) {
            cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
            cb.setLineWidth(1);
        }
        cb.endText();
    }

    private String replaceMissingCharacters(final String string) {
        final char[] charArr = string.toCharArray();
        final char replacementCharacter = Configuration.valueAsChar("xr.renderer.missing-character-replacement", '#');

        // first check to see if the replacement character even exists in the
        // given font. If not, then do nothing.
        if (!_font.getFontDescription().getFont().charExists(replacementCharacter)) {
            XRLog.render(Level.INFO, "Missing replacement character [" + replacementCharacter + ":" + (int) replacementCharacter
                    + "]. No replacement will occur.");
            return string;
        }

        // iterate through each character in the string and make an appropriate
        // replacement
        for (int i = 0; i < charArr.length; i++) {
            if (!(charArr[i] == ' ' || charArr[i] == '\u00a0' || charArr[i] == '\u3000' || _font.getFontDescription().getFont()
                    .charExists(charArr[i]))) {
                XRLog.render(Level.INFO, "Missing character [" + charArr[i] + ":" + (int) charArr[i] + "] in string [" + string
                        + "]. Replacing with '" + replacementCharacter + "'");
                charArr[i] = replacementCharacter;
            }
        }

        return String.valueOf(charArr);
    }

    private PdfTextArray makeJustificationArray(final String s, final JustificationInfo info) {
        final PdfTextArray array = new PdfTextArray();
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            array.add(Character.toString(c));
            if (i != len - 1) {
                float offset;
                if (c == ' ' || c == '\u00a0' || c == '\u3000') {
                    offset = info.getSpaceAdjust();
                } else {
                    offset = info.getNonSpaceAdjust();
                }
                array.add((-offset / _dotsPerPoint) * 1000 / (_font.getSize2D() / _dotsPerPoint));
            }
        }
        return array;
    }

    private AffineTransform getTransform() {
        return _transform;
    }

    private void ensureFillColor() {
        if (!(_color.equals(_fillColor))) {
            _fillColor = _color;
            _currentPage.setColorFill(_fillColor);
        }
    }

    private void ensureStrokeColor() {
        if (!(_color.equals(_strokeColor))) {
            _strokeColor = _color;
            _currentPage.setColorStroke(_strokeColor);
        }
    }

    public PdfContentByte getCurrentPage() {
        return _currentPage;
    }

    private void followPath(Shape s, final int drawType) {
        final PdfContentByte cb = _currentPage;
        if (s == null)
            return;

        if (drawType == STROKE) {
            if (!(_stroke instanceof BasicStroke)) {
                s = _stroke.createStrokedShape(s);
                followPath(s, FILL);
                return;
            }
        }
        if (drawType == STROKE) {
            setStrokeDiff(_stroke, _oldStroke);
            _oldStroke = _stroke;
            ensureStrokeColor();
        } else if (drawType == FILL) {
            ensureFillColor();
        }

        PathIterator points;
        if (drawType == CLIP) {
            points = s.getPathIterator(IDENTITY);
        } else {
            points = s.getPathIterator(_transform);
        }
        final float[] coords = new float[6];
        int traces = 0;
        while (!points.isDone()) {
            ++traces;
            final int segtype = points.currentSegment(coords);
            normalizeY(coords);
            switch (segtype) {
            case PathIterator.SEG_CLOSE:
                cb.closePath();
                break;

            case PathIterator.SEG_CUBICTO:
                cb.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                break;

            case PathIterator.SEG_LINETO:
                cb.lineTo(coords[0], coords[1]);
                break;

            case PathIterator.SEG_MOVETO:
                cb.moveTo(coords[0], coords[1]);
                break;

            case PathIterator.SEG_QUADTO:
                cb.curveTo(coords[0], coords[1], coords[2], coords[3]);
                break;
            }
            points.next();
        }

        switch (drawType) {
        case FILL:
            if (traces > 0) {
                if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                    cb.eoFill();
                else
                    cb.fill();
            }
            break;
        case STROKE:
            if (traces > 0)
                cb.stroke();
            break;
        default: // drawType==CLIP
            if (traces == 0)
                cb.rectangle(0, 0, 0, 0);
            if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                cb.eoClip();
            else
                cb.clip();
            cb.newPath();
        }
    }

    private float normalizeY(final float y) {
        return _pageHeight - y;
    }

    private void normalizeY(final float[] coords) {
        coords[1] = normalizeY(coords[1]);
        coords[3] = normalizeY(coords[3]);
        coords[5] = normalizeY(coords[5]);
    }

    private void setStrokeDiff(final Stroke newStroke, final Stroke oldStroke) {
        final PdfContentByte cb = _currentPage;
        if (newStroke == oldStroke)
            return;
        if (!(newStroke instanceof BasicStroke))
            return;
        final BasicStroke nStroke = (BasicStroke) newStroke;
        final boolean oldOk = (oldStroke instanceof BasicStroke);
        BasicStroke oStroke = null;
        if (oldOk)
            oStroke = (BasicStroke) oldStroke;
        if (!oldOk || nStroke.getLineWidth() != oStroke.getLineWidth())
            cb.setLineWidth(nStroke.getLineWidth());
        if (!oldOk || nStroke.getEndCap() != oStroke.getEndCap()) {
            switch (nStroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                cb.setLineCap(0);
                break;
            case BasicStroke.CAP_SQUARE:
                cb.setLineCap(2);
                break;
            default:
                cb.setLineCap(1);
            }
        }
        if (!oldOk || nStroke.getLineJoin() != oStroke.getLineJoin()) {
            switch (nStroke.getLineJoin()) {
            case BasicStroke.JOIN_MITER:
                cb.setLineJoin(0);
                break;
            case BasicStroke.JOIN_BEVEL:
                cb.setLineJoin(2);
                break;
            default:
                cb.setLineJoin(1);
            }
        }
        if (!oldOk || nStroke.getMiterLimit() != oStroke.getMiterLimit())
            cb.setMiterLimit(nStroke.getMiterLimit());
        boolean makeDash;
        if (oldOk) {
            if (nStroke.getDashArray() != null) {
                if (nStroke.getDashPhase() != oStroke.getDashPhase()) {
                    makeDash = true;
                } else if (!java.util.Arrays.equals(nStroke.getDashArray(), oStroke.getDashArray())) {
                    makeDash = true;
                } else
                    makeDash = false;
            } else if (oStroke.getDashArray() != null) {
                makeDash = true;
            } else
                makeDash = false;
        } else {
            makeDash = true;
        }
        if (makeDash) {
            final float dash[] = nStroke.getDashArray();
            if (dash == null)
                cb.setLiteral("[]0 d\n");
            else {
                cb.setLiteral('[');
                final int lim = dash.length;
                for (int k = 0; k < lim; ++k) {
                    cb.setLiteral(dash[k]);
                    cb.setLiteral(' ');
                }
                cb.setLiteral(']');
                cb.setLiteral(nStroke.getDashPhase());
                cb.setLiteral(" d\n");
            }
        }
    }

    public void setStroke(final Stroke s) {
        _originalStroke = s;
        this._stroke = transformStroke(s);
    }

    private Stroke transformStroke(final Stroke stroke) {
        if (!(stroke instanceof BasicStroke))
            return stroke;
        final BasicStroke st = (BasicStroke) stroke;
        final float scale = (float) Math.sqrt(Math.abs(_transform.getDeterminant()));
        final float dash[] = st.getDashArray();
        if (dash != null) {
            for (int k = 0; k < dash.length; ++k) {
              dash[k] *= scale;
            }
        }
        return new BasicStroke(st.getLineWidth() * scale, st.getEndCap(), st.getLineJoin(), st.getMiterLimit(), dash, st.getDashPhase()
                * scale);
    }

    public void clip(Shape s) {
        if (s != null) {
            s = _transform.createTransformedShape(s);
            if (_clip == null)
                _clip = new Area(s);
            else
                _clip.intersect(new Area(s));
            followPath(s, CLIP);
        } else {
            throw new XRRuntimeException("Shape is null, unexpected");
        }
    }

    public Shape getClip() {
        try {
            return _transform.createInverse().createTransformedShape(_clip);
        } catch (final NoninvertibleTransformException e) {
            return null;
        }
    }

    public void setClip(Shape s) {
        final PdfContentByte cb = _currentPage;
        cb.restoreState();
        cb.saveState();
        if (s != null)
            s = _transform.createTransformedShape(s);
        if (s == null) {
            _clip = null;
        } else {
            _clip = new Area(s);
            followPath(s, CLIP);
        }
        _fillColor = null;
        _strokeColor = null;
        _oldStroke = null;
    }

    public Stroke getStroke() {
        return _originalStroke;
    }

    public void drawImage(final FSImage fsImage, final int x, final int y) {
        if (fsImage instanceof PDFAsImage) {
            drawPDFAsImage((PDFAsImage) fsImage, x, y);
        } else {
            final Image image = ((ITextFSImage) fsImage).getImage();

            if (fsImage.getHeight() <= 0 || fsImage.getWidth() <= 0) {
                return;
            }

            final AffineTransform at = AffineTransform.getTranslateInstance(x, y);
            at.translate(0, fsImage.getHeight());
            at.scale(fsImage.getWidth(), fsImage.getHeight());

            final AffineTransform inverse = normalizeMatrix(_transform);
            final AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
            inverse.concatenate(at);
            inverse.concatenate(flipper);

            final double[] mx = new double[6];
            inverse.getMatrix(mx);

            try {
                _currentPage.addImage(image, (float) mx[0], (float) mx[1], (float) mx[2], (float) mx[3], (float) mx[4], (float) mx[5]);
            } catch (final DocumentException e) {
                throw new XRRuntimeException(e.getMessage(), e);
            }
        }
    }

    private void drawPDFAsImage(final PDFAsImage image, final int x, final int y) {
        final URL url = image.getURL();
        PdfReader reader = null;

        try {
            reader = getReader(url);
        } catch (final IOException e) {
            throw new XRRuntimeException("Could not load " + url + ": " + e.getMessage(), e);
        } catch (final URISyntaxException e) {
            throw new XRRuntimeException("Could not load " + url + ": " + e.getMessage(), e);
        }

        final PdfImportedPage page = getWriter().getImportedPage(reader, 1);

        final AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        at.translate(0, image.getHeightAsFloat());
        at.scale(image.getWidthAsFloat(), image.getHeightAsFloat());

        final AffineTransform inverse = normalizeMatrix(_transform);
        final AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
        inverse.concatenate(at);
        inverse.concatenate(flipper);

        final double[] mx = new double[6];
        inverse.getMatrix(mx);

        mx[0] = image.scaleWidth();
        mx[3] = image.scaleHeight();

        _currentPage.restoreState();
        _currentPage.addTemplate(page, (float) mx[0], (float) mx[1], (float) mx[2], (float) mx[3], (float) mx[4], (float) mx[5]);
        _currentPage.saveState();
    }

    public PdfReader getReader(final URL url) throws IOException, URISyntaxException {
        final URI uri = url.toURI();
        PdfReader result = _readerCache.get(uri);
        if (result == null) {
            result = new PdfReader(url);
            _readerCache.put(uri, result);
        }
        return result;
    }

    public float getDotsPerPoint() {
        return _dotsPerPoint;
    }

    public void start(final Document doc) {
        loadBookmarks(doc);
        loadMetadata(doc);
    }

    public void finish(final RenderingContext c, final Box root) {
        writeOutline(c, root);
        writeNamedDestinations(c);
    }

    private void writeOutline(final RenderingContext c, final Box root) {
        if (_bookmarks.size() > 0) {
            _writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);
            writeBookmarks(c, root, _writer.getRootOutline(), _bookmarks);
        }
    }

    private void writeBookmarks(final RenderingContext c, final Box root, final PdfOutline parent, final List<Bookmark> bookmarks) {
        for (final Bookmark bookmark : bookmarks) {
            writeBookmark(c, root, parent, bookmark);
        }
    }

    private void writeNamedDestinations(final RenderingContext c) {
        final Map<String, Box> idMap = getSharedContext().getIdMap();
        if ((idMap != null) && (!idMap.isEmpty())) {
            final PdfArray dests = new PdfArray();
            try {
                final Iterator<Entry<String, Box>> it = idMap.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Box> entry = it.next();

                    final Box targetBox = (Box) entry.getValue();

                    if (targetBox.getStyle().isIdent(CSSName.FS_NAMED_DESTINATION, IdentValue.CREATE)) {
                        final String anchorName = (String) entry.getKey();
                        dests.add(new PdfString(anchorName, PdfString.TEXT_UNICODE));

                        final PdfDestination dest = createDestination(c, targetBox);
                        if (dest != null) {
                            final PdfIndirectReference ref = _writer.addToBody(dest).getIndirectReference();
                            dests.add(ref);
                        }
                    }
                }

                if (!dests.isEmpty()) {
                    final PdfDictionary nametree = new PdfDictionary();
                    nametree.put(PdfName.NAMES, dests);
                    final PdfIndirectReference nameTreeRef = _writer.addToBody(nametree).getIndirectReference();

                    final PdfDictionary names = new PdfDictionary();
                    names.put(PdfName.DESTS, nameTreeRef);
                    final PdfIndirectReference destinationsRef = _writer.addToBody(names).getIndirectReference();

                    _writer.getExtraCatalog().put(PdfName.NAMES, destinationsRef);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int getPageRefY(final Box box) {
        if (box instanceof InlineLayoutBox) {
            final InlineLayoutBox iB = (InlineLayoutBox) box;
            return iB.getAbsY() + iB.getBaseline();
        } else {
            return box.getAbsY();
        }
    }

    private void writeBookmark(final RenderingContext c, final Box root, final PdfOutline parent, final Bookmark bookmark) {
        final String href = bookmark.getHRef();
        PdfDestination target = null;
        if (href.length() > 0 && href.charAt(0) == '#') {
            final Box box = _sharedContext.getBoxById(href.substring(1));
            if (box != null) {
                final PageBox page = root.getLayer().getPage(c, getPageRefY(box));
                int distanceFromTop = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
                distanceFromTop += box.getAbsY() - page.getTop();
                target = new PdfDestination(PdfDestination.XYZ, 0, normalizeY(distanceFromTop / _dotsPerPoint), 0);
                target.addPage(_writer.getPageReference(_startPageNo + page.getPageNo() + 1));
            }
        }
        if (target == null) {
            target = _defaultDestination;
        }
        final PdfOutline outline = new PdfOutline(parent, target, bookmark.getName());
        writeBookmarks(c, root, outline, bookmark.getChildren());
    }

    private void loadBookmarks(final Document doc) {
        final Element head = doc.head();
        if (head != null) {
            final Element bookmarks = JsoupUtil.firstChild(head.select("bookmarks"));
            if (bookmarks != null) {
                final Elements l = bookmarks.select("bookmark");
                if (l != null) {
                    for (final Element e : l) {
                        loadBookmark(null, e);
                    }
                }
            }
        }
    }

    private void loadBookmark(final Bookmark parent, final Element bookmark) {
        final Bookmark us = new Bookmark(bookmark.attr("name"), bookmark.attr("href"));
        if (parent == null) {
            _bookmarks.add(us);
        } else {
            parent.addChild(us);
        }
        final Elements l = bookmark.select("bookmark");
        if (l != null) {
            for (final Element e : l) {
                loadBookmark(us, e);
            }
        }
    }

    private static class Bookmark {
        private String _name;
        private String _HRef;

        private List<Bookmark> _children;

        public Bookmark() {
        }

        public Bookmark(final String name, final String href) {
            _name = name;
            _HRef = href;
        }

        public String getHRef() {
            return _HRef;
        }

        public void setHRef(final String href) {
            _HRef = href;
        }

        public String getName() {
            return _name;
        }

        public void setName(final String name) {
            _name = name;
        }

        public void addChild(final Bookmark child) {
            if (_children == null) {
                _children = new ArrayList<Bookmark>();
            }
            _children.add(child);
        }

        public List<Bookmark> getChildren() {
            return _children == null ? Collections.EMPTY_LIST : _children;
        }
    }

    // Metadata methods

    // Methods to load and search a document's metadata

    /**
     * Appends a name/content metadata pair to this output device. A name or
     * content value of null will be ignored.
     *
     * @param name
     *            the name of the metadata element to add.
     * @return the content value for this metadata.
     */
    public void addMetadata(final String name, final String value) {
        if ((name != null) && (value != null)) {
            final Metadata m = new Metadata(name, value);
            _metadata.add(m);
        }
    }

    /**
     * Searches the metadata name/content pairs of the current document and
     * returns the content value from the first pair with a matching name. The
     * search is case insensitive.
     *
     * @param name
     *            the metadata element name to locate.
     * @return the content value of the first found metadata element; otherwise
     *         null.
     */
    public String getMetadataByName(final String name) {
        if (name != null) {
            for (final Metadata m : _metadata) {
                if ((m != null) && m.getName().equalsIgnoreCase(name)) {
                    return m.getContent();
                }
            }
        }
        return null;
    }

    /**
     * Searches the metadata name/content pairs of the current document and
     * returns any content values with a matching name in an ArrayList. The
     * search is case insensitive.
     *
     * @param name
     *            the metadata element name to locate.
     * @return an ArrayList with matching content values; otherwise an empty
     *         list.
     */
    public ArrayList<String> getMetadataListByName(final String name) {
        final ArrayList<String> result = new ArrayList<String>();
        if (name != null) {
            for (final Metadata m : _metadata) {
                if ((m != null) && m.getName().equalsIgnoreCase(name)) {
                    result.add(m.getContent());
                }
            }
        }
        return result;
    }

    /**
     * Locates and stores all metadata values in the document head that contain
     * name/content pairs. If there is no pair with a name of "title", any
     * content in the title element is saved as a "title" metadata item.
     *
     * @param doc
     *            the Document level node of the parsed xhtml file.
     */
    private void loadMetadata(final Document doc) {
        final Element head = doc.head();
        if (head != null) {
            final Elements l = head.select("meta");
            if (l != null) {
                for (final Element e : l) {
                    final String name = e.attr("name");
                    if (name != null) { // ignore non-name metadata data
                        final String content = e.attr("content");
                        final Metadata m = new Metadata(name, content);
                        _metadata.add(m);
                    }
                }
            }
            // If there is no title meta data attribute, use the document title.
            String title = getMetadataByName("title");
            if (title == null) {
                final Element t = JsoupUtil.firstChild(head.select("title"));
                if (t != null) {
                    title = t.text().trim();
                    final Metadata m = new Metadata("title", title);
                    _metadata.add(m);
                }
            }
        }
    }

    /**
     * Replaces all copies of the named metadata with a single value. A a new
     * value of null will result in the removal of all copies of the named
     * metadata. Use <code>addMetadata</code> to append additional values with
     * the same name.
     *
     * @param name
     *            the metadata element name to locate.
     * @return the new content value for this metadata (null to remove all
     *         instances).
     */
    public void setMetadata(final String name, final String value) {
        if (name != null) {
            boolean remove = (value == null); // removing all instances of name?
            int free = -1; // first open slot in array
            for (int i = 0, len = _metadata.size(); i < len; i++) {
                final Metadata m = _metadata.get(i);
                if (m != null) {
                    if (m.getName().equalsIgnoreCase(name)) {
                        if (!remove) {
                            remove = true; // remove all other instances
                            m.setContent(value);
                        } else {
                            _metadata.set(i, null);
                        }
                    }
                } else if (free == -1) {
                    free = i;
                }
            }
            if (!remove) { // not found?
                final Metadata m = new Metadata(name, value);
                if (free == -1) { // no open slots?
                    _metadata.add(m);
                } else {
                    _metadata.set(free, m);
                }
            }
        }
    }

    // Class for storing metadata element name/content pairs from the head
    // section of an xhtml document.
    private static class Metadata {
        private String _name;
        private String _content;

        public Metadata(final String name, final String content) {
            _name = name;
            _content = content;
        }

        public String getContent() {
            return _content;
        }

        public void setContent(final String content) {
            _content = content;
        }

        public String getName() {
            return _name;
        }

        public void setName(final String name) {
            _name = name;
        }
    }

    // Metadata end

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void setSharedContext(final SharedContext sharedContext) {
        _sharedContext = sharedContext;
        sharedContext.getCss().setSupportCMYKColors(true);
    }

    public void setRoot(final Box root) {
        _root = root;
    }

    public int getStartPageNo() {
        return _startPageNo;
    }

    public void setStartPageNo(final int startPageNo) {
        _startPageNo = startPageNo;
    }

    public void drawSelection(final RenderingContext c, final InlineText inlineText) {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsSelection() {
        return false;
    }

    public boolean isSupportsCMYKColors() {
        return true;
    }

    public List<PagePosition> findPagePositionsByID(final CssContext c, final Pattern pattern) {
        final Map<String, Box> idMap = _sharedContext.getIdMap();
        if (idMap == null) {
            return Collections.EMPTY_LIST;
        }

        final List<PagePosition> result = new ArrayList<PagePosition>();
        for (final Entry<String, Box> entry : idMap.entrySet()) {
            final String id = (String) entry.getKey();
            if (pattern.matcher(id).find()) {
                final Box box = (Box) entry.getValue();
                final PagePosition pos = calcPDFPagePosition(c, id, box);
                if (pos != null) {
                    result.add(pos);
                }
            }
        }

        Collections.sort(result, new Comparator() {
            public int compare(final Object arg0, final Object arg1) {
                final PagePosition p1 = (PagePosition) arg0;
                final PagePosition p2 = (PagePosition) arg1;
                return p1.getPageNo() - p2.getPageNo();
            }
        });

        return result;
    }

    private PagePosition calcPDFPagePosition(final CssContext c, final String id, final Box box) {
        final PageBox page = _root.getLayer().getLastPage(c, box);
        if (page == null) {
            return null;
        }

        float x = box.getAbsX() + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
        float y = (page.getBottom() - (box.getAbsY() + box.getHeight())) + page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM);
        x /= _dotsPerPoint;
        y /= _dotsPerPoint;

        final PagePosition result = new PagePosition();
        result.setId(id);
        result.setPageNo(page.getPageNo());
        result.setX(x);
        result.setY(y);
        result.setWidth(box.getEffectiveWidth() / _dotsPerPoint);
        result.setHeight(box.getHeight() / _dotsPerPoint);

        return result;
    }

	@Override
	public void draw(final Shape s) {
		followPath(s, STROKE);
		
	}

	@Override
	public void drawBorderLine(final Shape bounds, final int side, final int width, final boolean solid) {
		draw(bounds);
		
	}

	@Override
	public void drawLinearGradient(final FSLinearGradient gradient, final int x, final int y,
			final int width, final int height) 
	{
		final FSRGBColor start = (FSRGBColor) gradient.getStopPoints().get(0).getColor();
		final FSRGBColor end = (FSRGBColor) gradient.getStopPoints().get(gradient.getStopPoints().size() - 1).getColor();
		final Color s = new Color(start.getRed(), start.getGreen(), start.getBlue());
		final Color e = new Color(end.getRed(), end.getGreen(), end.getBlue());
		final PdfShading shader = PdfShading.simpleAxial(_writer, x, y, x + width, y + height, s, e);
		_currentPage.setShadingFill(new PdfShadingPattern(shader));
		_currentPage.paintShading(shader);
	}

	@Override
	public void setOpacity(final float opacity) {
		if (opacity != 1f || haveOpacity)
		{
			final PdfGState gs = new PdfGState();
			gs.setBlendMode(PdfGState.BM_NORMAL);
			gs.setFillOpacity(opacity);
			_currentPage.setGState(gs);
			haveOpacity = true;
		}
	}
}
