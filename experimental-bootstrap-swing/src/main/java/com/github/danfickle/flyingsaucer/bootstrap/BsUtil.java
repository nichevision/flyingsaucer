package com.github.danfickle.flyingsaucer.bootstrap;

import java.awt.Color;
import java.awt.Dimension;
import org.xhtmlrenderer.simple.HtmlNamespaceHandler;
import com.github.danfickle.flyingsaucer.swing.XHTMLPanel;

public class BsUtil 
{
	private static final BsUserAgent uac = new BsUserAgent();
	static final Color TRANSPARENT = new Color(0, 0, 0, 0); 
	
	static XHTMLPanel setup(String html, Dimension sz)
	{
		XHTMLPanel panel = new XHTMLPanel(uac);
		panel.setPreferredSize(sz);
		panel.setDocumentFromString(HEAD + html, null, new HtmlNamespaceHandler());
		panel.setBackground(TRANSPARENT);
		return panel;
	}
	
	private static String HEAD = "<head><link rel=\"stylesheet\" href=\"demo:/css/bs.css\"></head>";
}
