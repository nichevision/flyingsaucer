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

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.util.*;

import com.lowagie.text.Rectangle;

import java.io.IOException;

public class TextFormField extends AbstractFormField
{
  private static final String FIELD_TYPE = "Text";

  private static final int DEFAULT_SIZE = 15;

  private final int _baseline;

  public TextFormField(final LayoutContext c, final BlockBox box, final int cssWidth, final int cssHeight)
  {
    initDimensions(c, box, cssWidth, cssHeight);

    final float fontSize = box.getStyle().getFSFont(c).getSize2D();
    // FIXME: findbugs possible loss of precision, cf. int / (float)2
    _baseline = (int) (getHeight() / 2 + (fontSize * 0.3f));
  }

  protected void initDimensions(final LayoutContext c, final BlockBox box, final int cssWidth, final int cssHeight)
  {
    if (cssWidth != -1)
    {
      setWidth(cssWidth);
    }
    else
    {
      setWidth(c.getTextRenderer().getWidth(
          c.getFontContext(),
          box.getStyle().getFSFont(c),
          spaces(getSize(box.getElement()))));
    }

    if (cssHeight != -1)
    {
      setHeight(cssHeight);
    }
    else
    {
      setHeight((int) (box.getStyle().getLineHeight(c)));
    }
  }

  protected String getFieldType()
  {
    return FIELD_TYPE;
  }

  public void paint(final RenderingContext c, final ITextOutputDevice outputDevice, final BlockBox box)
  {

    final PdfWriter writer = outputDevice.getWriter();

    final Element elem = box.getElement();

    final Rectangle targetArea = outputDevice.createLocalTargetArea(c, box);
    final TextField field = new TextField(writer, targetArea, getFieldName(outputDevice, elem));

    final String value = getValue(elem);
    field.setText(value);

    try
    {
      final PdfFormField formField = field.getTextField();
      createAppearance(c, outputDevice, box, formField, value);
      //TODO add max length back in
      if (isReadOnly(elem))
      {
        formField.setFieldFlags(PdfFormField.FF_READ_ONLY);
      }
      writer.addAnnotation(formField);
    } catch (final IOException ioe)
    {
      System.out.println(ioe);
    } catch (final DocumentException de)
    {
      System.out.println(de);
    }

  }

  private void createAppearance(final RenderingContext c, final ITextOutputDevice outputDevice, final BlockBox box, final PdfFormField field, final String value)
  {
    final PdfWriter writer = outputDevice.getWriter();
    final ITextFSFont font = (ITextFSFont) box.getStyle().getFSFont(c);

    final PdfContentByte cb = writer.getDirectContent();

    final float width = outputDevice.getDeviceLength(getWidth());
    final float height = outputDevice.getDeviceLength(getHeight());
    final float fontSize = outputDevice.getDeviceLength(font.getSize2D());

    final PdfAppearance tp = cb.createAppearance(width, height);
    final PdfAppearance tp2 = (PdfAppearance) tp.getDuplicate();
    tp2.setFontAndSize(font.getFontDescription().getFont(), fontSize);

    final FSColor color = box.getStyle().getColor();
    setFillColor(tp2, color);

    field.setDefaultAppearanceString(tp2);
    tp.beginVariableText();
    tp.saveState();
    tp.beginText();
    tp.setFontAndSize(font.getFontDescription().getFont(), fontSize);
    setFillColor(tp, color);
    tp.setTextMatrix(0, height / 2 - (fontSize * 0.3f));
    tp.showText(value);
    tp.endText();
    tp.restoreState();
    tp.endVariableText();
    field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
  }

  private int getSize(final Element elem)
  {
    final String sSize = elem.attr("size");
    if (Util.isNullOrEmpty(sSize))
    {
      return DEFAULT_SIZE;
    }
    else
    {
      try
      {
        return Integer.parseInt(sSize.trim());
      } catch (final NumberFormatException e)
      {
        return DEFAULT_SIZE;
      }
    }
  }

  private int getMaxLength(final Element elem)
  {
    final String sMaxLen = elem.attr("maxlength");
    if (Util.isNullOrEmpty(sMaxLen))
    {
      return 0;
    }
    else
    {
      try
      {
        return Integer.parseInt(sMaxLen.trim());
      } catch (final NumberFormatException e)
      {
        return 0;
      }
    }
  }

  protected String getValue(final Element e)
  {
    final String result = e.attr("value");
    if (Util.isNullOrEmpty(result))
    {
      return "";
    }
    else
    {
      return result;
    }
  }

  public int getBaseline()
  {
    return _baseline;
  }

  public boolean hasBaseline()
  {
    return true;
  }
}
