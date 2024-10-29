package com.timzaak.cloud.action;

import com.timzaak.cloud.mapper.ProductMapper;
import com.timzaak.cloud.product.api.BuyProductRequest;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

@Service
@LocalTCC
public class BuyProductActionImpl implements BuyProductAction  {
    private static final Logger logger = LoggerFactory.getLogger(BuyProductActionImpl.class);

    final private StringRedisTemplate stringRedisTemplate;
    final private ProductMapper productMapper;

    public BuyProductActionImpl(StringRedisTemplate stringRedisTemplate, ProductMapper productMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.productMapper = productMapper;
    }

    @TwoPhaseBusinessAction(name = "StockDeductAction")
    @Override
    public boolean prepare(BusinessActionContext actionContext, @BusinessActionContextParameter("request")BuyProductRequest request) {

        List<String> keys = new ArrayList<>(request.getProductsCount() + 1);
        List<String> args = new ArrayList<>(request.getProductsCount());

        for(var product: request.getProductsList()) {
            keys.add(ProductMapper.getProductStockRedisKey(product.getId()));
            args.add(String.valueOf(product.getCount()));
        }
        keys.add(actionContext.getXid());
        // 硬编码，cluster 会出问题。
        // if connect Redis Cluster, JedisConnection would change to JedisClusterConnection
        return Boolean.TRUE.equals(this.stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            var c = (Jedis) connection.getNativeConnection();
            var fc = c.fcall("buy", keys, args);
            if (fc != null) {
                if(((String)fc).startsWith("already")) {
                    logger.warn("stock in redis transaction error, result: {}", fc);
                }
                return false;
            }
            return true;
        }));

    }

    @Override
    @Transactional
    public boolean commit(BusinessActionContext actionContext, @BusinessActionContextParameter("request")BuyProductRequest request) {
        // commit 存在重复提交可能性
        for(var product: request.getProductsList()) {
           productMapper.buyProduct(product.getId(), product.getCount());
        }
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext, @BusinessActionContextParameter("request")BuyProductRequest request) {
        List<String> keys = new ArrayList<>(request.getProductsCount() + 1);
        List<String> args = new ArrayList<>(request.getProductsCount());

        for(var product: request.getProductsList()) {
            keys.add(ProductMapper.getProductStockRedisKey(product.getId()));
            args.add(String.valueOf(product.getCount()));
        }
        keys.add(actionContext.getXid());
        // 硬编码，cluster 会出问题。
        // if connect Redis Cluster, JedisConnection would change to JedisClusterConnection
        return Boolean.TRUE.equals(this.stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            var c = (Jedis) connection.getNativeConnection();
            c.fcall("rollback_buy", keys, args);
            return true;
        }));
    }
}
