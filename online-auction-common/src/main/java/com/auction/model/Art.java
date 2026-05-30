package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho sản phẩm đấu giá thuộc loại Nghệ thuật (Art).
 * Kế thừa từ lớp Item cơ sở.
 */
public class Art extends Item {
  private static final Logger logger = LoggerFactory.getLogger(Art.class);

  private final String artistName;

  /**
   * Khởi tạo một tác phẩm nghệ thuật.
   *
   * @param name          Tên tác phẩm.
   * @param startingPrice Giá khởi điểm.
   * @param endTime       Thời gian kết thúc đấu giá.
   * @param sellerId      ID của người bán.
   * @param artistName    Tên tác giả của tác phẩm.
   */
  public Art(String name, double startingPrice, String endTime, int sellerId, String artistName) {
    // 1. Khởi tạo các thuộc tính chung từ lớp cha
    super(name, startingPrice, endTime, sellerId);

    // 2. Gán thuộc tính riêng
    this.artistName = artistName;
  }

  /**
   * In thông tin tóm tắt của tác phẩm nghệ thuật ra log.
   */
  @Override
  public void printInfo() {
    logger.info("[Art] {} - Author: {}", name, artistName);
  }

  /**
   * Lấy loại của sản phẩm.
   *
   * @return Chuỗi "ART".
   */
  @Override
  public String getItemType() {
    return "ART";
  }

  /**
   * Lấy thông tin bổ sung của sản phẩm (dùng để lưu vào Database).
   *
   * @return Tên tác giả.
   */
  @Override
  public String getExtraInfo() {
    return this.artistName;
  }

  /**
   * Lấy tên tác giả.
   *
   * @return Tên tác giả của tác phẩm.
   */
  public String getAuthor() {
    return this.artistName;
  }
}