package com.butterfly.util;

/**
 * Utilty class that represents a tuple
 */
public class Tuple<E1, E2> {

    private final E1 element1;
    private final E2 element2;

    public Tuple(E1 element1, E2 element2) {
        this.element1 = element1;
        this.element2 = element2;
    }

    public E1 getElement1() {
        return element1;
    }

    public E2 getElement2() {
        return element2;
    }

    public static <E1, E2> Tuple<E1, E2> of(E1 e1, E2 e2) {
        return new Tuple<>(e1, e2);
    }
}
