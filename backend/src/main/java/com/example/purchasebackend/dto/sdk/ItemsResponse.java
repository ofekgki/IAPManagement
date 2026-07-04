package com.example.purchasebackend.dto.sdk;

import java.util.List;

/** Data of GET /api/v1/sdk/items. */
public record ItemsResponse(List<ItemDto> items) {
}
