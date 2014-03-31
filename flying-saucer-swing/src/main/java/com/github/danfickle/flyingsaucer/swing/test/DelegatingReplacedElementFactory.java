/*
 * {{{ header & license
 * Copyright (c) 2008 Patrick Wright
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
 * }}}
 */
package com.github.danfickle.flyingsaucer.swing.test;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;

import java.util.*;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

/**
 * @author patrick
 */
public class DelegatingReplacedElementFactory implements ReplacedElementFactory {
    private final List<ElementReplacer> replacers;
    private final Map<String, ElementReplacer> byNameReplacers;
    private final List<ERItem> elementReplacements;

    public DelegatingReplacedElementFactory() {
        replacers = new ArrayList<ElementReplacer>();
        elementReplacements = new ArrayList<ERItem>();
        byNameReplacers = new HashMap<String, ElementReplacer>();
    }

    public ReplacedElement createReplacedElement(final LayoutContext context,
                                                 final BlockBox box,
                                                 final UserAgentCallback uac,
                                                 final int cssWidth,
                                                 final int cssHeight
    ) {
        final ElementReplacer nameReplacer = byNameReplacers.get(box.getElement().nodeName());
        if (nameReplacer != null) {
            return replaceUsing(context, box, uac, cssWidth, cssHeight, nameReplacer);
        }
        for (Iterator<ElementReplacer> iterator = replacers.iterator(); iterator.hasNext();) {
            ElementReplacer replacer = iterator.next();
            if (replacer.accept(context, box.getElement())) {
                return replaceUsing(context, box, uac, cssWidth, cssHeight, replacer);
            }
        }
        return null;
    }

    private ReplacedElement replaceUsing(LayoutContext context, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight, ElementReplacer replacer) {
        final ReplacedElement re = replacer.replace(context, box, uac, cssWidth, cssHeight);
        elementReplacements.add(new ERItem(box.getElement(), re, replacer));
        return re;
    }

    public void reset() {
        System.out.println("\n\n***Factory reset()");
        elementReplacements.clear();
        for (Iterator<ElementReplacer> iterator = replacers.iterator(); iterator.hasNext();) {
            ElementReplacer elementReplacer = iterator.next();
            elementReplacer.reset();
        }
        for (Iterator<ElementReplacer> iterator = byNameReplacers.values().iterator(); iterator.hasNext();) {
            iterator.next().reset();
        }
    }

    public void remove(final Element element) {
        final int idx = elementReplacements.indexOf(element);
        ERItem item = elementReplacements.get(idx);
        elementReplacements.remove(idx);
        item.elementReplacer.clear(element);
    }

    public ElementReplacer addReplacer(final ElementReplacer replacer) {
        if (replacer.isElementNameMatch()) {
            byNameReplacers.put(replacer.getElementNameMatch(), replacer);
        } else {
            replacers.add(replacer);
        }
        return replacer;
    }

    public void removeReplacer(final ElementReplacer replacer) {
        replacers.remove(replacer);
    }

    public void setFormSubmissionListener(FormSubmissionListener listener) {
        // maybe there is nothing to do...
    }

    private static class ERItem {
        private final Element element;
        private final ElementReplacer elementReplacer;

        private ERItem(final Element e, final ReplacedElement re, final ElementReplacer er) {
            element = e;
            elementReplacer = er;
        }

        public int hashCode() {
            return element.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof ERItem)) return false;
            ERItem other = (ERItem) o;
            return other.element == this.element;
        }
    }
}
