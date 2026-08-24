package com.bstar.qolmod.hud.status;

import java.util.Objects;

/** Registry-owned lifetime metadata paired with immutable presentation data. */
public record VisibleStatus(
        StatusCardData data,
        long firstPublishedAtMillis,
        long updatedAtMillis,
        long terminalAtMillis,
        long fadeAtMillis,
        long expiresAtMillis
) {
    public VisibleStatus {
        Objects.requireNonNull(data, "data");
    }

    public boolean terminal() {
        return terminalAtMillis >= 0;
    }
}
