package com.auction.service;

import com.auction.dto.AddItemRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddItemValidatorTest {

    @Test
    void testValidate_Success() {
        AddItemRequestDTO dto = new AddItemRequestDTO.Builder()
                .setName("Test Item")
                .setType("ELECTRONICS")
                .setPriceStr("1,500.50")
                .setStepStr("10.0")
                .setDurationStr("02:30:00")
                .setSellerId(1)
                .build();

        AddItemValidator.ValidationResult result = AddItemValidator.validate(dto);

        assertTrue(result.isValid);
        assertEquals("OK", result.errorMessage);
        assertEquals(1500.5, result.startingPrice);
        assertEquals(10.0, result.stepPrice);
        assertEquals(2.5, result.durationHours); // 2 + 30/60
    }

    @Test
    void testValidate_MissingFields() {
        AddItemRequestDTO dto = new AddItemRequestDTO.Builder()
                .setName("") // Empty name
                .setType("ELECTRONICS")
                .setPriceStr("100")
                .setStepStr("10")
                .setDurationStr("01:00:00")
                .setSellerId(1)
                .build();

        AddItemValidator.ValidationResult result = AddItemValidator.validate(dto);
        assertFalse(result.isValid);
        assertEquals("Vui lòng điền đủ các trường bắt buộc (*)", result.errorMessage);
    }

    @Test
    void testValidate_InvalidTimeFormat() {
        AddItemRequestDTO dto = new AddItemRequestDTO.Builder()
                .setName("Test Item")
                .setType("ELECTRONICS")
                .setPriceStr("100")
                .setStepStr("10")
                .setDurationStr("01:60:00") // 60 minutes is invalid
                .setSellerId(1)
                .build();

        AddItemValidator.ValidationResult result = AddItemValidator.validate(dto);
        assertFalse(result.isValid);
        assertEquals("Thời gian không hợp lệ!", result.errorMessage);
    }

    @Test
    void testValidate_InvalidNumberFormat() {
        AddItemRequestDTO dto = new AddItemRequestDTO.Builder()
                .setName("Test Item")
                .setType("ELECTRONICS")
                .setPriceStr("abc")
                .setStepStr("10")
                .setDurationStr("01:00:00")
                .setSellerId(1)
                .build();

        AddItemValidator.ValidationResult result = AddItemValidator.validate(dto);
        assertFalse(result.isValid);
        assertEquals("Giá, Bước giá và Thời gian phải là số hợp lệ!", result.errorMessage);
    }

    @Test
    void testValidate_NegativeValues() {
        AddItemRequestDTO dto = new AddItemRequestDTO.Builder()
                .setName("Test Item")
                .setType("ELECTRONICS")
                .setPriceStr("-100")
                .setStepStr("10")
                .setDurationStr("01:00:00")
                .setSellerId(1)
                .build();

        AddItemValidator.ValidationResult result = AddItemValidator.validate(dto);
        assertFalse(result.isValid);
        assertEquals("Giá tiền và thời gian phải lớn hơn 0", result.errorMessage);
    }
}
