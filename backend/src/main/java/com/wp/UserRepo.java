package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepo extends JpaStore<AppUser> {
    public UserRepo() { super(AppUser.class); }

    public Optional<AppUser> findByEmailIgnoreCase(String email) {
        return em.createQuery("select u from AppUser u where lower(u.email) = lower(:email)", AppUser.class)
                .setParameter("email", email)
                .getResultStream().findFirst();
    }

    public List<AppUser> findByRoleIgnoreCase(String role) {
        return em.createQuery("select u from AppUser u where lower(u.role) = lower(:role) order by u.name", AppUser.class)
                .setParameter("role", role)
                .getResultList();
    }

    public Optional<AppUser> findByNameIgnoreCaseAndRoleIgnoreCase(String name, String role) {
        return em.createQuery("select u from AppUser u where lower(u.name) = lower(:name) and lower(u.role) = lower(:role)", AppUser.class)
                .setParameter("name", name)
                .setParameter("role", role)
                .getResultStream().findFirst();
    }
}
