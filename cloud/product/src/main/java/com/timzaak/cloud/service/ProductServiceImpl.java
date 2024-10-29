package com.timzaak.cloud.service;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.timzaak.cloud.action.BuyProductAction;
import com.timzaak.cloud.mapper.ProductMapper;
import com.timzaak.cloud.product.api.BuyProductReply;
import com.timzaak.cloud.product.api.BuyProductRequest;
import com.timzaak.cloud.product.api.DubboProductAPITriple;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.seata.core.context.RootContext;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

@DubboService
@Service
public class ProductServiceImpl extends DubboProductAPITriple.ProductAPIImplBase {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TCCTest tccTest;
    private final BuyProductAction buyProductAction;

    public ProductServiceImpl(ProductMapper productMapper, StringRedisTemplate stringRedisTemplate, TCCTest tccTest, BuyProductAction buyProductAction) {
        this.productMapper = productMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.tccTest = tccTest;
        this.buyProductAction = buyProductAction;
    }



    @Override
    public BuyProductReply buy(BuyProductRequest request) {
        var isOk = this.buyProductAction.prepare(null,  request);
        var reply = BuyProductReply.newBuilder().setIsOk(isOk);
        if(!isOk) {
            reply.setMessage("库存不足");
        }
        return reply.build();
    }

    @Override
    public BoolValue tcc(Empty request) {
        //var result = tccTest.prepare(null,  "product");
        var result = doTransaction();
        return BoolValue.newBuilder().setValue(result).build();
    }

    public boolean doTransaction() {
        return tccTest.prepare(null,1);
    }
}
