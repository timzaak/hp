package com.timzaak.backend.mapper;

import org.apache.ibatis.annotations.Update;

// 积分
public interface BonusMapper {


    @Update("update bonus set amount=amount-#{count} where user_id=#{userId} and amount-#{count}>=0")
    int useBonus(Integer userId, Integer count);

}
