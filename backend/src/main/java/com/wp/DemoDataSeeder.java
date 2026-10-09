package com.wp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Order(1)
public class DemoDataSeeder implements CommandLineRunner {
    private final CustomerRepo customers;
    private final ProductRepo products;
    private final PolicyRepo policies;
    private final WarrantyRepo warranties;
    private final RequestRepo requests;
    private final RepairRepo repairs;
    private final ReplacementRepo replacements;
    private final AuditRepo audits;
    private final UserRepo users;
    private final NotificationRepo notificationRepo;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(CustomerRepo customers, ProductRepo products, PolicyRepo policies,
                          WarrantyRepo warranties, RequestRepo requests, RepairRepo repairs,
                          ReplacementRepo replacements, AuditRepo audits, UserRepo users,
                          NotificationRepo notificationRepo, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.products = products;
        this.policies = policies;
        this.warranties = warranties;
        this.requests = requests;
        this.repairs = repairs;
        this.replacements = replacements;
        this.audits = audits;
        this.users = users;
        this.notificationRepo = notificationRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Customer demo = customer("Demo Customer", "customer@warrantyos.com", "+91 90000 00001");
        ensureAdmin();
        ensureTechnician("tech@warrantyos.com", "Demo Technician", "Tech@123");
        ensureTechnician("tech2@warrantyos.com", "Rohit Technician", "Tech2@123");
        ensureCustomerUser(demo);

        Customer priya = customer("Priya Sharma", "priya.sharma@example.com", "+91 90000 00002");
        Customer arjun = customer("Arjun Rao", "arjun.rao@example.com", "+91 90000 00003");
        ensureCustomerUser(priya);
        ensureCustomerUser(arjun);

        // Keep a pre-existing business database intact. Login users above are still ensured.
        if (products.count() > 0 || requests.count() > 0 || policies.count() > 0) return;

        Policy samsung = policy("Samsung Home Electronics Plus", "TV", "Samsung QLED", 24, "physical damage,liquid damage");
        Policy whirlpool = policy("Whirlpool Care 18", "AC", "Whirlpool Split AC", 18, "physical damage,voltage damage");
        Policy dell = policy("Dell Business Protect", "Laptop", "Dell Latitude 5440", 24, "liquid damage,screen crack");

        Product p1 = product("TV-10001", "Samsung QLED", "TV", demo.id, LocalDate.now().minusMonths(8));
        Product p2 = product("TV-10002", "Samsung QLED", "TV", demo.id, LocalDate.now().minusMonths(14));
        Product p3 = product("TV-10003", "Samsung QLED", "TV", arjun.id, LocalDate.now().minusMonths(2));
        Product p4 = product("AC-20001", "Whirlpool Split AC", "AC", priya.id, LocalDate.now().minusMonths(5));
        Product p5 = product("AC-20002", "Whirlpool Split AC", "AC", arjun.id, LocalDate.now().minusMonths(11));
        Product p6 = product("LAP-30001", "Dell Latitude 5440", "Laptop", priya.id, LocalDate.now().minusMonths(4));
        Product p7 = product("LAP-30002", "Dell Latitude 5440", "Laptop", arjun.id, LocalDate.now().minusMonths(20));

        warranty(p1, samsung); warranty(p2, samsung); warranty(p3, samsung);
        warranty(p4, whirlpool); warranty(p5, whirlpool);
        warranty(p6, dell); warranty(p7, dell);

        String tech1 = users.findByEmailIgnoreCase("tech@warrantyos.com").map(u -> u.name).orElse("Demo Technician");
        String tech2 = users.findByEmailIgnoreCase("tech2@warrantyos.com").map(u -> u.name).orElse("Rohit Technician");

        ServiceRequest r1 = request(p1.serialNumber, "Display flickering intermittently", "RESOLVED", tech1, "COVERED", "REPAIR", LocalDateTime.now().minusDays(7));
        ServiceRequest r2 = request(p2.serialNumber, "TV not powering on after standby", "UNDER_REPAIR", tech1, "COVERED", "REPAIR", LocalDateTime.now().minusHours(19));
        ServiceRequest r3 = request(p4.serialNumber, "AC is not cooling the room", "REGISTERED", null, "COVERED", "REPAIR", LocalDateTime.now().minusHours(5));
        ServiceRequest r4 = request(p5.serialNumber, "Indoor unit making unusual noise", "ASSIGNED", tech2, "COVERED", "REPAIR", LocalDateTime.now().minusHours(12));
        ServiceRequest r5 = request(p6.serialNumber, "Battery drains unusually fast", "RESOLVED", tech2, "COVERED", "REPAIR", LocalDateTime.now().minusDays(10));
        r5.resolvedAt = r5.createdAt.plusHours(5); requests.save(r5);
        ServiceRequest r6 = request(p7.serialNumber, "Keyboard and trackpad intermittently stop responding", "CLOSED", tech1, "COVERED", "REPLACE", LocalDateTime.now().minusDays(16));
        r6.resolvedAt = r6.createdAt.plusHours(10); requests.save(r6);
        ServiceRequest r7 = request(p1.serialNumber, "Remote control connectivity issue", "RESOLVED", tech2, "COVERED", "REPAIR", LocalDateTime.now().minusDays(3));
        r7.resolvedAt = r7.createdAt.plusHours(8); requests.save(r7);
        ServiceRequest r8 = request(p5.serialNumber, "AC cooling has stopped completely", "UNDER_REPAIR", tech2, "COVERED", "REPAIR", LocalDateTime.now().minusHours(60));
        r8.escalated = true; requests.save(r8);

        repair(r1, "Display control board fault", "Re-seated connector and replaced board", "Display control board", 1800);
        repair(r5, "Battery health below service threshold", "Replaced battery pack", "Battery pack", 3200);
        repair(r7, "Bluetooth module instability", "Updated firmware and replaced module", "Bluetooth module", 1400);

        Replacement rep = new Replacement();
        rep.requestId = r6.id;
        rep.originalSerial = p7.serialNumber;
        rep.replacementSerial = p3.serialNumber;
        rep.reason = "Repeated failures reached replacement threshold";
        rep.replacedOn = LocalDate.now().minusDays(15);
        replacements.save(rep);

        audit("DATASET", demo.id, "SEEDED", "WarrantyOS demo operational dataset");
        audit("REQUEST", r8.id, "ESCALATED", "SLA demonstration case");
        notification("admin@warrantyos.com", "ESCALATION", "SLA watch", "Request #" + r8.id + " is beyond the 48h service window.");
        notification("tech@warrantyos.com", "ASSIGNMENT", "Priority job", "Request #" + r2.id + " is assigned and awaiting repair action.");
        notification("customer@warrantyos.com", "STATUS", "Service update", "Request #" + r2.id + " is currently under repair.");
    }

    private void ensureAdmin() {
        if (users.findByEmailIgnoreCase("admin@warrantyos.com").isPresent()) return;
        AppUser user = new AppUser();
        user.name = "Warranty Administrator";
        user.email = "admin@warrantyos.com";
        user.passwordHash = passwordEncoder.encode("Admin@123");
        user.role = "ADMIN";
        users.save(user);
    }

    private void ensureTechnician(String email, String name, String password) {
        if (users.findByEmailIgnoreCase(email).isPresent()) return;
        AppUser user = new AppUser();
        user.name = name;
        user.email = email;
        user.passwordHash = passwordEncoder.encode(password);
        user.role = "TECHNICIAN";
        users.save(user);
    }

    private Customer customer(String name, String email, String phone) {
        return customers.findAll().stream()
                .filter(c -> email.equalsIgnoreCase(c.email))
                .findFirst()
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.name = name; c.email = email; c.phone = phone;
                    return customers.save(c);
                });
    }

    private Policy policy(String name, String category, String model, int months, String exclusions) {
        Policy p = new Policy(); p.name = name; p.category = category; p.model = model;
        p.durationMonths = months; p.exclusions = exclusions; p.replaceAfterRepairs = 3; p.active = true;
        return policies.save(p);
    }

    private Product product(String serial, String model, String category, Long customerId, LocalDate purchaseDate) {
        Product p = new Product(); p.serialNumber = serial; p.model = model; p.category = category;
        p.customerId = customerId; p.purchaseDate = purchaseDate;
        return products.save(p);
    }

    private void warranty(Product product, Policy policy) {
        Warranty w = new Warranty(); w.serialNumber = product.serialNumber; w.policyId = policy.id;
        w.activationDate = product.purchaseDate; w.expiryDate = product.purchaseDate.plusMonths(policy.durationMonths);
        warranties.save(w);
    }

    private ServiceRequest request(String serial, String issue, String status, String tech,
                                   String eligibility, String path, LocalDateTime created) {
        ServiceRequest r = new ServiceRequest();
        r.serialNumber = serial; r.issue = issue; r.status = status; r.assignedTo = tech;
        r.eligibility = eligibility; r.path = path; r.createdAt = created;
        r.reason = "Covered under active service policy";
        return requests.save(r);
    }

    private void repair(ServiceRequest r, String diagnosis, String action, String parts, double labour) {
        Repair x = new Repair(); x.requestId = r.id; x.diagnosis = diagnosis; x.action = action;
        x.parts = parts; x.laborCost = labour; x.completedAt = r.resolvedAt != null ? r.resolvedAt : r.createdAt.plusHours(6);
        repairs.save(x);
    }

    private void ensureCustomerUser(Customer customer) {
        users.findByEmailIgnoreCase(customer.email).ifPresent(existing -> {
            if (existing.customerId == null || !existing.customerId.equals(customer.id)) {
                existing.customerId = customer.id;
                users.save(existing);
            }
        });
        if (users.findByEmailIgnoreCase(customer.email).isPresent()) return;
        AppUser u = new AppUser();
        u.name = customer.name; u.email = customer.email;
        u.passwordHash = passwordEncoder.encode("Customer@123");
        u.role = "CUSTOMER"; u.customerId = customer.id;
        users.save(u);
    }

    private void audit(String entity, Long entityId, String action, String detail) {
        Audit a = new Audit(); a.entity = entity; a.entityId = entityId; a.action = action;
        a.detail = detail; a.at = LocalDateTime.now(); audits.save(a);
    }

    private void notification(String email, String type, String title, String message) {
        users.findByEmailIgnoreCase(email).ifPresent(u -> {
            Notification n = new Notification(); n.recipientUserId = u.id; n.type = type;
            n.title = title; n.message = message; n.readFlag = false; notificationRepo.save(n);
        });
    }
}
