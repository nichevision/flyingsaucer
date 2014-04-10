/*
 * StyleReference.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
 *
 */
package org.xhtmlrenderer.context;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.extend.AttributeResolver;
import org.xhtmlrenderer.css.extend.lib.DOMTreeResolver;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.newmatch.PageInfo;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.sheet.FontFaceRule;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.extend.UserInterface;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.util.XRLog;


/**
 * @author Torbjoern Gannholm
 */
public class StyleReference {
    /**
     * The Context this StyleReference operates in; used for property
     * resolution.
     */
    private SharedContext _context;
    private NamespaceHandler _nsh;
    private Document _doc;
    private final StylesheetFactoryImpl _stylesheetFactory;

    /**
     * Instance of our element-styles matching class. Will be null if new rules
     * have been added since last match.
     */
    private org.xhtmlrenderer.css.newmatch.Matcher _matcher;

    /** */
    private UserAgentCallback _uac;
    
    public StyleReference(final UserAgentCallback userAgent) {
        _uac = userAgent;
        _stylesheetFactory = new StylesheetFactoryImpl(userAgent);
    }

    /**
     * Sets the documentContext attribute of the StyleReference object
     */
    public void setDocumentContext(final SharedContext context, final NamespaceHandler nsh, final Document doc, final UserInterface ui) {
        _context = context;
        _nsh = nsh;
        _doc = doc;
        final AttributeResolver attRes = new StandardAttributeResolver(_nsh, _uac, ui);

        final List<StylesheetInfo> infos = getStylesheets();
        XRLog.match("media = " + _context.getMedia());
        _matcher = new org.xhtmlrenderer.css.newmatch.Matcher(
                new DOMTreeResolver(), 
                attRes, 
                _stylesheetFactory, 
                readAndParseAll(infos, _context.getMedia()), 
                _context.getMedia());
    }
    
    private List<Stylesheet> readAndParseAll(final List<StylesheetInfo> infos, final String medium) {
        final List<Stylesheet> result = new ArrayList<Stylesheet>(infos.size() + 15);
        for (final StylesheetInfo info : infos) {
            if (info.appliesToMedia(medium)) {
                Stylesheet sheet = info.getStylesheet();
                
                if (sheet == null) {
                    sheet = _stylesheetFactory.getStylesheet(info);
                }
                
                if (sheet.getImportRules().size() > 0) {
                    result.addAll(readAndParseAll(sheet.getImportRules(), medium));
                }
                
                result.add(sheet);
            }
        }
        
        return result;
    }
    
    public boolean isHoverStyled(final Element e) {
        return _matcher.isHoverStyled(e);
    }

    /**
     * Returns a Map keyed by CSS property names (e.g. 'border-width'), and the
     * assigned value as a SAC CSSValue instance. The properties should have
     * been matched to the element when the Context was established for this
     * StyleReference on the Document to which the Element belongs. See {@link
     * org.xhtmlrenderer.swing.BasicPanel#setDocument(Document, java.net.URL)}
     * for an example of how to establish a StyleReference and associate to a
     * Document.
     *
     * @param e The DOM Element for which to find properties
     * @return Map of CSS property names to CSSValue instance assigned to it.
     */
    public java.util.Map<String, PropertyValue> getCascadedPropertiesMap(final Element e) {
        final CascadedStyle cs = _matcher.getCascadedStyle(e, false);//this is only for debug, I think
        final java.util.LinkedHashMap<String, PropertyValue> props = new java.util.LinkedHashMap<>();
        for (final java.util.Iterator<PropertyDeclaration> i = cs.getCascadedPropertyDeclarations(); i.hasNext();) {
            final PropertyDeclaration pd = i.next();

            final String propName = pd.getPropertyName();
            final CSSName cssName = CSSName.getByPropertyName(propName);
            props.put(propName, cs.propertyByName(cssName).getValue());
        }
        return props;
    }

    /**
     * Gets the pseudoElementStyle attribute of the StyleReference object
     */
    public CascadedStyle getPseudoElementStyle(final Node node, final String pseudoElement) {
        Element e = null;
        if (node instanceof Element) {
            e = (Element) node;
        } else {
            e = (Element) node.parent();
        }
        return _matcher.getPECascadedStyle(e, pseudoElement);
    }

    /**
     * Gets the CascadedStyle for an element. This must then be converted in the
     * current context to a CalculatedStyle (use getDerivedStyle)
     */
    public CascadedStyle getCascadedStyle(final Element e, final boolean restyle) {
        if (e == null) return CascadedStyle.emptyCascadedStyle;
        return _matcher.getCascadedStyle(e, restyle);
    }
    
    public PageInfo getPageStyle(final String pageName, final String pseudoPage) {
        return _matcher.getPageCascadedStyle(pageName, pseudoPage);
    }

    /**
     * Flushes any stylesheet associated with this stylereference (based on the user agent callback) that are in cache.
     */
    public void flushStyleSheets() {
        final String uri = _uac.getBaseURL();
        final StylesheetInfo info = new StylesheetInfo();
        info.setUri(uri);
        info.setOrigin(StylesheetInfo.CSSOrigin.AUTHOR);
        if (_stylesheetFactory.getUac().getStylesheetCache().containsStylesheet(uri)) {
            _stylesheetFactory.getUac().getStylesheetCache().removeCachedStylesheet(uri);
            XRLog.cssParse("Removing stylesheet '" + uri + "' from cache by request.");
        } else {
            XRLog.cssParse("Requested removing stylesheet '" + uri + "', but it's not in cache.");
        }
    }
    
    public void flushAllStyleSheets() {
        _stylesheetFactory.getUac().getStylesheetCache().flushCachedStylesheets();
    }

    /**
     * Gets StylesheetInfos for all stylesheets and inline styles associated
     * with the current document. Default (user agent) stylesheet and the inline
     * style for the current media are loaded and cached in the
     * StyleSheetFactory by URI.
     *
     * @return The stylesheets value
     */
    private List<StylesheetInfo> getStylesheets() {
        final List<StylesheetInfo> infos = new LinkedList<StylesheetInfo>();
        final long st = System.currentTimeMillis();

        if (!_context.haveLookedUpDefaultStylesheet())
        {
        	_context.setLookedUpDefaultStylesheet(true);
        	final StylesheetInfo defaultStylesheet = _nsh.getDefaultStylesheet(_stylesheetFactory);

        	if (defaultStylesheet != null) 
        	{
        		_context.setDefaultStylesheet(defaultStylesheet);
        		infos.add(defaultStylesheet);
        	}
        }
        else if (_context.getDefaultStylesheet() != null)
        {
        	infos.add(_context.getDefaultStylesheet());
        }
        
        
        final List<StylesheetInfo> refs = _nsh.getStylesheets(_doc);
        int inlineStyleCount = 0;
        if (refs != null) {
            for (int i = 0; i < refs.size(); i++) {
                String uri;
                
                if (! refs.get(i).isInline()) {
                    uri = _uac.resolveURI(refs.get(i).getUri());
                    refs.get(i).setUri(uri);
                } else {
                    refs.get(i).setUri(_uac.getBaseURL() + "#inline_style_" + (++inlineStyleCount));
                    final Stylesheet sheet = _stylesheetFactory.parse(
                            new StringReader(refs.get(i).getContent()), refs.get(i));
                    refs.get(i).setStylesheet(sheet);
                    refs.get(i).setUri(null);
                }
            }
        }
        infos.addAll(refs);

        // TODO: here we should also get user stylesheet from userAgent

        final long el = System.currentTimeMillis() - st;
        XRLog.load("TIME: parse stylesheets  " + el + "ms");

        return infos;
    }
    
    public void removeStyle(final Element e) {
        if (_matcher != null) {
            _matcher.removeStyle(e);
        }
    }
    
    public List<FontFaceRule> getFontFaceRules() {
        return _matcher.getFontFaceRules();
    }
    
    public void setUserAgentCallback(final UserAgentCallback userAgentCallback) {
        _uac = userAgentCallback;
        _stylesheetFactory.setUserAgentCallback(userAgentCallback);
    }
    
    public void setSupportCMYKColors(final boolean b) {
        _stylesheetFactory.setSupportCMYKColors(b);
    }
}
