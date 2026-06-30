package com.delenicode.carcare.seeder;

import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.document.model.DocumentType;
import com.delenicode.carcare.document.model.ServiceDocument;
import com.delenicode.carcare.document.repository.ServiceDocumentRepository;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.model.OfferPart;
import com.delenicode.carcare.offer.model.OfferStatus;
import com.delenicode.carcare.offer.repository.OfferRepository;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.servicerecord.repository.ServiceRecordRepository;
import com.delenicode.carcare.user.model.AppUser;
import com.delenicode.carcare.user.model.Employee;
import com.delenicode.carcare.user.model.Role;
import com.delenicode.carcare.user.repository.AppUserRepository;
import com.delenicode.carcare.user.repository.EmployeeRepository;
import com.delenicode.carcare.user.repository.RoleRepository;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the database with realistic test data when app.seeder.enabled=true.
 * Safe to run on a fresh database only — the idempotency guard skips the entire run if
 * the sentinel customer email is already present.
 * <p>
 * Business scenarios covered:
 * - 7 active customers + 1 soft-deleted customer
 * - 11 vehicles spread across customers
 * - 3 staff users (manager + 2 mechanics) with linked employee profiles
 * - 8 appointments: past (scheduled/cancelled) and upcoming
 * - 22 service records arranged so some customers hit the loyalty threshold (≥5)
 * - 6 offers in all statuses, loyalty discount applied where applicable
 * - 4 service documents (placeholder metadata, no real files)
 */
@Component
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements ApplicationRunner {

  private static final ZoneId SKOPJE = ZoneId.of("Europe/Skopje");
  private static final String SEED_SENTINEL_EMAIL = "aleksandar.petrov@seed.carcare.test";

  private final CustomerRepository customerRepository;
  private final VehicleRepository vehicleRepository;
  private final AppUserRepository userRepository;
  private final RoleRepository roleRepository;
  private final EmployeeRepository employeeRepository;
  private final AppointmentRepository appointmentRepository;
  private final ServiceRecordRepository serviceRecordRepository;
  private final OfferRepository offerRepository;
  private final ServiceDocumentRepository serviceDocumentRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (customerRepository.existsByEmail(SEED_SENTINEL_EMAIL)) {
      log.info("Seed data already present — skipping.");
      return;
    }
    log.info("Seeding test data...");

    List<AppUser> users = seedUsers();
    seedEmployees(users);
    List<Customer> customers = seedCustomers();
    List<Vehicle> vehicles = seedVehicles(customers);
    seedAppointments(customers, vehicles);
    List<ServiceRecord> docRecords = seedServiceRecords(customers, vehicles);
    seedOffers(customers, vehicles);
    seedDocuments(customers, docRecords);

    log.info("Test data seeded: 3 users, 3 employees, 8 customers, 11 vehicles, "
        + "8 appointments, 22 service records, 6 offers, 4 documents.");
  }

  // -------------------------------------------------------------------------
  // Users & employees
  // -------------------------------------------------------------------------

  private List<AppUser> seedUsers() {
    AppUser manager = createUser("manager@seed.carcare.test", "Марко Стојанов", "ROLE_MANAGER");
    AppUser emp1 = createUser("employee1@seed.carcare.test", "Бојан Ивановски", "ROLE_EMPLOYEE");
    AppUser emp2 = createUser("employee2@seed.carcare.test", "Соња Митровска", "ROLE_EMPLOYEE");
    return List.of(manager, emp1, emp2);
  }

  private AppUser createUser(String email, String fullName, String roleName) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
    AppUser user = new AppUser();
    user.setEmail(email);
    user.setFullName(fullName);
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setRoles(Set.of(role));
    return userRepository.save(user);
  }

  private void seedEmployees(List<AppUser> users) {
    employeeRepository.save(employee("EMP-001", "Сервисен менаџер", "+389 70 111 001", users.get(0)));
    employeeRepository.save(employee("EMP-002", "Автомеханичар", "+389 70 111 002", users.get(1)));
    employeeRepository.save(employee("EMP-003", "Автомеханичар", "+389 70 111 003", users.get(2)));
  }

  private Employee employee(String code, String jobTitle, String phone, AppUser user) {
    Employee e = new Employee();
    e.setEmployeeCode(code);
    e.setJobTitle(jobTitle);
    e.setPhone(phone);
    e.setUser(user);
    return e;
  }

  // -------------------------------------------------------------------------
  // Customers  (indices: 0=Petrov 1=Kostadinova 2=Nikolov 3=Todorovska
  //                       4=Gjorgjiev 5=Blazevska 6=Arsov 7=Cvetkovska[deleted])
  // -------------------------------------------------------------------------

  private List<Customer> seedCustomers() {
    Customer c1 = customer("Александар", "Петров",
        SEED_SENTINEL_EMAIL, "+389 70 201 001", "ул. Македонија 15, Скопје");
    Customer c2 = customer("Марија", "Костадинова",
        "marija.kostadinova@seed.carcare.test", "+389 71 202 002", "ул. Партизанска 22, Скопје");
    Customer c3 = customer("Стефан", "Николов",
        "stefan.nikolov@seed.carcare.test", "+389 72 203 003", "бул. Гоце Делчев 8, Скопје");
    Customer c4 = customer("Елена", "Тодоровска",
        "elena.todorovska@seed.carcare.test", "+389 70 204 004", "ул. Вардарска 5, Тетово");
    Customer c5 = customer("Димитар", "Ѓорѓиев",
        "dimitar.gjorgjiev@seed.carcare.test", "+389 71 205 005", "ул. Климент Охридски 33, Охрид");
    Customer c6 = customer("Катарина", "Блажевска",
        "katarina.blazevska@seed.carcare.test", "+389 72 206 006", "ул. Отец Пајсиј 12, Скопје");
    Customer c7 = customer("Бојан", "Арсов",
        "bojan.arsov@seed.carcare.test", "+389 70 207 007", "ул. 11 Октомври 18, Куманово");

    // Soft-deleted — tests the deleted=true filter paths
    Customer c8 = customer("Ивона", "Цветковска",
        "ivona.cvetkovska@seed.carcare.test", "+389 71 208 008", "ул. Лондонска 3, Скопје");
    c8.setDeleted(true);
    customerRepository.save(c8);

    return List.of(c1, c2, c3, c4, c5, c6, c7, c8);
  }

  private Customer customer(String firstName, String lastName, String email, String phone, String address) {
    Customer c = new Customer();
    c.setFirstName(firstName);
    c.setLastName(lastName);
    c.setFullName(firstName + " " + lastName);
    c.setEmail(email);
    c.setPhone(phone);
    c.setAddress(address);
    c.setDeleted(false);
    return customerRepository.save(c);
  }

  // -------------------------------------------------------------------------
  // Vehicles  (indices match return order)
  //  0=Toyota/Petrov   1=BMW/Petrov    2=Golf/Kostadinova
  //  3=Merc/Nikolov    4=Audi/Nikolov  5=Astra/Todorovska
  //  6=Focus/Gjorgjiev 7=Clio/Blazevska 8=Octavia/Blazevska
  //  9=i30/Arsov       10=Peugeot/Cvetkovska
  // -------------------------------------------------------------------------

  private List<Vehicle> seedVehicles(List<Customer> cs) {
    Vehicle v0 = vehicle(cs.get(0), "SK1234AB", "Toyota", "Corolla", 2019, "4T1BF1FK5GU234567", "Бензин", "1.8 VVT-i 132hp");
    Vehicle v1 = vehicle(cs.get(0), "SK5678CD", "BMW", "320d", 2021, "WBA3A9C55CF123456", "Дизел", "2.0 TDI 190hp");
    Vehicle v2 = vehicle(cs.get(1), "SK9012EF", "Volkswagen", "Golf", 2017, "WVWZZZ1JZXW987654", "Бензин", "1.4 TSI 125hp");
    Vehicle v3 = vehicle(cs.get(2), "SK3456GH", "Mercedes-Benz", "C220", 2020, "WDD2050031A345678", "Дизел", "2.2 CDI 194hp");
    Vehicle v4 = vehicle(cs.get(2), "SK7890IJ", "Audi", "A4", 2018, "WAUZZZ8KXBA012345", "Бензин", "2.0 TFSI 190hp");
    Vehicle v5 = vehicle(cs.get(3), "SK1122KL", "Opel", "Astra", 2016, "W0L0AHL0865123456", "Бензин", "1.6 ECOTEC 115hp");
    Vehicle v6 = vehicle(cs.get(4), "SK3344MN", "Ford", "Focus", 2022, "WF0AXXGCDANH12345", "Дизел", "1.5 EcoBoost 120hp");
    Vehicle v7 = vehicle(cs.get(5), "SK5566OP", "Renault", "Clio", 2015, "VF1BR1S0A54123456", "Бензин", "1.2 TCe 90hp");
    Vehicle v8 = vehicle(cs.get(5), "SK7788QR", "Skoda", "Octavia", 2019, "TMBEG7NE0K0234567", "Дизел", "2.0 TDI 150hp");
    Vehicle v9 = vehicle(cs.get(6), "SK9900ST", "Hyundai", "i30", 2020, "KMHH351BFKU012345", "Бензин", "1.4 MPI 100hp");
    Vehicle v10 = vehicle(cs.get(7), "SK1111UV", "Peugeot", "208", 2018, "VF3CCHMZ6HW123456", "Бензин", "1.2 PureTech 82hp");
    return List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);
  }

  private Vehicle vehicle(Customer owner, String plate, String make, String model,
                          int year, String vin, String fuelType, String engine) {
    Vehicle v = new Vehicle();
    v.setCustomer(owner);
    v.setPlateNumber(plate);
    v.setMake(make);
    v.setModel(model);
    v.setModelYear(year);
    v.setVin(vin);
    v.setFuelType(fuelType);
    v.setEngine(engine);
    return vehicleRepository.save(v);
  }

  // -------------------------------------------------------------------------
  // Appointments — past (SCHEDULED / CANCELLED) + upcoming (SCHEDULED)
  // -------------------------------------------------------------------------

  private void seedAppointments(List<Customer> cs, List<Vehicle> vs) {
    // Past — already occurred (still SCHEDULED since no COMPLETED status exists)
    appointment(cs.get(0), vs.get(0), skopje(2026, 5, 15, 9, 0), skopje(2026, 5, 15, 10, 0),
        "Редовен сервис", AppointmentStatus.SCHEDULED,
        "Промена на масло 5W30 и маслен филтер");
    appointment(cs.get(1), vs.get(2), skopje(2026, 5, 22, 10, 0), skopje(2026, 5, 22, 11, 0),
        "Проверка на сопирачки", AppointmentStatus.SCHEDULED, null);
    appointment(cs.get(2), vs.get(3), skopje(2026, 6, 3, 11, 0), skopje(2026, 6, 3, 12, 0),
        "Дијагностика", AppointmentStatus.SCHEDULED,
        "Провери код P0420 на катализаторот");
    // Cancelled
    appointment(cs.get(0), vs.get(1), skopje(2026, 6, 10, 14, 0), skopje(2026, 6, 10, 15, 0),
        "Климатизација", AppointmentStatus.CANCELLED,
        "Откажано од страна на клиентот");
    appointment(cs.get(6), vs.get(9), skopje(2026, 6, 24, 9, 0), skopje(2026, 6, 24, 10, 0),
        "Редовен сервис", AppointmentStatus.CANCELLED, null);
    // Upcoming
    appointment(cs.get(4), vs.get(6), skopje(2026, 7, 8, 9, 0), skopje(2026, 7, 8, 10, 0),
        "Редовен сервис", AppointmentStatus.SCHEDULED, null);
    appointment(cs.get(5), vs.get(7), skopje(2026, 7, 10, 10, 0), skopje(2026, 7, 10, 11, 0),
        "Промена на гуми", AppointmentStatus.SCHEDULED,
        "Премин на летни гуми");
    appointment(cs.get(1), vs.get(2), skopje(2026, 7, 15, 14, 0), skopje(2026, 7, 15, 15, 0),
        "Технички преглед", AppointmentStatus.SCHEDULED, null);
  }

  private void appointment(Customer customer, Vehicle vehicle,
                           OffsetDateTime scheduledAt, OffsetDateTime endsAt,
                           String serviceType, AppointmentStatus status, String notes) {
    Appointment a = new Appointment();
    a.setCustomer(customer);
    a.setVehicle(vehicle);
    a.setScheduledAt(scheduledAt);
    a.setEndsAt(endsAt);
    a.setServiceType(serviceType);
    a.setStatus(status);
    a.setNotes(notes);
    appointmentRepository.save(a);
  }

  private OffsetDateTime skopje(int y, int m, int d, int h, int min) {
    return LocalDateTime.of(y, m, d, h, min).atZone(SKOPJE).toOffsetDateTime();
  }

  // -------------------------------------------------------------------------
  // Service records — distributed to exercise the loyalty threshold (≥5 = loyal)
  //
  //  Petrov       → 6 records → LOYAL
  //  Kostadinova  → 3 records → not loyal
  //  Nikolov      → 5 records → LOYAL (boundary)
  //  Todorovska   → 1 record  → not loyal
  //  Blazevska    → 5 records → LOYAL (boundary)
  //  Arsov        → 2 records → not loyal
  //  Gjorgjiev    → 0 records → not loyal (new customer)
  //  Cvetkovska   → 0 records → deleted customer
  // -------------------------------------------------------------------------

  // Returns the 4 service records that are linked to seeded documents, in this order:
  //   [0] Petrov  Toyota  Технички преглед       2026-06-12  → tehnicki-pregled-petrov-toyota-2026.pdf
  //   [1] Petrov  BMW     Поправка на климатизација 2026-04-08 → ponuda-klimatizacija-petrov.pdf
  //   [2] Nikolov Merc    Дијагностика и поправка 2026-05-30  → pregled-nikolov-mercedes-c220.pdf
  //   [3] Blazevska Clio  Технички преглед        2026-06-05  → tehnicki-pregled-blazevska-clio.pdf
  private List<ServiceRecord> seedServiceRecords(List<Customer> cs, List<Vehicle> vs) {
    // Petrov — 6 records on Toyota (v0) and BMW (v1)
    sr(cs.get(0), vs.get(0), ld(2025, 8, 10), "Редовен сервис",
        bd("1500.00"), bd("2300.00"), bd("3800.00"),
        54200, "Маслен филтер, воздушен филтер", null);
    sr(cs.get(0), vs.get(0), ld(2025, 10, 20), "Сервис на сопирачки",
        bd("2800.00"), bd("4400.00"), bd("7200.00"),
        57800, "Предни и задни плочки, дискови", null);
    sr(cs.get(0), vs.get(1), ld(2025, 12, 5), "Промена на ремен за дистрибуција",
        bd("4200.00"), bd("5600.00"), bd("9800.00"),
        42100, "Ремен, затегнувач, ролна", null);
    sr(cs.get(0), vs.get(0), ld(2026, 2, 14), "Редовен сервис",
        bd("1500.00"), bd("2000.00"), bd("3500.00"),
        60500, "Маслен филтер", null);
    ServiceRecord petrovAcRepair = sr(cs.get(0), vs.get(1), ld(2026, 4, 8), "Поправка на климатизација",
        bd("0.00"), bd("5500.00"), bd("5500.00"),
        45600, null, "Полнење на климатизациски систем");
    ServiceRecord petrovTechInspection = sr(cs.get(0), vs.get(0), ld(2026, 6, 12), "Технички преглед",
        bd("0.00"), bd("2800.00"), bd("2800.00"),
        63200, null, null);

    // Kostadinova — 3 records on Golf (v2)
    sr(cs.get(1), vs.get(2), ld(2025, 9, 15), "Редовен сервис",
        bd("1200.00"), bd("2000.00"), bd("3200.00"),
        89400, "Маслен филтер, воздушен филтер", null);
    sr(cs.get(1), vs.get(2), ld(2026, 1, 20), "Промена на гуми",
        bd("0.00"), bd("1800.00"), bd("1800.00"),
        93200, null, "Монтажа зимски гуми");
    sr(cs.get(1), vs.get(2), ld(2026, 5, 10), "Сервис на спојка",
        bd("3500.00"), bd("5300.00"), bd("8800.00"),
        96800, "Плоча за спојка, притискач", null);

    // Nikolov — 5 records on Merc (v3) and Audi (v4)
    sr(cs.get(2), vs.get(3), ld(2025, 7, 20), "Редовен сервис",
        bd("2000.00"), bd("3200.00"), bd("5200.00"),
        31200, "Маслен и воздушен филтер", null);
    sr(cs.get(2), vs.get(4), ld(2025, 9, 5), "Замена на свеќи",
        bd("800.00"), bd("1600.00"), bd("2400.00"),
        68400, "Комплет свеќи", null);
    sr(cs.get(2), vs.get(3), ld(2025, 11, 18), "Сервис на сопирачки",
        bd("2200.00"), bd("4600.00"), bd("6800.00"),
        34600, "Предни плочки и дискови", null);
    sr(cs.get(2), vs.get(4), ld(2026, 2, 25), "Редовен сервис",
        bd("1800.00"), bd("2400.00"), bd("4200.00"),
        71500, "Маслен и воздушен филтер", null);
    ServiceRecord nikolovDiagnostics = sr(cs.get(2), vs.get(3), ld(2026, 5, 30), "Дијагностика и поправка",
        bd("500.00"), bd("3300.00"), bd("3800.00"),
        37200, null, "Поправена грешка P0420 на катализаторот");

    // Todorovska — 1 record on Astra (v5)
    sr(cs.get(3), vs.get(5), ld(2026, 4, 22), "Редовен сервис",
        bd("1100.00"), bd("1800.00"), bd("2900.00"),
        115400, "Маслен филтер", null);

    // Blazevska — 5 records on Clio (v7) and Octavia (v8)
    sr(cs.get(5), vs.get(7), ld(2025, 8, 25), "Редовен сервис",
        bd("900.00"), bd("1600.00"), bd("2500.00"),
        143200, "Маслен и воздушен филтер", null);
    sr(cs.get(5), vs.get(8), ld(2025, 10, 12), "Промена на ремен за дистрибуција",
        bd("3800.00"), bd("5400.00"), bd("9200.00"),
        62400, "Ремен, затегнувач, водена пумпа", null);
    sr(cs.get(5), vs.get(7), ld(2026, 1, 8), "Сервис на сопирачки",
        bd("1800.00"), bd("3000.00"), bd("4800.00"),
        147600, "Плочки и течност за кочење", null);
    sr(cs.get(5), vs.get(8), ld(2026, 3, 15), "Редовен сервис",
        bd("1600.00"), bd("2600.00"), bd("4200.00"),
        65800, "Маслен и воздушен филтер, свеќи", null);
    ServiceRecord blazevskaInspection = sr(cs.get(5), vs.get(7), ld(2026, 6, 5), "Технички преглед",
        bd("0.00"), bd("2200.00"), bd("2200.00"),
        150100, null, null);

    // Arsov — 2 records on i30 (v9)
    sr(cs.get(6), vs.get(9), ld(2026, 3, 28), "Редовен сервис",
        bd("1200.00"), bd("1900.00"), bd("3100.00"),
        28400, "Маслен и воздушен филтер", null);
    sr(cs.get(6), vs.get(9), ld(2026, 6, 18), "Поправка на суспензија",
        bd("3500.00"), bd("3700.00"), bd("7200.00"),
        29100, "Предни амортизери", null);

    return List.of(petrovTechInspection, petrovAcRepair, nikolovDiagnostics, blazevskaInspection);
  }

  private ServiceRecord sr(Customer customer, Vehicle vehicle, LocalDate date, String serviceType,
                           BigDecimal partsCost, BigDecimal laborCost, BigDecimal totalAmount,
                           int odometer, String replacedParts, String notes) {
    ServiceRecord record = new ServiceRecord();
    record.setCustomer(customer);
    record.setVehicle(vehicle);
    record.setServiceDate(date);
    record.setServiceType(serviceType);
    record.setPartsCost(partsCost);
    record.setLaborCost(laborCost);
    record.setTotalAmount(totalAmount);
    record.setOdometer(odometer);
    record.setReplacedParts(replacedParts);
    record.setNotes(notes);
    return serviceRecordRepository.save(record);
  }

  // -------------------------------------------------------------------------
  // Offers — all statuses, loyalty discount where customer qualifies
  //
  //  o0: Petrov    Toyota  SENT            10% loyalty
  //  o1: Nikolov   Merc    PENDING_DELIVERY 10% loyalty
  //  o2: Blazevska Octavia DRAFT            10% loyalty
  //  o3: Kostadinova Golf  SENT             no discount
  //  o4: Gjorgjiev Focus   DELIVERY_FAILED  no discount
  //  o5: Petrov    BMW     DRAFT            10% loyalty
  // -------------------------------------------------------------------------

  private void seedOffers(List<Customer> cs, List<Vehicle> vs) {
    Offer o0 = buildOffer(cs.get(0), vs.get(0),
        "Комплетен сервис на климатизација",
        "Сервис на климатизациски систем, промена на кабински филтер, дезинфекција",
        bd("3000.00"), bd("5500.00"), bd("10.00"),
        ld(2026, 7, 31), OfferStatus.SENT);
    addPart(o0, "Кабински филтер", bd("800.00"), 1);
    addPart(o0, "Хладилно средство R134a 500g", bd("1200.00"), 2);
    addPart(o0, "Средство за дезинфекција", bd("1000.00"), 3);
    offerRepository.save(o0);

    Offer o1 = buildOffer(cs.get(2), vs.get(3),
        "Сервис на кочиони систем",
        "Замена на предни и задни кочиони плочки и дискови, промена на кочиона течност",
        bd("4500.00"), bd("6000.00"), bd("10.00"),
        ld(2026, 8, 15), OfferStatus.PENDING_DELIVERY);
    addPart(o1, "Предни кочиони плочки", bd("1800.00"), 1);
    addPart(o1, "Задни кочиони плочки", bd("1400.00"), 2);
    addPart(o1, "Предни дискови (пар)", bd("1300.00"), 3);
    offerRepository.save(o1);

    Offer o2 = buildOffer(cs.get(5), vs.get(8),
        "Промена на ремен за дистрибуција",
        "Комплетна замена на временски ремен со сет на затегнувачи и водена пумпа",
        bd("4800.00"), bd("7200.00"), bd("10.00"),
        ld(2026, 9, 1), OfferStatus.DRAFT);
    addPart(o2, "Комплет ремен за дистрибуција", bd("3200.00"), 1);
    addPart(o2, "Затегнувач", bd("900.00"), 2);
    addPart(o2, "Водена пумпа", bd("700.00"), 3);
    offerRepository.save(o2);

    Offer o3 = buildOffer(cs.get(1), vs.get(2),
        "Редовен годишен сервис",
        "Промена на масло и сите филтри, проверка на течности, визуелен преглед",
        bd("1500.00"), bd("2700.00"), bd("0.00"),
        ld(2026, 7, 20), OfferStatus.SENT);
    addPart(o3, "Моторно масло 5W30 5L", bd("900.00"), 1);
    addPart(o3, "Маслен филтер", bd("350.00"), 2);
    addPart(o3, "Воздушен филтер", bd("250.00"), 3);
    offerRepository.save(o3);

    Offer o4 = buildOffer(cs.get(4), vs.get(6),
        "Проверка и замена на кочиони плочки",
        "Преглед на целиот кочиони систем, замена на плочки по потреба",
        bd("2200.00"), bd("3600.00"), bd("0.00"),
        ld(2026, 8, 5), OfferStatus.DELIVERY_FAILED);
    addPart(o4, "Предни кочиони плочки", bd("1400.00"), 1);
    addPart(o4, "Задни кочиони плочки", bd("800.00"), 2);
    offerRepository.save(o4);

    Offer o5 = buildOffer(cs.get(0), vs.get(1),
        "Сервис на трансмисија",
        "Замена на ATF масло во автоматски менувач, промена на менувачки филтер",
        bd("2800.00"), bd("4200.00"), bd("10.00"),
        null, OfferStatus.DRAFT);
    addPart(o5, "ATF масло 6L", bd("2200.00"), 1);
    addPart(o5, "Менувачки филтер", bd("600.00"), 2);
    offerRepository.save(o5);
  }

  /**
   * Constructs (but does not save) an Offer with all pricing fields computed from
   * partsCost + laborCost and the given discountPercent.
   */
  private Offer buildOffer(Customer customer, Vehicle vehicle, String title, String description,
                           BigDecimal partsCost, BigDecimal laborCost, BigDecimal discountPercent,
                           LocalDate expiresOn, OfferStatus status) {
    BigDecimal subtotal = partsCost.add(laborCost);
    BigDecimal discountAmount = subtotal.multiply(discountPercent)
        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    BigDecimal amount = subtotal.subtract(discountAmount);

    Offer o = new Offer();
    o.setCustomer(customer);
    o.setVehicle(vehicle);
    o.setTitle(title);
    o.setDescription(description);
    o.setPartsCost(partsCost);
    o.setLaborCost(laborCost);
    o.setSubtotalAmount(subtotal);
    o.setDiscountPercent(discountPercent);
    o.setDiscountAmount(discountAmount);
    o.setAmount(amount);
    o.setExpiresOn(expiresOn);
    o.setStatus(status);
    return o;
  }

  private void addPart(Offer offer, String name, BigDecimal price, int position) {
    OfferPart part = new OfferPart();
    part.setOffer(offer);
    part.setName(name);
    part.setPrice(price);
    part.setPosition(position);
    offer.getParts().add(part);
  }

  // -------------------------------------------------------------------------
  // Service documents — placeholder metadata (no real file bytes stored)
  // -------------------------------------------------------------------------

  // docRecords order matches the comment in seedServiceRecords:
  //   [0] petrovTechInspection  [1] petrovAcRepair
  //   [2] nikolovDiagnostics    [3] blazevskaInspection
  private void seedDocuments(List<Customer> cs, List<ServiceRecord> docRecords) {
    doc(cs.get(0), docRecords.get(0), DocumentType.INSPECTION,
        "tehnicki-pregled-petrov-toyota-2026.pdf", "application/pdf",
        "documents/petrov/tehnicki-pregled-toyota-2026-06-12.pdf");
    doc(cs.get(0), docRecords.get(1), DocumentType.OTHER,
        "ponuda-klimatizacija-petrov.pdf", "application/pdf",
        "documents/petrov/ponuda-klimatizacija-2026.pdf");
    doc(cs.get(2), docRecords.get(2), DocumentType.INSPECTION,
        "pregled-nikolov-mercedes-c220.pdf", "application/pdf",
        "documents/nikolov/pregled-c220-2026-05-30.pdf");
    doc(cs.get(5), docRecords.get(3), DocumentType.INSPECTION,
        "tehnicki-pregled-blazevska-clio.pdf", "application/pdf",
        "documents/blazevska/tehnicki-clio-2026-06-05.pdf");
  }

  private void doc(Customer customer, ServiceRecord serviceRecord, DocumentType type,
                   String fileName, String contentType, String storageKey) {
    ServiceDocument d = new ServiceDocument();
    d.setCustomer(customer);
    d.setServiceRecord(serviceRecord);
    d.setType(type);
    d.setFileName(fileName);
    d.setContentType(contentType);
    d.setStorageKey(storageKey);
    serviceDocumentRepository.save(d);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private LocalDate ld(int y, int m, int d) {
    return LocalDate.of(y, m, d);
  }

  private BigDecimal bd(String val) {
    return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
  }
}
