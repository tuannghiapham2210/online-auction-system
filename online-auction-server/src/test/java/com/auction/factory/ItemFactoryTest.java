package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử Unit Test cho ItemFactory.
 * Áp dụng kỹ thuật:
 * - EP (Equivalence Partitioning): Kiểm thử các loại sản phẩm hợp lệ và không hợp lệ.
 * - BVA (Boundary Value Analysis): Kiểm thử chuỗi rỗng và null.
 */
class ItemFactoryTest {

    private static final Logger logger = LoggerFactory.getLogger(ItemFactoryTest.class);

    @Test
    @DisplayName("Kiểm thử EP: Khởi tạo thành công các loại Item hợp lệ")
    void testCreateValidItems() {
        logger.info("Running testCreateValidItems...");

        // 1. Phân hoạch hợp lệ: Đồ điện tử
        Item electronics = ItemFactory.createItem("ELECTRONICS", "Laptop", 1000, "2026-12-31", 1, "12 months");
        assertNotNull(electronics, "Sản phẩm không được null");
        assertTrue(electronics instanceof Electronics, "Phải là thể hiện của lớp Electronics");
        assertEquals("ELECTRONICS", electronics.getItemType(), "Type phải là ELECTRONICS");

        // 2. Phân hoạch hợp lệ: Tác phẩm nghệ thuật
        Item art = ItemFactory.createItem("ART", "Bức tranh A", 500, "2026-12-31", 1, "Picasso");
        assertTrue(art instanceof Art, "Phải là thể hiện của lớp Art");

        // 3. Phân hoạch hợp lệ: Phương tiện
        Item vehicle = ItemFactory.createItem("VEHICLE", "Xe máy", 2000, "2026-12-31", 1, "Gasoline");
        assertTrue(vehicle instanceof Vehicle, "Phải là thể hiện của lớp Vehicle");

        logger.info("testCreateValidItems passed!");
    }

    @Test
    @DisplayName("Kiểm thử EP: Ném ra lỗi khi truyền loại sản phẩm không hỗ trợ")
    void testCreateInvalidItemType() {
        logger.info("Running testCreateInvalidItemType...");

        // Phân hoạch không hợp lệ: Truyền một loại sản phẩm tào lao
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("FURNITURE", "Bàn ghế", 100, "2026-12-31", 1, "Wood");
        });

        // Kiểm tra xem câu thông báo lỗi có đúng như code mình thiết kế không
        assertTrue(exception.getMessage().contains("Loại sản phẩm không được hệ thống hỗ trợ"),
                "Thông báo lỗi phải chứa chuỗi 'Loại sản phẩm không được hệ thống hỗ trợ'");

        logger.info("testCreateInvalidItemType passed!");
    }

    @Test
    @DisplayName("Kiểm thử BVA: Xử lý an toàn khi tham số type là null hoặc chuỗi rỗng")
    void testCreateItemWithNullOrEmptyType() {
        logger.info("Running testCreateItemWithNullOrEmptyType...");

        // Biên 1: Chuỗi rỗng
        Item emptyTypeItem = ItemFactory.createItem("", "Test", 100, "2026-12-31", 1, "None");
        assertNull(emptyTypeItem, "Hàm phải trả về null khi type rỗng");

        // Biên 2: Null
        Item nullTypeItem = ItemFactory.createItem(null, "Test", 100, "2026-12-31", 1, "None");
        assertNull(nullTypeItem, "Hàm phải trả về null khi type là null");

        logger.info("testCreateItemWithNullOrEmptyType passed!");
    }
}