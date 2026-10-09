package com.wp;
import org.springframework.beans.factory.annotation.*; import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*; import java.time.temporal.ChronoUnit; import java.util.*; import java.util.stream.*;

@Service
@Transactional
public class WarrantyService {
 @Autowired ProductRepo products; @Autowired PolicyRepo policies; @Autowired WarrantyRepo warranties; @Autowired RequestRepo requests;
 @Autowired RepairRepo repairs; @Autowired ReplacementRepo replacements; @Autowired AuditRepo audits; @Autowired CustomerRepo customers; @Autowired UserRepo users; @Autowired NotificationService notifications;
 @Value("${sla.hours}") long slaHours;
 static final List<String> FLOW = List.of("REGISTERED","ASSIGNED","UNDER_REPAIR","RESOLVED","CLOSED");

 public void audit(String e,Long id,String a,String d){ Audit x=new Audit(); x.entity=e; x.entityId=id; x.action=a; x.detail=d; audits.save(x); }
 static ResponseStatusException bad(String m){ return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }

 public Product registerProduct(Product p){
  if(p.serialNumber==null||p.serialNumber.isBlank()) throw bad("Serial number required");
  if(products.findBySerialNumber(p.serialNumber).isPresent()) throw bad("Product already registered: "+p.serialNumber);
  if(p.customerId==null||!customers.existsById(p.customerId)) throw bad("Valid customer required");
  Product s=products.save(p); audit("PRODUCT",s.id,"REGISTERED",s.serialNumber);
  notifyCustomerForProduct(s, "PRODUCT", "Product registered", "Product "+s.serialNumber+" was registered successfully.");
  return s; }

 // Picks model-specific policy first, then category policy
 public Warranty activateWarranty(String serial){
  Product p=products.findBySerialNumber(serial).orElseThrow(()->bad("Product not registered"));
  if(warranties.findBySerialNumber(serial).isPresent()) throw bad("Warranty already activated");
  List<Policy> act=policies.findAll().stream().filter(x->x.active).toList();
  Policy pol=act.stream().filter(x->p.model.equalsIgnoreCase(x.model)).findFirst()
    .orElse(act.stream().filter(x->(x.model==null||x.model.isBlank())&&p.category!=null&&p.category.equalsIgnoreCase(x.category)).findFirst()
    .orElseThrow(()->bad("No applicable policy for this model/category")));
  Warranty w=new Warranty(); w.serialNumber=serial; w.policyId=pol.id; w.activationDate=p.purchaseDate;
  w.expiryDate=p.purchaseDate.plusMonths(pol.durationMonths); w=warranties.save(w);
  audit("WARRANTY",w.id,"ACTIVATED",serial+" until "+w.expiryDate);
  notifyCustomerForSerial(serial, "WARRANTY", "Warranty activated", "Warranty for "+serial+" is active until "+w.expiryDate+".");
  return w; }

 public Map<String,Object> status(Warranty w){ Map<String,Object> m=new LinkedHashMap<>();
  m.put("id",w.id); m.put("serialNumber",w.serialNumber); m.put("policyId",w.policyId); m.put("activationDate",w.activationDate); m.put("expiryDate",w.expiryDate);
  m.put("status",LocalDate.now().isAfter(w.expiryDate)?"EXPIRED":"ACTIVE"); return m; }

 /** CORE ENGINE: returns eligibility, path and reason */
 public ServiceRequest validate(ServiceRequest r){
  List<String> why=new ArrayList<>();
  Optional<Product> p=products.findBySerialNumber(r.serialNumber);
  if(p.isEmpty()){ r.eligibility="REJECTED"; r.path="NONE"; r.reason="Product not registered"; return r; }
  Optional<Warranty> w=warranties.findBySerialNumber(r.serialNumber);
  if(w.isEmpty()){ r.eligibility="NOT_COVERED"; r.path="PAID_REPAIR"; r.reason="No warranty activated"; return r; }
  Policy pol=policies.findById(w.get().policyId).orElse(null);
  if(pol==null||!pol.active){ r.eligibility="NOT_COVERED"; r.path="PAID_REPAIR"; r.reason="Policy inactive/missing"; return r; }
  if(LocalDate.now().isAfter(w.get().expiryDate)){ r.eligibility="NOT_COVERED"; r.path="PAID_REPAIR"; r.reason="Warranty expired on "+w.get().expiryDate; return r; }
  if(pol.exclusions!=null&&r.issue!=null) for(String k:pol.exclusions.split(",")){ k=k.trim().toLowerCase();
    if(!k.isEmpty()&&r.issue.toLowerCase().contains(k)){ r.eligibility="NOT_COVERED"; r.path="PAID_REPAIR"; r.reason="Excluded by policy: '"+k+"'"; return r; } }
  long prevRepairs=requests.findBySerialNumber(r.serialNumber).stream().filter(x->!Objects.equals(x.id,r.id)&&"COVERED".equals(x.eligibility)&&x.resolvedAt!=null).count();
  if(!replacements.findByOriginalSerial(r.serialNumber).isEmpty()){ r.eligibility="COVERED"; r.path="REPAIR"; r.reason="Covered; unit was previously replaced"; return r; }
  r.eligibility="COVERED";
  if(prevRepairs>=pol.replaceAfterRepairs){ r.path="REPLACE"; r.reason="Covered; "+prevRepairs+" prior repairs reached replacement threshold "+pol.replaceAfterRepairs; }
  else { r.path="REPAIR"; r.reason="Covered under "+pol.name+" ("+prevRepairs+" prior repairs)"; }
  return r; }

 public ServiceRequest raise(ServiceRequest r){
  r.status="REGISTERED"; r.createdAt=LocalDateTime.now(); r.id=null; validate(r); r=requests.save(r);
  audit("REQUEST",r.id,"VALIDATED",r.eligibility+"/"+r.path+": "+r.reason);
  notifications.sendToRole("ADMIN", "SERVICE_REQUEST", "New service request", "Request #"+r.id+" was created for "+r.serialNumber+".");
  notifyCustomerForSerial(r.serialNumber, "SERVICE_REQUEST", "Service request created", "Your request #"+r.id+" has been registered and is now " + r.status + ".");
  return r; }

 public ServiceRequest assign(Long id,String tech){ ServiceRequest r=req(id); r.assignedTo=tech; r.status="ASSIGNED"; audit("REQUEST",id,"ASSIGNED",tech);
  users.findByNameIgnoreCaseAndRoleIgnoreCase(tech,"TECHNICIAN").ifPresent(u -> notifications.sendToUser(u.id,"ASSIGNMENT","New job assigned","Service request #"+id+" has been assigned to you."));
  notifyCustomerForSerial(r.serialNumber,"ASSIGNMENT","Technician assigned","A technician has been assigned to request #"+id+".");
  return requests.save(r); }

 public ServiceRequest changeStatus(Long id,String st){ ServiceRequest r=req(id);
  if(!FLOW.contains(st)||FLOW.indexOf(st)<FLOW.indexOf(r.status)) throw bad("Invalid transition "+r.status+" -> "+st);
  audit("REQUEST",id,"STATUS",r.status+" -> "+st); r.status=st;
  if(st.equals("RESOLVED")) r.resolvedAt=LocalDateTime.now();
  notifyCustomerForSerial(r.serialNumber,"STATUS","Request status updated","Request #"+id+" is now " + st.replace('_',' ') + ".");
  return requests.save(r); }

 public Repair repair(Repair rp){ ServiceRequest r=req(rp.requestId); rp.completedAt=LocalDateTime.now(); rp=repairs.save(rp);
  audit("REPAIR",rp.id,"RECORDED","req "+r.id+": "+rp.action); notifyCustomerForSerial(r.serialNumber,"REPAIR","Repair completed","Repair work for request #"+r.id+" has been recorded."); changeStatus(r.id,"RESOLVED"); return rp; }

 public Replacement replace(Replacement x){ ServiceRequest r=req(x.requestId);
  if(!"REPLACE".equals(r.path)&&!"COVERED".equals(r.eligibility)) throw bad("Request not eligible for replacement");
  if(x.replacementSerial==null||products.findBySerialNumber(x.replacementSerial).isEmpty()) throw bad("Replacement serial must be a registered product");
  x.originalSerial=r.serialNumber; x=replacements.save(x); audit("REPLACEMENT",x.id,"CREATED",x.originalSerial+" -> "+x.replacementSerial);
  notifyCustomerForSerial(r.serialNumber,"REPLACEMENT","Replacement processed","Replacement for request #"+r.id+" is now recorded.");
  changeStatus(r.id,"RESOLVED"); return x; }

 ServiceRequest req(Long id){ return requests.findById(id).orElseThrow(()->bad("Request not found")); }

 @Scheduled(fixedRate=60000) public void escalate(){
  for(ServiceRequest r:requests.findAll()){
   if(r.escalated||r.status.equals("RESOLVED")||r.status.equals("CLOSED")) continue;
   if(ChronoUnit.HOURS.between(r.createdAt,LocalDateTime.now())>=slaHours){ r.escalated=true; requests.save(r);
    audit("REQUEST",r.id,"ESCALATED","SLA of "+slaHours+"h exceeded; routed to supervisor");
    notifications.sendToRole("ADMIN","ESCALATION","SLA escalation","Request #"+r.id+" exceeded the "+slaHours+"h SLA.");
    notifyCustomerForSerial(r.serialNumber,"ESCALATION","Service delay alert","Request #"+r.id+" needs attention because it exceeded the expected SLA.");
   } } }

 private void notifyCustomerForSerial(String serial, String type, String title, String message){
  products.findBySerialNumber(serial).ifPresent(p -> {
    if(p.customerId!=null) users.findAll().stream().filter(u -> "CUSTOMER".equalsIgnoreCase(u.role) && Objects.equals(u.customerId,p.customerId)).findFirst().ifPresent(u -> notifications.sendToUser(u.id,type,title,message));
  });
 }

 private void notifyCustomerForProduct(Product p, String type, String title, String message){
  if(p.customerId!=null) users.findAll().stream().filter(u -> "CUSTOMER".equalsIgnoreCase(u.role) && Objects.equals(u.customerId,p.customerId)).findFirst().ifPresent(u -> notifications.sendToUser(u.id,type,title,message));
 }

 public Map<String,Object> analytics(){ List<ServiceRequest> all=requests.findAll(); Map<String,Object> m=new LinkedHashMap<>();
  Map<String,String> modelOf=products.findAll().stream().collect(Collectors.toMap(p->p.serialNumber,p->p.model,(a,b)->a));
  m.put("totalRequests",all.size());
  m.put("byStatus",all.stream().collect(Collectors.groupingBy(x->x.status,Collectors.counting())));
  m.put("byEligibility",all.stream().filter(x->x.eligibility!=null).collect(Collectors.groupingBy(x->x.eligibility,Collectors.counting())));
  m.put("escalated",all.stream().filter(x->x.escalated).count());
  m.put("replacementRatePct",all.isEmpty()?0:Math.round(replacements.count()*1000.0/all.size())/10.0);
  m.put("avgTurnaroundHours",Math.round(all.stream().filter(x->x.resolvedAt!=null).mapToLong(x->ChronoUnit.MINUTES.between(x.createdAt,x.resolvedAt)).average().orElse(0)/6)/10.0);
  Map<String,Long> byModel=all.stream().collect(Collectors.groupingBy(x->modelOf.getOrDefault(x.serialNumber,"Unknown"),Collectors.counting()));
  m.put("requestsByModel",byModel);
  m.put("recurringIssueModels",byModel.entrySet().stream().filter(e->e.getValue()>=3).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue)));
  return m; }
}
