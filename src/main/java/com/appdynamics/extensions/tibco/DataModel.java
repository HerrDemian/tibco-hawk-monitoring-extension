package com.appdynamics.extensions.tibco;


public class DataModel {

    private String columnName;
    private String columnType;
    private boolean indexColumn;
    
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public boolean isIndexColumn() {
        return indexColumn;
    }

    public void setIndexColumn(boolean indexColumn) {
        this.indexColumn = indexColumn;
    }
}
