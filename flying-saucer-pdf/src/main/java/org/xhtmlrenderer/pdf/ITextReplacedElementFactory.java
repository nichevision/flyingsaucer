/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
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
package org.xhtmlrenderer.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.util.JsoupUtil;

import static org.xhtmlrenderer.util.GeneralUtil.ciEquals;

public class ITextReplacedElementFactory implements ReplacedElementFactory {
    private ITextOutputDevice _outputDevice;

    private Map<Element, RadioButtonFormField> _radioButtonsByElem = new HashMap<Element, RadioButtonFormField>();
    private Map<String, List<RadioButtonFormField>> _radioButtonsByName = new HashMap<String, List<RadioButtonFormField>>();

    public ITextReplacedElementFactory(ITextOutputDevice outputDevice) {
        _outputDevice = outputDevice;
    }

    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
            UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.nodeName();
        if (nodeName.equals("img")) {
            String srcAttr = e.attr("src");
            if (srcAttr != null && srcAttr.length() > 0) {
                FSImage fsImage = uac.getImageResource(srcAttr).getImage();
                if (fsImage != null) {
                    if (cssWidth != -1 || cssHeight != -1) {
                        fsImage.scale(cssWidth, cssHeight);
                    }
                    return new ITextImageElement(fsImage);
                }                    
            }

        } else if (nodeName.equals("input")) {
            String type = e.attr("type");
            if (ciEquals(type, "hidden")) {
                return new EmptyReplacedElement(1, 1);
            } else if (ciEquals(type, "checkbox")) {
                return new CheckboxFormField(c, box, cssWidth, cssHeight);
            } else if (ciEquals(type, "radio")) {
                //TODO finish support for Radio button
                //RadioButtonFormField result = new RadioButtonFormField(
                //			this, c, box, cssWidth, cssHeight);
                //		saveResult(e, result);
                //return result;
                return new EmptyReplacedElement(0, 0);

            } else {
                return new TextFormField(c, box, cssWidth, cssHeight);
            }
            /*
             } else if (nodeName.equals("select")) {//TODO Support select
             return new SelectFormField(c, box, cssWidth, cssHeight);
             } else if (isTextarea(e)) {//TODO Review if this is needed the textarea item prints fine currently
             return new TextAreaFormField(c, box, cssWidth, cssHeight);
             */
        } else if (ciEquals(nodeName, "bookmark")) {
            // HACK Add box as named anchor and return placeholder
            BookmarkElement result = new BookmarkElement();
            if (e.hasAttr("name")) {
                String name = e.attr("name");
                c.addBoxId(name, box);
                result.setAnchorName(name);
            }
            return result;
        }

        return null;
    }

    private boolean isTextarea(Element e) {
        if (!ciEquals(e.nodeName(), "textarea"))
            return false;

        Node n = JsoupUtil.firstChild(e);

        while (n != null) 
        {
        	if (!JsoupUtil.isText(n)) 
                return false;

        	n = n.nextSibling();
        }

        return true;
    }

    private void saveResult(Element e, RadioButtonFormField result) {
        _radioButtonsByElem.put(e, result);

        String fieldName = result.getFieldName(_outputDevice, e);
        List<RadioButtonFormField> fields = _radioButtonsByName.get(fieldName);
        if (fields == null) {
            fields = new ArrayList<RadioButtonFormField>();
            _radioButtonsByName.put(fieldName, fields);
        }
        fields.add(result);
    }

    public void reset() {
        _radioButtonsByElem = new HashMap<Element, RadioButtonFormField>();
        _radioButtonsByName = new HashMap<String, List<RadioButtonFormField>>();
    }

    public void remove(Element e) {
        RadioButtonFormField field = _radioButtonsByElem.remove(e);
        if (field != null) {
            String fieldName = field.getFieldName(_outputDevice, e);
            List<RadioButtonFormField> values = _radioButtonsByName.get(fieldName);
            if (values != null) {
                values.remove(field);
                if (values.size() == 0) {
                    _radioButtonsByName.remove(fieldName);
                }
            }
        }
    }

    public void remove(String fieldName) {
        List<RadioButtonFormField> values = _radioButtonsByName.get(fieldName);
        if (values != null) {
            for (RadioButtonFormField field : values) {
                _radioButtonsByElem.remove(field.getBox().getElement());
            }
        }

        _radioButtonsByName.remove(fieldName);
    }

    public List<RadioButtonFormField> getRadioButtons(String name) {
        return _radioButtonsByName.get(name);
    }

    public void setFormSubmissionListener(FormSubmissionListener listener) {
        // nothing to do, form submission is handled by pdf readers
    }
}
