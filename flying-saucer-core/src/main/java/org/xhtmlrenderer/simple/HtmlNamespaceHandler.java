/*
 *
 * XhtmlDocument.java
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

package org.xhtmlrenderer.simple;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.xhtmlrenderer.css.extend.StylesheetFactory;
import org.xhtmlrenderer.css.extend.TreeResolver;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.simple.xhtml.XhtmlForm;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.XRLog;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

/**
 * Handles a general HTML document
 */
public class HtmlNamespaceHandler implements NamespaceHandler 
{
    private Map<String, String> _metadata = null;
	
	@Override
	public String getAttributeValue(final Element e, final String attrName) 
    {
        return e.attr(attrName);
    }
    
    @Override
    public String getClass(final Element e) 
    {
        return e.attr("class");
    }
    
    @Override
    public String getID(final Element e) 
    {
    	if (!e.hasAttr("id"))
    		return null;
    	
    	final String result = e.attr("id").trim();
        return result.isEmpty() ? null : result;
    }
    
    @Override
    public String getAttributeValue(final Element e, final String namespaceURI, final String attrName) 
    {
        if (namespaceURI == TreeResolver.NO_NAMESPACE) 
        {
            return e.attr(attrName);
        }
        else if (namespaceURI == null)
        {
            if (e.nodeName().indexOf(':') == -1)
            {
            	// No namespace case.
            	return e.attr(attrName);
            }
            else
            {
            	// Has namespace, try stripping the namespace from attribute
            	// names and comparing with the local part only.
            	final List<Attribute> attrs = e.attributes().asList();
            	
            	for (final Attribute attr : attrs) 
            	{
                	String key = attr.getKey();

                	if (key.indexOf(':') != -1)
                	{
                		key = key.substring(key.indexOf(':') + 1);
                	}

                	// Namspaces other than HTML may be case sensitive.
                	if (attrName.equals(key)) 
                	{
                    	return attr.getValue();
                    }
                }
                
                return null;
            }
        } 
        else 
        {
            return e.attr(namespaceURI + ':' + attrName);
        }
    }

    protected StylesheetInfo readLinkElement(final Node link)
    {
    	if (link.hasAttr("rel") &&
    		link.attr("rel").contains("alternate"))
    		return null;

    	if (link.hasAttr("type") &&
    		!ciEquals(link.attr("type"), "text/css"))
    		return null;

    	final StylesheetInfo info = new StylesheetInfo();

        info.setType("text/css");
        info.setOrigin(StylesheetInfo.CSSOrigin.AUTHOR);
        info.setUri(link.attr("href"));
        info.setTitle(link.attr("title"));
        
        if (!link.hasAttr("media") || link.attr("media").isEmpty()) 
            info.setMedia("all");
        else
        	info.setMedia(link.attr("media"));

        return info;
    }
    
    @Override
    public List<StylesheetInfo> getStylesheets(final Document doc) 
    {
    	final List<StylesheetInfo> list = new ArrayList<>();

    	// Style and link elements should only appear in the head element.
    	final List<Node> nl = doc.head().childNodes();

        for (final Node node : nl) 
        {
        	if (ciEquals(node.nodeName(), "link"))
        	{
        		final StylesheetInfo info = readLinkElement(node);

        		if (info != null)
        			list.add(info);

        		continue;
        	}
        	else if (!ciEquals(node.nodeName(), "style"))
            	continue;
        	
            final Node piNode = node;

            if (piNode.hasAttr("alternate") && 
            	ciEquals(piNode.attr("alternate"), "yes"))
            {
                // TODO: handle alternate stylesheets
            	XRLog.cssParse(Level.INFO, "Alternate stylesheet not handled");
            	continue;
            }
            else if (piNode.hasAttr("type") &&
            		 !ciEquals(piNode.attr("type"), "text/css"))
            {
            	// TODO: handle other stylesheet types
            	XRLog.cssParse(Level.INFO, "Style type other than CSS not handled");
            	continue;
            }

            final StylesheetInfo info = new StylesheetInfo();
            
            info.setOrigin(StylesheetInfo.CSSOrigin.AUTHOR);
            info.setType("text/css");
            info.setUri(piNode.attr("href"));
           	info.setTitle(piNode.attr("title"));

           	if (piNode.hasAttr("media") &&
            	!piNode.attr("media").isEmpty())
            	info.setMedia(piNode.attr("media"));
            else
            	info.setMedia("all");

            // Deal with the common case first.
            if (piNode.childNodeSize() == 1 &&
            	piNode.childNode(0) instanceof DataNode)
            {
                info.setContent(((DataNode) piNode.childNode(0)).getWholeData());
            }
            else
            {
            	final String content = readTextContent((Element) piNode);

            	if (!content.isEmpty())
            	   	info.setContent(content);
            }
            
            list.add(info);
        }

        return list;
    }

    @Override
    public String getLang(final Element e) 
    {
        if (!e.hasAttr("lang") || e.attr("lang").isEmpty()) 
        {
            final String lang = this.getMetaInfo(e.ownerDocument()).get("Content-Language");
            return lang == null ? "" : lang;
        }

        return e.attr("lang");
    }
    
    @Override
    public boolean isImageElement(final Element e) 
    {
        return (e != null && ciEquals(e.nodeName(), "img"));
    }
    
    @Override
    public boolean isFormElement(final Element e) 
    {
        return (e != null && ciEquals(e.nodeName(), "form"));
    }

    @Override
    public String getImageSourceURI(final Element e) 
    {
        return (e != null ? e.attr("src") : null);
    }

    @Override
    public String getNonCssStyling(final Element e) 
    {
    	switch(e.nodeName().toLowerCase(Locale.US))
    	{
    	case "table":
    		return applyTableStyles(e);
    	case "tr":
    		return applyTableRowStyles(e);
    	case "td": /* Fall through */
    	case "th":
    		return applyTableCellStyles(e);
    	case "img":
    		return applyImgStyles(e);
    	case "p": /* Fall through */
    	case "div":
            return applyTextAlign(e);
    	default:
    		return "";
    	}
    }
    
    private String applyTextAlign(final Element e) 
    {
    	final StringBuilder style = new StringBuilder();
    	String s;
        s = getAttribute(e, "align");
        if (s != null) {
            s = s.toLowerCase(Locale.US).trim();
            if (s.equals("left") || s.equals("right") || 
                    s.equals("center") || s.equals("justify")) {
                style.append("text-align: ");
                style.append(s);
                style.append(";");
            }
        }
        return style.toString();
    }
    
    private String applyImgStyles(final Element e)
    {
        final StringBuilder style = new StringBuilder();
        applyFloatingAlign(e, style);
        return style.toString();
    }

    private String applyTableCellStyles(final Element e) 
    {
        final StringBuilder style = new StringBuilder();
        String s;

        // Check for cellpadding
        final Element table = findTable(e);
        
        if (table != null) 
        {
            s = getAttribute(table, "cellpadding");
            if (s != null) 
            {
                style.append("padding: ");
                style.append(convertToLength(s));
                style.append(";");
            }

            s = getAttribute(table, "border");

            if (s != null && !s.equals("0")) 
            {
                style.append("border: 1px outset black;");
            }
        }

        s = getAttribute(e, "width");

        if (s != null) 
        {
            style.append("width: ");
            style.append(convertToLength(s));
            style.append(";");
        }
        
        s = getAttribute(e, "height");

        if (s != null) 
        {
            style.append("height: ");
            style.append(convertToLength(s));
            style.append(";");
        }        

        applyAlignment(e, style);
        s = getAttribute(e, "bgcolor");

        if (s != null) 
        {
            s = s.toLowerCase(Locale.US);
            style.append("background-color: ");

            if (looksLikeAMangledColor(s)) 
            {
                style.append('#');
                style.append(s);
            }
            else
            {
                style.append(s);
            }
            style.append(';');
        }

        s = getAttribute(e, "background");
        
        if (s != null) 
        {
            style.append("background-image: url(");
            style.append(s);
            style.append(");");
        }

        return style.toString();
    }

    private String applyTableStyles(final Element e)
    {
        final StringBuilder style = new StringBuilder();
        String s;
        
        s = getAttribute(e, "width");

        if (s != null) 
        {
            style.append("width: ");
            style.append(convertToLength(s));
            style.append(";");
        }
        
        s = getAttribute(e, "border");

        if (s != null) 
        {
            style.append("border: ");
            style.append(convertToLength(s));
            style.append(" inset black;");
        }
        
        s = getAttribute(e, "cellspacing");
        
        if (s != null) 
        {
        	style.append("border-collapse: separate; border-spacing: ");
            style.append(convertToLength(s));
            style.append(";");
        }
        
        s = getAttribute(e, "bgcolor");

        if (s != null) 
        {
            s = s.toLowerCase();
            style.append("background-color: ");
            if (looksLikeAMangledColor(s)) 
            {
                style.append('#');
                style.append(s);
            }
            else
            {
                style.append(s);
            }
            style.append(';');
        }

        s = getAttribute(e, "background");

        if (s != null) 
        {
            style.append("background-image: url(");
            style.append(s);
            style.append(");");
        }

        applyFloatingAlign(e, style);
        return style.toString();
    }
    
    private String applyTableRowStyles(final Element e)
    {
        final StringBuilder style = new StringBuilder();
        applyAlignment(e, style);
        return style.toString();
    }
    
    private void applyFloatingAlign(final Element e, final StringBuilder style) 
    {
        String s;
        s = getAttribute(e, "align");
        if (s != null) {
            s = s.toLowerCase(Locale.US).trim();
            if (s.equals("left")) {
                style.append("float: left;");
            } else if (s.equals("right")) {
                style.append("float: right;");
            } else if (s.equals("center")) {
                style.append("margin-left: auto; margin-right: auto;");
            }
        }
    }
    
    private void applyAlignment(final Element e, final StringBuilder style) 
    {
        String s;
        s = getAttribute(e, "align");
        if (s != null) {
            style.append("text-align: ");
            style.append(s.toLowerCase());
            style.append(";");
        }
        s = getAttribute(e, "valign");
        if (s != null) {
            style.append("vertical-align: ");
            style.append(s.toLowerCase());
            style.append(";");
        }
    }
    
    private boolean looksLikeAMangledColor(final String s) 
    {
        if (s.length() != 6) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            final boolean valid = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!valid) {
                return false;
            }
        }
        return true;
    }
    
    private Element findTable(final Element cell) 
    {
        Node n = cell.parentNode();
        Element next;

        if (n instanceof Element) {
            next = (Element) n;
            if (ciEquals(next.nodeName(), "tr")) 
            {
                n = next.parentNode();
                if (n instanceof Element) {
                    next = (Element) n;
                    final String name = next.nodeName();
                    if (ciEquals(name, "table")) {
                        return next;
                    }
                    
                    if (ciEquals(name, "tbody") || ciEquals(name, "tfoot") || ciEquals(name, "thead")) 
                    {
                        n = next.parentNode();
                        if (n instanceof Element) {
                            next = (Element) n;
                            if (ciEquals(next.nodeName(), "table")) {
                                return next;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    public XhtmlForm createForm(final Element e)
    {
        if(e == null) {
            return new XhtmlForm("", "get");
        } else if(isFormElement(e)) {
            return new XhtmlForm(e.attr("action"),
                e.attr("method"));
        } else {
            return null;
        }
    }
    
    protected String convertToLength(final String value)
    {
        if (isInteger(value)) {
            return value + "px";
        } else {
            return value;
        }
    }

    protected boolean isInteger(final String value) 
    {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (! (c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    protected String getAttribute(final Element e, final String attrName)
    {
        String result = e.attr(attrName);
        result = result.trim();
        return result.length() == 0 ? null : result;
    }

    @Override
    public String getElementStyling(final Element e)
    {
        final StringBuilder style = new StringBuilder();
        if (ciEquals(e.nodeName(), "td") || ciEquals(e.nodeName(), "th")) {
            String s;
            s = getAttribute(e, "colspan");
            if (s != null) {
                style.append("-fs-table-cell-colspan: ");
                style.append(s);
                style.append(";");
            }
            s = getAttribute(e, "rowspan");
            if (s != null) {
                style.append("-fs-table-cell-rowspan: ");
                style.append(s);
                style.append(";");
            }
        } else if (ciEquals(e.nodeName(), "img")) {
            String s;
            s = getAttribute(e, "width");
            if (s != null) {
                style.append("width: ");
                style.append(convertToLength(s));
                style.append(";");
            }
            s = getAttribute(e, "height");
            if (s != null) {
                style.append("height: ");
                style.append(convertToLength(s));
                style.append(";");
            }
        } else if (ciEquals(e.nodeName(), "colgroup") || ciEquals(e.nodeName(), "col")) {
            String s;
            s = getAttribute(e, "span");
            if (s != null) {
                style.append("-fs-table-cell-colspan: ");
                style.append(s);
                style.append(";");
            }
            s = getAttribute(e, "width");
            if (s != null) {
                style.append("width: ");
                style.append(convertToLength(s));
                style.append(";");
            }
        }

        style.append(e.attr("style"));
        return style.toString();
    }

    @Override
    public String getLinkUri(final Element e) 
    {
        if (ciEquals(e.nodeName(), "a") && e.hasAttr("href")) 
        	return e.attr("href");

        return null;
    }

    @Override
    public String getAnchorName(final Element e)
    {
        if (e != null && ciEquals(e.nodeName(), "a") && e.hasAttr("name")) 
            return e.attr("name");

        return null;
    }

    private static String readTextContent(final Element element) 
    {
        final StringBuilder result = new StringBuilder();
        Node current = element.childNodeSize() > 0 ? element.childNode(0) : null;
        while (current != null) {
            final Node nodeType = current;
            if (nodeType instanceof TextNode || nodeType instanceof DataNode) {
                result.append(nodeType instanceof TextNode ? ((TextNode) current).text() : ((DataNode) current).getWholeData());
            }
            current = current.nextSibling();
        }
        return result.toString();
    }

    private static String collapseWhiteSpace(final String text)
    {
        final StringBuilder result = new StringBuilder();
        final int l = text.length();
        for (int i = 0; i < l; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                result.append(' ');
                while (++i < l) {
                    c = text.charAt(i);
                    if (! Character.isWhitespace(c)) {
                        i--;
                        break;
                    }
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Returns the title of the document as located in the contents
     * of /html/head/title, or "" if none could be found.
     */
    @Override
    public String getDocumentTitle(final Document doc) 
    {
        String title = "";
        final Element head = doc.head();
        
        if (head != null) {
            final Element titleElem = findFirstChild(head, "title");
            if (titleElem != null) {
                title = collapseWhiteSpace(readTextContent(titleElem).trim());
            }
        }

        return title;
    }

    private Element findFirstChild(final Element parent, final String targetName)
    {
        final List<Node> children = parent.childNodes();

        for (final Node n : children) 
        {
            if (n instanceof Element && ciEquals(n.nodeName(), targetName)) 
                return (Element)n;
        }

        return null;
    }

    @Override
    public StylesheetInfo getDefaultStylesheet(final StylesheetFactory factory) 
    {
		final StylesheetInfo info = new StylesheetInfo();
		info.setOrigin(StylesheetInfo.CSSOrigin.USER_AGENT);
		info.setMedia("all");
		info.setType("text/css");

		InputStream is = null;
		try {
			is = getDefaultStylesheetStream();

			if (is == null)
				return null;
			
			final Stylesheet sheet = factory.parse(new InputStreamReader(is), info);
			info.setStylesheet(sheet);

		} catch (final Exception e) {
			XRLog.exception("Could not parse default stylesheet", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (final IOException e) {
					// ignore
				}
			}
		}

		return info;
    }

    private InputStream getDefaultStylesheetStream() 
    {
        InputStream stream = null;
        final String defaultStyleSheet = Configuration.valueFor("xr.css.user-agent-default-css") + "XhtmlNamespaceHandler.css";
        stream = this.getClass().getResourceAsStream(defaultStyleSheet);

        if (stream == null)
        {
            XRLog.exception("Can't load default CSS from " + defaultStyleSheet + "." +
                    "This file must be on your CLASSPATH. Please check before continuing.");
        }

        return stream;
    }

    private Map<String, String> getMetaInfo(final Document doc)
    {
        if(this._metadata != null) {
            return this._metadata;
        }

        final Map<String, String> metadata = new HashMap<>(1);
        final Element head = doc.head();

        if (head != null) {
            Node current = head.childNodeSize() > 0 ? head.childNode(0) : null;
            while (current != null) {
                if (current instanceof Element) {
                    final Element elem = (Element)current;
                    final String elemName = elem.nodeName();

                    if (ciEquals(elemName, "meta")) {
                        final String http_equiv = elem.attr("http-equiv");
                        final String content = elem.attr("content");

                        if(!http_equiv.isEmpty() && !content.isEmpty()) {
                            metadata.put(http_equiv, content);
                        }
                    }
                }
                current = current.nextSibling();
            }
        }

        _metadata = metadata;
        return metadata;
    }
}
