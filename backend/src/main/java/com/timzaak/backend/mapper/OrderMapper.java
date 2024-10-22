package com.timzaak.backend.mapper;

import com.timzaak.backend.entity.order.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;



public interface OrderMapper {

    final Short WaitPay = 1;


    @Select("select nextval('order_1_id_seq')")
    long getOrder1Id();

    @Insert("insert  into order_1(id, user_id, info, total_amount, status) values (#{id},#{userId},#{info},#{totalAmount}, 1)")
    int createOrder1(Order order);

}
