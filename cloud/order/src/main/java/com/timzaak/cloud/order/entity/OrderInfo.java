package com.timzaak.cloud.order.entity;

import java.util.List;
import java.util.Optional;

public record OrderInfo(List<OrderProduct> products, Optional<Long> coupon, Optional<Integer> bonus) {}
