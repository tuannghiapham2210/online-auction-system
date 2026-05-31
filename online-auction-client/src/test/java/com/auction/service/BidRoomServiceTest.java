package com.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BidRoomServiceTest {

    private BidRoomService service;

    @BeforeEach
    void setUp() {
        service = new BidRoomService();
    }

    @Test
    void testValidateBid_Success() {
        // bidAmount = 150, currentPrice = 100, stepPrice = 10, userBalance = 200
        assertDoesNotThrow(() -> service.validateBid(150, 100, 10, 200));
    }

    @Test
    void testValidateBid_BelowMinimum() {
        // bidAmount = 105, currentPrice = 100, stepPrice = 10 -> minBid = 110
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateBid(105, 100, 10, 200)
        );
        assertEquals("Giá đặt tối thiểu phải là $110", exception.getMessage());
    }

    @Test
    void testValidateBid_ExceedsBalance() {
        // bidAmount = 150, currentPrice = 100, stepPrice = 10, userBalance = 120
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateBid(150, 100, 10, 120)
        );
        assertEquals("Bạn chỉ còn $120", exception.getMessage());
    }

    @Test
    void testValidateAutoBid_Success() {
        // maxBudget = 200, increment = 15, currentPrice = 100, stepPrice = 10, balance = 250
        assertDoesNotThrow(() -> service.validateAutoBid(200, 15, 100, 10, 250));
    }

    @Test
    void testValidateAutoBid_MaxBudgetBelowMinimum() {
        // maxBudget = 105, minMaxBid = 110
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateAutoBid(105, 15, 100, 10, 250)
        );
        assertEquals("Giá max phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($110)!", exception.getMessage());
    }

    @Test
    void testValidateAutoBid_MaxBudgetExceedsBalance() {
        // maxBudget = 200, balance = 150
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateAutoBid(200, 15, 100, 10, 150)
        );
        assertEquals("Ngân sách tối đa không được vượt quá số dư tài khoản ($150)!", exception.getMessage());
    }

    @Test
    void testValidateAutoBid_IncrementBelowStepPrice() {
        // increment = 5, stepPrice = 10
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateAutoBid(200, 5, 100, 10, 250)
        );
        assertEquals("Bước giá tự động ít nhất bằng bước giá của sản phẩm ($10)!", exception.getMessage());
    }
}
