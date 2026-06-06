package com.jqhc.dataplatform.util;

public final class NumberUtils {

    private NumberUtils() {}

    public static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static String resolveFactoryId(String factoryId, String defaultFactoryId) {
        return factoryId != null && !factoryId.isBlank() ? factoryId : defaultFactoryId;
    }
}
