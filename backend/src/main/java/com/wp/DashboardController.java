package com.wp;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserRepo users;
    private final CustomerRepo customers;
    private final ProductRepo products;
    private final WarrantyRepo warranties;
    private final RequestRepo requests;
    private final RepairRepo repairs;

    public DashboardController(UserRepo users, CustomerRepo customers, ProductRepo products,
                               WarrantyRepo warranties, RequestRepo requests, RepairRepo repairs) {
        this.users = users; this.customers = customers; this.products = products;
        this.warranties = warranties; this.requests = requests; this.repairs = repairs;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(Authentication auth) {
        AppUser user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        return switch (user.role.toUpperCase()) {
            case "TECHNICIAN" -> technician(user);
            case "CUSTOMER" -> customer(user);
            default -> admin();
        };
    }

    private Map<String, Object> admin() {
        List<ServiceRequest> all = requests.findAll();
        long activeWarranties = warranties.findAll().stream().filter(w -> !LocalDate.now().isAfter(w.expiryDate)).count();
        long open = all.stream().filter(r -> !List.of("RESOLVED", "CLOSED").contains(r.status)).count();
        return map("role", "ADMIN", "customers", customers.count(), "products", products.count(),
                "activeWarranties", activeWarranties, "openRequests", open, "escalated", all.stream().filter(r -> r.escalated).count(),
                "recentRequests", all.stream().sorted(Comparator.comparing(r -> r.createdAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(6).toList());
    }

    private Map<String, Object> technician(AppUser user) {
        List<ServiceRequest> assigned = requests.findByAssignedToIgnoreCase(user.name);
        long open = assigned.stream().filter(r -> !List.of("RESOLVED", "CLOSED").contains(r.status)).count();
        long escalated = assigned.stream().filter(r -> r.escalated).count();
        long resolved = assigned.stream().filter(r -> "RESOLVED".equals(r.status) || "CLOSED".equals(r.status)).count();
        return map("role", "TECHNICIAN", "assigned", assigned.size(), "open", open, "resolved", resolved,
                "escalated", escalated, "recentRequests", assigned.stream().sorted(Comparator.comparing(r -> r.createdAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).toList());
    }

    private Map<String, Object> customer(AppUser user) {
        List<Product> owned = user.customerId == null ? List.of() : products.findByCustomerId(user.customerId);
        Set<String> serials = owned.stream().map(p -> p.serialNumber).collect(Collectors.toSet());
        List<ServiceRequest> ownRequests = requests.findAll().stream().filter(r -> serials.contains(r.serialNumber)).toList();
        List<Warranty> ownWarranties = warranties.findAll().stream().filter(w -> serials.contains(w.serialNumber)).toList();
        long active = ownWarranties.stream().filter(w -> !LocalDate.now().isAfter(w.expiryDate)).count();
        long open = ownRequests.stream().filter(r -> !List.of("RESOLVED", "CLOSED").contains(r.status)).count();
        return map("role", "CUSTOMER", "products", owned.size(), "activeWarranties", active, "openRequests", open,
                "resolvedRequests", ownRequests.stream().filter(r -> "RESOLVED".equals(r.status) || "CLOSED".equals(r.status)).count(),
                "recentRequests", ownRequests.stream().sorted(Comparator.comparing(r -> r.createdAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).toList());
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) m.put(String.valueOf(values[i]), values[i + 1]);
        return m;
    }
}
