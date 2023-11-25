package edu.dcs;

import java.util.Iterator;
import java.util.List;

public class RoundRobinIterator<T> implements Iterable<T> {

    List<T> coll;
    int index = 0;
    public RoundRobinIterator(List<T> coll) {
        this.coll = coll;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                T server = coll.get(index);
                index = (index + 1) % coll.size();
                return server;
            }

        };
    }

}
