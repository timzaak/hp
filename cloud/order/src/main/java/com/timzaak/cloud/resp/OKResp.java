package com.timzaak.cloud.resp;

public record OKResp<T>(T data) implements Response {
    
}
