package eu.occtet.bocfrontend.engine;

import javax.annotation.Nonnull;

/**
 * simplified queue interface
 * @param <T>
 */
public interface SimpleQueue<T> {
    void add(@Nonnull T t);
    T poll();
    int size();
    void remove(T t);
    void clear();
}
