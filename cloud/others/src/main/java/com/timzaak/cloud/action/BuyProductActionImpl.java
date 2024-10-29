package com.timzaak.cloud.action;

import com.timzaak.cloud.mapper.BonusMapper;
import com.timzaak.cloud.mapper.CouponMapper;
import com.timzaak.cloud.others.api.BuyRequest;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@LocalTCC
@Service
public class BuyProductActionImpl implements BuyProductAction {

    private final CouponMapper couponMapper;
    private final BonusMapper bonusMapper;

    public BuyProductActionImpl(CouponMapper couponMapper, BonusMapper bonusMapper) {
        this.couponMapper = couponMapper;
        this.bonusMapper = bonusMapper;
    }

    // 因为 useTCCFence, 不考虑 try、cancel 执行顺序问题。
    @TwoPhaseBusinessAction(name = "BuyOtherAction", useTCCFence = true)
    @Transactional
    @Override
    public boolean prepare(BusinessActionContext actionContext, @BusinessActionContextParameter("userId") Integer userId, @BusinessActionContextParameter("request") BuyRequest request) {

        var ok = false;
        var coupon = request.getCoupon();

        if(request.hasCoupon()) {
            var rule = coupon.getRule();
            ok = couponMapper.useCoupon(coupon.getId(), userId, CouponMapper.orderRefId(request.getOrderId()), BigDecimal.valueOf(rule.getNum()), rule.getType().getNumber()) == 1;
            if(!ok) {
                return false;
            }
        }
        final var bonus = request.getBonus();
        if(request.hasBonus() && bonus!=0) {
            ok = bonusMapper.useBonus(userId, bonus) == 1;
            if(!ok) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean commit(BusinessActionContext actionContext,@BusinessActionContextParameter("userId")  Integer userId,@BusinessActionContextParameter("request") BuyRequest request) {
        // do nothing.
        return true;
    }

    @Transactional
    @Override
    public boolean rollback(BusinessActionContext actionContext, @BusinessActionContextParameter("userId") Integer userId, @BusinessActionContextParameter("request")BuyRequest request) {
        var coupon = request.getCoupon();
        if(request.hasCoupon()) {
            couponMapper.revokeCoupon(coupon.getId(), userId, CouponMapper.orderRefId(request.getOrderId()));
        }
        final var bonus = request.getBonus();
        if(request.hasBonus() && bonus!=0) {
            bonusMapper.revokeUseBonus(userId, bonus);
        }
        return true;
    }
}
