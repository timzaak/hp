package com.timzaak.backend.common.resp;

public record FailResp(String msg) implements Response {
    @Override
    public boolean isOk() {
        return false;
    }
}
