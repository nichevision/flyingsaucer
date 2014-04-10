package org.xhtmlrenderer.util;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

public class JsoupUtil
{
	public static boolean isText(final Node n)
	{
		return n instanceof TextNode;
	}

	public static boolean isElement(final Node n)
	{
		return n instanceof Element;
	}
	
	public static boolean isDocument(final Node n)
	{
		return n instanceof Document;
	}
	
	public static boolean isNode(final Node n, final String tag)
	{
		return n != null && ciEquals(n.nodeName(), tag); 
	}

	public static Node firstChild(final Node n)
	{
		return n != null && n.childNodeSize() > 0 ? n.childNode(0) : null;
	}

	public static Node lastChild(final Node n)
	{
		return n != null && n.childNodeSize() > 0 ? n.childNode(n.childNodeSize() - 1) : null;
	}

	public static Element firstChild(final Elements select) 
	{
		return select != null && !select.isEmpty() ? select.first() : null;
	}
}
