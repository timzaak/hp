package com.timzaak.backend.common.resp;

public record OKResp<T>(T data) implements Response {
    
}
