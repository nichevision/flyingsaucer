/*
 * {{{ header & license
 * Copyright (c) 2007 Sean Bright
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
package com.github.danfickle.flyingsaucer.swing;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.MouseInputAdapter;

import org.xhtmlrenderer.render.Box;


/**
 * A MouseTracker is used to delegate mouse events to the {@link org.xhtmlrenderer.swing.FSMouseListener} instances
 *  associated with a {@link org.xhtmlrenderer.swing.BasicPanel}. The tracker will start receiving events as soon
 * as the first listener is added (via {@link #addListener(FSMouseListener)} and will stop receiving events as soon
 * as the last listener is removed via {@link #removeListener(FSMouseListener)}. This binding is handled automatically
 * via the add and remove methods and the tracker will remain active as long as the tracker has at least one listener.
 * The MouseTracker is also responsible for using MouseEvent coordinates to located the Box on which the mouse is
 * acting. 
 */
public class MouseTracker extends MouseInputAdapter {
    private final BasicPanel _panel;
    private final Map<FSMouseListener, FSMouseListener> _handlers;
    private Box _last;
    private boolean _enabled;

    /**
     * Instantiates a MouseTracker to listen to mouse events for the given panel.
     * @param panel the panel for which mouse events should be delegated.
     */
    public MouseTracker(final BasicPanel panel) {
        _panel = panel;
        _handlers = new LinkedHashMap<FSMouseListener, FSMouseListener>();
    }

    /**
     * Adds a listener to receive callbacks on mouse events.
     *
     * @param l the listener
     */
    public void addListener(final FSMouseListener l) {
        if (l == null) {
            return;
        }
        
        if (!_handlers.containsKey(l)) {
            _handlers.put(l, l);
        }
        
        if (!_enabled && _handlers.size() > 0) {
            _panel.addMouseListener(this);
            _panel.addMouseMotionListener(this);
            
            _enabled = true;
        }
    }

    /**
     * Removes the given listener, after which it will no longer receive callbacks on mouse events.
     *
     * @param l the listener to remove
     */
    public void removeListener(final FSMouseListener l) {
        if (l == null) {
            return;
        }
        
        if (_handlers.containsKey(l)) {
            _handlers.remove(l);
        }
        
        if (_enabled && _handlers.size() == 0) {
            _panel.removeMouseListener(this);
            _panel.removeMouseMotionListener(this);
            
            _enabled = false;
        }
    }

    /**
     * Returns a (new) list of all listeners currently tracked for receiving events.
     * @return a (new) list of all listeners currently tracked for receiving events.
     */
    public List<FSMouseListener> getListeners() {
        return new ArrayList<FSMouseListener>(_handlers.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(final MouseEvent e) {
        handleMouseMotion(_panel.find(e));
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(final MouseEvent e) {
        handleMouseMotion(_panel.find(e));
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(final MouseEvent e) {
        handleMouseMotion(_panel.find(e));
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(final MouseEvent e) {
        handleMouseUp(_panel.find(e));
    }
    
    /**
     * {@inheritDoc}
     */
    public void mousePressed(final MouseEvent e) {
        fireMousePressed(e);
    }
    
    /**
     * {@inheritDoc}
     */
    public void mouseDragged(final MouseEvent e) {
        fireMouseDragged(e);
    }

    /**
     * Utility method; calls {@link FSMouseListener#reset()} for all listeners currently being tracked.
     */
    public void reset() {
        _last = null;
        final Iterator<FSMouseListener> iterator = _handlers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().reset();
        }
    }

    // handles delegation of mouse events to listeners
    private void handleMouseMotion(final Box box) {
        if (box == null || box.equals(_last)) {
            return;
        }
        
        if (_last != null) {
            fireMouseOut(_last);
        }
        
        fireMouseOver(box);

        _last = box;
    }


    private void handleMouseUp(final Box box) {
        if (box == null) {
            return;
        }
        
        fireMouseUp(box);
    }
    
    // delegates onMouseOver() to all listeners
    private void fireMouseOver(final Box box) {
        final Iterator<FSMouseListener> iterator = _handlers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().onMouseOver(_panel, box);
        }
    }

    // delegates onMouseOut() to all listeners
    private void fireMouseOut(final Box box) {
        final Iterator<FSMouseListener> iterator = _handlers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().onMouseOut(_panel, box);
        }
    }
    
    // delegates onMouseUp() to all listeners
    private void fireMouseUp(final Box box) {
        final Iterator<FSMouseListener> iterator = _handlers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().onMouseUp(_panel, box);
        }
    }
    
    // delegates onMousePressed() to all listeners
    private void fireMousePressed(final MouseEvent e) {
        final Iterator<FSMouseListener> iterator = _handlers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().onMousePressed(_panel, e);
        }
    }
    
    // delegates onMouseDragged() to all listeners
    private void fireMouseDragged(final MouseEvent e) {
        final Iterator<FSMouseListener> iterator = _handlers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().onMouseDragged(_panel, e);
        }
    }
}
