/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package org.xhtmlrenderer.layout;

import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.context.AWTFontResolver;
import org.xhtmlrenderer.context.StyleReference;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.EmptyStyle;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FSCanvas;
import org.xhtmlrenderer.extend.FontContext;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.Java2DTextRenderer;
import org.xhtmlrenderer.util.XRLog;

/**
 * The SharedContext is that which is kept between successive
 * layout and render runs.
 *
 * @author empty
 */
public class SharedContext {
    private TextRenderer text_renderer;
    private String media;
    private UserAgentCallback uac;

    private boolean interactive = true;

    private Map<String, Box> idMap;

    private StylesheetInfo defaultStylesheet;
    private boolean lookedUpDefaultStylesheet;
    private Locale localeTextBreaker = Locale.US;
    private final Locale localeErrorMessages = Locale.US;

    public static final ThreadLocal<ResourceBundle> ERRS = new ThreadLocal<>();    
    public static final ThreadLocal<Set<String>> USER_ERRORS = new ThreadLocal<>();
    
    /*
     * used to adjust fonts, ems, points, into screen resolution
     */
    /**
     * Description of the Field
     */
    private float dpi;
    /**
     * Description of the Field
     */
    private final static int MM__PER__CM = 10;
    /**
     * Description of the Field
     */
    private final static float CM__PER__IN = 2.54F;
    /**
     * dpi in a more usable way
     */
    private float mm_per_dot;

    private final static float DEFAULT_DPI = 72;
    private boolean print;

    private int dotsPerPixel = 1;

    private Map<Element, CalculatedStyle> styleMap;

    private ReplacedElementFactory replacedElementFactory;
    private Rectangle temp_canvas;

    public SharedContext()
    {
    	ERRS.remove();
    	ERRS.set(ResourceBundle.getBundle("languages.ErrorMessages", localeErrorMessages));
    	
    	USER_ERRORS.remove();
    	USER_ERRORS.set(new LinkedHashSet<String>());
    }

    /**
     * Constructor for the Context object
     */
    public SharedContext(final UserAgentCallback uac) {
    	this();
    	
    	font_resolver = new AWTFontResolver();
        replacedElementFactory = new ReplacedElementFactory() {
			
			@Override
			public void setFormSubmissionListener(final FormSubmissionListener listener) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void reset() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void remove(final Element e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public ReplacedElement createReplacedElement(final LayoutContext c, final BlockBox box,
					final UserAgentCallback uac, final int cssWidth, final int cssHeight) {
				// TODO Auto-generated method stub
				return null;
			}
		}; // TODO new SwingReplacedElementFactory();
        setMedia("screen");
        this.uac = uac;
        setCss(new StyleReference(uac));
        XRLog.render("Using CSS implementation from: " + getCss().getClass().getName());
        setTextRenderer(new Java2DTextRenderer());
        try {
            setDPI(Toolkit.getDefaultToolkit().getScreenResolution());
        } catch (final HeadlessException e) {
            setDPI(DEFAULT_DPI);
        }
    }


    /**
     * Constructor for the Context object
     */
    public SharedContext(final UserAgentCallback uac, final FontResolver fr, final ReplacedElementFactory ref, final TextRenderer tr, final float dpi) {
    	this();
    	
    	font_resolver = fr;
        replacedElementFactory = ref;
        setMedia("screen");
        this.uac = uac;
        setCss(new StyleReference(uac));
        XRLog.render("Using CSS implementation from: " + getCss().getClass().getName());
        setTextRenderer(tr);
        setDPI(dpi);
    }

    public void setFormSubmissionListener(final FormSubmissionListener fsl) {
        replacedElementFactory.setFormSubmissionListener(fsl);
    }

    public LayoutContext newLayoutContextInstance() {
        final LayoutContext c = new LayoutContext(this);
        return c;
    }

    public RenderingContext newRenderingContextInstance() {
        final RenderingContext c = new RenderingContext(this);
        return c;
    }

    /*
=========== Font stuff ============== */

    /**
     * Gets the fontResolver attribute of the Context object
     *
     * @return The fontResolver value
     */
    public FontResolver getFontResolver() {
        return font_resolver;
    }

    public void flushFonts() {
        font_resolver.flushCache();
    }

    /**
     * Description of the Field
     */
    protected FontResolver font_resolver;

    /**
     * The media for this context
     */
    public String getMedia() {
        return media;
    }

    /**
     * Description of the Field
     */
    protected StyleReference css;

    /**
     * Description of the Field
     */
    protected boolean debug_draw_boxes;

    /**
     * Description of the Field
     */
    protected boolean debug_draw_line_boxes;
    protected boolean debug_draw_inline_boxes;
    protected boolean debug_draw_font_metrics;

    /**
     * Description of the Field
     */
    protected FSCanvas canvas;

    /*
     * selection management code
     */
    /**
     * Description of the Field
     */
    protected Box selection_start, selection_end;

    /**
     * Description of the Field
     */
    protected int selection_end_x, selection_start_x;


    /**
     * Description of the Field
     */
    protected boolean in_selection = false;

    public TextRenderer getTextRenderer() {
        return text_renderer;
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawBoxes() {
        return debug_draw_boxes;
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawLineBoxes() {
        return debug_draw_line_boxes;
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawInlineBoxes() {
        return debug_draw_inline_boxes;
    }

    public boolean debugDrawFontMetrics() {
        return debug_draw_font_metrics;
    }

    public void setDebug_draw_boxes(final boolean debug_draw_boxes) {
        this.debug_draw_boxes = debug_draw_boxes;
    }

    public void setDebug_draw_line_boxes(final boolean debug_draw_line_boxes) {
        this.debug_draw_line_boxes = debug_draw_line_boxes;
    }

    public void setDebug_draw_inline_boxes(final boolean debug_draw_inline_boxes) {
        this.debug_draw_inline_boxes = debug_draw_inline_boxes;
    }

    public void setDebug_draw_font_metrics(final boolean debug_draw_font_metrics) {
        this.debug_draw_font_metrics = debug_draw_font_metrics;
    }


    /*
=========== Selection Management ============== */


    public StyleReference getCss() {
        return css;
    }

    public void setCss(final StyleReference css) {
        this.css = css;
    }

    public FSCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(final FSCanvas canvas) {
        this.canvas = canvas;
    }

    public void set_TempCanvas(final Rectangle rect) {
        this.temp_canvas = rect;
    }


    public Rectangle getFixedRectangle() {
        //Uu.p("this = " + canvas);
        if (getCanvas() == null) {
            return this.temp_canvas;
        } else {
            final Rectangle rect = getCanvas().getFixedRectangle();
            rect.translate(getCanvas().getX(), getCanvas().getY());
            return rect;
        }
    }

    private NamespaceHandler namespaceHandler;

    public void setNamespaceHandler(final NamespaceHandler nh) {
        namespaceHandler = nh;
    }

    public NamespaceHandler getNamespaceHandler() {
        return namespaceHandler;
    }

    public void addBoxId(final String id, final Box box) {
        if (idMap == null) {
            idMap = new HashMap<String, Box>();
        }
        idMap.put(id, box);
    }

    public Box getBoxById(final String id) {
        if (idMap == null) {
            idMap = new HashMap<String, Box>();
        }
        return idMap.get(id);
    }

    public void removeBoxId(final String id) {
        if (idMap != null) {
            idMap.remove(id);
        }
    }

    public Map<String, Box> getIdMap()
    {
        return idMap;
    }

    /**
     * Sets the textRenderer attribute of the RenderingContext object
     *
     * @param text_renderer The new textRenderer value
     */
    public void setTextRenderer(final TextRenderer text_renderer) {
        this.text_renderer = text_renderer;
    }// = "screen";

    /**
     * <p/>
     * <p/>
     * Set the current media type. This is usually something like <i>screen</i>
     * or <i>print</i> . See the <a href="http://www.w3.org/TR/CSS21/media.html">
     * media section</a> of the CSS 2.1 spec for more information on media
     * types.</p>
     *
     * @param media The new media value
     */
    public void setMedia(final String media) {
        this.media = media;
    }

    /**
     * Gets the uac attribute of the RenderingContext object
     *
     * @return The uac value
     */
    public UserAgentCallback getUac() {
        return uac;
    }

    public UserAgentCallback getUserAgentCallback() {
        return uac;
    }

    public void setUserAgentCallback(final UserAgentCallback userAgentCallback) {
        final StyleReference styleReference = getCss();
        if (styleReference != null) {
            styleReference.setUserAgentCallback(userAgentCallback);
        }
        uac = userAgentCallback;
    }

    /**
     * Gets the dPI attribute of the RenderingContext object
     *
     * @return The dPI value
     */
    public float getDPI() {
        return this.dpi;
    }

    /**
     * Sets the effective DPI (Dots Per Inch) of the screen. You should normally
     * never need to override the dpi, as it is already set to the system
     * default by <code>Toolkit.getDefaultToolkit().getScreenResolution()</code>
     * . You can override the value if you want to scale the fonts for
     * accessibility or printing purposes. Currently the DPI setting only
     * affects font sizing.
     *
     * @param dpi The new dPI value
     */
    public void setDPI(final float dpi) {
        this.dpi = dpi;
        this.mm_per_dot = (CM__PER__IN * MM__PER__CM) / dpi;
    }

    /**
     * Gets the dPI attribute in a more useful form of the RenderingContext object
     *
     * @return The dPI value
     */
    public float getMmPerPx() {
        return this.mm_per_dot;
    }

    public FSFont getFont(final FontSpecification spec) {
        return getFontResolver().resolveFont(this, spec);
    }

    //strike-through offset should always be half of the height of lowercase x...
    //and it is defined even for fonts without 'x'!
    public float getXHeight(final FontContext fontContext, final FontSpecification fs) {
        final FSFont font = getFontResolver().resolveFont(this, fs);
        final FSFontMetrics fm = getTextRenderer().getFSFontMetrics(fontContext, font, " ");
        final float sto = fm.getStrikethroughOffset();
        return fm.getAscent() - 2 * Math.abs(sto) + fm.getStrikethroughThickness();
    }

    /**
     * Gets the baseURL attribute of the RenderingContext object
     *
     * @return The baseURL value
     */
    public String getBaseURL() {
        return uac.getBaseURL();
    }

    /**
     * Sets the baseURL attribute of the RenderingContext object
     *
     * @param url The new baseURL value
     */
    public void setBaseURL(final String url) {
        uac.setBaseURL(url);
    }

    /**
     * Returns true if the currently set media type is paged. Currently returns
     * true only for <i>print</i> , <i>projection</i> , and <i>embossed</i> ,
     * <i>handheld</i> , and <i>tv</i> . See the <a
     * href="http://www.w3.org/TR/CSS21/media.html">media section</a> of the CSS
     * 2.1 spec for more information on media types.
     *
     * @return The paged value
     */
    public boolean isPaged() {
        if (media.equals("print")) {
            return true;
        }
        if (media.equals("projection")) {
            return true;
        }
        if (media.equals("embossed")) {
            return true;
        }
        if (media.equals("handheld")) {
            return true;
        }
        if (media.equals("tv")) {
            return true;
        }
        return false;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(final boolean interactive) {
        this.interactive = interactive;
    }

    public boolean isPrint() {
        return print;
    }

    public void setPrint(final boolean print) {
        this.print = print;
        if (print) {
            setMedia("print");
        } else {
            setMedia("screen");
        }
    }

    /**
     * <p/>
     * <p/>
     * Adds or overrides a font mapping, meaning you can associate a particular
     * font with a particular string. For example, the following would load a
     * font out of the cool.ttf file and associate it with the name <i>CoolFont
     * </i>:</p> <p/>
     * <p/>
     * <pre>
     *   Font font = Font.createFont(Font.TRUETYPE_FONT,
     *   new FileInputStream("cool.ttf");
     *   setFontMapping("CoolFont", font);
     * </pre> <p/>
     * <p/>
     * <p/>
     * <p/>
     * You could then put the following css in your page </p> <pre>
     *   p { font-family: CoolFont Arial sans-serif; }
     * </pre> <p/>
     * <p/>
     * <p/>
     * <p/>
     * You can also override existing font mappings, like replacing Arial with
     * Helvetica.</p>
     *
     * @param name The new font name
     * @param font The actual Font to map
     */
    /*
     * add a new font mapping, or replace an existing one
     */
    public void setFontMapping(final String name, final Font font) {
        final FontResolver resolver = getFontResolver();
        if (resolver instanceof AWTFontResolver) {
            ((AWTFontResolver)resolver).setFontMapping(name, font);
        }
    }

    public void setFontResolver(final FontResolver resolver) {
        font_resolver = resolver;
    }

    public int getDotsPerPixel() {
        return dotsPerPixel;
    }

    public void setDotsPerPixel(final int pixelsPerDot) {
        this.dotsPerPixel = pixelsPerDot;
    }

    public CalculatedStyle getStyle(final Element e) {
        return getStyle(e, false);
    }

    public CalculatedStyle getStyle(final Element e, final boolean restyle) {
        if (styleMap == null) {
            styleMap = new HashMap<Element, CalculatedStyle>(1024, 0.75f);
        }

        CalculatedStyle result = null;
        if (! restyle) {
            result = styleMap.get(e);
        }
        if (result == null) {
            CalculatedStyle parentCalculatedStyle;
        	
        	if (e instanceof Document)
        	{
        		parentCalculatedStyle = new EmptyStyle();
        	}
        	else
        	{
        		final Node parent = e.parentNode();

        		if (parent instanceof Document) {
        			parentCalculatedStyle = new EmptyStyle();
        		} else {
        			parentCalculatedStyle = getStyle((Element)parent, false);
        		}
        	}

            result = parentCalculatedStyle.deriveStyle(getCss().getCascadedStyle(e, restyle));

            styleMap.put(e, result);
        }

        return result;
    }

    public void reset() {
       styleMap = null;
       idMap = null;
       replacedElementFactory.reset();
    }

    public ReplacedElementFactory getReplacedElementFactory() {
        return replacedElementFactory;
    }

    public void setReplacedElementFactory(final ReplacedElementFactory ref) {
        if (ref == null) {
            throw new NullPointerException("replacedElementFactory may not be null");
        }

        if (this.replacedElementFactory != null) {
            this.replacedElementFactory.reset();
        }
        this.replacedElementFactory = ref;
    }

    public void removeElementReferences(final Element e) {
        final String id = namespaceHandler.getID(e);
        if (id != null && id.length() > 0) {
            removeBoxId(id);
        }

        if (styleMap != null) {
            styleMap.remove(e);
        }

        getCss().removeStyle(e);
        getReplacedElementFactory().remove(e);

        if (e.childNodeSize() > 0) {
            for (final Node child : e.childNodes())  {
            	if (child instanceof Element) {
            		removeElementReferences((Element) child);
            	}
            }
        }
    }

	public StylesheetInfo getDefaultStylesheet() 
	{
		return defaultStylesheet;
	}

	public void setDefaultStylesheet(final StylesheetInfo defaultStylesheet) 
	{
		this.defaultStylesheet = defaultStylesheet;
	}

	public boolean haveLookedUpDefaultStylesheet()
	{
		return lookedUpDefaultStylesheet;
	}

	public void setLookedUpDefaultStylesheet(final boolean lookedUpDefaultStylesheet)
	{
		this.lookedUpDefaultStylesheet = lookedUpDefaultStylesheet;
	}

	public Locale getLocale() {
		return localeTextBreaker;
	}

	public void setLocale(final Locale locale) {
		this.localeTextBreaker = locale;
	}
}
