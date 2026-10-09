package com.wp;

import org.springframework.stereotype.Repository;

@Repository
public class PolicyRepo extends JpaStore<Policy> {
    public PolicyRepo() { super(Policy.class); }
}
