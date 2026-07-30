package io.omnidepot.format.oci;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Lightweight CDI {@link Instance} wrapper for unit and component test scenarios.
 */
public class TestInstance<T> implements Instance<T> {

    private final @Nullable T value;

    public static <T> Instance<T> of(@Nullable T value) {
        return new TestInstance<>(value);
    }

    public static <T> Instance<T> empty() {
        return new TestInstance<>(null);
    }

    private TestInstance(@Nullable T value) {
        this.value = value;
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return (Instance<U>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        return (Instance<U>) this;
    }

    @Override
    public boolean isUnsatisfied() {
        return isNull(value);
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public void destroy(T instance) {
    }

    @Override
    public Handle<T> getHandle() {
        return null;
    }

    @Override
    public Iterable<Handle<T>> handles() {
        return List.of();
    }

    @Override
    public T get() {
        if (isNull(value)) {
            throw new IllegalStateException("TestInstance is empty");
        }
        return value;
    }

    @Override
    public Iterator<T> iterator() {
        return nonNull(value) ? List.of(value).iterator() : Collections.emptyIterator();
    }

    @Override
    public boolean isResolvable() {
        return nonNull(value);
    }
}
