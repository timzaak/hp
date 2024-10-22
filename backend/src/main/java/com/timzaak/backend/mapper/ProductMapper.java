package com.timzaak.backend.mapper;

import com.timzaak.backend.common.dsl.Pair;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

public interface ProductMapper {

    @Update("update product set stock=stock-#{count}, updated_at=now() "+
            " where id=#{productId} and snap_id=#{productSnapId} and price=#{price} and stock-#{count}>=0")
    int buyProduct(Integer productId, Integer productSnapId, Integer count, BigDecimal price);

    @Update("update product set stock=stock+#{count}, updated_at=now() where id=#{productId}")
    int refundStock(Integer productId, Integer count);

    @Update("update product set stock=stock+#{count}, updated_at=now() where id=#{productId}")
    int increaseStock(Integer productId, Integer count);
    @Update("update product set stock=stock-#{count}, updated_at=now() where id=#{productId} and stock>=stock-#{count}>=0")
    int decreaseStock(Integer productId, Integer count);

    @Update("update product set stock=0, updated_at=now() where id=#{productId} and stock>0")
    int clearStock(Integer productId, Integer count);

    @Update("update product set is_on = #{status}, updated_at=now() where id=#{productId} and is_on!=#{status}")
    int changeStatus(Integer productId, Boolean status);

    @Select("select id as key, data->'price' as value from product_snapshot where id=any(#{id})")
    List<Pair<Integer, BigDecimal>> getProductSnapPrice(Integer[] id);

    static String getProductStockRedisKey(Integer productId) { return "p_" + productId; }
}
