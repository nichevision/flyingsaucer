package org.xhtmlrenderer.render;

public class BorderRadiusPair {

	private final float _left;
	private final float _right;
	
	public BorderRadiusPair(final float l, final float r) {
		this._left = l;
		this._right = r;
	}
	public boolean hasRadius() {
		return _left > 0 || _right > 0;
	}

	public float getMaxLeft(final float max) {

		if (_left > max - _left)
			return max / 2;
		
		return _left;
	}

	public float getMaxRight(final float max) {

		if (_right > max - _right)
            return max / 2;
		
		return _right;
	}

	public float left() {
		return _left;
	}
	public float right() {
		return _right;
	}	
}
