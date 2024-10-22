package com.timzaak.backend.entity.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Order(Long id, Integer userId, BigDecimal totalAmount, OrderInfo info,short Status,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt){}
