package com.example.purchasebackend.common;

import java.util.UUID;

/** Generates short, prefixed, human-readable ids (e.g. {@code pur_a1b2c3d4e5f6}). */
public final class Ids {

    private Ids() {
    }

    public static String newId(String prefix) {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return prefix + "_" + raw.substring(0, 12);
    }
}
