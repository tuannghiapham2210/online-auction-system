package com.auction.controller.helper;

import com.auction.model.Item;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardModel {
    private final ObservableList<Item> allItems = FXCollections.observableArrayList();

    public ObservableList<Item> getAllItems() {
        return allItems;
    }

    public void setAllItems(List<Item> items) {
        allItems.setAll(items);
    }

    public void addItem(Item item) {
        allItems.add(item);
    }

    public void removeItemById(int itemId) {
        allItems.removeIf(item -> item.getId() == itemId);
    }

    public Item getItemById(int itemId) {
        return allItems.stream().filter(item -> item.getId() == itemId).findFirst().orElse(null);
    }

    public boolean isFinished(Item item) {
        if ("FINISHED".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
            return true;
        }
        if (("ACTIVE".equalsIgnoreCase(item.getStatus()) || "RUNNING".equalsIgnoreCase(item.getStatus()))
                && item.getEndTime() != null && !item.getEndTime().isEmpty()) {
            try {
                LocalDateTime end = LocalDateTime.parse(item.getEndTime(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (!LocalDateTime.now().isBefore(end)) {
                    item.setStatus("FINISHED");
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public List<Item> getFilteredItems(String category, String searchText) {
        List<Item> targetList = getFilteredItemsByCategory(category);
        if (searchText == null || searchText.isEmpty()) {
            return targetList;
        }

        String lowerCaseFilter = searchText.toLowerCase();
        return targetList.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerCaseFilter))
                .collect(Collectors.toList());
    }

    private List<Item> getFilteredItemsByCategory(String category) {
        if ("ART".equalsIgnoreCase(category)) {
            return allItems.stream()
                    .filter(i -> "ART".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if ("VEHICLE".equalsIgnoreCase(category)) {
            return allItems.stream()
                    .filter(i -> "VEHICLE".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if ("ELECTRONICS".equalsIgnoreCase(category)) {
            return allItems.stream()
                    .filter(i -> "ELECTRONICS".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if ("OTHER".equalsIgnoreCase(category)) {
            return allItems.stream()
                    .filter(i -> "OTHER".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if ("FINISHED".equalsIgnoreCase(category)) {
            return allItems.stream()
                    .filter(this::isFinished)
                    .collect(Collectors.toList());
        } else {
            // Default: ALL (excluding finished/closed)
            return allItems.stream()
                    .filter(i -> !isFinished(i))
                    .collect(Collectors.toList());
        }
    }
}
