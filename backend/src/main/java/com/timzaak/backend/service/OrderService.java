package com.timzaak.backend.service;

import com.timzaak.backend.controller.request.PlaceOrderRequest;
import com.timzaak.backend.mapper.CouponMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
public class OrderService {

    public boolean checkSumAmount(PlaceOrderRequest request)  {
        // 需要对参数做仔细校验。放在 PlaceOrderRequest 里写 Validation

        var sumAmount = request.products().stream().map(p -> p.price().multiply(new BigDecimal(p.count()))
                .setScale(2, RoundingMode.DOWN)).reduce(BigDecimal.ZERO, BigDecimal::add);
        if(request.coupon().isPresent()) {
            final var coupon = request.coupon().get();
            final var couponRule = coupon.rule();
            final var couponType = couponRule.type();
            if(Objects.equals(couponType, CouponMapper.DiscountReductionType)) {
                sumAmount = sumAmount.multiply(couponRule.num());
            } else if(Objects.equals(couponType, CouponMapper.FullReductionType)) {
                sumAmount = sumAmount.subtract(couponRule.num());
            }
        }
        if(request.bonus().isPresent())  {
            final var bonus = request.bonus().get();
            sumAmount = sumAmount.subtract(new BigDecimal(bonus).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN));
        }
        return sumAmount.compareTo(request.totalAmount()) == 0;
    }
}
