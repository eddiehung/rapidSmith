/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.design.explorer;

import java.util.ArrayList;

import com.trolltech.qt.core.QAbstractItemModel;
import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QRegExp;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QGridLayout;
import com.trolltech.qt.gui.QItemSelectionModel;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QSortFilterProxyModel;
import com.trolltech.qt.gui.QStandardItem;
import com.trolltech.qt.gui.QStandardItemModel;
import com.trolltech.qt.gui.QTreeView;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QAbstractItemView.EditTrigger;
import com.trolltech.qt.gui.QAbstractItemView.ScrollHint;

import edu.byu.ece.rapidSmith.design.Attribute;
import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.design.Module;
import edu.byu.ece.rapidSmith.design.ModuleInstance;
import edu.byu.ece.rapidSmith.design.Net;

/**
 * This class creates the various tabs of the DesignExplorer.
 * @author Chris Lavin
 */
public class FilterWindow extends QWidget{
	/** The associated design explorer for this tab */
	private DesignExplorer explorer;
	/** There are 4 types of tabs, this is the type of this tab instance */
	private FilterType type; 
	/** The view associated with this window */
	private QTreeView view;
	/** The listing model for the data */
	private QStandardItemModel model;
	/** A sorting and filter model for the data */
	private MySortFilterProxyModel proxyModel;
	/** A check box to flag case sensitivity in the filtering */
	private QCheckBox filterCaseSensitivityCheckBox;
	/** The selection box of how to filter */
	private QComboBox filterSyntaxComboBox;
	/** The layout object for the window */
	private QGridLayout proxyLayout;
	/** The editable text box for the user to enter a filter for the data */
	private QLineEdit filterPatternLineEdit;
	/** The label for the filter pattern edit box */
	private QLabel filterPatternLabel; 
	/** The main selection model */
	private QItemSelectionModel selectionModel;
	/** Keeps a pointing finger cursor handy */
	private QCursor pointingFinger = new QCursor(CursorShape.PointingHandCursor);
	/** Keeps an arrow cursor handy */
	private QCursor arrow = new QCursor(CursorShape.ArrowCursor);
	/** Some tabs have other models (Instances have attributes) */
	private QStandardItemModel[] subModels;
	/** The associated views with the other subModels */
	private QTreeView[] subViews;
	/** Helps differentiate tab windows */
	enum FilterType {
		NETS,
		INSTANCES,
		MODULES,
		MODULE_INSTANCES
	}
	
	public QTreeView createNewView(boolean signals){
		QTreeView newView = new QTreeView();
		newView.setMouseTracking(true);
		newView.setRootIsDecorated(false);
		newView.setAlternatingRowColors(true);
		if(signals){
			newView.clicked.connect(this, "singleClick(QModelIndex)");
			newView.entered.connect(this, "onHover(QModelIndex)");
		}
		newView.setEditTriggers(EditTrigger.NoEditTriggers);
		return newView;
	}
	
	public FilterWindow(QWidget parent, FilterType type){
		super(parent);
		this.type = type;
		this.explorer = (DesignExplorer) parent;
		
		view = createNewView(true);
        
		loadCurrentDesignData();
		
        filterPatternLineEdit = new QLineEdit();
        filterPatternLabel = new QLabel(tr("&Filter pattern:"));
        filterPatternLabel.setBuddy(filterPatternLineEdit);

        filterSyntaxComboBox = new QComboBox();
        filterSyntaxComboBox.addItem(tr("Regular expression"),
                                     QRegExp.PatternSyntax.RegExp);
        filterSyntaxComboBox.addItem(tr("Wildcard"),
                                     QRegExp.PatternSyntax.Wildcard);
        filterSyntaxComboBox.addItem(tr("Fixed string"),
                                     QRegExp.PatternSyntax.FixedString);
        
        filterCaseSensitivityCheckBox = new QCheckBox(tr("Case sensitive"));
        filterCaseSensitivityCheckBox.setChecked(true);
        
        filterPatternLineEdit.textChanged.connect(this, "textFilterChanged()");
        filterSyntaxComboBox.currentIndexChanged.connect(this, "textFilterChanged()");
        filterCaseSensitivityCheckBox.toggled.connect(this, "textFilterChanged()");
        
        proxyLayout = new QGridLayout();
        proxyLayout.addWidget(view, 0, 0, 1, 4);
        proxyLayout.addWidget(filterPatternLabel, 1, 0);
        proxyLayout.addWidget(filterPatternLineEdit, 1, 1);
        proxyLayout.addWidget(filterSyntaxComboBox, 1, 2);
        proxyLayout.addWidget(filterCaseSensitivityCheckBox, 1, 3);
		
        switch(type){
        	case INSTANCES:
        		subViews = new QTreeView[1];
        		subViews[0] = createNewView(false);
        		proxyLayout.addWidget(new QLabel("Attributes"));
        		proxyLayout.addWidget(subViews[0], 3, 0, 1, 3);
        		subModels = new QStandardItemModel[1];
        		subModels[0] = new QStandardItemModel(0, 3, this);
        		subModels[0].setHeaderData(0, Qt.Orientation.Horizontal, tr("Physical Name"));
        		subModels[0].setHeaderData(1, Qt.Orientation.Horizontal, tr("Logical Name"));
        		subModels[0].setHeaderData(2, Qt.Orientation.Horizontal, tr("Value"));
        		subViews[0].setModel(subModels[0]);
        		break;
        	case NETS:
        		break;
        	case MODULES:
        		break;        		
        	case MODULE_INSTANCES:
        		break;        		
        }
        
        setLayout(proxyLayout);
        
        textFilterChanged();
	}
	
	public void loadCurrentDesignData(){
		QFont hyperlink = new QFont();
		hyperlink.setUnderline(true);
		QBrush blue = new QBrush(QColor.blue);
		switch(this.type) {
			case NETS:
				model = new QStandardItemModel(0, 7, this);
	            model.setHeaderData(0, Qt.Orientation.Horizontal, tr("Name"));
	            model.setHeaderData(1, Qt.Orientation.Horizontal, tr("Type"));
	            model.setHeaderData(2, Qt.Orientation.Horizontal, tr("Source Instance"));
	            model.setHeaderData(3, Qt.Orientation.Horizontal, tr("Fanout"));
	            model.setHeaderData(4, Qt.Orientation.Horizontal, tr("PIP Count"));
	            model.setHeaderData(5, Qt.Orientation.Horizontal, tr("Module Instance Name"));
	            model.setHeaderData(6, Qt.Orientation.Horizontal, tr("Module Name"));
	            if(explorer.currDesign != null){	        	
		        	for(Net net : explorer.currDesign.getNets()){
			        	if(net.getPins().size() > 0){
			        		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
			        		
			        		// Net Name
			        		items.add(new QStandardItem(net.getName()));
			        		
			        		// Net Type
			        		items.add(new QStandardItem(net.getType().toString()));
			        		
			        		// Net Source Instance
			        		QStandardItem item = new QStandardItem(net.getSource() == null ? null : net.getSource().getInstanceName());
			        		item.setFont(hyperlink);
			        		item.setForeground(blue);
			        		items.add(item);
			        		
			        		// Net Pin Count
			        		items.add(new QStandardItem(String.format("%3d", net.getPins().size()-1)));
			        		
			        		// Net PIP Count
			        		items.add(new QStandardItem(String.format("%5d", net.getPIPs().size())));

			        		item = new QStandardItem(net.getModuleInstance()==null ? null : net.getModuleInstance().getName());
			        		item.setFont(hyperlink);
			        		item.setForeground(blue);
			        		items.add(item);
			        		
			        		item = new QStandardItem(net.getModuleTemplate()==null ? null : net.getModuleTemplate().getName());
			        		item.setFont(hyperlink);
			        		item.setForeground(blue);
			        		items.add(item);
			        		
			        		model.appendRow(items);			        		
			        	}
			        }
	            }
	            break;
			case INSTANCES:
				model = new QStandardItemModel(0, 6, this);
	            model.setHeaderData(0, Qt.Orientation.Horizontal, tr("Name"));
	            model.setHeaderData(1, Qt.Orientation.Horizontal, tr("Type"));
	            model.setHeaderData(2, Qt.Orientation.Horizontal, tr("Primitive Site"));
	            model.setHeaderData(3, Qt.Orientation.Horizontal, tr("Primitive Tile"));
	            model.setHeaderData(4, Qt.Orientation.Horizontal, tr("Module Instance Name"));
	            model.setHeaderData(5, Qt.Orientation.Horizontal, tr("Module Name"));
	            if(explorer.currDesign != null){
	            	for(Instance instance : explorer.currDesign.getInstances()){
	            		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
	            		
	            		items.add(new QStandardItem(instance.getName()));
	            		items.add(new QStandardItem(instance.getType().toString()));
	            		
	            		QStandardItem item = new QStandardItem(instance.getPrimitiveSiteName());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);
		        		
		        		item = new QStandardItem(instance.getPrimitiveSite()==null ? null : instance.getPrimitiveSite().getTile().toString());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);
	            		
		        		item = new QStandardItem(instance.getModuleInstanceName());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);
		        		
		        		item = new QStandardItem(instance.getModuleTemplate()==null ? null : instance.getModuleTemplate().getName());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);
		        				
		        		model.appendRow(items);
	                }
	            }
	            break;
			case MODULES:
				model = new QStandardItemModel(0, 6, this);
	            model.setHeaderData(0, Qt.Orientation.Horizontal, tr("Name"));
	            model.setHeaderData(1, Qt.Orientation.Horizontal, tr("Anchor Name"));
	            model.setHeaderData(2, Qt.Orientation.Horizontal, tr("Anchor Site"));
	            model.setHeaderData(3, Qt.Orientation.Horizontal, tr("Instance Count"));
	            model.setHeaderData(4, Qt.Orientation.Horizontal, tr("Net Count"));
	            model.setHeaderData(5, Qt.Orientation.Horizontal, tr("Port Count"));
	            if(explorer.currDesign != null){
		        	for(Module module : explorer.currDesign.getModules()){
	            		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
	            		
	            		items.add(new QStandardItem(module.getName()));
	            		
	            		items.add(new QStandardItem(module.getAnchor().getName()));
	            		
	            		QStandardItem item = new QStandardItem(module.getAnchor().getPrimitiveSiteName());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);
		        		
		        		items.add(new QStandardItem(String.format("%5d", module.getInstances().size())));
            		
		        		items.add(new QStandardItem(String.format("%5d", module.getNets().size())));
		        		
		        		items.add(new QStandardItem(String.format("%5d", module.getPorts().size())));
		        				
		        		model.appendRow(items);
			        }
	            }
	            break;
			case MODULE_INSTANCES:
				model = new QStandardItemModel(0, 4, this);
	            model.setHeaderData(0, Qt.Orientation.Horizontal, tr("Name"));
	            model.setHeaderData(1, Qt.Orientation.Horizontal, tr("Anchor Name"));
	            model.setHeaderData(2, Qt.Orientation.Horizontal, tr("Anchor Site"));
	            model.setHeaderData(3, Qt.Orientation.Horizontal, tr("Module Template"));
	            if(explorer.currDesign != null){
		        	for(ModuleInstance moduleInstance : explorer.currDesign.getModuleInstances()){
		        		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
	            		
	            		items.add(new QStandardItem(moduleInstance.getName()));
	            		
	            		items.add(new QStandardItem(moduleInstance.getAnchor().getName()));
	            		
	            		QStandardItem item = new QStandardItem(moduleInstance.getAnchor().getPrimitiveSiteName());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);

	            		item = new QStandardItem(moduleInstance.getModule().getName());
		        		item.setFont(hyperlink);
		        		item.setForeground(blue);
		        		items.add(item);

		        		model.appendRow(items);
			        }
	            }
	            break;
		}

		proxyModel = new MySortFilterProxyModel(this);
        proxyModel.setSourceModel(model);
        proxyModel.setDynamicSortFilter(true);
        this.selectionModel = new QItemSelectionModel(proxyModel);
        
        view.setModel(proxyModel);
        view.setSelectionModel(selectionModel);
        
        view.setSortingEnabled(true);
        view.sortByColumn(0, Qt.SortOrder.AscendingOrder);
        
	}
	
	protected void singleClick(QModelIndex index){
		switch(type){
			case NETS:
				if(index.column() == 2){
					explorer.tabs.setCurrentWidget(explorer.instanceWindow);
					for(int i = 0; i < explorer.instanceWindow.proxyModel.rowCount(); i++){
						if(explorer.instanceWindow.proxyModel.data(i, 0).toString().equals(proxyModel.data(index).toString())){
							QModelIndex dstIndex = explorer.instanceWindow.proxyModel.index(i, 0);
							explorer.instanceWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
							explorer.instanceWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
						}
					}
				}
				break;
			case INSTANCES:
				// populate attributes
				System.out.println(subModels[0].removeRows(0, subModels[0].rowCount()));
				Instance instance = explorer.currDesign.getInstance(proxyModel.data(index.row(),0).toString());
				for(Attribute attribute : instance.getAttributes()){
					ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
					items.add(new QStandardItem(attribute.getPhysicalName()));
					items.add(new QStandardItem(attribute.getLogicalName()));
					items.add(new QStandardItem(attribute.getValue()));
					subModels[0].appendRow(items);
				}
				subViews[0].setSortingEnabled(true);
				subViews[0].sortByColumn(0, Qt.SortOrder.AscendingOrder);
				
				// check for hyperlinks
				if(index.column() == 2){
					explorer.tabs.setCurrentWidget(explorer.tileWindow);
					explorer.tileWindow.moveToTile(explorer.device.getPrimitiveSite(proxyModel.data(index).toString()).getTile().getName());
				}
				else if(index.column() == 3){
					explorer.tabs.setCurrentWidget(explorer.tileWindow);
					explorer.tileWindow.moveToTile(proxyModel.data(index).toString());
				}
				else if(index.column() == 4){
					explorer.tabs.setCurrentWidget(explorer.moduleInstanceWindow);
					for(int i = 0; i < explorer.moduleInstanceWindow.proxyModel.rowCount(); i++){
						if(explorer.moduleInstanceWindow.proxyModel.data(i, 0).toString().equals(proxyModel.data(index).toString())){
							QModelIndex dstIndex = explorer.moduleInstanceWindow.proxyModel.index(i, 0);
							explorer.moduleInstanceWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
							explorer.moduleInstanceWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
						}
					}
				}
				else if(index.column() == 5){
					explorer.tabs.setCurrentWidget(explorer.moduleWindow);
					for(int i = 0; i < explorer.moduleWindow.proxyModel.rowCount(); i++){
						if(explorer.moduleWindow.proxyModel.data(i, 0).toString().equals(proxyModel.data(index).toString())){
							QModelIndex dstIndex = explorer.moduleWindow.proxyModel.index(i, 0);
							explorer.moduleWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
							explorer.moduleWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
						}
					}
				}
				break;
			case MODULES:
				if(index.column() == 2){
					explorer.tabs.setCurrentWidget(explorer.tileWindow);
					explorer.tileWindow.moveToTile(explorer.device.getPrimitiveSite(proxyModel.data(index).toString()).getTile().getName());
				}
				break;
			case MODULE_INSTANCES:
				if(index.column() == 2){
					explorer.tabs.setCurrentWidget(explorer.tileWindow);
					explorer.tileWindow.moveToTile(explorer.device.getPrimitiveSite(proxyModel.data(index).toString()).getTile().getName());
				}
				else if(index.column() == 3){
					explorer.tabs.setCurrentWidget(explorer.moduleWindow);
					for(int i = 0; i < explorer.moduleWindow.proxyModel.rowCount(); i++){
						if(explorer.moduleWindow.proxyModel.data(i, 0).toString().equals(proxyModel.data(index).toString())){
							QModelIndex dstIndex = explorer.moduleWindow.proxyModel.index(i, 0);
							explorer.moduleWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
							explorer.moduleWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
						}
					}
				}
				break;
		}
		
	}
	
	protected void onHover(QModelIndex index){
		switch(type){
			case NETS:
				if(index.column() == 2 || index.column() > 4) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case INSTANCES:
				if(index.column() > 1) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case MODULES:
				if(index.column() == 2) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case MODULE_INSTANCES:
				if(index.column() > 1) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
		}
	}
	
	private void textFilterChanged(){
        QRegExp.PatternSyntax syntax;
        int index = filterSyntaxComboBox.currentIndex();
        syntax = (QRegExp.PatternSyntax) filterSyntaxComboBox.itemData(index);

        Qt.CaseSensitivity caseSensitivity;
        if (filterCaseSensitivityCheckBox.isChecked())
            caseSensitivity = Qt.CaseSensitivity.CaseSensitive;
        else
            caseSensitivity = Qt.CaseSensitivity.CaseInsensitive;

        QRegExp regExp = new QRegExp(filterPatternLineEdit.text(),
                                     caseSensitivity, syntax);
        proxyModel.setFilterRegExp(regExp);
    }
	
    private class MySortFilterProxyModel extends QSortFilterProxyModel {

        private MySortFilterProxyModel(QObject parent) {
            super(parent);
        }

        @Override
        protected boolean filterAcceptsRow(int sourceRow, QModelIndex sourceParent){


            QRegExp filter = filterRegExp();
            QAbstractItemModel model = sourceModel();
            boolean matchFound = false;

            for(int i=0; i < model.columnCount(); i++){
            	Object data = model.data(sourceModel().index(sourceRow, i, sourceParent));
            	matchFound |= data != null && filter.indexIn(data.toString()) != -1;
            }

            return matchFound;
        }

        @Override
        protected boolean lessThan(QModelIndex left, QModelIndex right) {

            boolean result = false;
            Object leftData = sourceModel().data(left);
            Object rightData = sourceModel().data(right);

            QRegExp emailPattern = new QRegExp("([\\w\\.]*@[\\w\\.]*)");

            String leftString = leftData.toString();
            if(left.column() == 1 && emailPattern.indexIn(leftString) != -1)
                leftString = emailPattern.cap(1);

            String rightString = rightData.toString();
            if(right.column() == 1 && emailPattern.indexIn(rightString) != -1)
                rightString = emailPattern.cap(1);

            result = leftString.compareTo(rightString) < 0;
            return result;
        }
    }
}
