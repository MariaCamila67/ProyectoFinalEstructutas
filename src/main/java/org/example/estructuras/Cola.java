package org.example.estructuras;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;


public class Cola<T> implements Iterable<T> {

    private final Queue<T> elementos = new ArrayDeque<>();

    public boolean offer(T elemento) { return elementos.offer(elemento); }

    public T poll() { return elementos.poll(); }

    public T peek() { return elementos.peek(); }

    public boolean isEmpty() { return elementos.isEmpty(); }

    public int size() { return elementos.size(); }

    public boolean remove(T elemento) { return elementos.remove(elemento); }

    @Override public Iterator<T> iterator() { return elementos.iterator(); }
}
