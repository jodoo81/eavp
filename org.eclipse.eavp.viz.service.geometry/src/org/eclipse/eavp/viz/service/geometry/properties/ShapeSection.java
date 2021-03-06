/*******************************************************************************
 * Copyright (c) 2016 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Robert Smith
 *******************************************************************************/
package org.eclipse.eavp.viz.service.geometry.properties;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.eavp.geometry.view.model.ComboDisplayOptionData;
import org.eclipse.eavp.geometry.view.model.DisplayOption;
import org.eclipse.eavp.geometry.view.model.DisplayOptionType;
import org.eclipse.eavp.geometry.view.model.DoubleTextDisplayOptionData;
import org.eclipse.eavp.geometry.view.model.IDisplayOptionData;
import org.eclipse.eavp.geometry.view.model.IRenderElement;
import org.eclipse.eavp.geometry.view.model.IntegerTextDisplayOptionData;
import org.eclipse.eavp.geometry.view.model.RenderObject;
import org.eclipse.eavp.viz.service.IRenderElementHolder;
import org.eclipse.eavp.viz.service.geometry.widgets.ISpinner;
import org.eclipse.eavp.viz.service.geometry.widgets.ISpinnerListener;
import org.eclipse.eavp.viz.service.geometry.widgets.RealSpinner;
import org.eclipse.eavp.viz.service.geometry.widgets.ShapeTreeView;
import org.eclipse.eavp.viz.service.geometry.widgets.TransformationPropertyWidget;
import org.eclipse.january.geometry.GeometryFactory;
import org.eclipse.january.geometry.INode;
import org.eclipse.january.geometry.Triangle;
import org.eclipse.january.geometry.Vertex;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

public class ShapeSection extends AbstractPropertySection {

	/**
	 * The number of lines of properties to display in the view. If a shape has
	 * more than NUM_PROPERTIES properties defined, extra properties can not be
	 * displayed or edited in this view.
	 */
	final static private int NUM_PROPERTIES = 5;

	private boolean triangleDataLoaded;

	/**
	 * The display group
	 */
	private Group displayGroup;

	/**
	 * A list of controls in the display which need to be disposed.
	 */
	private ArrayList<Control> disposableControls;

	/**
	 * The text box in which the INode's ID will be editable.
	 */
	private Text idText;

	/**
	 * The text box in which the INode's name will be editable
	 */
	private Text nameText;

	/**
	 * A list of optional spinners which have been added to the view to expose
	 * all of the shape's properties.
	 */
	private ArrayList<RealSpinner> propertySpinners = new ArrayList<RealSpinner>();

	/**
	 * The list of widgets which will allow the shape's properties to be
	 * displayed and edited.
	 */
	private ArrayList<TransformationPropertyWidget> propertyWidgets;

	/**
	 * The object whose properties are displayed by this section.
	 */
	private IRenderElement source;

	/**
	 * The index showing where in the tree of the selected object to find the
	 * source IRenderElement. The tree is laid out linearly according to a
	 * breadth first search for the purpose of this index.
	 */
	private int sourceIndex;

	/**
	 * The tree spinners that set the object's center
	 */
	private RealSpinner[] translateSpinners = new RealSpinner[3];

	/**
	 * The table showing the coordinates for the triangle vertices of the
	 * shape's mesh.
	 */
	private TableViewer triangleTable;
	
	private ArrayList<TableViewerColumn> triangleTableColumns;

	/**
	 * A constructor allowing for the section to be set to display one of the
	 * selected object's children instead of the object itself.
	 * 
	 * @param index
	 *            The index showing where in the tree of the selected object to
	 *            find the source IRenderElement. The tree is laid out linearly
	 *            according to a breadth first search for the purpose of this
	 *            index, so an index of 0 will display the selected
	 *            IRenderElement itself.
	 *
	 */
	public ShapeSection(int index) {
		super();
		sourceIndex = index;
		disposableControls = new ArrayList<Control>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#
	 * createControls(org.eclipse.swt.widgets.Composite,
	 * org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {

		// Set the parent to a FormLayout so that it will resize with the view
		parent.getParent().setLayout(new FormLayout());
		FormData parentData = new FormData();
		parentData.top = new FormAttachment(0);
		parentData.left = new FormAttachment(0);
		parentData.right = new FormAttachment(100);
		parentData.bottom = new FormAttachment(100);
		parent.setLayoutData(parentData);
		parent.setLayout(new FormLayout());

		// Get the system's white color
		Color white = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

		// Create a group for the IRenderElement's INode's data, taking up the
		// left half of the screen.
		Group dataGroup = createGroup(parent, "Data", 0, 0, 50, 100);

		// A composite to hold the nameLabel and nameText, located in the upper
		// left corner
		Composite nameComp = new Composite(dataGroup, SWT.NONE);
		FormData nameData = new FormData();
		nameData.top = new FormAttachment(0, 5);
		nameData.left = new FormAttachment(0, 5);
		nameData.right = new FormAttachment(25);
		nameComp.setLayoutData(nameData);
		nameComp.setLayout(new GridLayout(2, false));
		nameComp.setBackground(white);

		// A label reading "Name:"
		Label nameLabel = new Label(nameComp, SWT.CENTER);
		nameLabel.setText("Name:");
		nameLabel.setBackground(white);

		// A text box in which the INode's name will be displayed and can be
		// edited.
		nameText = new Text(nameComp, SWT.BORDER);
		nameText.setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		nameText.addListener(SWT.DefaultSelection, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// Set the INode's name to the textbox's value
				source.getBase().setName(nameText.getText());
				nameText.selectAll();
			}
		});

		nameText.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent event) {

				// Select all the text in the Text widget
				nameText.selectAll();
			}

			@Override
			public void focusLost(FocusEvent event) {

				// Set the INode's name to the textbox's value
				source.getBase().setName(nameText.getText());
			}
		});

		// A composite to hold the idLabel and idText, located to the right of
		// nameComp
		Composite idComp = new Composite(dataGroup, SWT.NONE);
		FormData idData = new FormData();
		idData.top = new FormAttachment(0, 5);
		idData.left = new FormAttachment(nameComp, 5, 0);
		idData.right = new FormAttachment(35);
		idComp.setLayoutData(idData);
		idComp.setLayout(new GridLayout(2, false));
		idComp.setBackground(white);

		// A label containing the text "ID:"
		Label idLabel = new Label(idComp, SWT.NONE);
		idLabel.setText("ID:");
		idLabel.setBackground(white);

		// A text box in which the INode's ID will be displayed and can be
		// edited.
		idText = new Text(idComp, SWT.BORDER);
		idText.setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		idText.addListener(SWT.DefaultSelection, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// Set the INode's ID to the textbox's value
				source.getBase().setId(Long.valueOf(idText.getText()));
				idText.selectAll();
			}
		});

		idText.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent event) {

				// Select all the text in the Text widget
				idText.selectAll();
			}

			@Override
			public void focusLost(FocusEvent event) {

				// Set the INode's ID to the textbox's value
				source.getBase().setId(Long.valueOf(idText.getText()));
			}
		});

		// A group containing the three coordinates of the shape's center, along
		// with labels for them
		Group centerGroup = createGroup(dataGroup, "Center", null, null, idComp,
				0, null, 70, null, null);
		centerGroup.setLayout(new GridLayout(3, true));

		// Coordinate labels
		Label labelX = new Label(centerGroup, SWT.NONE);
		labelX.setLayoutData(
				new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		labelX.setBackground(white);
		labelX.setText("X");

		Label labelY = new Label(centerGroup, SWT.NONE);
		labelY.setLayoutData(
				new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		labelY.setText("Y");
		labelY.setBackground(white);

		Label labelZ = new Label(centerGroup, SWT.NONE);
		labelZ.setLayoutData(
				new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		labelZ.setText("Z");
		labelZ.setBackground(white);

		// The three spinners that will control translation for the center point
		for (int i = 0; i < 3; i++) {
			translateSpinners[i] = new RealSpinner(centerGroup);
			translateSpinners[i].getControl().setLayoutData(
					new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			translateSpinners[i].getControl().setBackground(white);
			translateSpinners[i].setBounds(-1.0e6, 1.0e6);
		}

		// A group to display the INode's properties, located below the
		// centerGroup
		Group meshPropertiesGroup = createGroup(dataGroup, "Mesh Properties",
				centerGroup, 0, null, 0, null, 100, null, null);
		meshPropertiesGroup.setLayout(new RowLayout());

		// Create property widgets to display any properties the shape has.
		propertyWidgets = new ArrayList<TransformationPropertyWidget>();
		for (int i = 0; i < NUM_PROPERTIES; i++) {

			// Create a child composite to hold the widget
			Composite widgetComp = new Composite(meshPropertiesGroup, SWT.NONE);
			widgetComp.setLayout(new GridLayout(2, false));
			widgetComp.setBackground(white);

			// Create the widget and add it to the list
			propertyWidgets.add(new TransformationPropertyWidget(widgetComp));
		}

		// Empty the old list of property spinners
		propertySpinners.clear();

		// A group that will contain the table of triangle data, located below
		// the meshPropertiesGroup
		Group meshDataGroup = createGroup(dataGroup, "Triangle Mesh Data",
				meshPropertiesGroup, 0, null, 0, null, 100, null, 100);
		meshDataGroup.setLayout(new GridLayout(2, false));

		// A table which will display the nine coordinates that define each
		// triangle, in the order XYZ for each of the three vertices in turn.
		// This table should fit inside the screen, displaying the full table
		// only with a scrollbar and only load those values currently visible
		triangleTable = new TableViewer(meshDataGroup , SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.BORDER);
		GridData tableData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		tableData.heightHint = 90;
		triangleTable.getTable().setLayoutData(tableData);
		triangleTable.getTable().setHeaderVisible(true);
	
		// Create the formatter that will set the coordinates'
		// strings
		DecimalFormat formatter = new DecimalFormat("#.########");
		formatter.setRoundingMode(RoundingMode.DOWN);
		
		//Create the X column for the first vertex
		TableViewerColumn col1X = new TableViewerColumn(triangleTable, SWT.NONE);
		col1X.getColumn().setText("X1");
		col1X.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(0).getX());
			}
		});
		col1X.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(0).getX());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(0).setX(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col1Y = new TableViewerColumn(triangleTable, SWT.NONE);
		col1Y.getColumn().setText("Y1");
		col1Y.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(0).getY());
			}
		});
		col1Y.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(0).getY());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(0).setY(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col1Z = new TableViewerColumn(triangleTable, SWT.NONE);
		col1Z.getColumn().setText("Z1");
		col1Z.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(0).getZ());
			}
		});
		col1Z.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(0).getZ());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(0).setZ(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col2X = new TableViewerColumn(triangleTable, SWT.NONE);
		col2X.getColumn().setText("X2");
		col2X.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(1).getX());
			}
		});
		col2X.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(1).getX());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(1).setX(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col2Y = new TableViewerColumn(triangleTable, SWT.NONE);
		col2Y.getColumn().setText("Y2");
		col2Y.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(1).getY());
			}
		});
		col2Y.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(1).getY());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(1).setY(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col2Z = new TableViewerColumn(triangleTable, SWT.NONE);
		col2Z.getColumn().setText("Z2");
		col2Z.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(1).getZ());
			}
		});
		col2Z.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(1).getZ());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(1).setZ(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col3X = new TableViewerColumn(triangleTable, SWT.NONE);
		col3X.getColumn().setText("X3");
		col3X.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(2).getX());
			}
		});
		col3X.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(2).getX());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(2).setX(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col3Y = new TableViewerColumn(triangleTable, SWT.NONE);
		col3Y.getColumn().setText("Y3");
		col3Y.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(2).getY());
			}
		});
		col3Y.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(2).getY());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(2).setY(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		TableViewerColumn col3Z = new TableViewerColumn(triangleTable, SWT.NONE);
		col3Z.getColumn().setText("Z3");
		col3Z.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object triangle){
				
				//Each cell will display the value of the corresponding vertex's coordinate
				return formatter.format(((Triangle) triangle).getVertices().get(2).getZ());
			}
		});
		col3Z.setEditingSupport(new TriangleTableEditingSupport(triangleTable){

			@Override
			protected Object getValue(Object element) {
				
				//Display the full value of the cell for editing purposes
				return String.valueOf(((Triangle) element).getVertices().get(2).getZ());
			}

			@Override
			protected void setValue(Object element, Object value) {
				
				//Set the value to the coordinate.
				((Triangle) element).getVertices().get(2).setZ(Double.valueOf((String) value));
				triangleTable.update(element, null);
			}
			
		});
		
		//Set the table to lazily load the list of triangles
		triangleTable.setContentProvider(new ILazyContentProvider(){

			//All the triangles belonging to the source shape
			private List<Triangle> triangles;
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
				triangles = (List<Triangle>) newInput;
			}
			
			@Override
			public void updateElement(int index) {
				triangleTable.replace(triangles.get(index), index);
			}
			
		});
		
		//Save the columns to the list
		triangleTableColumns = new ArrayList<TableViewerColumn>();
		triangleTableColumns.add(col1X);
		triangleTableColumns.add(col1Y);
		triangleTableColumns.add(col1Z);
		triangleTableColumns.add(col2X);
		triangleTableColumns.add(col2Y);
		triangleTableColumns.add(col2Z);
		triangleTableColumns.add(col3X);
		triangleTableColumns.add(col3Y);
		triangleTableColumns.add(col3Z);
		
		//Set up the triangle table to load dynamically
		triangleTable.setUseHashlookup(true);
		
		//Set the table to be empty initially
		triangleTable.setInput(new ArrayList<Triangle>());
		triangleTable.setItemCount(0);

		// A display group taking up the right half of the view. This group will
		// display the properties of the IRenderElement, which control how the
		// data is displayed.
		displayGroup = createGroup(parent, "Display", 0, 50, 100, 100);

		// Set the initial shape
		createListeners();
		refresh();
	}

	/**
	 * Create the controls for an option group with data of type
	 * ComboDisplayOptionData. This will consist of a group containing a combo
	 * box.
	 * 
	 * @param displayGroup
	 *            The parent group in which the controls will be created.
	 * @param groupName
	 *            The text to display for the group.
	 * @param options
	 *            The data containing the text and properties settings needed to
	 *            configure this option.
	 * @return A list of all the controls that should be activated when this
	 *         option group is active. It will consist solely of the combo box.
	 */
	private ArrayList<Control> createComboControl(Group displayGroup,
			String groupName, List<IDisplayOptionData> options) {

		// All the controls which need to be activated in this group
		ArrayList<Control> activatedControls = new ArrayList<Control>();

		// Create a group to contain the controls
		Group comboGroup = new Group(displayGroup, SWT.NONE);
		comboGroup.setText(groupName);
		comboGroup.setLayout(new GridLayout());
		comboGroup.setBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		// This group should be disposed when no longer used
		disposableControls.add(comboGroup);

		// A set of all text values for the combo box, combined from those
		// available to all options.
		Set<String> textOptions = new HashSet<String>();
		for (IDisplayOptionData option : options) {
			textOptions.addAll(((ComboDisplayOptionData) option)
					.getTextToPropertyValuesMap().keySet());
		}

		// Remove the special "default" text string, which should not be
		// displayed to the user
		textOptions.remove("default");

		// The map from combo options to maps between property names and
		// property values
		final HashMap<String, HashMap<String, Object>> propertyValueMap = new HashMap<String, HashMap<String, Object>>();

		// Populate the map for each of the text strings in the combo box
		for (String displayText : textOptions) {

			// A map of property names to the values they should be set to when
			// this selection is made from the combo box
			HashMap<String, Object> propertyValues = new HashMap<String, Object>();

			// Populate the map with values from each DisplayOption.
			for (IDisplayOptionData option : options) {

				// Get the values map for the current combo box selection
				Map<String, Object> value = ((ComboDisplayOptionData) option)
						.getTextToPropertyValuesMap().get(displayText);

				// if the map was not found, then this DisplayOption did not
				// have values set for this combo selection. Get the default
				// values for it instead.
				if (value == null) {
					value = ((ComboDisplayOptionData) option)
							.getTextToPropertyValuesMap().get("default");
				}

				// Place the property values into the map under the property's
				// name
				for (String propertyName : value.keySet()) {
					propertyValues.put(propertyName, value.get(propertyName));
				}
			}

			// Put the map of all property values in the map, keyed on the combo
			// box selection
			propertyValueMap.put(displayText, propertyValues);
		}

		// Create a combo box containing the list of options
		Combo combo = new Combo(comboGroup, SWT.READ_ONLY);
		combo.setLayoutData(
				new GridData(SWT.NONE, SWT.NONE, false, false, 1, 1));
		combo.setItems(textOptions.toArray(new String[textOptions.size()]));
		activatedControls.add(combo);

		// Find the current selection for the combo box by finding the source's
		// current proeprties
		for (String textOption : textOptions) {

			// Whether the current combo box selection matched the properties
			boolean match = true;

			// Get the property settings for this selection
			Map<String, Object> textOptionMap = propertyValueMap
					.get(textOption);

			// Check each of the properties in the map. If any are different,
			// this is not a match
			for (String propertyName : textOptionMap.keySet()) {
				if (!textOptionMap.get(propertyName)
						.equals(source.getProperty(propertyName))) {
					match = false;
					break;
				}
			}

			// If a matching set of properties was found, set the combo to that
			// selection
			if (match) {
				combo.setText(textOption);
				break;
			}
		}

		combo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// Get the user selected value
				String selection = combo.getText();

				// Get the property map for the user selection
				Map<String, Object> selectionMap = propertyValueMap
						.get(selection);

				// Set each property to the value associated with this selection
				for (String property : selectionMap.keySet()) {
					source.setProperty(property, selectionMap.get(property));
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

				// Get the user selected value
				String selection = combo.getText();

				// Get the property map for the user selection
				Map<String, Object> selectionMap = propertyValueMap
						.get(selection);

				// Set each property to the value associated with this selection
				for (String property : selectionMap.keySet()) {
					source.setProperty(property, selectionMap.get(property));
				}
			}

		});

		return activatedControls;
	}

	/**
	 * Create the controls for an option group with data of type
	 * DoublTextDisplayOptionData. This will consist of a group containing a
	 * series of labels and text boxes, one for each property, which accept
	 * double values.
	 * 
	 * @param displayGroup
	 *            The parent group in which the controls will be created.
	 * @param groupName
	 *            The text to display for the group.
	 * @param options
	 *            The data containing the text and properties settings needed to
	 *            configure this option.
	 * @return A list of all the controls that should be activated when this
	 *         option group is active. It will consist of the editable Text
	 *         boxes.
	 */
	private List<Control> createDoubleTextControl(Group displayGroup,
			String groupName, List<IDisplayOptionData> options) {

		// Create a group for the properties
		Group textGroup = new Group(displayGroup, SWT.NONE);
		textGroup.setText(groupName);
		textGroup.setLayout(new RowLayout());
		textGroup.setBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		// The group needs to be disposed of when no longer in user
		disposableControls.add(textGroup);

		// The list of controls that need to be activated
		ArrayList<Control> activatedControls = new ArrayList<Control>();

		// The set of property names
		Set<String> propertyNames = new HashSet<String>();

		// The bounds for each property, if any
		HashMap<String, Double> minValues = new HashMap<String, Double>();
		HashMap<String, Double> maxValues = new HashMap<String, Double>();

		// Get all the properties required by all the DisplayOptions
		for (IDisplayOptionData option : options) {

			// Get the names of all properties in this data
			Set<String> properties = ((DoubleTextDisplayOptionData) option)
					.getPropertyToValueMap().keySet();
			propertyNames.addAll(properties);

			// Get the minimum and maximum values from the data
			Map<String, Double> mins = ((DoubleTextDisplayOptionData) option)
					.getPropertyMinValues();
			Map<String, Double> maxes = ((DoubleTextDisplayOptionData) option)
					.getPropertyMaxValues();

			// For each property, set the minimum and maximum values
			for (String property : properties) {
				Double min = mins.get(property);
				if (min != null) {
					minValues.put(property, min);
				}

				Double max = maxes.get(property);
				if (max != null) {
					maxValues.put(property, max);
				}
			}
		}

		// Add controls for each property
		for (String property : propertyNames) {

			// Create a child composite to hold the widget
			Composite widgetComp = new Composite(textGroup, SWT.NONE);
			widgetComp.setLayout(new GridLayout(2, false));
			widgetComp.setLayoutData(new RowData());
			widgetComp.setBackground(
					Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

			// Create a widget that will display the given property
			TransformationPropertyWidget widget = new TransformationPropertyWidget(
					widgetComp);
			widget.setDecoratorProperty(true);
			widget.setProperty(source.getBase(), property,
					(double) source.getProperty(property));
			activatedControls.add(widget.getTextControl());

			// Set the bounds on the widget, if specified
			if (minValues.get(property) != null
					|| maxValues.get(property) != null) {
				widget.setBounds(minValues.get(property),
						maxValues.get(property));
			}

		}

		return activatedControls;
	}

	/**
	 * Create a new Group with a FormLayoutData.
	 * 
	 * @param parent
	 *            The parent Composite in which the Group will be made.
	 * @param name
	 *            The name to display as the Group's header.
	 * @param top
	 *            The offset from the top of the parent. If null, no attachment
	 *            will be created for this side.
	 * @param left
	 *            The offset from the left side of the parent. If null, no
	 *            attachment will be created for this side.
	 * @param right
	 *            The offset from the right side of the parent. If null, no
	 *            attachment will be created for this side.
	 * @param bottom
	 *            The offset from the bottom of the parent. If null, no
	 *            attachment will be created for this side.
	 * @return A Group contained in parent, named name, with a FormData
	 *         containing the given offsets from parent's sides.
	 */
	private Group createGroup(Composite parent, String name, Integer top,
			Integer left, Integer right, Integer bottom) {
		return createGroup(parent, name, null, top, null, left, null, right,
				null, bottom);
	}

	/**
	 * Create a new Group with a FormLayoutData.
	 * 
	 * @param parent
	 *            The parent Composite in which the Group will be made.
	 * @param name
	 *            The name to display as the Group's header.
	 * @param topNeighbor
	 *            The Control defining the top edge of the Group. If null, the
	 *            offset will apply from parent.
	 * @param topOffset
	 *            The offset from the top of the parent. If null, no attachment
	 *            will be created for this side.
	 * @param leftNeighbor
	 *            The Control defining the left edge of the Group. If null, the
	 *            offset will apply from parent.
	 * @param leftOffset
	 *            The offset from the left side of the parent. If null, no
	 *            attachment will be created for this side.
	 * @param rightNeighbor
	 *            The Control defining the right edge of the Group. If null, the
	 *            offset will apply from parent.
	 * @param rightOffset
	 *            The offset from the right side of the parent. If null, no
	 *            attachment will be created for this side.
	 * @param bottomNeighbor
	 *            The Control defining the bottom edge of the Group. If null,
	 *            the offset will apply from parent.
	 * @param bottomOffset
	 *            The offset from the bottom of the parent. If null, no
	 *            attachment will be created for this side.
	 * @return A Group contained in parent, named name, with its boundaries set
	 *         according to the rules for given by the neighbor and offset
	 *         parameters.
	 */
	private Group createGroup(Composite parent, String name,
			Control topNeighbor, Integer topOffset, Control leftNeighbor,
			Integer leftOffset, Control rightNeighbor, Integer rightOffset,
			Control bottomNeighbor, Integer bottomOffset) {

		// The group to return. Set it up.
		Group group = new Group(parent, SWT.NONE);
		group.setText(name);
		group.setBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		group.setLayout(new FormLayout());
		FormData data = new FormData();

		// For each of the sides, if the offset is null, ignore that side. If
		// not, set the group to have an attachment to either the parent, if the
		// neighbor argument is null, or the neighbor, with the given offset.
		if (topOffset != null) {
			data.top = topNeighbor == null ? new FormAttachment(topOffset)
					: new FormAttachment(topNeighbor, 5, topOffset);
		}
		if (leftOffset != null) {
			data.left = leftNeighbor == null ? new FormAttachment(leftOffset)
					: new FormAttachment(leftNeighbor, 5, leftOffset);
		}
		if (rightOffset != null) {
			data.right = rightNeighbor == null ? new FormAttachment(rightOffset)
					: new FormAttachment(rightNeighbor, 5, rightOffset);
		}
		if (bottomOffset != null) {
			data.bottom = bottomNeighbor == null
					? new FormAttachment(bottomOffset)
					: new FormAttachment(bottomNeighbor, 5, bottomOffset);
		}

		// Set the constructed layout data
		group.setLayoutData(data);
		return group;
	}

	/**
	 * Create the controls for an option group with data of type
	 * DoublTextDisplayOptionData. This will consist of a group containing a
	 * series of labels and text boxes, one for each property, which accept
	 * integer values.
	 * 
	 * @param displayGroup
	 *            The parent group in which the controls will be created.
	 * @param groupName
	 *            The text to display for the group.
	 * @param options
	 *            The data containing the text and properties settings needed to
	 *            configure this option.
	 * @return A list of all the controls that should be activated when this
	 *         option group is active. It will consist of the editable Text
	 *         boxes.
	 */
	private List<Control> createIntegerTextControl(Group displayGroup,
			String groupName, List<IDisplayOptionData> options) {

		// Create a group for the properties
		Group textGroup = new Group(displayGroup, SWT.NONE);
		textGroup.setText(groupName);
		textGroup.setLayout(new RowLayout());
		textGroup.setBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		// The group needs to be disposed of when no longer in use
		disposableControls.add(textGroup);

		// The controls which needed to be activated to activate this option
		// group
		ArrayList<Control> activatedControls = new ArrayList<Control>();

		// The set of all property names for this option group
		Set<String> propertyNames = new HashSet<String>();

		// The bounds for each property, if any
		HashMap<String, Integer> minValues = new HashMap<String, Integer>();
		HashMap<String, Integer> maxValues = new HashMap<String, Integer>();

		// Add each DisplayOption's properties to the list
		// Get all the properties required by all the DisplayOptions
		for (IDisplayOptionData option : options) {

			// Get the names of all properties in this data
			Set<String> properties = ((IntegerTextDisplayOptionData) option)
					.getPropertyToValueMap().keySet();
			propertyNames.addAll(properties);

			// Get the minimum and maximum values from the data
			Map<String, Integer> mins = ((IntegerTextDisplayOptionData) option)
					.getPropertyMinValues();
			Map<String, Integer> maxes = ((IntegerTextDisplayOptionData) option)
					.getPropertyMaxValues();

			// For each property, set the minimum and maximum values
			for (String property : properties) {
				Integer min = mins.get(property);
				if (min != null) {
					minValues.put(property, min);
				}

				Integer max = maxes.get(property);
				if (max != null) {
					maxValues.put(property, max);
				}
			}
		}

		// Create a widget for each property
		for (String property : propertyNames) {

			// Create a child composite to hold the widget
			Composite widgetComp = new Composite(textGroup, SWT.NONE);
			widgetComp.setLayout(new GridLayout(2, false));
			widgetComp.setLayoutData(new RowData());
			widgetComp.setBackground(
					Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

			// Create a widget to allow the property to be edited.
			TransformationPropertyWidget widget = new TransformationPropertyWidget(
					widgetComp);
			widget.setDecoratorProperty(true);
			widget.setProperty(source.getBase(), property,
					(int) source.getProperty(property));
			widget.getTextControl().setLayoutData(new GridData(30, 15));

			activatedControls.add(widget.getTextControl());

			// Set the bounds on the widget, if specified
			if (minValues.get(property) != null
					|| maxValues.get(property) != null) {
				widget.setBounds(minValues.get(property),
						maxValues.get(property));
			}
		}

		return activatedControls;
	}

	/**
	 * Create the listeners for each control on the view.
	 */
	private void createListeners() {

		// Create anonymous listener

		ISpinnerListener listener = new ISpinnerListener() {

			@Override
			public void update(ISpinner realSpinner) {

				// Handle a null source
				if (source == null) {
					return;
				}

				// Get the center point from the spinners
				Vertex center = GeometryFactory.eINSTANCE.createVertex();
				center.setX(translateSpinners[0].getValue());
				center.setY(translateSpinners[1].getValue());
				center.setZ(translateSpinners[2].getValue());

				// Reset the shape's center
				if (!source.getBase().getCenter().equals(center)) {
					source.getBase().setCenter(center);
				}
			}
		};

		// Add the listener to each spinner
		for (ISpinner spinner : translateSpinners) {
			spinner.listen(listener);
		}

		// Create a spinner listener that will update the shape's properties
		ISpinnerListener propertyListener = new ISpinnerListener() {

			@Override
			public void update(ISpinner realSpinner) {

				// Get the spinner's name and value
				String name = realSpinner.getName();
				double value = (double) realSpinner.getValue();

				// If the value in the spinner has been changed, set the new
				// value to the shape
				if (value != source.getBase().getProperty(name)) {
					source.setProperty(realSpinner.getName(),
							realSpinner.getValue());
				}
			}
		};

		// Add the listener to each of the property spinners
		for (ISpinner spinner : propertySpinners) {
			spinner.listen(propertyListener);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {

		// Only populate the section if the source is valid
		if (source != null) {

			for (Control control : disposableControls) {
				control.dispose();
			}
			disposableControls.clear();

			// Set the name and ID text boxs' values.
			nameText.setText(source.getBase().getName());
			idText.setText(String.valueOf(source.getBase().getId()));

			// Set the spinner values

			Vertex center = source.getBase().getCenter();
			double[] translations = new double[] { center.getX(), center.getY(),
					center.getZ() };
			for (int i = 0; i < 3; i++) {
				translateSpinners[i].setValue(translations[i]);
			}

			// Set the properties
			List<String> properties = source.getBase().getPropertyNames();

			// Pad the list to the correct number of properties
			while (properties.size() < NUM_PROPERTIES) {
				properties.add(null);
			}

			// Set the property widgets for the new shape's properties
			for (int i = 0; i < NUM_PROPERTIES; i++) {

				// Get the property name
				String property = properties.get(i);

				double value = (property != null)
						? source.getBase().getProperty(property) : 0d;

				// Set the property widget to display this property
				propertyWidgets.get(i).setProperty(source.getBase(), property,
						value);

			}

			// The map from option groups to the IDisplayOptionData objects
			// publishing their configurations to that group
			HashMap<String, ArrayList<IDisplayOptionData>> optionGroupMap = new HashMap<String, ArrayList<IDisplayOptionData>>();

			// All options for the present object
			List<DisplayOption> optionList = ((RenderObject) source)
					.getDisplayOptions();

			// Add each option to the correct group
			for (DisplayOption option : optionList) {
				String type = option.getOptionGroup();

				// Get the current list of IDisplayOptionData for this group, or
				// a new one if none exists.
				ArrayList<IDisplayOptionData> options;

				if (!optionGroupMap.containsKey(type)) {
					options = new ArrayList<IDisplayOptionData>();
				} else {
					options = optionGroupMap.get(type);
				}

				// Add the new option to the map
				options.add(option.getDisplayOptionData());
				optionGroupMap.put(type, options);
			}

			displayGroup.setLayout(new RowLayout());

			// Add the controls for each group
			for (String group : optionGroupMap.keySet()) {

				// The list of all IDisplayOptionData for this option group
				final List<IDisplayOptionData> groupData = optionGroupMap
						.get(group);

				// Add a check box to activate/deactivate the option.
				Button activator = new Button(displayGroup, SWT.CHECK);
				disposableControls.add(activator);

				// Set the checkbox to the data's current active state
				boolean active = groupData.get(0).getDisplayOption().isActive();
				activator.setSelection(active);

				// The Controls that will be turned on or off by activator
				List<Control> activatedControls = null;

				// The type of controls made
				DisplayOptionType type = groupData.get(0)
						.getDisplayOptionType();

				// TODO Replace this switch
				// Create an appropriate group for this type of data
				if (type == DisplayOptionType.COMBO) {
					activatedControls = createComboControl(displayGroup, group,
							optionGroupMap.get(group));
				} else if (type == DisplayOptionType.DOUBLE_TEXT) {
					activatedControls = createDoubleTextControl(displayGroup,
							group, optionGroupMap.get(group));
				} else if (type == DisplayOptionType.INTEGER_TEXT) {
					activatedControls = createIntegerTextControl(displayGroup,
							group, optionGroupMap.get(group));
				}

				// Set up the active controls, if any
				if (activatedControls != null) {

					// Disable the controls if the option is deactivated
					for (Control child : activatedControls) {
						child.setEnabled(active);
					}

					// Get a final reference to the group's controls
					final List<Control> finalControls = activatedControls;

					activator.addSelectionListener(new SelectionListener() {

						@Override
						public void widgetSelected(SelectionEvent e) {

							// When the activator is selected, disable/enable
							// the controls and the options
							boolean active = activator.getSelection();

							for (IDisplayOptionData data : groupData) {
								data.getDisplayOption().setActive(active);
							}

							for (Control child : finalControls) {
								child.setEnabled(active);
							}
						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {

							// When the activator is selected, disable/enable
							// the controls and the options
							boolean active = activator.getSelection();

							for (IDisplayOptionData data : groupData) {
								data.getDisplayOption().setActive(active);
							}

							for (Control child : finalControls) {
								child.setEnabled(active);
							}
						}
					});
				}
			}

			// Update the triangle mesh data
			List<Triangle> triangles = source.getBase().getTriangles();
			triangleTable.setInput(triangles);
			triangleTable.setItemCount(triangles.size());

			//Resize the columns to fit their new contents
			for(TableViewerColumn column : triangleTableColumns){
				column.getColumn().pack();
			}
			
		}

		// Set the enabled state of the spinners, depending on whether the
		// shape parameter is null
		// Enable each spinner
		for (ISpinner translateSpinner : translateSpinners) {
			translateSpinner.getControl().setEnabled(source != null);
		}
		for (ISpinner spinner : propertySpinners) {
			spinner.getControl().setEnabled(source != null);
		}

		// Layout the group now that it is fully populated
		displayGroup.layout();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#setInput(
	 * org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);

		// Check that the selection is valid before attempting to set it
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {

			// The list of all elements in the tree starting with the selected
			// node
			ArrayList<INode> elements = new ArrayList<INode>();

			// Get the selected INode
			elements.add(((IRenderElement) ((IStructuredSelection) selection)
					.getFirstElement()).getBase());

			// Get the holder that associates IRenderElements with INodes
			IRenderElementHolder holder = ((ShapeTreeView) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.findView(ShapeTreeView.ID)).getHolder();

			// Find the index in the flattenned tree and set it as the source.
			for (int i = 0; i < elements.size(); i++) {
				if (sourceIndex < elements.size()) {
					source = holder.getRender(elements.get(sourceIndex));
					refresh();
					return;
				}

				// Add more elements if the tree does not let contain the index
				elements.addAll(elements.get(i).getNodes());
			}

			refresh();
		}

		return;
	}

	/**
	 * An editing support for use with the triangle table in this section. 
	 */
	private abstract class TriangleTableEditingSupport extends EditingSupport{

		//The editor to be displayed when the cell is selected
		private CellEditor editor;
		
		public TriangleTableEditingSupport(ColumnViewer viewer) {
			super(viewer);
			 editor = new TextCellEditor(triangleTable.getTable());
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.EditingSupport#getCellEditor(java.lang.Object)
		 */
		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
		 */
		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
		
	}
}
