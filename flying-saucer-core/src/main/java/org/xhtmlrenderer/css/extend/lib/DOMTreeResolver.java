/*
 * DOMTreeResolver.java
 * Copyright (c) 2005 Scott Cytacki
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
 *
 */
package org.xhtmlrenderer.css.extend.lib;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.xhtmlrenderer.css.extend.TreeResolver;

/**
 * @author scott
 *         <p/>
 *         works for a w3c DOM tree
 */
public class DOMTreeResolver implements TreeResolver {
    public Object getParentElement(final Object element) {
        Node parent = ((Element) element).parentNode();
        if (!(parent instanceof Element) || parent instanceof Document) parent = null;
        return parent;
    }

    public Object getPreviousSiblingElement(final Object element) {
        Node sibling = ((Element) element).previousSibling();
        while (sibling != null && !(sibling instanceof Element)) {
            sibling = sibling.previousSibling();
        }
        if (sibling == null || !(sibling instanceof Element)) {
            return null;
        }
        return sibling;
    }

    public String getElementName(final Object element) {
        final String name = ((Element) element).nodeName();
//        if (name == null) name = ((Element) element).getNodeName();
        return name;
    }

    public boolean isFirstChildElement(final Object element) {
        final Node parent = ((Element) element).parentNode();
        Node currentChild = parent.childNodeSize() > 0 ? parent.childNode(0) : null;
        while (currentChild != null && !(currentChild instanceof Element)) {
            currentChild = currentChild.nextSibling();
        }
        return currentChild == element;
    }

    public boolean isLastChildElement(final Object element) {
        final Node parent = ((Element) element).parentNode();
        Node currentChild = parent.childNodeSize() > 0 ? parent.childNode(parent.childNodeSize() - 1) : null;
        while (currentChild != null && !(currentChild instanceof Element)) {
            currentChild = currentChild.previousSibling();
        }
        return currentChild == element;
    }

    public boolean matchesElement(final Object element, final String namespaceURI, final String name) {
        final Element e = (Element)element;
        final String localName = e.nodeName();
        final String eName = localName;

//        if (localName == null) {
//            eName = e.getNodeName();
//        } else {
//            eName = localName;
//        }

        if (namespaceURI != null) {
        	return (namespaceURI + ':' + name).equals(localName);
//        	return name.equals(localName) && namespaceURI.equals(e.getNamespaceURI());
        } else if (namespaceURI == TreeResolver.NO_NAMESPACE) {
            return name.equals(eName) && eName.indexOf(':') == -1;
        } else /* if (namespaceURI == null) */ {
            return name.equals(eName);
        }
    }
    
    public int getPositionOfElement(final Object element) {
        final Node parent = ((Element) element).parentNode();
        final List<Node> nl = parent.childNodes();

        int elt_count = 0;
        int i = 0;
        while (i < nl.size()) {
            if (nl.get(i) instanceof Element) {
                if(nl.get(i) == element) {
                    return elt_count;
                } else {
                    elt_count++;
                }
            }
            i++;
        }
        
        //should not happen
        return -1;
    }
}
