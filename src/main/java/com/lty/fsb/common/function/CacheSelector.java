package com.lty.fsb.common.function;

@FunctionalInterface
public interface CacheSelector<T> {
    T select() throws Exception;
}
