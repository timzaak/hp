package com.timzaak.cloud.resp;

public record FailResp(String msg) implements Response {
    @Override
    public boolean isOk() {
        return false;
    }
}
