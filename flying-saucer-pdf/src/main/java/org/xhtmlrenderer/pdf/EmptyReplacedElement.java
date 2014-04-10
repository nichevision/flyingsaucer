package org.xhtmlrenderer.pdf;

import com.lowagie.text.pdf.PdfAcroForm;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfWriter;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.RenderingContext;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: beck
 * Date: 11/4/11
 * Time: 12:42 PM
 */

public class EmptyReplacedElement extends AbstractFormField
{
  private static final String FIELD_TYPE = "Hidden";

  private final int _width;
  private final int _height;

  private Point _location = new Point(0, 0);

  public EmptyReplacedElement(final int width, final int height)
  {
    _width = width;
    _height = height;
  }

  public void paint(final RenderingContext c, final ITextOutputDevice outputDevice, final BlockBox box)
  {
    final PdfContentByte cb = outputDevice.getCurrentPage();

    final PdfWriter writer = outputDevice.getWriter();

    final PdfAcroForm acroForm = writer.getAcroForm();
    final Element elem = box.getElement();
    final String name = getFieldName(outputDevice, elem);
    final String value = getValue(elem);
    acroForm.addHiddenField(name, value);


  }

  public int getIntrinsicWidth()
  {
    return _width;
  }

  public int getIntrinsicHeight()
  {
    return _height;
  }

  public Point getLocation()
  {
    return _location;
  }

  public void setLocation(final int x, final int y)
  {
    _location = new Point(0, 0);
  }

  protected String getFieldType()
  {
    return FIELD_TYPE;
  }

  public void detach(final LayoutContext c)
  {
  }

  public boolean isRequiresInteractivePaint()
  {
    return false;
  }

  public boolean hasBaseline()
  {
    return false;
  }

  public int getBaseline()
  {
    return 0;
  }
}
