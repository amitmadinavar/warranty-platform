package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepo extends JpaStore<Product> {
    public ProductRepo() { super(Product.class); }

    public Optional<Product> findBySerialNumber(String serialNumber) {
        return em.createQuery("select p from Product p where lower(p.serialNumber) = lower(:serial)", Product.class)
                .setParameter("serial", serialNumber)
                .getResultStream().findFirst();
    }

    public List<Product> findByCustomerId(Long customerId) {
        return em.createQuery("select p from Product p where p.customerId = :customerId order by p.id desc", Product.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }
}
