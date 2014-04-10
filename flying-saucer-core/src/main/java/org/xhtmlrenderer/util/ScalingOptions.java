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

import java.awt.*;


/**
 * POJO used when calling
 * {@link org.xhtmlrenderer.util.ImageUtil#getScaledInstance(ScalingOptions,java.awt.Image)}.
 * Encapsulates a set of parameters related to scaling quality and output. Values are final once constructed, except
 * for target width and height, which can be change and the options instance reused.
 * There is a default constructor for average quality and performance.
 */
public class ScalingOptions {
	private final DownscaleQuality downscalingHint;
	private int targetWidth;
	private int targetHeight;

	/**
	 * Constructor with all options.
	 *
	 * @param downscalingHint   Directs downscaling quality. One of the enumerated types of
	 *                          {@link org.xhtmlrenderer.util.DownscaleQuality} such as
	 *                          {@link org.xhtmlrenderer.util.ImageUtil.DOWNSCALE_FAST}.
	 * @param interpolationHint Hint for interpolation to AWT image renderer, one of the Object constants from
	 *                          {@link java.awt.RenderingHints} using {@link java.awt.RenderingHints.KEY_INTERPOLATION}
	 */
	public ScalingOptions(final DownscaleQuality downscalingHint) {
		this.downscalingHint = downscalingHint;
	}

	/**
	 * Default scaling options, nearest neighbor interpolation, and fast downscaling. This is fast, but not great
	 * quality.
	 */
	public ScalingOptions() {
		this(DownscaleQuality.FAST);
	}

	/**
	 * Constructor with all options.
	 *
	 * @param targetWidth  Target width in pixels of image once scaled
	 * @param targetHeight Target height in pixels of image once scaled
	 * @param type		 Type of {@link java.awt.image.BufferedImage} to create for output; see docs for
	 *                     {@link java.awt.image.BufferedImage#BufferedImage(int,int,int)}
	 * @param downscalingHint   Directs downscaling quality. One of the enumerated types of
	 *                          {@link org.xhtmlrenderer.util.DownscaleQuality} such as
	 *                          {@link org.xhtmlrenderer.util.ImageUtil.DOWNSCALE_FAST}.
	 * @param hint		 Hint for interpolation to AWT image renderer, one of the Object constants from
	 *                     {@link java.awt.RenderingHints} using {@link java.awt.RenderingHints.KEY_INTERPOLATION}
	 */
	public ScalingOptions(final int targetWidth, final int targetHeight, final int type, final DownscaleQuality downscalingHint) {
		this(downscalingHint);
		this.setTargetHeight(Math.max(1, targetHeight));
		this.setTargetWidth(Math.max(1, targetWidth));
	}

	/**
	 * @return {@link ScalingOptions#ScalingOptions(int,DownscaleQuality,Object)} docs.
	 */
	public DownscaleQuality getDownscalingHint() {
		return downscalingHint;
	}

	/**
	 * Returns true if the target size specified by these options matches the size provided (e.g. image is
	 * already at target size).
	 *
	 * @param w an image width
	 * @param h an image height
	 * @return true if image dimensions already match target size
	 */
	public boolean sizeMatches(final int w, final int h) {
		return (w == getTargetWidth() && h == getTargetHeight());
	}

	/**
	 * Returns true if the target size specified by these options matches the size provided (e.g. image is
	 * already at target size).
	 *
	 * @param img
	 * @return true if image dimensions already match target size
	 */
	public boolean sizeMatches(final Image img) {
		return sizeMatches(img.getWidth(null), img.getHeight(null));
	}

	public int getTargetWidth() {
		return targetWidth;
	}

	public int getTargetHeight() {
		return targetHeight;
	}

	public void setTargetWidth(final int targetWidth) {
		this.targetWidth = targetWidth;
	}

	public void setTargetHeight(final int targetHeight) {
		this.targetHeight = targetHeight;
	}

	public void setTargetDimensions(final Dimension dim) {
		setTargetWidth((int) dim.getWidth());
		setTargetHeight((int) dim.getHeight());
	}
}
