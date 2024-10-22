package com.timzaak.backend.entity.order;

import java.util.List;
import java.util.Optional;

public record OrderInfo(List<OrderProduct> products, Optional<Long> coupon, Optional<Integer> bonus) {}
