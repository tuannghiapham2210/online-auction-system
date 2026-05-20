package com.auction.util;

import javafx.util.StringConverter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class NumberUtil {

    private static final DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

    static {
        formatter.applyPattern("#,##0");
    }

    public static String format(Number number) {
        if (number == null) {
            return "0";
        }
        return formatter.format(number);
    }

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

    public static StringConverter<Integer> getIntegerConverter() {
        return new StringConverter<Integer>() {
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
