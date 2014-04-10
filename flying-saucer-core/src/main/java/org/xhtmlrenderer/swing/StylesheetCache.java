package org.xhtmlrenderer.swing;

import java.util.Map;

import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.util.XRLog;

public class StylesheetCache {
	/**
	 * an LRU cache
	 */
	private static final int DEFAULT_CSS_CACHE_SIZE = 64;
	private final Map<String, Stylesheet> _cache = new java.util.LinkedHashMap<String, Stylesheet>(
			DEFAULT_CSS_CACHE_SIZE, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(
				final java.util.Map.Entry<String, Stylesheet> eldest) {
			return size() > DEFAULT_CSS_CACHE_SIZE;
		}
	};

	/**
	 * Adds a stylesheet to the factory cache. Will overwrite older entry for
	 * same key.
	 * 
	 * @param key
	 *            Key to use to reference sheet later; must be unique in
	 *            factory.
	 * @param sheet
	 *            The sheet to cache.
	 */
	public void putStylesheet(final String key, final Stylesheet sheet) {
		XRLog.load("Receiving stylesheet for " + key);
		
		_cache.put(key, sheet);
	}

	/**
	 * @param key
	 * @return true if a Stylesheet with this key has been put in the cache.
	 *         Note that the Stylesheet may be null.
	 */
	public boolean containsStylesheet(final String key) {
		return _cache.containsKey(key);
	}

	/**
	 * Returns a cached sheet by its key; null if no entry for that key.
	 * 
	 * @param key
	 *            The key for this sheet; same as key passed to putStylesheet();
	 * @return The stylesheet
	 */
	public Stylesheet getStylesheet(final StylesheetInfo key) {
		if (_cache.containsKey(key.getUri()))
			XRLog.load("Stylesheet hit for " + key.getUri());
		else
			XRLog.load("Stylesheet miss for " + key.getUri());
		
		return _cache.get(key.getUri());
	}

	/**
	 * Removes a cached sheet by its key.
	 * 
	 * @param key
	 *            The key for this sheet; same as key passed to putStylesheet();
	 */
	public Stylesheet removeCachedStylesheet(final String key) {
		return _cache.remove(key);
	}

	public void flushCachedStylesheets() {
		_cache.clear();
	}
}
