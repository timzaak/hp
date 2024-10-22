package com.timzaak.backend.common.dsl;


public interface Either<L, R> {
    boolean isRight();
    boolean isLeft();
    L getLeft();
    R getRight();

    static <L,R> Either<L,R> ofRight(R d) {return new Right<L,R>(d);}
    static <L,R> Either<L,R> ofLeft(L d) {return new Left<L,R>(d);}
}

record  Right<L,R>(R d) implements Either<L,R> {
    @Override
    public boolean isRight() {
        return true;
    }

    @Override
    public boolean isLeft() {
        return false;
    }

    @Override
    public L getLeft() {
        return null;
    }

    @Override
    public R getRight() {
        return d;
    }
}
record Left<L,R>(L d) implements Either<L,R> {
    @Override
    public boolean isRight() {
        return false;
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public L getLeft() {
        return d;
    }

    @Override
    public R getRight() {
        return null;
    }
}