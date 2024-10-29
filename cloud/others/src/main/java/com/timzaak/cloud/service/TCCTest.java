package com.timzaak.cloud.service;


import org.apache.seata.rm.tcc.api.BusinessActionContext;

public interface TCCTest {

    boolean prepare(String a);

    boolean commit(BusinessActionContext actionContext);

    boolean rollback(BusinessActionContext actionContext);

}
