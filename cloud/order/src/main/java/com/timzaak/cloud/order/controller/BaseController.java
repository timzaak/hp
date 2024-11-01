package com.timzaak.cloud.order.controller;

import com.timzaak.cloud.order.service.TCCTestService;
import com.timzaak.cloud.others.api.Greeter;
import com.timzaak.cloud.others.api.GreeterRequest;
import com.timzaak.cloud.resp.SampleResp;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseController {

    @DubboReference
    private final Greeter greeter;

    private final TCCTestService tccTestService;

    public BaseController(Greeter greeter, TCCTestService tccTestService) {
        this.greeter = greeter;
        this.tccTestService = tccTestService;
    }


    @GetMapping("/ping")
    public SampleResp ping(@RequestParam(value="t", defaultValue = "default") String t) {
        return new SampleResp(t);
    }

    // curl http://127.0.0.1:8080/rpc\?t\=world -H "Authorization: t"
    @GetMapping("/rpc")
    public SampleResp rpc(@RequestParam(value="t", defaultValue = "default") String t) {
        var reply = greeter.greet(GreeterRequest.newBuilder().setName(t).build());
        return new SampleResp(reply.getMessage());
    }

    // curl http://127.0.0.1:8080/transaction\?t\=world -H "Authorization: t"
    @GetMapping("/transaction")
    public SampleResp transaction(@RequestParam(value="t", defaultValue = "default") String t) {
        try {
            tccTestService.doTransaction();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return new SampleResp(t);
    }




}
