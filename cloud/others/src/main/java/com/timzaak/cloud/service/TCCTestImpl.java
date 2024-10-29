package com.timzaak.cloud.service;


import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.stereotype.Service;

@LocalTCC
@Service
public class TCCTestImpl implements TCCTest {
    @TwoPhaseBusinessAction(name = "TccOtherTestAction")
    @Override
    public boolean prepare(String a) {
        // System.out.println("TccOtherTestAction prepare, xid:" + RootContext.getXID() + ", a:" + a);
        return true;
    }

    @Override
    public boolean commit(BusinessActionContext actionContext) {
        //System.out.println("commit....");
        //String xid = actionContext.getXid();
        //System.out.println("TccOtherTestAction commit, xid:" + xid);
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext) {
        System.out.println("rollback....");
        String xid = actionContext.getXid();
        System.out.println("TccOtherTestAction rollback, xid:" + xid);
        return true;
    }
}
