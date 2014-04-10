/*
 * {{{ header & license
 * Copyright (c) 2008 elbart0 at free.fr (submitted via email)
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

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.extend.FSCanvas;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.util.XRLog;

import com.github.danfickle.flyingsaucer.swing.SwingReplacedElement;
import com.github.danfickle.flyingsaucer.swing.SwingReplacedElementFactory;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;


/**
 * Sample for handling image maps in XHTML, as replaced elements.
 *
 * Sample is incomplete in current state and meant as a starting point for future work.
 */
public class ImageMapReplacedElementFactory extends SwingReplacedElementFactory {
   private final ImageMapListener listener;
   private static final String IMG_USEMAP_ATTR = "usemap";
   private static final String MAP_ELT = "map";
   private static final String MAP_NAME_ATTR = "name";
   private static final String AREA_ELT = "area";
   private static final String AREA_SHAPE_ATTR = "shape";
   private static final String AREA_COORDS_ATTR = "coords";
   private static final String AREA_HREF_ATTR = "href";
   private static final String RECT_SHAPE = "rect";
   private static final String RECTANGLE_SHAPE = "rectangle";
   private static final String CIRC_SHAPE = "circ";
   private static final String CIRCLE_SHAPE = "circle";
   private static final String POLY_SHAPE = "poly";
   private static final String POLYGON_SHAPE = "polygon";

   public ImageMapReplacedElementFactory(final ImageMapListener listener) {
       super(null);
       if (null == listener) {
         throw new IllegalArgumentException("listener required");
      }
      this.listener = listener;
   }

   public ReplacedElement createReplacedElement(final LayoutContext context, final BlockBox box, final UserAgentCallback uac, final int cssWidth, final int cssHeight) {
      final Element e = box.getElement();
      if (e == null) {
         return null;
      } else if (context.getNamespaceHandler().isImageElement(e)) {
         final String usemapAttr = context.getNamespaceHandler().getAttributeValue(e, IMG_USEMAP_ATTR);
         if (isNotBlank(usemapAttr)) {
            final ReplacedElement re = replaceImageMap(uac, context, e, usemapAttr, cssWidth, cssHeight);
            if (context.isInteractive() && re instanceof SwingReplacedElement) {
                final FSCanvas canvas = context.getCanvas();
                if (canvas instanceof JComponent) {
                    ((JComponent) canvas).add(((SwingReplacedElement) re).getJComponent());
                }
            }
            return re;
         } else {
            return replaceImage(uac, context, e, cssWidth, cssHeight);
         }
      } else {
         return null;
      }
   }

    private boolean isNotBlank(final String _v) {
        if (_v == null || _v.length() == 0) {
            return false;
        }
        for (int i = 0; i < _v.length(); i++) {
             if (Character.isWhitespace(_v.charAt(i))) continue;
            return false;
        }
        return true;
    }

    // See SwingReplacedElementFactory#replaceImage
   protected ReplacedElement replaceImageMap(final UserAgentCallback uac, final LayoutContext context, final Element elem, final String usemapAttr, final int cssWidth, final int cssHeight) {
      ReplacedElement re;
      // lookup in cache, or instantiate
      re = lookupImageReplacedElement(elem, "");
      if (re == null) {
         Image im = null;
         final String imageSrc = context.getNamespaceHandler().getImageSourceURI(elem);
         if (imageSrc == null || imageSrc.length() == 0) {
            XRLog.layout(Level.WARNING, "No source provided for img element.");
            re = newIrreplaceableImageElement(cssWidth, cssHeight);
         } else {
            final FSImage fsImage = uac.getImageResource(imageSrc).getImage();
            if (fsImage != null) {
               im = ((AWTFSImage) fsImage).getImage();
            }

            if (im != null) {
               final String mapName = usemapAttr.substring(1);
               Node map = elem.ownerDocument().getElementById(mapName);
               if (null == map) {
                  final List<Element> maps = elem.ownerDocument().getElementsByTag(MAP_ELT);
                  for (int i = 0; i < maps.size(); i++) {
                      final String mapAttr = maps.get(i).attr(MAP_NAME_ATTR);
                      if (areEqual(mapName, mapAttr)) {
                        map = maps.get(i);
                        break;
                     }
                  }
                  if (null == map) {
                     XRLog.layout(Level.INFO, "No map named: '" + mapName + "'");
                  }
               }
               re = new ImageMapReplacedElement(im, map, cssWidth, cssHeight, listener);
            } else {
               re = newIrreplaceableImageElement(cssWidth, cssHeight);
            }
         }
         storeImageReplacedElement(elem, re, "", -1, -1);
      }
      return re;
   }

    private static boolean areEqual(final String str1, final String str2) {
        return (str1 == null && str2 == null) || (str1 != null && str1.equals(str2));
    }
    private static boolean areEqualIgnoreCase(final String str1, final String str2) {
        return (str1 == null && str2 == null) || (str1 != null && str1.equalsIgnoreCase(str2));
    }

    private static class ImageMapReplacedElement extends SwingReplacedElement {
      private final Map<Shape, String> areas;

      public ImageMapReplacedElement(final Image image, final Node map, final int targetWidth, final int targetHeight, final ImageMapListener listener) {
         super(create(image, targetWidth, targetHeight));
         areas = parseMap(map);
         getJComponent().addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
               final Point point = e.getPoint();
                final Set<Entry<Shape, String>> set = areas.entrySet();
                for (final Entry<Shape, String> entry : set) {
                    if (entry.getKey().contains(point)) {
                        listener.areaClicked(new ImageMapEvent(this, (String) entry.getValue()));
                    }
                }
            }

            public void mouseExited(final MouseEvent e) {
               getJComponent().setCursor(Cursor.getDefaultCursor());
            }
         });
         getJComponent().addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(final MouseEvent e) {
               final JComponent c = getJComponent();
               final Point point = e.getPoint();
                final Set<Entry<Shape, String>> set = areas.entrySet();
                for (final Entry<Shape, String> entry : set) {
                    if ((entry.getKey()).contains(point)) {
                        updateCursor(c, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
               updateCursor(c, Cursor.getDefaultCursor());
            }
         });
      }

      private static void updateCursor(final JComponent c, final Cursor cursor) {
         if (!c.getCursor().equals(cursor)) {
            c.setCursor(cursor);
         }
      }

      private static Map<Shape, String> parseMap(final Node map) {
         if (null == map) {
            return Collections.emptyMap();
         } else if (map.childNodeSize() > 0) {
            final List<Node> children = map.childNodes();
            final Map<Shape, String> areas = new HashMap<>(children.size());
            for (int i = 0; i < children.size(); i++) {
               final Node area = children.get(i);
               if (areEqualIgnoreCase(AREA_ELT, area.nodeName())) {
                  if (area.attributes().size() > 0) {
                     final String shapeAttr = area.attr(AREA_SHAPE_ATTR);
                     final String[] coords = area.attr(AREA_COORDS_ATTR).split(","); // TODO 
                     final String href = area.attr(AREA_HREF_ATTR);
                     if (areEqualIgnoreCase(RECT_SHAPE, shapeAttr) || areEqualIgnoreCase(RECTANGLE_SHAPE, shapeAttr)) {
                        final Shape shape = getCoords(coords, 4);
                        if (null != shape) {
                           areas.put(shape, href);
                        }
                     } else if (areEqualIgnoreCase(CIRC_SHAPE, shapeAttr) || areEqualIgnoreCase(CIRCLE_SHAPE, shapeAttr)) {
                        final Shape shape = getCoords(coords, 3);
                        if (null != shape) {
                           areas.put(shape, href);
                        }
                     } else if (areEqualIgnoreCase(POLY_SHAPE, shapeAttr) || areEqualIgnoreCase(POLYGON_SHAPE, shapeAttr)) {
                        final Shape shape = getCoords(coords, -1);
                        if (null != shape) {
                           areas.put(shape, href);
                        }
                     } else {
                        if (XRLog.isLoggingEnabled()) {
                           XRLog.layout(Level.INFO, "Unsupported shape: '" + shapeAttr + "'");
                        }
                     }
                  }
               }
            }
            return areas;
         } else {
            return Collections.emptyMap();
         }
      }

      private static Shape getCoords(final String[] coordValues, final int length) {
         if ((-1 == length && 0 == coordValues.length % 2) || length == coordValues.length) {
            final int[] coords = new int[coordValues.length];
            int i = 0;
             for (final String coord : coordValues) {
                 try {
                     coords[i++] = Integer.parseInt(coord.trim());
                 } catch (final NumberFormatException e) {
                     XRLog.layout(Level.WARNING, "Error while parsing shape coords", e);
                     return null;
                 }
             }
            if (4 == length) {
               return new Rectangle2D.Float(coords[0], coords[1], coords[2] - coords[0], coords[3] - coords[1]);
            } else if (3 == length) {
               final int radius = coords[2];
               return new Ellipse2D.Float(coords[0] - radius, coords[1] - radius, radius * 2, radius * 2);
            } else if (-1 == length) {
               final int npoints = coords.length / 2;
               final int[] xpoints = new int[npoints];
               final int[] ypoints = new int[npoints];
               for (int c = 0, p = 0; p < npoints; p++) {
                  xpoints[p] = coords[c++];
                  ypoints[p] = coords[c++];
               }
               return new Polygon(xpoints, ypoints, npoints);
            } else {
               XRLog.layout(Level.INFO, "Unsupported shape: '" + length + "'");
               return null;
            }
         } else {
            return null;
         }
      }

      private static JComponent create(final Image image, final int targetWidth, final int targetHeight) {
         final JLabel component = new JLabel(new ImageIcon(image));
         component.setSize(component.getPreferredSize());
         return component;
      }
   }
}
