package com.example.util;
import javax.persistence.EntityManager;
import java.util.function.Supplier;
public final class Transactions {
    private Transactions() {}
    public static <T> T execute(EntityManager em, Supplier<T> work) {
        if (em.getTransaction().isActive()) throw new IllegalStateException("Transaction deja active");
        em.getTransaction().begin();
        try {
            T result = work.get();
            em.getTransaction().commit();
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }
}
