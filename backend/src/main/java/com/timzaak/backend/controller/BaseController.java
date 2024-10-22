package com.timzaak.backend.controller;

import com.timzaak.backend.common.dsl.Pair;
import com.timzaak.backend.common.resp.Response;
import com.timzaak.backend.common.resp.SampleResp;
import com.timzaak.backend.common.security.CurrentUser;
import com.timzaak.backend.mapper.ProductMapper;
import com.timzaak.backend.mapper.TestDBMapper;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class BaseController {
    private final TestDBMapper testDBMapper;

    private final ProductMapper productMapper;
    public BaseController(TestDBMapper testDBMapper, ProductMapper productMapper) {
        this.testDBMapper = testDBMapper;
        this.productMapper = productMapper;
    }

    // curl http://127.0.0.1:8080/ping?t=123 -H "Authorization: t"
    @GetMapping("/ping")
    public SampleResp ping(@RequestParam(value="t", defaultValue = "default") String t) {
        Assert.isTrue(testDBMapper.check(), "");
        return new SampleResp(t);
    }

    private boolean  isOk = false;
    // curl http://127.0.0.1:8080/t -H "Authorization: t"
    @GetMapping("/t")
    public Response test(CurrentUser user) {

        var a = productMapper.getProductSnapPrice(List.of(1).toArray(Integer[]::new));
        System.out.println(Pair.toMap(a));
        System.out.println(CurrentUser.getUserId());

        isOk = !isOk;
        if(isOk) {
            return Response.ok(new SampleResp("ok"));
        }else {
            return Response.fail("bad thing");
        }
    }
}
