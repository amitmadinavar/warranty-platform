package com.wp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "WarrantyOS", "time", java.time.OffsetDateTime.now().toString());
    }
    private final WarrantyService s;
    private final CustomerRepo customers;
    private final ProductRepo products;
    private final PolicyRepo policies;
    private final WarrantyRepo warranties;
    private final RequestRepo requests;
    private final RepairRepo repairs;
    private final ReplacementRepo replacements;
    private final AuditRepo audits;
    private final UserRepo users;

    public ApiController(WarrantyService s, CustomerRepo customers, ProductRepo products, PolicyRepo policies,
                         WarrantyRepo warranties, RequestRepo requests, RepairRepo repairs,
                         ReplacementRepo replacements, AuditRepo audits, UserRepo users) {
        this.s=s; this.customers=customers; this.products=products; this.policies=policies; this.warranties=warranties;
        this.requests=requests; this.repairs=repairs; this.replacements=replacements; this.audits=audits; this.users=users;
    }

    @GetMapping("/customers") @PreAuthorize("hasRole('ADMIN')")
    List<Customer> customers(){ return customers.findAll(); }
    @PostMapping("/customers") @PreAuthorize("hasRole('ADMIN')")
    Customer addCustomer(@RequestBody Customer c){ Customer x=customers.save(c); s.audit("CUSTOMER",x.id,"CREATED",x.name); return x; }

    @GetMapping("/products") @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    List<Product> products(Authentication auth){
        AppUser u=current(auth);
        return "CUSTOMER".equalsIgnoreCase(u.role) ? products.findByCustomerId(u.customerId) : products.findAll();
    }
    @PostMapping("/products") @PreAuthorize("hasRole('ADMIN')")
    Product addProduct(@RequestBody Product p){ return s.registerProduct(p); }

    @GetMapping("/policies") @PreAuthorize("hasRole('ADMIN')") List<Policy> policies(){ return policies.findAll(); }
    @PostMapping("/policies") @PreAuthorize("hasRole('ADMIN')") Policy addPolicy(@RequestBody Policy p){ Policy x=policies.save(p); s.audit("POLICY",x.id,"SAVED",x.name); return x; }

    @GetMapping("/warranties") @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    List<Map<String,Object>> warranties(Authentication auth){
        AppUser u=current(auth);
        List<Warranty> ws = "CUSTOMER".equalsIgnoreCase(u.role)
                ? warranties.findAll().stream().filter(w -> products.findByCustomerId(u.customerId).stream().anyMatch(p -> p.serialNumber.equalsIgnoreCase(w.serialNumber))).toList()
                : warranties.findAll();
        return ws.stream().map(s::status).toList();
    }
    @PostMapping("/warranties/activate/{serial}") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> activate(@PathVariable String serial){ return s.status(s.activateWarranty(serial)); }

    @GetMapping("/requests") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN','CUSTOMER')")
    List<ServiceRequest> requests(Authentication auth){
        AppUser u=current(auth);
        if("TECHNICIAN".equalsIgnoreCase(u.role)) return requests.findByAssignedToIgnoreCase(u.name);
        if("CUSTOMER".equalsIgnoreCase(u.role)) {
            Set<String> serials = new HashSet<>(products.findByCustomerId(u.customerId).stream().map(p -> p.serialNumber).toList());
            return requests.findAll().stream().filter(r -> serials.contains(r.serialNumber)).toList();
        }
        return requests.findAll();
    }

    @PostMapping("/requests") @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    ServiceRequest raise(@RequestBody ServiceRequest r, Authentication auth){
        AppUser u=current(auth);
        if("CUSTOMER".equalsIgnoreCase(u.role)) {
            Product p=products.findBySerialNumber(r.serialNumber).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Product not found"));
            if(!Objects.equals(p.customerId,u.customerId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Product does not belong to this customer");
        }
        return s.raise(r);
    }

    @PutMapping("/requests/{id}/assign") @PreAuthorize("hasRole('ADMIN')")
    ServiceRequest assign(@PathVariable Long id,@RequestParam String tech){ return s.assign(id,tech); }
    @PutMapping("/requests/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    ServiceRequest status(@PathVariable Long id,@RequestParam String status, Authentication auth){
        AppUser u=current(auth);
        if("TECHNICIAN".equalsIgnoreCase(u.role)) {
            ServiceRequest r=requests.findById(id).orElseThrow();
            if(r.assignedTo==null || !r.assignedTo.equalsIgnoreCase(u.name)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Request is not assigned to this technician");
        }
        return s.changeStatus(id,status);
    }

    @PostMapping("/repairs") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    Repair repair(@RequestBody Repair r, Authentication auth){
        if("TECHNICIAN".equalsIgnoreCase(current(auth).role)) {
            ServiceRequest req=requests.findById(r.requestId).orElseThrow();
            if(req.assignedTo==null || !req.assignedTo.equalsIgnoreCase(current(auth).name)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Request is not assigned to this technician");
        }
        return s.repair(r);
    }
    @GetMapping("/repairs") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    List<Repair> repairs(Authentication auth){
        if("TECHNICIAN".equalsIgnoreCase(current(auth).role)) {
            Set<Long> ids=requests.findByAssignedToIgnoreCase(current(auth).name).stream().map(r->r.id).collect(java.util.stream.Collectors.toSet());
            return repairs.findAll().stream().filter(r -> ids.contains(r.requestId)).toList();
        }
        return repairs.findAll();
    }

    @PostMapping("/replacements") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    Replacement replace(@RequestBody Replacement r){ return s.replace(r); }
    @GetMapping("/replacements") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    List<Replacement> replacements(){ return replacements.findAll(); }

    @GetMapping("/history/{serial}") @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN','CUSTOMER')")
    Map<String,Object> history(@PathVariable String serial, Authentication auth){
        AppUser u=current(auth);
        if("CUSTOMER".equalsIgnoreCase(u.role)) {
            Product p=products.findBySerialNumber(serial).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Product not found"));
            if(!Objects.equals(p.customerId,u.customerId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Product does not belong to this customer");
        }
        Map<String,Object> m=new LinkedHashMap<>();
        List<ServiceRequest> rs=requests.findBySerialNumber(serial); m.put("requests",rs);
        m.put("repairs",rs.stream().flatMap(r->repairs.findByRequestId(r.id).stream()).toList()); m.put("replacements",replacements.findByOriginalSerial(serial)); return m;
    }

    @GetMapping("/analytics") @PreAuthorize("hasRole('ADMIN')") Map<String,Object> analytics(){ return s.analytics(); }
    @GetMapping("/audit") @PreAuthorize("hasRole('ADMIN')") List<Audit> audit(){ return audits.findAllByOrderByAtDesc(); }
    @GetMapping("/escalations") @PreAuthorize("hasRole('ADMIN')") List<ServiceRequest> esc(){ return requests.findAll().stream().filter(r->r.escalated).toList(); }
    @GetMapping("/users/technicians") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> technicians(){
        return users.findByRoleIgnoreCase("TECHNICIAN").stream().map(u -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", u.id);
            m.put("name", u.name);
            m.put("email", u.email);
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    private AppUser current(Authentication auth){ return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(); }
}
