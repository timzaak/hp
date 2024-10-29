package com.timzaak.cloud.service;

import com.timzaak.cloud.action.BuyProductAction;
import com.timzaak.cloud.others.api.BuyReply;
import com.timzaak.cloud.others.api.BuyRequest;
import com.timzaak.cloud.others.api.DubboOthersAPITriple;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

@Service
@DubboService
public class OthersServiceImpl extends DubboOthersAPITriple.OthersAPIImplBase {

   private final BuyProductAction buyProductAction;

    public OthersServiceImpl(BuyProductAction buyProductAction) {
        this.buyProductAction = buyProductAction;
    }


    @Override
    public BuyReply buy(BuyRequest request) {
        final var reply = BuyReply.newBuilder();
        final var userId = request.getUserId();
        buyProductAction.prepare(null,  userId, request);
        return reply.setIsOk(true).build();
    }
}
