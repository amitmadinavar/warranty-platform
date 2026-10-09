package com.wp;

import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepo extends JpaStore<Customer> {
    public CustomerRepo() { super(Customer.class); }
}
