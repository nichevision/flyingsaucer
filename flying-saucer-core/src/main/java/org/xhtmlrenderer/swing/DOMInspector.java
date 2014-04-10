/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package org.xhtmlrenderer.swing;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.xhtmlrenderer.context.StyleReference;
import org.xhtmlrenderer.css.constants.ValueConstants;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.layout.SharedContext;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Description of the Class
 *
 * @author empty
 */
public class DOMInspector extends JPanel {
    private static final long serialVersionUID = 1L;

    // PW
    /**
     * Description of the Field
     */
    StyleReference styleReference;
    /**
     * Description of the Field
     */
    SharedContext context;
    /**
     * Description of the Field
     */
    ElementPropertiesPanel elementPropPanel;
    /**
     * Description of the Field
     */
    DOMSelectionListener nodeSelectionListener;
    /**
     * Description of the Field
     */
    JSplitPane splitPane;
    // PW

    /**
     * Description of the Field
     */
    Document doc;
    /**
     * Description of the Field
     */
    JButton close;
    /**
     * Description of the Field
     */
    JTree tree;

    /**
     * Description of the Field
     */
    JScrollPane scroll;

    /**
     * Constructor for the DOMInspector object
     *
     * @param doc PARAM
     */
    public DOMInspector(final Document doc) {
        this(doc, null, null);
    }

    /**
     * Constructor for the DOMInspector object
     *
     * @param doc     PARAM
     * @param context PARAM
     * @param sr      PARAM
     */
    public DOMInspector(final Document doc, final SharedContext context, final StyleReference sr) {
        super();

        this.setLayout(new java.awt.BorderLayout());

        //JPanel treePanel = new JPanel();
        this.tree = new JTree();
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.scroll = new JScrollPane(tree);

        splitPane = null;
        if (sr == null) {
            add(scroll, "Center");
        } else {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(150);

            this.add(splitPane, "Center");
            splitPane.setLeftComponent(scroll);
        }

        close = new JButton("close");
        this.add(close, "South");
        this.setPreferredSize(new Dimension(300, 300));

        setForDocument(doc, context, sr);

        close.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                getFrame(DOMInspector.this).setVisible(false);
            }
        });
    }

    /**
     * Description of the Method
     *
     * @param g PARAM
     */
    public void paintComponent(final Graphics g) {

        super.paintComponent(g);

        g.drawLine(0, 0, 100, 100);

    }

    /**
     * Sets the forDocument attribute of the DOMInspector object
     *
     * @param doc The new forDocument value
     */
    public void setForDocument(final Document doc) {
        setForDocument(doc, null, null);
    }

    /**
     * Sets the forDocument attribute of the DOMInspector object
     *
     * @param doc     The new forDocument value
     * @param context The new forDocument value
     * @param sr      The new forDocument value
     */
    public void setForDocument(final Document doc, final SharedContext context, final StyleReference sr) {
        this.doc = doc;
        this.styleReference = sr;
        this.context = context;
        this.initForCurrentDocument();
    }

    /**
     * Gets the frame attribute of the DOMInspector object
     *
     * @param comp PARAM
     * @return The frame value
     */
    public JFrame getFrame(final Component comp) {
        if (comp instanceof JFrame) {
            return (JFrame) comp;
        }
        return getFrame(comp.getParent());
    }

    /**
     * Description of the Method
     */
    private void initForCurrentDocument() {
        // tree stuff
        final TreeModel model = new DOMTreeModel(doc);
        tree.setModel(model);
        if (!(tree.getCellRenderer() instanceof DOMTreeCellRenderer)) {
            tree.setCellRenderer(new DOMTreeCellRenderer());
        }

        if (styleReference != null) {
            if (elementPropPanel != null) {
                splitPane.remove(elementPropPanel);
            }
            elementPropPanel = new ElementPropertiesPanel(styleReference);
            splitPane.setRightComponent(elementPropPanel);

            tree.removeTreeSelectionListener(nodeSelectionListener);

            //nodeSelectionListener = new DOMSelectionListener( tree, styleReference, elementPropPanel );
            nodeSelectionListener = new DOMSelectionListener(tree, elementPropPanel);
            tree.addTreeSelectionListener(nodeSelectionListener);
        }
    }
}

//-{{{ ElementPropertiesPanel

/**
 * Description of the Class
 *
 * @author empty
 */
class ElementPropertiesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /**
     * Description of the Field
     */
    //private SharedContext _context;
    /**
     * Description of the Field
     */
    private final StyleReference _sr;
    /**
     * Description of the Field
     */
    private final JTable _properties;
    /**
     * Description of the Field
     */
    private final TableModel _defaultTableModel;

    /**
     * Constructor for the ElementPropertiesPanel object
     *
     * @param sr PARAM
     */
    ElementPropertiesPanel(final StyleReference sr) {
        super();
        //this._context = context;
        this._sr = sr;

        this._properties = new PropertiesJTable();
        this._defaultTableModel = new DefaultTableModel();

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(_properties), BorderLayout.CENTER);
    }

    /**
     * Sets the forElement attribute of the ElementPropertiesPanel object
     *
     * @param node The new forElement value
     */
    public void setForElement(final Node node) {
        try {
            _properties.setModel(tableModel(node));
            final TableColumnModel model = _properties.getColumnModel();
            if (model.getColumnCount() > 0) {
                model.getColumn(0).sizeWidthToFit();
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Description of the Method
     *
     * @param node PARAM
     * @return Returns
     * @throws Exception Throws
     */
    private TableModel tableModel(final Node node) {
        if (!(node instanceof Element)) {
            Toolkit.getDefaultToolkit().beep();
            return _defaultTableModel;
        }
        final Map<String, PropertyValue> props = _sr.getCascadedPropertiesMap((Element) node);
        return new PropertiesTableModel(props);
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    static class PropertiesJTable extends JTable {
        private static final long serialVersionUID = 1L;

        /**
         * Description of the Field
         */
        Font propLabelFont;
        /**
         * Description of the Field
         */
        Font defaultFont;

        /**
         * Constructor for the PropertiesJTable object
         */
        PropertiesJTable() {
            super();
            this.setColumnSelectionAllowed(false);
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            propLabelFont = new Font("Courier New", Font.BOLD, 12);
            defaultFont = new Font("Default", Font.PLAIN, 12);
        }

        /**
         * Gets the cellRenderer attribute of the PropertiesJTable object
         *
         * @param row PARAM
         * @param col PARAM
         * @return The cellRenderer value
         */
        public TableCellRenderer getCellRenderer(final int row, final int col) {
            final JLabel label = (JLabel) super.getCellRenderer(row, col);
            label.setBackground(Color.white);
            label.setFont(defaultFont);
            if (col == 0) {
                // BUG: not working?
                label.setFont(propLabelFont);
            } else if (col == 2) {
                final PropertiesTableModel pmodel = (PropertiesTableModel) this.getModel();
                final Map.Entry<String, PropertyValue> me = (Map.Entry<String, PropertyValue>) pmodel._properties.entrySet().toArray()[row];
                final PropertyValue cpv = me.getValue();
                if (cpv.getCssText().startsWith("rgb")) {
                    label.setBackground(org.xhtmlrenderer.css.util.ConversionUtil.rgbToColor(cpv.getRGBColorValue()));
                }
            }
            return (TableCellRenderer) label;
        }
    }

    /**
     * Description of the Class
     *
     * @author Patrick Wright
     */
    static class PropertiesTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        /**
         * Description of the Field
         */
        //String _colNames[] = {"Property Name", "Text", "Value", "Important-Inherit"};
        String _colNames[] = {"Property Name", "Text", "Value"};

        /**
         * Description of the Field
         */
        Map<String, PropertyValue> _properties;

        /**
         * Constructor for the PropertiesTableModel object
         *
         * @param cssProperties PARAM
         */
        PropertiesTableModel(final Map<String, PropertyValue> cssProperties) {
            _properties = cssProperties;
        }

        /**
         * Gets the columnName attribute of the PropertiesTableModel object
         *
         * @param col PARAM
         * @return The columnName value
         */
        public String getColumnName(final int col) {
            return _colNames[col];
        }

        /**
         * Gets the columnCount attribute of the PropertiesTableModel object
         *
         * @return The columnCount value
         */
        public int getColumnCount() {
            return _colNames.length;
        }

        /**
         * Gets the rowCount attribute of the PropertiesTableModel object
         *
         * @return The rowCount value
         */
        public int getRowCount() {
            return _properties.size();
        }

        /**
         * Gets the valueAt attribute of the PropertiesTableModel object
         *
         * @param row PARAM
         * @param col PARAM
         * @return The valueAt value
         */
        public Object getValueAt(final int row, final int col) {
            final Map.Entry<String, PropertyValue> me = (Map.Entry<String, PropertyValue>) _properties.entrySet().toArray()[row];
            final PropertyValue cpv = me.getValue();

            Object val = null;
            switch (col) {

                case 0:
                    val = me.getKey();
                    break;
                case 1:
                    val = cpv.getCssText();
                    break;
                case 2:
                    if (ValueConstants.isNumber(cpv.getPrimitiveTypeN())) {
                        val = new Float(cpv.getFloatValue());
                    } else {
                        val = "";//actual.cssValue().getCssText();
                    }
                    break;
                    /* ouch, can't do this now: case 3:
                        val = ( cpv.actual.isImportant() ? "!Imp" : "" ) +
                                " " +
                                ( actual.forcedInherit() ? "Inherit" : "" );
                        break;
                     */
            }
            return val;
        }

        /**
         * Gets the cellEditable attribute of the PropertiesTableModel object
         *
         * @param row PARAM
         * @param col PARAM
         * @return The cellEditable value
         */
        public boolean isCellEditable(final int row, final int col) {
            return false;
        }
    }
}//}}}

//-{{{ DOMSelectionListener

/**
 * Description of the Class
 *
 * @author empty
 */
class DOMSelectionListener implements TreeSelectionListener {

    /**
     * Description of the Field
     */
    private final JTree _tree;
    /** Description of the Field */
    //private StyleReference _sr;
    /**
     * Description of the Field
     */
    private final ElementPropertiesPanel _elemPropPanel;

    /**
     * Constructor for the DOMSelectionListener object
     *
     * @param tree  PARAM
     * @param panel PARAM
     */
    //DOMSelectionListener( JTree tree, StyleReference sr, ElementPropertiesPanel panel ) {
    DOMSelectionListener(final JTree tree, final ElementPropertiesPanel panel) {
        _tree = tree;
        //_sr = sr;
        _elemPropPanel = panel;
    }

    /**
     * Description of the Method
     *
     * @param e PARAM
     */
    public void valueChanged(final TreeSelectionEvent e) {
        final Node node = (Node) _tree.getLastSelectedPathComponent();

        if (node == null) {
            return;
        }

        _elemPropPanel.setForElement(node);
    }
}//}}}

//-{{{

/**
 * Description of the Class
 *
 * @author empty
 */
class DOMTreeModel implements TreeModel {

    /**
     * Description of the Field
     */
    Document doc;

    /**
     * Our root for display
     */
    Node root;

    /**
     * Description of the Field
     */
    HashMap<Node, List<Node>> displayableNodes;

    /**
     * Description of the Field
     */
    List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    /**
     * Constructor for the DOMTreeModel object
     *
     * @param doc PARAM
     */
    public DOMTreeModel(final Document doc) {
        this.displayableNodes = new HashMap<>();
        this.doc = doc;
        setRoot("body");
    }

    private void setRoot(final String rootNodeName) {
        final Node tempRoot = doc;
        for (final Node node : tempRoot.childNodes())
        {
            if (node.nodeName().toLowerCase(Locale.US).equals(rootNodeName))
            {
                this.root = node;
            }
        }
    }


    //Adds a listener for the TreeModelEvent posted after the tree changes.

    /**
     * Adds the specified TreeModel listener to receive TreeModel events from
     * this component. If listener l is null, no exception is thrown and no
     * action is performed.
     *
     * @param l Contains the TreeModelListener for TreeModelEvent data.
     */
    public void addTreeModelListener(final TreeModelListener l) {

        this.listeners.add(l);

    }


    //Removes a listener previously added with addTreeModelListener.

    /**
     * Removes the specified TreeModel listener so that it no longer receives
     * TreeModel events from this component. This method performs no function,
     * nor does it throw an exception, if the listener specified by the argument
     * was not previously added to this component. If listener l is null, no
     * exception is thrown and no action is performed.
     *
     * @param l Contains the TreeModelListener for TreeModelEvent data.
     */
    public void removeTreeModelListener(final TreeModelListener l) {

        this.listeners.remove(l);

    }


    //Messaged when the user has altered the value for the item identified by path to newValue.

    /**
     * Description of the Method
     *
     * @param path     PARAM
     * @param newValue PARAM
     */
    public void valueForPathChanged(final TreePath path, final Object newValue) {

        // no-op

    }

    //Returns the child of parent at index index in the parent's child array.

    /**
     * Gets the child attribute of the DOMTreeModel object
     *
     * @param parent PARAM
     * @param index  PARAM
     * @return The child value
     */
    public Object getChild(final Object parent, final int index) {

        final Node node = (Node) parent;

        List<Node> children = this.displayableNodes.get(parent);
        if (children == null) {
            children = addDisplayable(node);
        }

        return children.get(index);
    }


    //Returns the number of children of parent.

    /**
     * Gets the childCount attribute of the DOMTreeModel object
     *
     * @param parent PARAM
     * @return The childCount value
     */
    public int getChildCount(final Object parent) {

        final Node node = (Node) parent;
        List<Node> children = this.displayableNodes.get(parent);
        if (children == null) {
            children = addDisplayable(node);
        }

        return children.size();
    }


    //Returns the index of child in parent.

    /**
     * Gets the indexOfChild attribute of the DOMTreeModel object
     *
     * @param parent PARAM
     * @param child  PARAM
     * @return The indexOfChild value
     */
    public int getIndexOfChild(final Object parent, final Object child) {

        final Node node = (Node) parent;
        List<Node> children = this.displayableNodes.get(parent);
        if (children == null) {
            children = addDisplayable(node);
        }
        if (children.contains(child)) {
            return children.indexOf(child);
        } else {
            return -1;
        }
    }


    //Returns the root of the tree.

    /**
     * Gets the root attribute of the DOMTreeModel object
     *
     * @return The root value
     */
    public Object getRoot() {

        return this.root;
    }


    //Returns true if node is a leaf.

    /**
     * Gets the leaf attribute of the DOMTreeModel object
     *
     * @param nd PARAM
     * @return The leaf value
     */
    public boolean isLeaf(final Object nd) {

        final Node node = (Node) nd;

        return (node.childNodeSize() <= 0);
    }

    // only adds displayable nodes--not stupid DOM text filler nodes
    /**
     * Adds a feature to the Displayable attribute of the DOMTreeModel object
     *
     * @param parent The feature to be added to the Displayable attribute
     * @return Returns
     */
    private List<Node> addDisplayable(final Node parent) {
        List<Node> children = this.displayableNodes.get(parent);
        if (children == null) {
            children = new ArrayList<Node>();
            this.displayableNodes.put(parent, children);
            for (final Node child : parent.childNodes())
            {
            	if (child instanceof Element ||
            		child instanceof Comment ||
            		(child instanceof TextNode &&
            		 !((TextNode) child).text().trim().isEmpty()))
            	{
            		children.add(child);
            	}
            }
            return children;
        } else {
            return new ArrayList<Node>();
        }
    }

}//}}}

//-{{{ DOMTreeCellRenderer

/**
 * Description of the Class
 *
 * @author empty
 */
class DOMTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    /**
     * Gets the treeCellRendererComponent attribute of the DOMTreeCellRenderer
     * object
     *
     * @param tree     PARAM
     * @param value    PARAM
     * @param selected PARAM
     * @param expanded PARAM
     * @param leaf     PARAM
     * @param row      PARAM
     * @param hasFocus PARAM
     * @return The treeCellRendererComponent value
     */
    public Component getTreeCellRendererComponent(final JTree tree, Object value,
                                                  final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {

        final Node node = (Node) value;

        if (node instanceof Element) {

            String cls = "";
            if (node.attributes().size() > 0) {
                final String cn = node.attr("class");
                if (cn != null) {
                    cls = " class='" + cn + "'";
                }
            }
            value = "<" + node.nodeName() + cls + ">";

        }

        if (node instanceof TextNode) {

            if (((TextNode) node).text().trim().length() > 0) {
                value = "\"" + ((TextNode) node).text() + "\"";
            }
        }

        if (node instanceof Comment) {

            value = "<!-- " + ((Comment) node).getData() + " -->";

        }

        final DefaultTreeCellRenderer tcr = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        tcr.setOpenIcon(null);
        tcr.setClosedIcon(null);

        return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
}
