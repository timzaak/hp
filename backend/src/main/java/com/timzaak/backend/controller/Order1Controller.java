package com.timzaak.backend.controller;

import com.timzaak.backend.common.dsl.Either;
import com.timzaak.backend.common.resp.Response;
import com.timzaak.backend.common.security.CurrentUser;
import com.timzaak.backend.controller.request.PlaceOrderRequest;
import com.timzaak.backend.entity.order.Order;
import com.timzaak.backend.entity.order.OrderInfo;
import com.timzaak.backend.entity.order.OrderProduct;
import com.timzaak.backend.mapper.BonusMapper;
import com.timzaak.backend.mapper.CouponMapper;
import com.timzaak.backend.mapper.OrderMapper;
import com.timzaak.backend.mapper.ProductMapper;
import com.timzaak.backend.service.OrderService;
import org.postgresql.util.PSQLException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.stream.Collectors;


@RestController
public class Order1Controller {

    private final TransactionTemplate transactionTemplate;

    private final BonusMapper bonusMapper;
    private final OrderMapper orderMapper;
    private final CouponMapper couponMapper;
    private final ProductMapper productMapper;

    private final OrderService orderService;

    public Order1Controller(PlatformTransactionManager transactionManager, BonusMapper bonusMapper, OrderMapper orderMapper, CouponMapper couponMapper, ProductMapper productMapper, OrderService orderService) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        //this.transactionTemplate.setTimeout(60);

        this.bonusMapper = bonusMapper;
        this.orderMapper = orderMapper;
        this.couponMapper = couponMapper;
        this.productMapper = productMapper;

        this.orderService = orderService;
    }

    private Long dobuySQL(Integer userId, PlaceOrderRequest request) throws Exception {
        var ok = false;
        for (final var product : request.products()) {
            ok = productMapper.buyProduct(product.id(), product.snapId(), product.count(), product.price()) == 1;
            if (!ok) throw new Exception("商品库存不足");
        }
        var orderId = orderMapper.getOrder1Id();
        if (request.coupon().isPresent()) {
            var coupon = request.coupon().get();
            var rule = coupon.rule();
            ok = couponMapper.useCoupon(coupon.id(), userId, CouponMapper.orderRefId(orderId), rule.num(), rule.type()) == 1;
            if (!ok) throw new Exception("优惠券已失效");
        }
        if (request.bonus().isPresent()) {
            var bonus = request.bonus().get();
            ok = bonusMapper.useBonus(userId, bonus) == 1;
            if (!ok) throw new Exception("积分不够");
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
        return orderId;
    }

    @PostMapping("/order1")
    public Response placeOrder(@RequestBody PlaceOrderRequest request) {
        if (!orderService.checkSumAmount(request)) {
            return Response.fail("价格计算错误");
        }
        final var userId = CurrentUser.getUserId();
        Either<String, Long> result = transactionTemplate.execute((status) -> {
            try {
                return Either.ofRight(dobuySQL(userId, request));
            } catch (Throwable e) {
                status.setRollbackOnly();
                return Either.ofLeft(e.getMessage());
            }
        });
        if (null == result) {
            return Response.fail("服务（事务）出现异常");
        }
        return Response.ofEither(result);
    }

    @PostMapping("/order1_retry")
    public Response placeOrderWithRetry(@RequestBody PlaceOrderRequest request) {
        if (!orderService.checkSumAmount(request)) {
            return Response.fail("价格计算错误");
        }
        final var userId = CurrentUser.getUserId();

        var retry = 0;
        Either<String, Long> result = null;
        while(retry < 3) {
            result = transactionTemplate.execute((status) -> {
                try {
                    return Either.ofRight(dobuySQL(userId, request));
                } catch (Throwable e) {
                    var cause = e.getCause();
                    if(cause instanceof  PSQLException) {
                        var sqlState = ((PSQLException) cause).getSQLState();
                        if ("40001".equals(sqlState)) {
                            status.setRollbackOnly();
                            return null;
                        } else {
                            status.setRollbackOnly();
                            return Either.ofLeft(e.getMessage());
                        }
                    }
                    status.setRollbackOnly();
                    return Either.ofLeft(e.getMessage());
                }
            });
            if(result !=  null) {
                break;
            } else {
                retry +=1;
            }
        }
        if (Objects.isNull(result)) {
            System.out.println("max retry fail");
            return Response.fail("服务（事务）出现异常");
        }
        return Response.ofEither(result);
    }


    @PostMapping("/order1_without_transaction")
    public Response placeOrderWithoutTransaction(@RequestBody PlaceOrderRequest request) {
        if (!orderService.checkSumAmount(request)) {
            return Response.fail("价格计算错误");
        }
        final var userId = CurrentUser.getUserId();
        try {
            return Response.ok(dobuySQL(userId, request));
        } catch (Throwable e) {
            return Response.fail("服务（事务）出现异常");
        }
    }

}
