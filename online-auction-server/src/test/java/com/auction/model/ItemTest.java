package com.auction.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) dành cho các thực thể kế thừa từ lớp Item (Sản phẩm).
 * Mục tiêu: Đảm bảo luồng khởi tạo dữ liệu và các hàm Đa hình (Polymorphism) hoạt động chính xác.
 */
class ItemTest {

    /**
     * Kiểm thử khởi tạo đối tượng đồ Điện tử (Electronics) và các hàm lấy dữ liệu (Getter).
     */
    @Test
    void testElectronicsCreationAndGetters() {
        Electronics laptop = new Electronics("Macbook Pro", 1500.0, "2026-12-31", 1, "12 Months");

        assertEquals("Macbook Pro", laptop.getName());
        assertEquals(1500.0, laptop.getStartingPrice());
        assertEquals(1500.0, laptop.getCurrentPrice());
        assertEquals("2026-12-31", laptop.getEndTime());
        assertEquals(1, laptop.getSellerId());

        assertEquals("ELECTRONICS", laptop.getItemType());
        assertEquals("12 Months", laptop.getExtraInfo());
        assertEquals("12 Months", laptop.getWarranty());
    }

    /**
     * Kiểm thử khởi tạo đối tượng Tác phẩm Nghệ thuật (Art).
     */
    @Test
    void testArtCreationAndGetters() {
        Art monaLisa = new Art("Mona Lisa", 50000.0, "2026-10-15", 2, "Leonardo da Vinci");

        assertEquals("Mona Lisa", monaLisa.getName());
        assertEquals(50000.0, monaLisa.getStartingPrice());

        assertEquals("ART", monaLisa.getItemType());
        assertEquals("Leonardo da Vinci", monaLisa.getExtraInfo());
        assertEquals("Leonardo da Vinci", monaLisa.getAuthor());
    }

    /**
     * Kiểm thử khởi tạo đối tượng Phương tiện (Vehicle).
     */
    @Test
    void testVehicleCreationAndGetters() {
        Vehicle tesla = new Vehicle("Tesla Model S", 50000.0, "2026-12-01", 10, "Electric");

        assertEquals("Tesla Model S", tesla.getName());
        assertEquals(50000.0, tesla.getStartingPrice());

        assertEquals("VEHICLE", tesla.getItemType());
        assertEquals("Electric", tesla.getExtraInfo());
        assertEquals("Electric", tesla.getEngineType());
    }

    /**
     * Kiểm thử các hàm cập nhật dữ liệu (Setter) và ID của thực thể gốc (Entity).
     */
    @Test
    void testItemSettersAndEntityId() {
        Electronics phone = new Electronics("iPhone", 1000.0, "2026-11-11", 3, "6");

        phone.setId(99);
        assertEquals(99, phone.getId());

        phone.setCurrentPrice(1200.0);
        assertEquals(1200.0, phone.getCurrentPrice());

        phone.setDescription("New condition");
        assertEquals("New condition", phone.getDescription());
    }
}