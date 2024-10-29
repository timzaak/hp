package com.timzaak.cloud.service;


import org.apache.seata.rm.tcc.api.BusinessActionContext;

public interface TCCTest {

    boolean prepare(BusinessActionContext actionContext, int a);

    boolean commit(BusinessActionContext actionContext);

    boolean rollback(BusinessActionContext actionContext);

}
