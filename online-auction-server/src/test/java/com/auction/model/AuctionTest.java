package com.auction.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) cho thực thể phiên đấu giá (Auction).
 */
class AuctionTest {

    @Test
    void testAuctionCreationAndDefaults() {
        Auction auction = new Auction(101, "OPEN");

        assertEquals(101, auction.getItemId());
        assertEquals("OPEN", auction.getStatus());
        // Kiểm tra giá trị mặc định của winnerId khi mới tạo phải là -1
        assertEquals(-1, auction.getWinnerId());
    }

    @Test
    void testAuctionSetters() {
        Auction auction = new Auction(102, "RUNNING");

        auction.setStatus("FINISHED");
        assertEquals("FINISHED", auction.getStatus());

        auction.setWinnerId(5);
        assertEquals(5, auction.getWinnerId());

        auction.setItemId(200);
        assertEquals(200, auction.getItemId());
    }
}