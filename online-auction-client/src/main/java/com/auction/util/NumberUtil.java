package com.auction.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javafx.util.StringConverter;

/**
 * Lớp tiện ích hỗ trợ định dạng và xử lý phân tích các chuỗi số trong hệ thống.
 */
public class NumberUtil {

  private static final DecimalFormat formatter =
      (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

  static {
    formatter.applyPattern("#,##0");
  }

  /**
   * Định dạng một đối tượng số thành chuỗi phân tách hàng phần nghìn theo định dạng cấu hình.
   *
   * @param number Đối tượng số cần định dạng (Integer, Double, Long, v.v.).
   * @return Chuỗi văn bản đã được định dạng, hoặc "0" nếu đối tượng truyền vào bị rỗng.
   */
  public static String format(Number number) {
    if (number == null) {
      return "0";
    }
    return formatter.format(number);
  }

  /**
   * Phân tích một chuỗi văn bản thành đối tượng số tương ứng.
   *
   * @param text Chuỗi văn bản chứa ký tự số cần xử lý.
   * @return Đối tượng số Number được trích xuất thành công, hoặc 0 nếu có lỗi xảy ra.
   */
  public static Number parse(String text) {
    try {
      if (text == null || text.trim().isEmpty()) {
        return 0;
      }
      // Remove any non-digit characters except comma and minus if needed
      // Actually formatter.parse handles it but let's be safe
      return formatter.parse(text);
    } catch (ParseException e) {
      // Fallback for raw numbers without commas if parse fails, though US locale handles it
      try {
        return Double.parseDouble(text.replaceAll("[^\\d.-]", ""));
      } catch (NumberFormatException ex) {
        return 0;
      }
    }
  }

  /**
   * Cung cấp bộ chuyển đổi kiểu dữ liệu StringConverter chuyên biệt cho dữ liệu Integer.
   * Được tối ưu hóa bằng Diamond Operator để tinh gọn mã nguồn.
   *
   * @return Khối đối tượng StringConverter xử lý logic chuyển đổi văn bản và số nguyên.
   */
  public static StringConverter<Integer> getIntegerConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(Integer object) {
        return format(object);
      }

      @Override
      public Integer fromString(String string) {
        return parse(string).intValue();
      }
    };
  }
}