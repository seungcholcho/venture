package com.dji.sdk.venture;

public class CircularQueue<T> implements IQueue<T> {
    private T[] elements;
    private int front;
    private int rear;
    private int maxSize;

    public CircularQueue(int size) {
        this.elements = (T[]) new Object[size + 1];
        //더미 공간, isEmpty와 Full 상태를 구별하기 위함
        this.front = 0;
        this.rear = 0;
        this.maxSize = size + 1;
    }

    @Override
    public void offer(T data) {
        if (this.isFull()) {
            poll();
        }

        this.rear = (this.rear + 1) % this.maxSize;
        this.elements[this.rear] = data;
    }

    @Override
    public T poll() {
        if (this.isEmpty()) {
            throw new IllegalStateException();
        }
        this.front = (this.front + 1) % this.maxSize;
        return this.elements[this.front];
        // 어차피 데이터의 삽입과 인출은 front와 rear에 의해 일어나고,
        // 배열을 선언한 시점에서 그 위치를 쓰고있으니, 데이터를 지워줄 필요가 없다.
    }

    @Override
    public T peek() { // 데이터를 빼지 않고 확인만
        if (this.isEmpty()) {
            throw new IllegalStateException();
        }
        return this.elements[this.front + 1];
    }

    @Override
    public int size() {
        if (this.front <= this.rear) {
            return this.rear - this.front;
        }
        return this.maxSize - this.front + this.rear;
    }

    @Override
    public void clear() {
        this.front = 0; // 어차피 데이터 넣으면 초기화되니 이렇게 하면
        this.rear = 0; // 초기화
    }

    @Override
    public boolean isEmpty() {
        return this.front == this.rear;
    }

    private boolean isFull() {
        return (this.rear + 1) % this.maxSize == this.front;
        // rear 바로 뒤에 front가 있으면 큐가 꽉 찬 상태이다.
        // 한바퀴 돌면 front와 rear위치가 다시 바뀐다. rear+1 이 큐 사이즈보다
        // 커질 수 있기에, %연산으로 확실하게 확인한다.
    }
}
