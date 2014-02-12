/*
 * {{{ header & license
 * Copyright (c) 2007 Patrick Wright
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
package org.xhtmlrenderer.util;


/**
 * Simple enumerated constants for downscaling (scaling to smaller image size)--since we have various options
 * for what algorithm to use. Not general-purpose, applies only to methods used in ImageUtil. Types constants
 * can be looked up using {@link #forString(String, DownscaleQuality)} and the corresponding string
 * for the quality
*/
// made a separate class only to reduce size of ImageUtil
public enum DownscaleQuality {

	HIGH_QUALITY,
	STANDARD,
	MEDIUM,
	FAST;
}
