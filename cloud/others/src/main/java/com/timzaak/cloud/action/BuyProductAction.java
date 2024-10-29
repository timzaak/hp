package com.timzaak.cloud.action;

import com.timzaak.cloud.others.api.BuyRequest;
import org.apache.seata.rm.tcc.api.BusinessActionContext;

public interface BuyProductAction {

    boolean prepare(BusinessActionContext actionContext, Integer userId,  BuyRequest request);

    boolean commit(BusinessActionContext actionContext, Integer userId,  BuyRequest request);

    boolean rollback(BusinessActionContext actionContext, Integer userId, BuyRequest request);

}
