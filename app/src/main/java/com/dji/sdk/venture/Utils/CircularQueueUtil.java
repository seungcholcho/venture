package com.dji.sdk.venture.Utils;

import com.dji.sdk.venture.Interface.IQueue;

public class CircularQueueUtil<T> implements IQueue<T> {
    private T[] elements;
    private int front;
    private int rear;
    private int maxSize;

    //Constructor
    public CircularQueueUtil(int size) {
        this.elements = (T[]) new Object[size + 1];
        // Here is dummy space
        // that is used to differentiate between the "isEmpty" and "Full" states.
        this.front = 0;
        this.rear = 0;
        this.maxSize = size + 1;
    }

    //enqueue function
    @Override
    public void offer(T data) {
        if (this.isFull()) {
            poll();
        }

        this.rear = (this.rear + 1) % this.maxSize;
        this.elements[this.rear] = data;
    }

    //remove function
    @Override
    public T poll() {
        if (this.isEmpty()) {
            throw new IllegalStateException();
        }
        this.front = (this.front + 1) % this.maxSize;
        return this.elements[this.front];
    }

    //view data on front function
    @Override
    public T peek() {
        if (this.isEmpty()) {
            throw new IllegalStateException();
        }
        return this.elements[this.front];
    }

    //return the queue size function
    @Override
    public int size() {
        if (this.front <= this.rear) {
            return this.rear - this.front;
        }
        return this.maxSize - this.front + this.rear;
    }

    //clear all the queue function
    @Override
    public void clear() {
        this.front = 0;
        this.rear = 0;
    }

    //check if empty function
    @Override
    public boolean isEmpty() {
        return this.front == this.rear;
    }

    //check if full fuction
    private boolean isFull() {
        return (this.rear + 1) % this.maxSize == this.front;

    }
}
