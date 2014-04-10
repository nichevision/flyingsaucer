/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
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
package org.xhtmlrenderer.pdf;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.util.*;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfAppearance;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfWriter;


public class SelectFormField extends AbstractFormField {
    private static final String FIELD_TYPE = "Select";
    
    private static final int EMPTY_SPACE_COUNT = 10;
    private static final int EXTRA_SPACE_COUNT = 4;
    
    private final List<Option> _options;

    private final int _baseline;
    
    public SelectFormField(final LayoutContext c, final BlockBox box, final int cssWidth, final int cssHeight) {
        _options = readOptions(box.getElement());
        initDimensions(c, box, cssWidth, cssHeight);
        
        final float fontSize = box.getStyle().getFSFont(c).getSize2D();
        // FIXME: findbugs possible loss of precision, cf. int / (float)2
        _baseline = (int)(getHeight() / 2 + (fontSize * 0.3f));
    }
    
    private int getSelectedIndex() {
        int result = 0;
        
        final List<Option> options = _options;
        
        final int offset = 0;
        for (final Option option : options) {
            if (option.isSelected()) {
                result = offset;
            }
        }
        
        return result;
    }
    
    private String[][] getPDFOptions() {
        final List<Option> options = _options;
        final String[][] result = new String[options.size()][];
        
        final int offset = 0;
        for (final Option option : options) {
            result[offset] = new String[] { option.getValue(), option.getLabel() };
        }
        
        return result;
    }
    
    private int calcDefaultWidth(final LayoutContext c, final BlockBox box) {
        final List<Option> options = _options;
        
        if (options.size() == 0) {
            return c.getTextRenderer().getWidth(
                    c.getFontContext(),
                    box.getStyle().getFSFont(c),
                    spaces(EMPTY_SPACE_COUNT));
        } else {
            int maxWidth = 0;
            for (final Option option : options) {
                final String result = option.getLabel() + spaces(EXTRA_SPACE_COUNT);
                
                final int width = c.getTextRenderer().getWidth(
                        c.getFontContext(),
                        box.getStyle().getFSFont(c),
                        result);
                
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
            
            return maxWidth;
        }
    }
    
    private List<Option> readOptions(final Element e) {
        final List<Option> result = new ArrayList<>();
        
        Node n = e.childNodeSize() > 0 ? e.childNode(0) : null;
        while (n != null) {
            if (n instanceof Element && ciEquals(n.nodeName(), "option")) 
            {
                final Element optionElem = (Element)n;
                final String label = collectText(optionElem);
                String value;

                if (!optionElem.hasAttr("value")) {
                    value = label;
                } else {
                    value = optionElem.attr("value");
                }
                
                if (label != null) {
                    final Option option = new Option();
                    option.setLabel(label);
                    option.setValue(value);
                    if (isSelected(optionElem)) {
                        option.setSelected(true);
                    }
                    result.add(option);
                }
            }
            
            n = n.nextSibling();
        }
        
        return result;
    }
    
    private String collectText(final Element e) {
        final StringBuilder result = new StringBuilder();
        
        Node n = e.childNodeSize() > 0 ? e.childNode(0) : null;
        while (n != null) 
        {
        	if (n instanceof TextNode)
            	result.append(((TextNode) n).text());

            n = n.nextSibling();
        }
        
        return result.length() > 0 ? result.toString() : null;
    }

    protected void initDimensions(final LayoutContext c, final BlockBox box, final int cssWidth, final int cssHeight) {
        if (cssWidth != -1) {
            setWidth(cssWidth);
        } else {
            setWidth(calcDefaultWidth(c, box));
        }

        if (cssHeight != -1) {
            setHeight(cssHeight);
        } else {
            setHeight((int) (box.getStyle().getLineHeight(c) * getSize(box.getElement())));
        }
    } 
    
    private int getSize(final Element elem) {
        int result = 1;
        try {
            final String v = elem.hasAttr("size") ? elem.attr("size").trim() : "";
            if (!v.isEmpty()) {
                final int i = Integer.parseInt(v);
                if (i > 1) {
                    result = i;
                }
                
            }
        } catch (final NumberFormatException e) {
            // ignore
        }
        
        return result;
    }
    
    protected boolean isMultiple(final Element e) {
        return !Util.isNullOrEmpty(e.attr("multiple"));
    }
    
    protected String getFieldType() {
        return FIELD_TYPE;
    }

    public void paint(final RenderingContext c, final ITextOutputDevice outputDevice, final BlockBox box) {
        final PdfWriter writer = outputDevice.getWriter();
        
        final String[][] options = getPDFOptions();
        final int selectedIndex = getSelectedIndex();
        
        PdfFormField field;
        
        /*
         * Comment out for now.  We need to draw an appropriate appearance for
         * this to work correctly.
         */
        /*
        if (isMultiple(box.getElement())) {
            field = PdfFormField.createList(writer, options, selectedIndex);  
        } else {
            field = PdfFormField.createCombo(writer, false, options, selectedIndex);    
        }
        */
        
        field = PdfFormField.createCombo(writer, false, options, selectedIndex);    
        
        field.setWidget(outputDevice.createLocalTargetArea(c, box), PdfAnnotation.HIGHLIGHT_INVERT);
        field.setFieldName(getFieldName(outputDevice, box.getElement()));
        if (options.length > 0) {
            field.setValueAsString(options[selectedIndex][0]);
        }
        
        createAppearance(c, outputDevice, box, field);

        if (isReadOnly(box.getElement())) {
            field.setFieldFlags(PdfFormField.FF_READ_ONLY);
        }       
        
        /*
        if (isMultiple(box.getElement())) {
            field.setFieldFlags(PdfFormField.FF_MULTISELECT);
        }
        */
        
        writer.addAnnotation(field);
    }
    
    private void createAppearance(
            final RenderingContext c, final ITextOutputDevice outputDevice, 
            final BlockBox box, final PdfFormField field) {
        final PdfWriter writer = outputDevice.getWriter();
        final ITextFSFont font = (ITextFSFont)box.getStyle().getFSFont(c);
        
        final PdfContentByte cb = writer.getDirectContent();
        
        final float width = outputDevice.getDeviceLength(getWidth());
        final float height = outputDevice.getDeviceLength(getHeight());
        final float fontSize = outputDevice.getDeviceLength(font.getSize2D());
        
        final PdfAppearance tp = cb.createAppearance(width, height);
        tp.setFontAndSize(font.getFontDescription().getFont(), fontSize);
        
        final FSColor color = box.getStyle().getColor();
        setFillColor(tp, color);
        
        field.setDefaultAppearanceString(tp);
    }    

    public int getBaseline() {
        return _baseline;
    }

    public boolean hasBaseline() {
        return true;
    }
    
    private static final class Option {
        private String _value;
        private String _label;
        private boolean _selected;
        
        public String getValue() {
            return _value;
        }
        
        public void setValue(final String value) {
            _value = value;
        }
        
        public String getLabel() {
            return _label;
        }
        
        public void setLabel(final String label) {
            _label = label;
        }
        
        public boolean isSelected() {
            return _selected;
        }
        
        public void setSelected(final boolean selected) {
            _selected = selected;
        }
    }
}
