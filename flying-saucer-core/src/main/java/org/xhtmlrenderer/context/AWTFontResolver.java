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
package org.xhtmlrenderer.context;

import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.swing.AWTFSFont;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Joshua Marinacci
 */
public class AWTFontResolver implements FontResolver
{
	// Contains actual instances of fonts at the correct size, weight, etc.
	private Map<String, Font> _instanceHash = new HashMap<>();

	// Contains the root fonts that are loaded. That is Font.PLAIN, etc.
	private Map<String, Font> _availableFontsHash = new HashMap<>();

	// Contains a set of avaialable font families.
	private Set<String> _availableFontsSet = new HashSet<>();
    
    public AWTFontResolver() {
        init();
    }
    
    private void init() {
        GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = gfx.getAvailableFontFamilyNames();
        
        // preload the font map with the font names as keys in a set.
        for (String availableFont : availableFonts) {
            _availableFontsSet.add(availableFont);
        }

        // preload sans, serif, and monospace into the available font hash
        _availableFontsHash.put("Serif", new Font("Serif", Font.PLAIN, 1));
        _availableFontsHash.put("SansSerif", new Font("SansSerif", Font.PLAIN, 1));
        _availableFontsHash.put("Monospaced", new Font("Monospaced", Font.PLAIN, 1));
    }
    
    public void flushCache() {
        init();
    }

    private FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        // Try to create a font for each font family provided as CSS
    	// can specify fallback fonts.
        if (families != null) {
            for (String family : families) {
                Font font = resolveFont(ctx, family, size, weight, style, variant);
                if (font != null) {
                    return new AWTFSFont(font);
                }
            }
        }

        // if we get here then no font worked, so just return default sans
        String family = "SansSerif";
        if (style == IdentValue.ITALIC) {
            family = "Serif";
        }

        Font fnt = createFont(ctx, (Font) _availableFontsHash.get(family), size, weight, style, variant);
        _instanceHash.put(getFontInstanceHashName(ctx, family, size, weight, style, variant), fnt);
        return new AWTFSFont(fnt);
    }

    /**
     * This allows the user to replace one font family with another.
     */
    public void setFontMapping(String name, Font font) {
        _availableFontsHash.put(name, font.deriveFont(1f));
    }

    protected static Font createFont(SharedContext ctx, Font rootFont, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        int fontFlags = Font.PLAIN;
        if (weight != null &&
                (weight == IdentValue.BOLD ||
                weight == IdentValue.FONT_WEIGHT_700 ||
                weight == IdentValue.FONT_WEIGHT_800 ||
                weight == IdentValue.FONT_WEIGHT_900)) {

            fontFlags = fontFlags | Font.BOLD;
        }

        if (style != null && (style == IdentValue.ITALIC || style == IdentValue.OBLIQUE)) {
            fontFlags = fontFlags | Font.ITALIC;
        }

        // scale vs font scale value too
        size *= ctx.getTextRenderer().getFontScale();

        Font fnt = rootFont.deriveFont(fontFlags, size);
        if (variant != null) {
            if (variant == IdentValue.SMALL_CAPS) {
                fnt = fnt.deriveFont((float) (((float) fnt.getSize()) * 0.6));
            }
        }

        return fnt;
    }

    protected Font resolveFont(SharedContext ctx, String font, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        // Strip off the "s if they are there
        if (font.startsWith("\"")) {
            font = font.substring(1);
        }
        if (font.endsWith("\"")) {
            font = font.substring(0, font.length() - 1);
        }

        // Normalize the font family name
        if (ciEquals(font, "sans-serif"))
            font = "SansSerif";
        else if (ciEquals(font, "serif"))
            font = "Serif";
        else if (ciEquals(font, "monospace"))
            font = "Monospaced";

        if (ciEquals(font, "Serif") && style == IdentValue.OBLIQUE) font = "SansSerif";
        if (ciEquals(font, "SansSerif") && style == IdentValue.ITALIC) font = "Serif";

        // assemble a font instance hash name
        String fontInstanceName = getFontInstanceHashName(ctx, font, size, weight, style, variant);

        // check if the font instance exists in the hash table
        if (_instanceHash.containsKey(fontInstanceName)) {
            // if so then return it
            return _instanceHash.get(fontInstanceName);
        }

        // Check for it in the root fonts family map.
        if (!_availableFontsHash.containsKey(font))
        {
        	if (!_availableFontsSet.contains(font))
        		return null;

        	Font rootFont = new Font(font, Font.PLAIN, 1);
            _availableFontsHash.put(font, rootFont);
        }
        
        Font value = _availableFontsHash.get(font);

        // now that we have a root font, we need to create the correct version of it
        Font fnt = createFont(ctx, value, size, weight, style, variant);

        // add the font to the hash so we don't have to do this again
        _instanceHash.put(fontInstanceName, fnt);
         return fnt;
     }

    /*
     * Gets a hash key containing name, size, weight, style, etc.
     */
    protected static String getFontInstanceHashName(SharedContext ctx, String name, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        return name + "-" + (size * ctx.getTextRenderer().getFontScale()) + "-" + weight + "-" + style + "-" + variant;
    }

    public FSFont resolveFont(SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }
}
