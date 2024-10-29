package com.timzaak.cloud.order.controller;

import com.timzaak.cloud.order.controller.request.PlaceOrderRequest;
import com.timzaak.cloud.order.entity.Order;
import com.timzaak.cloud.order.entity.OrderInfo;
import com.timzaak.cloud.order.entity.OrderProduct;
import com.timzaak.cloud.order.mapper.OrderMapper;
import com.timzaak.cloud.order.service.OrderService;
import com.timzaak.cloud.others.api.*;
import com.timzaak.cloud.product.api.BuyProductRequest;
import com.timzaak.cloud.product.api.Product;
import com.timzaak.cloud.product.api.ProductAPI;
import com.timzaak.cloud.resp.Response;
import com.timzaak.cloud.user.security.CurrentUser;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class OrderController {


    @DubboReference
    private final ProductAPI productAPI;
    @DubboReference
    private final OthersAPI othersAPI;

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(ProductAPI productAPI, OthersAPI othersAPI, OrderService orderService, OrderMapper orderMapper) {
        this.productAPI = productAPI;
        this.othersAPI = othersAPI;
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }


    @PostMapping("/order")
    public Response order(@RequestBody PlaceOrderRequest request) {
        if (!orderService.checkSumAmount(request)) {
            return Response.fail("价格计算错误");
        }
        final var userId = CurrentUser.getUserId();

        try {
            var orderId = orderMapper.getOrder1Id();
            orderService.doPlaceOrderTransaction(userId, orderId, request);
            return Response.ok(orderId);
        }catch (Exception e) {
            return  Response.fail(e.getMessage());
        }

    }
}
