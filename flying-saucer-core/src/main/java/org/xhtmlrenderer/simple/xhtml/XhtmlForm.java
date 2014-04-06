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
package org.xhtmlrenderer.simple.xhtml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.simple.extend.URLUTF8Encoder;
import org.xhtmlrenderer.simple.xhtml.controls.ButtonControl;
import org.xhtmlrenderer.simple.xhtml.controls.CheckControl;
import org.xhtmlrenderer.simple.xhtml.controls.HiddenControl;
import org.xhtmlrenderer.simple.xhtml.controls.SelectControl;
import org.xhtmlrenderer.simple.xhtml.controls.TextControl;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

public class XhtmlForm {

    protected String _action, _method;

    public XhtmlForm(String action, String method) {
        _action = action;
        _method = method;
    }

    protected List<FormControl> _controls = new LinkedList<FormControl>();

    private List<FormListener> _listeners = new ArrayList<FormListener>();

    public void addFormListener(FormListener listener) {
        _listeners.add(listener);
    }

    public void removeFormListener(FormListener listener) {
        _listeners.remove(listener);
    }

    public FormControl getControl(String name) {
        for (FormControl control : _controls) {
            if (control.getName().equals(name)) {
                return control;
            }
        }
        return null;
    }

    public List<FormControl> getAllControls(String name) {
        List<FormControl> result = new ArrayList<FormControl>();
        for (FormControl control : _controls) {
            if (control.getName().equals(name)) {
                result.add(control);
            }
        }
        return result;
    }

    public Iterator<FormControl> getControls() {
        return _controls.iterator();
    }

    public FormControl createControl(Element e) {
        return createControl(this, e);
    }

    public static FormControl createControl(XhtmlForm form, Element e) {
        if (e == null)
            return null;

        FormControl control;
        String name = e.nodeName();
        if (name.equals("input")) {
            String type = e.attr("type");
            if (ciEquals(type, "text") || ciEquals(type, "password")) {
                control = new TextControl(form, e);
            } else if (ciEquals(type, "hidden")) {
                control = new HiddenControl(form, e);
            } else if (ciEquals(type, "button") || ciEquals(type, "submit")
                    || ciEquals(type, "reset")) {
                control = new ButtonControl(form, e);
            } else if (ciEquals(type, "checkbox") || ciEquals(type, "radio")) {
                control = new CheckControl(form, e);
            } else {
                return null;
            }
        } else if (ciEquals(name, "textarea")) {
            control = new TextControl(form, e);
        } else if (ciEquals(name, "button")) {
            control = new ButtonControl(form, e);
        } else if (ciEquals(name, "select")) {
            control = new SelectControl(form, e);
        } else {
            return null;
        }

        if (form != null) {
            form._controls.add(control);
        }
        return control;
    }

    public void reset() {
        for (FormListener formListener : _listeners) {
            formListener.resetted(this);
        }
    }

    public void submit() {
        // TODO other encodings than urlencode?
        StringBuffer data = new StringBuffer();
        for (Iterator<FormControl> iter = getControls(); iter.hasNext();) {
            FormControl control = iter.next();
            if (control.isSuccessful()) {
                if (control.isMultiple()) {
                    String[] values = control.getMultipleValues();
                    for (String value : values) {
                        if (data.length() > 0) {
                            data.append('&');
                        }
                        data.append(URLUTF8Encoder.encode(control.getName()));
                        data.append('=');
                        data.append(URLUTF8Encoder.encode(value));
                    }
                } else {
                    if (data.length() > 0) {
                        data.append('&');
                    }
                    data.append(URLUTF8Encoder.encode(control.getName()));
                    data.append('=');
                    data.append(URLUTF8Encoder.encode(control.getValue()));
                }
            }
        }

        // TODO effectively submit form
        System.out.println("Form submitted!");
        System.out.println("Action: ".concat(_action));
        System.out.println("Method: ".concat(_method));
        System.out.println("Data: ".concat(data.toString()));

        for (FormListener formListener : _listeners) {
            formListener.submitted(this);
        }
    }

}
