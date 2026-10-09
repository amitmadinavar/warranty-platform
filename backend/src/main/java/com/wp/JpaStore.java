package com.wp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Small, explicit JPA data-access base class.
 * This project deliberately does not depend on Spring Data repository
 * interface discovery. Each concrete repository is a normal Spring bean.
 */
@Transactional(readOnly = true)
public abstract class JpaStore<T> {
    @PersistenceContext
    protected EntityManager em;

    private final Class<T> entityType;

    protected JpaStore(Class<T> entityType) {
        this.entityType = entityType;
    }

    public List<T> findAll() {
        return em.createQuery("select e from " + entityType.getSimpleName() + " e", entityType)
                .getResultList();
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(id == null ? null : em.find(entityType, id));
    }

    public long count() {
        return em.createQuery("select count(e) from " + entityType.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Transactional
    public T save(T entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        try {
            var field = entityType.getField("id");
            Long id = (Long) field.get(entity);
            if (id == null) {
                em.persist(entity);
                em.flush();
                return entity;
            }
            return em.merge(entity);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Entity must expose a public Long id field: " + entityType.getName(), ex);
        }
    }
}
