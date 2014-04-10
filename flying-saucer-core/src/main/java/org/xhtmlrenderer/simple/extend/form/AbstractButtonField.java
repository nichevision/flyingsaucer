package org.xhtmlrenderer.simple.extend.form;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.FSDerivedValue;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.css.style.derived.LengthValue;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.simple.extend.XhtmlForm;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;

public abstract class AbstractButtonField extends InputField {

    public AbstractButtonField(final Element e, final XhtmlForm form, final LayoutContext context, final BlockBox box) {
        super(e, form, context, box);
    }

    protected void applyComponentStyle(final JButton button) {

        super.applyComponentStyle(button);

        final CalculatedStyle style = getBox().getStyle();
        final BorderPropertySet border = style.getBorder(null);
        final boolean disableOSBorder = (border.leftStyle() != null && border.rightStyle() != null || border.topStyle() != null || border.bottomStyle() != null);

        final FSColor backgroundColor = style.getBackgroundColor();

        //if a border is set or a background color is set, then use a special JButton with the BasicButtonUI.
        if (disableOSBorder || backgroundColor instanceof FSRGBColor) {
            //when background color is set, need to use the BasicButtonUI, certainly when using XP l&f
            final BasicButtonUI ui = new BasicButtonUI();
            button.setUI(ui);

            if (backgroundColor instanceof FSRGBColor) {
                final FSRGBColor rgb = (FSRGBColor)backgroundColor;
                button.setBackground(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue()));
            }

            if (disableOSBorder)
                button.setBorder(new BasicBorders.MarginBorder());
            else
                button.setBorder(BasicBorders.getButtonBorder());
        }

        final Integer paddingTop = getLengthValue(style, CSSName.PADDING_TOP);
        final Integer paddingLeft = getLengthValue(style, CSSName.PADDING_LEFT);
        final Integer paddingBottom = getLengthValue(style, CSSName.PADDING_BOTTOM);
        final Integer paddingRight = getLengthValue(style, CSSName.PADDING_RIGHT);


        final int top = paddingTop == null ? 2 : Math.max(2, paddingTop.intValue());
        final int left = paddingLeft == null ? 12 : Math.max(12, paddingLeft.intValue());
        final int bottom = paddingBottom == null ? 2 : Math.max(2, paddingBottom.intValue());
        final int right = paddingRight == null ? 12 : Math.max(12, paddingRight.intValue());

        button.setMargin(new Insets(top, left, bottom, right));

        final RectPropertySet padding = style.getCachedPadding();
        padding.setRight(0);
        padding.setLeft(0);
        padding.setTop(0);
        padding.setBottom(0);

        final FSDerivedValue widthValue = style.valueByName(CSSName.WIDTH);
        if (widthValue instanceof LengthValue)
            intrinsicWidth = new Integer(getBox().getContentWidth());

        final FSDerivedValue heightValue = style.valueByName(CSSName.HEIGHT);
        if (heightValue instanceof LengthValue)
            intrinsicHeight = new Integer(getBox().getHeight());
    }
}
