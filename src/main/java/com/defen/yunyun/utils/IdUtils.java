package com.defen.yunyun.utils;

public class IdUtils {
    private static long lastId = 0;

    public static synchronized long generateUniqueId() {
        long currentId = System.currentTimeMillis();
        if (currentId <= lastId) {
            currentId = lastId + 1;
        }
        lastId = currentId;
        return currentId;
    }
}
