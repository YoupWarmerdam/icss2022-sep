package nl.han.ica.datastructures;

import java.util.LinkedList;
import java.util.EmptyStackException;

public class HANStack<T> implements IHANStack<T> {
    private LinkedList<T> list = new LinkedList<>();

    public void push(T value) {
        list.add(value);
    }

    public T pop() {
        if (list.size() == 0) {
            throw new EmptyStackException();
        }
        return list.remove(list.size() - 1);
    }

    public T peek() {
        if (list.size() == 0) {
            throw new EmptyStackException();
        }
        return list.get(list.size() - 1);
    }
}