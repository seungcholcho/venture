package com.dji.sdk.venture.Interface;

//This is an interface class for queue classes.
public interface IQueue<T> {
    void offer(T data);
    T poll();
    T peek();
    int size();
    void clear();
    boolean isEmpty();
}
