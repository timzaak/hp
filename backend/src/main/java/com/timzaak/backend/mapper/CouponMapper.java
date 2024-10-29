package com.timzaak.backend.mapper;


import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface CouponMapper {
    Short NormalStatus = 1; //正常
    Short UsedStatus = 2;

    Integer FullReductionType = 0; //满减
    Integer DiscountReductionType = 1; // 折扣

    @Update("update user_coupon set status=2, ref_id= #{refId}, updated_at=now() "+
            " where user_id=#{userId} and id=#{id} and rule->'num'=#{num} and rule->'type'=#{type} and status=1")
    int useCoupon(Long id, Integer userId, String refId, BigDecimal num, Integer type);

    static String orderRefId(Long id) {
        return "o_" + id;
    }
}
