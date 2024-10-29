package com.timzaak.cloud.service;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.timzaak.cloud.others.api.DubboGreeterTriple.GreeterImplBase;
import com.timzaak.cloud.others.api.GreeterReply;
import com.timzaak.cloud.others.api.GreeterRequest;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class GreeterImpl extends GreeterImplBase {
    private final TCCTest tccTest;

    public GreeterImpl(TCCTest tccTest) {
        this.tccTest = tccTest;
    }

    @Override
    public GreeterReply greet(GreeterRequest request) {
        return GreeterReply.newBuilder().setMessage("Hello " + request.getName()).build();
    }

    @Override
    public BoolValue tcc(Empty request) {
        var result = tccTest.prepare("param...");
        return BoolValue.newBuilder().setValue(result).build();
    }
}
