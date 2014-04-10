package org.xhtmlrenderer.css.mediaquery;

import java.util.HashMap;
import java.util.Map;

public enum MediaFeatureName
{
	COLOR("color"),
	COLOR_INDEX("color-index"),
	GRID("grid"),
	MONOCHROME("monochrome"),
	HEIGHT("height"),
	HOVER("hover"),
	WIDTH("width"),
	ORIENTATION("orientation"),
	ASPECT_RATIO("aspect-ratio"),
	DEVICE_ASPECT_RATIO("device-aspect-ratio"),
	DEVICE_HEIGHT("device-height"),
	DEVICE_WIDTH("device-width"),
	MAX_COLOR("max-color"),
	MAX_COLOR_INDEX("max-color-index"),
	MAX_ASPECT_RATIO("max-aspect-ratio"),
	MAX_DEVICE_ASPECT_RATIO("max-device-aspect-ratio"),
	MAX_DEVICE_HEIGHT("max-device-height"),
	MAX_DEVICE_WIDTH("max-device-width"),
	MAX_HEIGHT("max-height"),
	MAX_MONOCHROME("max-monochrome"),
	MAX_WIDTH("max-width"),
	MAX_RESOLUTION("max-resolution"),
	MIN_COLOR("min-color"),
	MIN_COLOR_INDEX("min-color-index"),
	MIN_ASPECT_RATIO("min-aspect-ratio"),
	MIN_DEVICE_ASPECT_RATIO("min-device-aspect-ratio"),
	MIN_DEVICE_HEIGHT("min-device-height"),
	MIN_DEVICE_WIDTH("min-device-width"),
	MIN_HEIGHT("min-height"),
	MIN_MONOCHROME("min-monochrome"),
	MIN_WIDTH("min-width"),
	MIN_RESOLUTION("min-resolution"),
	POINTER("pointer"),
	RESOLUTION("resolution");
	
	private final String cssName;
	
	private MediaFeatureName(final String name)
	{
		cssName = name;
	}

	@Override
	public String toString() 
	{
		return cssName;
	}

	private static final Map<String, MediaFeatureName> map = new HashMap<>(values().length);
	
	static 
	{
		for (final MediaFeatureName nm : values())
		{
			map.put(nm.cssName, nm);
		}
	}
	
	public static MediaFeatureName fsValueOf(final String mediaFeatureStr)
	{
		return map.get(mediaFeatureStr);
	}
}
