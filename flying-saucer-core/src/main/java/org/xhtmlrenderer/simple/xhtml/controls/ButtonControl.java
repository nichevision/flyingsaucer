/*
 * {{{ header & license
 * Copyright (c) 2007 Vianney le Clément
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
package org.xhtmlrenderer.simple.xhtml.controls;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.simple.xhtml.XhtmlForm;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

public class ButtonControl extends AbstractControl {

    private String _type, _label;
    private final boolean _extended;
    private final List<ButtonControlListener> _listeners = new ArrayList<ButtonControlListener>();

    public ButtonControl(final XhtmlForm form, final Element e) {
        super(form, e);

        _extended = ciEquals(e.nodeName(), "button");
        if (_extended) {
            _label = collectText(e);
        } else {
            _label = getValue();
        }

        _type = e.attr("type");
        if (!ciEquals(_type, "reset") && !ciEquals(_type, "button")) {
            _type = "submit";
        }
    }

    public String getType() {
        return _type;
    }
    
    public String getLabel() {
        return _label;
    }

    /**
     * @return <code>true</code> if this button has been defined with
     *         <code>&lt;button&gt;</code>, <code>false</code> if this
     *         button has been defined with <code>&lt;input&gt;</code>
     */
    public boolean isExtended() {
        return _extended;
    }

    public void addButtonControlListener(final ButtonControlListener listener) {
        _listeners.add(listener);
    }

    public void removeButtonControlListener(final ButtonControlListener listener) {
        _listeners.remove(listener);
    }

    public boolean press() {
        for (final ButtonControlListener buttonControlListener : _listeners) {
            if(!buttonControlListener.pressed(this))
                return false;
        }
        return true;
    }
}
