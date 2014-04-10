package org.xhtmlrenderer.css.style.derived;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.style.BorderRadiusCorner;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.newtable.CollapsedBorderValue;

public class BorderPropertySet extends RectPropertySet 
{
    public static final BorderPropertySet EMPTY_BORDER = new BorderPropertySet();
    
    private final IdentValue _topStyle;
    private final IdentValue _rightStyle;
    private final IdentValue _bottomStyle;
    private final IdentValue _leftStyle;

    private final FSColor _topColor;
    private final FSColor _rightColor;
    private final FSColor _bottomColor;
    private final FSColor _leftColor;
    
    private final float _topLeft1;
    private final float _topRight1;
    private final float _bottomRight1;
    private final float _bottomLeft1;

    private final float _topLeft2;
    private final float _topRight2;
    private final float _bottomRight2;
    private final float _bottomLeft2;
    
    private BorderPropertySet()
    {
        this._top = 0;
        this._right = 0;
        this._bottom = 0;
        this._left = 0;
        
        this._bottomLeft1 = 0;
        this._bottomRight1 = 0;
        this._topLeft1 = 0;
        this._topRight1 = 0;

        this._bottomLeft2 = 0;
        this._bottomRight2 = 0;
        this._topLeft2 = 0;
        this._topRight2 = 0;
        
        this._topStyle = null;
        this._rightStyle = null;
        this._bottomStyle = null;
        this._leftStyle = null;
        
        this._bottomColor = null;
        this._topColor = null;
        this._leftColor = null;
        this._rightColor = null;
    }
    
    public BorderPropertySet(final BorderPropertySet r,
            final float top,
            final float right,
            final float bottom,
            final float left
    ) {
        this._top = top;
        this._right = right;
        this._bottom = bottom;
        this._left = left;
        
        this._bottomLeft1 = r._bottomLeft1;
        this._bottomRight1 = r._bottomRight1;
        this._topLeft1 = r._topLeft1;
        this._topRight1 = r._topRight1;

        this._bottomLeft2 = r._bottomLeft2;
        this._bottomRight2 = r._bottomRight2;
        this._topLeft2 = r._topLeft2;
        this._topRight2 = r._topRight2;
        
        this._topStyle = r._topStyle;
        this._rightStyle = r._rightStyle;
        this._bottomStyle = r._bottomStyle;
        this._leftStyle = r._leftStyle;
        
        this._bottomColor = r._bottomColor;
        this._topColor = r._topColor;
        this._leftColor = r._leftColor;
        this._rightColor = r._rightColor;
    }
    
    public BorderPropertySet(
           final CollapsedBorderValue top,
           final CollapsedBorderValue right,
           final CollapsedBorderValue bottom,
           final CollapsedBorderValue left
    ) {
        this._top =  top.width();
        this._right = right.width();
        this._bottom = bottom.width();
        this._left = left.width();
        
        this._topStyle = top.style();
        this._rightStyle = right.style();
        this._bottomStyle = bottom.style();
        this._leftStyle = left.style();

        this._topColor = top.color();
        this._rightColor = right.color();
        this._bottomColor = bottom.color();
        this._leftColor = left.color();

        this._bottomLeft1 = 0;
        this._bottomRight1 = 0;
        this._topLeft1 = 0;
        this._topRight1 = 0;

        this._bottomLeft2 = 0;
        this._bottomRight2 = 0;
        this._topLeft2 = 0;
        this._topRight2 = 0;
    }

    private BorderPropertySet(
            final CalculatedStyle style,
            final CssContext ctx
    ) {
        _top = ( style.isIdent(CSSName.BORDER_TOP_STYLE, IdentValue.NONE) ||
                 style.isIdent(CSSName.BORDER_TOP_STYLE, IdentValue.HIDDEN)  
                ?
            0 : style.getFloatPropertyProportionalHeight(CSSName.BORDER_TOP_WIDTH, 0, ctx));
        _right = ( style.isIdent(CSSName.BORDER_RIGHT_STYLE, IdentValue.NONE) || 
                   style.isIdent(CSSName.BORDER_RIGHT_STYLE, IdentValue.HIDDEN) 
                  ?
            0 : style.getFloatPropertyProportionalHeight(CSSName.BORDER_RIGHT_WIDTH, 0, ctx));
        _bottom = ( style.isIdent(CSSName.BORDER_BOTTOM_STYLE, IdentValue.NONE) ||
                    style.isIdent(CSSName.BORDER_BOTTOM_STYLE, IdentValue.HIDDEN)
                   ?
            0 : style.getFloatPropertyProportionalHeight(CSSName.BORDER_BOTTOM_WIDTH, 0, ctx));
        _left = ( style.isIdent(CSSName.BORDER_LEFT_STYLE, IdentValue.NONE) ||
                  style.isIdent(CSSName.BORDER_LEFT_STYLE, IdentValue.HIDDEN)
                 ?
            0 : style.getFloatPropertyProportionalHeight(CSSName.BORDER_LEFT_WIDTH, 0, ctx));

        _topColor = style.asColor(CSSName.BORDER_TOP_COLOR);
        _rightColor = style.asColor(CSSName.BORDER_RIGHT_COLOR);
        _bottomColor = style.asColor(CSSName.BORDER_BOTTOM_COLOR);
        _leftColor = style.asColor(CSSName.BORDER_LEFT_COLOR);

        _topStyle = style.getIdent(CSSName.BORDER_TOP_STYLE);
        _rightStyle = style.getIdent(CSSName.BORDER_RIGHT_STYLE);
        _bottomStyle = style.getIdent(CSSName.BORDER_BOTTOM_STYLE);
        _leftStyle = style.getIdent(CSSName.BORDER_LEFT_STYLE);
        
        final BorderRadiusCorner blCorner = style.getBorderRadiusCorner(CSSName.BORDER_BOTTOM_LEFT_RADIUS);
        final BorderRadiusCorner brCorner = style.getBorderRadiusCorner(CSSName.BORDER_BOTTOM_RIGHT_RADIUS);
        final BorderRadiusCorner tlCorner = style.getBorderRadiusCorner(CSSName.BORDER_TOP_LEFT_RADIUS);
        final BorderRadiusCorner trCorner = style.getBorderRadiusCorner(CSSName.BORDER_TOP_RIGHT_RADIUS);

        if (blCorner == BorderRadiusCorner.ZERO)
        {
        	_bottomLeft1 = 0;
        	_bottomLeft2 = 0;
        }
        else
        {
        	_bottomLeft1 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_BOTTOM_LEFT_RADIUS,
        			blCorner.getRadiusOne().getStringValue(),
        			blCorner.getRadiusOne().getFloatValue(),
        			blCorner.getRadiusOne().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);

        	_bottomLeft2 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_BOTTOM_LEFT_RADIUS,
        			blCorner.getRadiusTwo().getStringValue(),
        			blCorner.getRadiusTwo().getFloatValue(),
        			blCorner.getRadiusTwo().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);
        }

        if (brCorner == BorderRadiusCorner.ZERO)
        {
        	_bottomRight1 = 0;
        	_bottomRight2 = 0;
        }
        else
        {
        	_bottomRight1 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_BOTTOM_RIGHT_RADIUS,
        			brCorner.getRadiusOne().getStringValue(),
        			brCorner.getRadiusOne().getFloatValue(),
        			brCorner.getRadiusOne().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);

        	_bottomRight2 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_BOTTOM_RIGHT_RADIUS,
        			brCorner.getRadiusTwo().getStringValue(),
        			brCorner.getRadiusTwo().getFloatValue(),
        			brCorner.getRadiusTwo().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);
        }
        
        if (tlCorner == BorderRadiusCorner.ZERO)
        {
        	_topLeft1 = 0;
        	_topLeft2 = 0;
        }
        else
        {
        	_topLeft1 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_TOP_LEFT_RADIUS,
        			tlCorner.getRadiusOne().getStringValue(),
        			tlCorner.getRadiusOne().getFloatValue(),
        			tlCorner.getRadiusOne().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);

        	_topLeft2 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_TOP_LEFT_RADIUS,
        			tlCorner.getRadiusTwo().getStringValue(),
        			tlCorner.getRadiusTwo().getFloatValue(),
        			tlCorner.getRadiusTwo().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);
        }
        
        if (trCorner == BorderRadiusCorner.ZERO)
        {
        	_topRight1 = 0;
        	_topRight2 = 0;
        }
        else
        {
        	_topRight1 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_TOP_RIGHT_RADIUS,
        			trCorner.getRadiusOne().getStringValue(),
        			trCorner.getRadiusOne().getFloatValue(),
        			trCorner.getRadiusOne().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);

        	_topRight2 = LengthValue.calcFloatProportionalValue(style,
        			CSSName.BORDER_TOP_RIGHT_RADIUS,
        			trCorner.getRadiusTwo().getStringValue(),
        			trCorner.getRadiusTwo().getFloatValue(),
        			trCorner.getRadiusTwo().getPrimitiveTypeN(),
        			style.getFont(ctx).size,
        			ctx);
        }
    }

    private BorderPropertySet(final BorderPropertySet r,
			final FSColor top, final FSColor bottom, final FSColor left,
			final FSColor right)
    {
        this._top = r._top;
        this._right = r._right;
        this._bottom = r._bottom;
        this._left = r._left;
        
        this._bottomLeft1 = r._bottomLeft1;
        this._bottomRight1 = r._bottomRight1;
        this._topLeft1 = r._topLeft1;
        this._topRight1 = r._topRight1;

        this._bottomLeft2 = r._bottomLeft2;
        this._bottomRight2 = r._bottomRight2;
        this._topLeft2 = r._topLeft2;
        this._topRight2 = r._topRight2;
        
        this._topStyle = r._topStyle;
        this._rightStyle = r._rightStyle;
        this._bottomStyle = r._bottomStyle;
        this._leftStyle = r._leftStyle;
        
        this._topColor = top;
        this._bottomColor = bottom;
        this._leftColor = left;
        this._rightColor = right;
    }

	public boolean hasBorderRadius()
    {
    	return _bottomLeft1 != 0 || _bottomLeft2 != 0 ||
    		   _bottomRight1 != 0 || _bottomRight2 != 0 ||
    		   _topLeft1 != 0 || _topLeft2 != 0 ||
    		   _topRight1 != 0 || _topRight2 != 0;
    }

    /**
     * Determines if all sides are the same.
     */
    public boolean isSquareRectStandard()
    {
    	return !hasBorderRadius() && isRoundedRectStandard();
    }
    
    public boolean isRoundedRectStandard()
    {
    	final FSColor startc = _leftColor;
    	
    	if (startc == null &&
    		(_rightColor != null ||
    		 _bottomColor != null ||
    		 _topColor != null))
    		return false;
    	
    	if (startc != null &&
    		((!startc.equals(_rightColor)) ||
    		(!startc.equals(_bottomColor)) ||
    		(!startc.equals(_topColor))))
    		return false;
    	
    	final float start = _bottomLeft1;
    	
    	if ((_bottomLeft2 != start) ||
    		(_bottomRight1 != start) ||
    		(_bottomRight2 != start) ||
    		(_topLeft1 != start) ||
    		(_topLeft2 != start) ||
    		(_topRight1 != start) ||
    		(_topRight2 != start))
    		return false;
    	
    	final float startw = _left;
    	
    	if ((_right != startw) ||
    		(_bottom != startw) ||
    		(_top != startw))
    		return false;
    	
    	final IdentValue starts = _leftStyle;
    	
    	if ((_rightStyle != starts) ||
    		(_bottomStyle != starts) ||
    		(_topStyle != starts))
    		return false;
    	
    	return true;
    }
    
    public float radiusBottomLeftOne()
    {
    	return _bottomLeft1;
    }

    public float radiusBottomLeftTwo()
    {
    	return _bottomLeft2;
    }

    public float radiusBottomRightOne()
    {
    	return _bottomRight1;
    }

    public float radiusBottomRightTwo()
    {
    	return _bottomRight2;
    }
    
    public float radiusTopLeftOne()
    {
    	return _topLeft1;
    }

    public float radiusTopLeftTwo()
    {
    	return _topLeft2;
    }
    
    public float radiusTopRightOne()
    {
    	return _topRight1;
    }

    public float radiusTopRightTwo()
    {
    	return _topRight2;
    }
    
    /**
     * Returns the colors for brighter parts of each side for a particular decoration style
     *
     * @param style
     * @return Returns
     */
    public BorderPropertySet lighten(final IdentValue style) 
    {
        final BorderPropertySet bc = new BorderPropertySet(this,
        		_topColor.lightenColor(),
        		_bottomColor.lightenColor(),
        		_leftColor.lightenColor(),
        		 _rightColor.lightenColor()
        		);

        return bc;
    }

    /**
     * Returns the colors for brighter parts of each side for a particular decoration style
     *
     * @param style
     * @return Returns
     */
    public BorderPropertySet darken(final IdentValue style) {
        final BorderPropertySet bc = new BorderPropertySet(this,
        		_topColor.darkenColor(),
        		_bottomColor.darkenColor(),
        		_leftColor.darkenColor(),
        		_rightColor.darkenColor()
        		);

        return bc;
    }

    public static BorderPropertySet newInstance(
            final CalculatedStyle style,
            final CssContext ctx
    ) {
        return new BorderPropertySet(style, ctx);
    }

    public String toString() {
        return "BorderPropertySet[top=" + _top + ",right=" + _right + ",bottom=" + _bottom + ",left=" + _left + "]";
    }

    public boolean noTop() {
        return this._topStyle == IdentValue.NONE || (int) _top == 0;
    }

    public boolean noRight() {
        return this._rightStyle == IdentValue.NONE || (int) _right == 0;
    }

    public boolean noBottom() {
        return this._bottomStyle == IdentValue.NONE || (int) _bottom == 0;
    }

    public boolean noLeft() {
        return this._leftStyle == IdentValue.NONE || (int) _left == 0;
    }

    public IdentValue topStyle() {
        return _topStyle;
    }

    public IdentValue rightStyle() {
        return _rightStyle;
    }

    public IdentValue bottomStyle() {
        return _bottomStyle;
    }

    public IdentValue leftStyle() {
        return _leftStyle;
    }

    public FSColor topColor() {
        return _topColor;
    }

    public FSColor rightColor() {
        return _rightColor;
    }

    public FSColor bottomColor() {
        return _bottomColor;
    }

    public FSColor leftColor() {
        return _leftColor;
    }
    
    public boolean hasHidden() {
        return _topStyle == IdentValue.HIDDEN || _rightStyle == IdentValue.HIDDEN ||
               _bottomStyle == IdentValue.HIDDEN || _leftStyle == IdentValue.HIDDEN;
    }    
}

