package org.xhtmlrenderer.swing;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;

public class ContentStore
{
	private final Document doc;
	private final Map<String, String> store = new HashMap<>();
	
	public ContentStore(final Document doc)
	{
		this.doc = doc;
	}

	public void addContent(final String uri, final String content)
	{
		store.put(uri, content);
	}
	
	public boolean isAvailable(final String uri)
	{
		return store.containsKey(uri);
	}

	public String getContent(final String uri)
	{
		return store.get(uri);
	}

	public Document getDocument()
	{
		return doc;
	}
}
