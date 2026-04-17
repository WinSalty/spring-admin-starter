package com.salty.admin.common.util;

import java.util.UUID;

public final class TraceIdUtils {

    private TraceIdUtils() {
    }

    public static String nextTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
