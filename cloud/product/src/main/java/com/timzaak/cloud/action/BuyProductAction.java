package com.timzaak.cloud.action;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import com.timzaak.cloud.product.api.BuyProductRequest;

public interface BuyProductAction {

    boolean prepare(BusinessActionContext actionContext, BuyProductRequest request);

    boolean commit(BusinessActionContext actionContext, BuyProductRequest request);

    boolean rollback(BusinessActionContext actionContext, BuyProductRequest request);

}
