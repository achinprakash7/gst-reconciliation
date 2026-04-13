package com.powerloom.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

public class FilteredResultRow {

    private final ObservableList<ResultRow> sourceList;
    private final FilteredList<ResultRow>   filteredList;

    public FilteredResultRow(ObservableList<ResultRow> sourceList) {
        this.sourceList   = sourceList;
        this.filteredList = new FilteredList<>(sourceList, r -> true);
    }

    public FilteredList<ResultRow> getFilteredList() {
        return filteredList;
    }

    public void filter(String query) {
        if (query == null || query.isBlank()) {
            filteredList.setPredicate(r -> true);
            return;
        }
        String q = query.trim().toLowerCase();
        filteredList.setPredicate(r -> {
            if (r == null) return false;
            return contains(r.getType(),    q)
                    || contains(r.getGstin(),   q)
                    || contains(r.getName(),    q)
                    || contains(r.getInvoice(), q)
                    || contains(r.getDate(),    q);
        });
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    public ObservableList<ResultRow> getSourceList() {
        return sourceList;
    }
}