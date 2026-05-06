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
    @Test
    void createItem_electronics_returnsElectronics() {
        Item item = ItemFactory.createItem("ELECTRONICS", "Phone", 100.0, "2026-12-31", 10, "18");

        assertNotNull(item);
        assertInstanceOf(Electronics.class, item);
        assertEquals("ELECTRONICS", item.getItemType());
        assertEquals("18", item.getExtraInfo());

        Electronics electronics = (Electronics) item;
        assertEquals("Phone", electronics.getName());
        assertEquals(100.0, electronics.getStartingPrice());
        assertEquals("2026-12-31", electronics.getEndTime());
        assertEquals(10, electronics.getSellerId());
        assertEquals(18, electronics.getWarranty());
    }
}
