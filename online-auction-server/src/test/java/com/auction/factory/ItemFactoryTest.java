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
        assertEquals("18", electronics.getWarranty());
    }
    @Test
    void createItem_art_returnsArt() {
        Item item = ItemFactory.createItem("ART", "Mona Lisa", 1000.0, "2027-01-01", 20, "Leonardo da Vinci");

        assertNotNull(item);
        assertInstanceOf(Art.class, item);
        assertEquals("ART", item.getItemType());
        assertEquals("Leonardo da Vinci", item.getExtraInfo());

        Art art = (Art) item;
        assertEquals("Mona Lisa", art.getName());
        assertEquals(1000.0, art.getStartingPrice());
        assertEquals("2027-01-01", art.getEndTime());
        assertEquals(20, art.getSellerId());
        assertEquals("Leonardo da Vinci", art.getAuthor());
    }
    @Test
    void createItem_vehicle_returnsVehicle() {
        Item item = ItemFactory.createItem("VEHICLE", "Tesla Model S", 50000.0, "2028-02-02", 30, "Electric");

        assertNotNull(item);
        assertInstanceOf(Vehicle.class, item);
        assertEquals("VEHICLE", item.getItemType());
        assertEquals("Electric", item.getExtraInfo());

        Vehicle vehicle = (Vehicle) item;
        assertEquals("Tesla Model S", vehicle.getName());
        assertEquals(50000.0, vehicle.getStartingPrice());
        assertEquals("2028-02-02", vehicle.getEndTime());
        assertEquals(30, vehicle.getSellerId());
        assertEquals("Electric", vehicle.getEngineType());
    }
    @Test
    void createItem_unknownType_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ItemFactory.createItem("BOOK", "Dune", 10.0, "2026-01-01", 1, "n/a")
        );
        assertTrue(ex.getMessage().toUpperCase().contains("KHÔNG"));
        assertTrue(ex.getMessage().toUpperCase().contains("HỆ THỐNG"));
    }
}
