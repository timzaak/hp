package com.timzaak.backend.common.resp;

import com.timzaak.backend.common.dsl.Either;

public interface Response {
    default boolean isOk() {return true;}

    static <T> Response ok(T data) {return new OKResp<T>(data);}
    static Response fail(String  message) {return new FailResp(message);}

    static <R> Response ofEither(Either<String, R> either) {
        if(either.isLeft()) {
            return Response.fail(either.getLeft());
        }else {
            return Response.ok(either.getRight());
        }
    }
}

