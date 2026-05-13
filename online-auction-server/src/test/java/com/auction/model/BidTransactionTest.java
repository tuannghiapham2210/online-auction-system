package com.auction.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) cho thực thể giao dịch đặt giá (BidTransaction).
 */
class BidTransactionTest {

    @Test
    void testBidTransactionCreation() {
        BidTransaction bid = new BidTransaction(10, 5, 1500.0, "2026-05-14 10:00:00");

        assertEquals(10, bid.getItemId());
        assertEquals(5, bid.getBidderId());
        assertEquals(1500.0, bid.getBidAmount());
        assertEquals("2026-05-14 10:00:00", bid.getBidTime());
    }

    @Test
    void testBidTransactionSetters() {
        BidTransaction bid = new BidTransaction(1, 1, 10.0, "time");

        bid.setItemId(20);
        assertEquals(20, bid.getItemId());

        bid.setBidderId(8);
        assertEquals(8, bid.getBidderId());

        bid.setBidAmount(2500.0);
        assertEquals(2500.0, bid.getBidAmount());

        bid.setBidTime("2026-05-14 11:11:11");
        assertEquals("2026-05-14 11:11:11", bid.getBidTime());
    }
}