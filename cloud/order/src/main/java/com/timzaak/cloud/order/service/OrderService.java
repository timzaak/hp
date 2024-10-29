package com.timzaak.cloud.order.service;

import com.timzaak.cloud.order.controller.request.PlaceOrderRequest;
import com.timzaak.cloud.order.entity.Order;
import com.timzaak.cloud.order.entity.OrderInfo;
import com.timzaak.cloud.order.entity.OrderProduct;
import com.timzaak.cloud.order.mapper.OrderMapper;
import com.timzaak.cloud.others.api.*;
import com.timzaak.cloud.product.api.BuyProductRequest;
import com.timzaak.cloud.product.api.Product;
import com.timzaak.cloud.product.api.ProductAPI;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @DubboReference
    private final ProductAPI productAPI;
    @DubboReference
    private final OthersAPI othersAPI;

    private final OrderMapper orderMapper;

    public OrderService(ProductAPI productAPI, OthersAPI othersAPI, OrderMapper orderMapper) {
        this.productAPI = productAPI;
        this.othersAPI = othersAPI;
        this.orderMapper = orderMapper;
    }

    public boolean checkSumAmount(PlaceOrderRequest request)  {
        // 需要对参数做仔细校验。放在 PlaceOrderRequest 里写 Validation

        var sumAmount = request.products().stream().map(p -> p.price().multiply(new BigDecimal(p.count()))
                .setScale(2, RoundingMode.DOWN)).reduce(BigDecimal.ZERO, BigDecimal::add);
        if(request.coupon().isPresent()) {
            final var coupon = request.coupon().get();
            final var couponRule = coupon.rule();
            final var couponType = couponRule.type();

            if(Objects.equals(couponType, CouponRuleType.DiscountReductionType.getNumber())) {
                sumAmount = sumAmount.multiply(couponRule.num());
            } else if(Objects.equals(couponType, CouponRuleType.FullReductionType.getNumber())) {
                sumAmount = sumAmount.subtract(couponRule.num());
            }

        }
        if(request.bonus().isPresent())  {
            final var bonus = request.bonus().get();
            sumAmount = sumAmount.subtract(new BigDecimal(bonus).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN));
        }
        if(sumAmount.compareTo(request.totalAmount()) != 0) {
            return false;
        }
        return true;
    }

    @GlobalTransactional(name = "placeOrder")
    public void doPlaceOrderTransaction(Integer userId,long orderId, PlaceOrderRequest request) throws Exception {
        var req = BuyProductRequest.newBuilder();
        var products = request.products().stream().map(p -> {
            var _p = Product.newBuilder();
            _p.setCount(p.count());
            _p.setId(p.id());
            _p.setPrice(p.price().doubleValue());
            _p.setSnapId(p.snapId());
            return _p.build();
        }).toList();

        req.addAllProducts(products);

        // 锁库存
        var productReply = productAPI.buy(req.build());

        if(!productReply.getIsOk()) {
            //rollback
            throw new Exception("库存失败");

        }
        var c = request.coupon().map((v)-> Coupon.newBuilder()
                .setId(v.id())
                .setRule(CouponRule.newBuilder()
                        .setNum(v.rule().num().doubleValue())
                        .setType(CouponRuleType.forNumber(v.rule().type()))
                        .build())
                .build()).orElseGet(() -> Coupon.newBuilder().build());


        var p2 = BuyRequest.newBuilder().setOrderId(orderId)
                .setBonus(request.bonus().orElse(0))
                .setCoupon(c)
                .setUserId(userId)
                .build();
        var otherReply = othersAPI.buy(p2);
        if(!otherReply.getIsOk()) {
            throw new Exception("Other Error");
        }

        var info = new OrderInfo(
                request.products().stream().map(p -> new OrderProduct(p.snapId(), p.count()))
                        .collect(Collectors.toList()),
                request.coupon().map(PlaceOrderRequest.Coupon::id),
                request.bonus());
        var order = new Order(
                orderId,
                userId,
                request.totalAmount(),
                info,
                (short) 1,
                null, null
        );
        orderMapper.createOrder1(order);
    }
}
