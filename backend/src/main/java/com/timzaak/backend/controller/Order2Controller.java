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
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class Order2Controller {
    private final TransactionTemplate transactionTemplate;

    private final BonusMapper bonusMapper;
    private final OrderMapper orderMapper;
    private final CouponMapper couponMapper;
    private final ProductMapper productMapper;

    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;

    public Order2Controller(PlatformTransactionManager transactionManager, BonusMapper bonusMapper, OrderMapper orderMapper, CouponMapper couponMapper, ProductMapper productMapper, OrderService orderService, StringRedisTemplate stringRedisTemplate) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

        this.bonusMapper = bonusMapper;
        this.orderMapper = orderMapper;
        this.couponMapper = couponMapper;
        this.productMapper = productMapper;

        this.orderService = orderService;

        this.stringRedisTemplate = stringRedisTemplate;
    }


    private Boolean doStockSQL(PlaceOrderRequest request) {
        return transactionTemplate.execute((status) -> {
            var ok = false;
            try {
                for (final var product : request.products()) {
                    ok = productMapper.buyProduct(product.id(), product.snapId(), product.count(), product.price()) == 1;
                    if (!ok) throw new Exception("商品库存不足");
                }
            }catch (Throwable e) {
                status.setRollbackOnly();
                return false;
            }
            return true;
        });
    }

    private Boolean doBuyRedisStock(PlaceOrderRequest request) {
        // 硬编码，cluster 会出问题。
        // if connect Redis Cluster, JedisConnection would change to JedisClusterConnection
        List<String> keys = new ArrayList<>(request.products().size());
        List<String> args = new ArrayList<>(request.products().size());
        for(var product: request.products()) {
            keys.add(ProductMapper.getProductStockRedisKey(product.id()));
            args.add(product.count().toString());
        }
        return this.stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            var c = (Jedis)connection.getNativeConnection();
            var result = c.fcall("buy", keys, args);
            return null == result;
        });
    }
    private void doRefundRedisStock(PlaceOrderRequest request) {
        for(var product: request.products()) {
            stringRedisTemplate.opsForValue()
                    .increment(ProductMapper.getProductStockRedisKey(product.id()), product.count());
        }
    }

    private  Long doPersonalSQL(Integer userId, PlaceOrderRequest request) throws Exception {
        var ok = false;
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


    @PostMapping("/order2")
    public Response placeOrder(@RequestBody PlaceOrderRequest request) {
        if (!orderService.checkSumAmount(request)) {
            return Response.fail("价格计算错误");
        }
        final var userId = CurrentUser.getUserId();
        var isSuccess = doStockSQL(request);
        if(!isSuccess) {
            return Response.fail("库存不足");
        }
        Either<String, Long> result = transactionTemplate.execute((status) -> {
            try {
                return Either.ofRight(doPersonalSQL(userId, request));
            } catch (Throwable e) {
                status.setRollbackOnly();
                return Either.ofLeft(e.getMessage());
            }
        });
        if (null == result) {
            // 回滚库存
            for (final var product : request.products()) {
                productMapper.refundStock(product.id(), product.count());
            }
            return Response.fail("个人信息出现异常");
        }
        return Response.ofEither(result);
    }


    // curl http://127.0.0.1:8080/order2_redis -X POST -d '{"products":[{"id":9,"snapId":9,"count":1,"price":9.99}],"totalAmount":9.99}, "bonus":1}'   -H "Authorization: t" -H "Content-Type: application/json" --verbose
    @PostMapping("/order2_redis")
    public Response placeOrderWithRedis(@RequestBody PlaceOrderRequest request) {
        if (!orderService.checkSumAmount(request)) {
            return Response.fail("价格计算错误");
        }
        final var userId = CurrentUser.getUserId();
        var isSuccess = doBuyRedisStock(request);
        if(!isSuccess) {
            return Response.fail("库存不足");
        }
        Either<String, Long> result = transactionTemplate.execute((status) -> {
            try {
                return Either.ofRight(doPersonalSQL(userId, request));
            } catch (Throwable e) {
                status.setRollbackOnly();
                return Either.ofLeft(e.getMessage());
            }
        });
        if (null == result) {
            // 回滚库存
            doRefundRedisStock(request);
            return Response.fail("个人信息出现异常");
        } else {
            //TODO: 异步改数据库 商品库存
        }
        return Response.ofEither(result);
    }

}
