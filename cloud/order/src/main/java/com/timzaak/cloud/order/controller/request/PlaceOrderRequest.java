package com.timzaak.cloud.order.controller.request;

import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record PlaceOrderRequest(
        @NonNull
        List<Product> products,
        Optional<Coupon> coupon,
        Optional<Integer> bonus,
        @NonNull
        BigDecimal totalAmount
){
    public record Product(Integer snapId, Integer id, Integer count, BigDecimal price) {}

    public record Coupon(Long id, CouponRule rule){
        public record CouponRule(Integer type, BigDecimal num){}
    }

}

