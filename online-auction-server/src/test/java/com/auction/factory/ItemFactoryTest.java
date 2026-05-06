package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void createItem_nullType_returnsNull() {
        Item item = ItemFactory.createItem(null, "Phone", 100.0, "2026-12-31", 1, "12");
        assertNull(item);
    }
    @Test
    void createItem_emptyType_returnsNull() {
        Item item = ItemFactory.createItem("", "Phone", 100.0, "2026-12-31", 1, "12");
        assertNull(item);
    }
}
