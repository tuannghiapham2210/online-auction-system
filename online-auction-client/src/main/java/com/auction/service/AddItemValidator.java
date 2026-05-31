package com.auction.service;

import com.auction.dto.AddItemRequestDto;
import com.auction.util.NumberUtil;

/**
 * Component chuyên biệt làm nhiệm vụ Kiểm duyệt dữ liệu (Validation).
 *
 * <p>Áp dụng: Single Responsibility Principle (SRP).
 * Tách biệt hoàn toàn logic kiểm tra đúng sai ra khỏi Service, giúp code Service
 * cực kỳ gọn gàng và dễ test (Unit Test) cho riêng phần điều kiện nghiệp vụ.
 */
public class AddItemValidator {

  /**
   * Khối dữ liệu chứa kết quả kiểm duyệt thông tin sản phẩm.
   */
  public static class ValidationResult {
    public final boolean isValid;
    public final String errorMessage;
    public final double startingPrice;
    public final double stepPrice;
    public final double durationHours;

    /**
     * Khởi tạo một đối tượng ValidationResult.
     *
     * @param isValid       Trạng thái dữ liệu có hợp lệ hay không.
     * @param errorMessage  Thông báo lỗi chi tiết nếu dữ liệu không hợp lệ.
     * @param startingPrice Giá khởi điểm sau khi parse thành công.
     * @param stepPrice     Bước giá sau khi parse thành công.
     * @param durationHours Thời lượng phiên đấu giá tính theo giờ.
     */
    public ValidationResult(boolean isValid, String errorMessage, double startingPrice,
                            double stepPrice, double durationHours) {
      this.isValid = isValid;
      this.errorMessage = errorMessage;
      this.startingPrice = startingPrice;
      this.stepPrice = stepPrice;
      this.durationHours = durationHours;
    }
  }

  /**
   * Kiểm tra tính hợp lệ của gói dữ liệu DTO.
   *
   * @param dto Gói dữ liệu AddItemRequestDto.
   * @return ValidationResult chứa kết quả và dữ liệu đã ép kiểu hợp lệ.
   */
  public static ValidationResult validate(AddItemRequestDto dto) {
    if (dto.getName() == null || dto.getName().trim().isEmpty()
        || dto.getType() == null
        || dto.getPriceStr() == null || dto.getPriceStr().trim().isEmpty()
        || dto.getStepStr() == null || dto.getStepStr().trim().isEmpty()
        || dto.getDurationStr() == null || dto.getDurationStr().trim().isEmpty()) {
      return new ValidationResult(false, "Vui lòng điền đủ các trường bắt buộc (*)", 0, 0, 0);
    }

    try {
      double startingPrice = NumberUtil.parse(dto.getPriceStr()).doubleValue();
      double stepPrice = NumberUtil.parse(dto.getStepStr()).doubleValue();

      String[] timeParts = dto.getDurationStr().trim().split(":");
      if (timeParts.length != 3) {
        return new ValidationResult(false, "Thời gian phải đúng định dạng HH:mm:ss", 0, 0, 0);
      }

      int hours = Integer.parseInt(timeParts[0]);
      int minutes = Integer.parseInt(timeParts[1]);
      int seconds = Integer.parseInt(timeParts[2]);

      if (hours < 0 || minutes < 0 || seconds < 0 || minutes >= 60 || seconds >= 60) {
        return new ValidationResult(false, "Thời gian không hợp lệ!", 0, 0, 0);
      }

      double durationHours = hours + (minutes / 60.0) + (seconds / 3600.0);

      if (startingPrice <= 0 || stepPrice <= 0 || durationHours <= 0) {
        return new ValidationResult(false, "Giá tiền và thời gian phải lớn hơn 0", 0, 0, 0);
      }

      // Dữ liệu hợp lệ, trả về kèm các thông số đã tính toán để Service sử dụng
      return new ValidationResult(true, "OK", startingPrice, stepPrice, durationHours);

    } catch (NumberFormatException e) {
      return new ValidationResult(false, "Giá, Bước giá và Thời gian phải là số hợp lệ!", 0, 0, 0);
    } catch (Exception e) {
      return new ValidationResult(false, "Lỗi định dạng dữ liệu không xác định", 0, 0, 0);
    }
  }
}