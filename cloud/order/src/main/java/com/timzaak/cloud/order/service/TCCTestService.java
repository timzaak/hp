package com.timzaak.cloud.order.service;

import com.google.protobuf.Empty;
import com.timzaak.cloud.others.api.Greeter;
import com.timzaak.cloud.product.api.ProductAPI;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
public class TCCTestService {


    @DubboReference
    private final ProductAPI productAPI;
    @DubboReference
    private final Greeter greeter;

    public TCCTestService(ProductAPI productAPI, Greeter greeter) {
        this.productAPI = productAPI;
        this.greeter = greeter;
    }

    @GlobalTransactional(name = "testTransaction", rollbackFor = Exception.class)
    public void doTransaction() throws Exception {
        var in = RootContext.inGlobalTransaction();
        System.out.println("is in Transaction 1" + in);

        var result = productAPI.tcc(Empty.newBuilder().build());
        if (!result.getValue()) {
            throw new RuntimeException("TCC test failed");

        }
        var r2 = greeter.tcc(Empty.newBuilder().build());
        if (!r2.getValue()) {
            throw new RuntimeException("TCC test failed");
        }
    }


    @GlobalTransactional(name = "testAsyncTransaction", rollbackFor = Exception.class)
    public void doTransactionAsync() throws Exception {
        var in = RootContext.inGlobalTransaction();
        System.out.println("is in Transaction 1" + in);

        var r1 = productAPI.tccAsync(Empty.newBuilder().build());
        var r2 = greeter.tccAsync(Empty.newBuilder().build());
        CompletableFuture.allOf(r1, r2).join();

        if (!r1.join().getValue()) {
            throw new RuntimeException("TCC test failed");

        }
        if (!r2.join().getValue()) {
            throw new RuntimeException("TCC test failed");
        }
    }
}
