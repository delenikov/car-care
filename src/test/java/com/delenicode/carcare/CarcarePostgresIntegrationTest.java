package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class CarcarePostgresIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
      .withDatabaseName("carcare_it")
      .withUsername("carcare")
      .withPassword("carcare");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("app.jwt.secret", () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    registry.add("app.jwt.access-minutes", () -> "15");
    registry.add("app.jwt.refresh-days", () -> "30");
  }

  @LocalServerPort
  int port;

  @Autowired
  ObjectMapper objectMapper;

  HttpClient http = HttpClient.newHttpClient();

  @Test
  void authLifecycleAndAdminRbacWorkAgainstPostgres() throws Exception {
    TokenPair admin = login("admin@carcare.local", "admin213");
    String employeeEmail = "employee-" + UUID.randomUUID() + "@carcare.test";

    JsonNode createdUser = post("/api/admin/users", admin, Map.of(
        "email", employeeEmail,
        "fullName", "Integration Employee",
        "password", "password123",
        "roles", Set.of("ROLE_EMPLOYEE")), 200).get("data");
    assertThat(createdUser.get("email").asText()).isEqualTo(employeeEmail);
    assertThat(createdUser.get("roles").get(0).asText()).isEqualTo("ROLE_EMPLOYEE");
    long employeeId = createdUser.get("id").asLong();

    JsonNode updatedUser = put("/api/admin/users/" + employeeId, admin, Map.of(
        "email", employeeEmail,
        "fullName", "Updated Integration Employee",
        "enabled", true,
        "password", "password124",
        "roles", Set.of("ROLE_MANAGER")), 200).get("data");
    assertThat(updatedUser.get("fullName").asText()).isEqualTo("Updated Integration Employee");
    assertThat(updatedUser.get("roles").get(0).asText()).isEqualTo("ROLE_MANAGER");

    TokenPair employee = login(employeeEmail, "password124");

    get("/api/admin/users", employee, 403);

    post("/api/auth/change-password", employee, Map.of("currentPassword", "password123", "newPassword", "password456"), 200);

    post("/api/auth/refresh", null, Map.of("refreshToken", employee.refreshToken()), 401);

    TokenPair relogin = login(employeeEmail, "password456");

    post("/api/auth/logout", null, Map.of("refreshToken", relogin.refreshToken()), 200);

    post("/api/auth/refresh", null, Map.of("refreshToken", relogin.refreshToken()), 401);

    delete("/api/admin/users/" + employeeId, admin, 200);
    post("/api/auth/login", null, Map.of("email", employeeEmail, "password", "password456"), 401);
  }

  @Test
  void implementedOperationalCrudWorksAgainstPostgres() throws Exception {
    TokenPair admin = login("admin@carcare.local", "admin213");
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    long customerId = createAndReadId("/api/customers", admin, Map.of(
        "firstName", "Customer",
        "lastName", suffix,
        "email", "customer-" + suffix + "@carcare.test",
        "phone", "+38970123456",
        "address", "Test Street 1"));

    JsonNode firstNameSearch = get("/api/customers?firstName=Customer", admin, 200).get("data");
    assertArrayContainsId(firstNameSearch, customerId);

    JsonNode lastNameSearch = get("/api/customers?lastName=" + suffix, admin, 200).get("data");
    assertArrayContainsId(lastNameSearch, customerId);

    long vehicleId = createAndReadId("/api/vehicles", admin, Map.of(
        "customerId", customerId,
        "plateNumber", "SK-" + suffix,
        "make", "Volkswagen",
        "model", "Golf",
        "modelYear", 2020,
        "vin", "VIN" + suffix));

    JsonNode vinSearch = get("/api/vehicles?vin=VIN" + suffix, admin, 200).get("data");
    assertArrayContainsId(vinSearch, vehicleId);

    JsonNode plateSearch = get("/api/vehicles?plateNumber=SK-" + suffix, admin, 200).get("data");
    assertArrayContainsId(plateSearch, vehicleId);

    JsonNode ownerSearch = get("/api/vehicles?owner=Customer", admin, 200).get("data");
    assertArrayContainsId(ownerSearch, vehicleId);

    JsonNode updatedVehicle = put("/api/vehicles/" + vehicleId, admin, Map.of(
        "customerId", customerId,
        "plateNumber", "OH-" + suffix,
        "make", "Toyota",
        "model", "Corolla",
        "modelYear", 2022,
        "vin", "UPDATEDVIN" + suffix), 200).get("data");
    assertThat(updatedVehicle.get("plateNumber").asText()).isEqualTo("OH-" + suffix);
    assertThat(updatedVehicle.get("vin").asText()).isEqualTo("UPDATEDVIN" + suffix);

    JsonNode vehicleDetail = get("/api/vehicles/" + vehicleId, admin, 200).get("data");
    assertThat(vehicleDetail.get("make").asText()).isEqualTo("Toyota");

    OffsetDateTime appointmentStart = OffsetDateTime.of(2026, 6, 20, 9, 0, 0, 0, ZoneOffset.UTC);
    JsonNode appointment = post("/api/appointments", admin, Map.of(
        "customerId", customerId,
        "vehicleId", vehicleId,
        "scheduledAt", appointmentStart.toString(),
        "serviceType", "Minor Service",
        "notes", "Integration booking"), 200).get("data");
    assertThat(appointment.get("status").asText()).isEqualTo("SCHEDULED");
    assertThat(appointment.get("endsAt").asText()).isEqualTo(appointmentStart.plusHours(1).toString());
    String cancellationUrl = appointment.get("cancellationUrl").asText();
    assertThat(cancellationUrl).startsWith("/api/appointments/cancel/");

    post("/api/appointments", admin, Map.of(
        "customerId", customerId,
        "vehicleId", vehicleId,
        "scheduledAt", appointmentStart.plusMinutes(30).toString(),
        "serviceType", "Major Service",
        "notes", "Conflicting booking"), 400);

    JsonNode availableSlots = get("/api/appointments/available?date=2026-06-20", admin, 200).get("data");
    assertThat(availableSlots).allSatisfy(slot -> assertThat(slot.get("startsAt").asText()).isNotEqualTo(appointmentStart.toString()));

    JsonNode reminders = post("/api/appointments/reminders?date=2026-06-20", admin, Map.of(), 200).get("data");
    assertThat(reminders.get("sent").asInt()).isGreaterThanOrEqualTo(1);

    String cancellationToken = cancellationUrl.substring(cancellationUrl.lastIndexOf('/') + 1);
    JsonNode cancelledAppointment = post("/api/appointments/cancel/" + cancellationToken, null, Map.of(), 200).get("data");
    assertThat(cancelledAppointment.get("status").asText()).isEqualTo("CANCELLED");
    post("/api/appointments/cancel/" + cancellationToken, null, Map.of(), 400);

    JsonNode serviceRecord = post("/api/service-records", admin, Map.of(
        "customerId", customerId,
        "vehicleId", vehicleId,
        "serviceDate", "2026-06-12",
        "serviceType", "Minor Service",
        "partsCost", new BigDecimal("1500.00"),
        "laborCost", new BigDecimal("2000.00"),
        "odometer", 123456,
        "replacedParts", "Oil filter",
        "notes", "Oil and filters"), 200).get("data");
    long serviceRecordId = serviceRecord.get("id").asLong();
    assertThat(serviceRecord.get("totalAmount").decimalValue()).isEqualByComparingTo("3500.00");
    assertThat(serviceRecord.get("replacedParts").asText()).isEqualTo("Oil filter");

    JsonNode customerVehicles = get("/api/customers/" + customerId + "/vehicles", admin, 200).get("data");
    assertArrayContainsId(customerVehicles, vehicleId);

    JsonNode customerHistory = get("/api/customers/" + customerId + "/service-history", admin, 200).get("data");
    assertArrayContainsId(customerHistory, serviceRecordId);

    JsonNode vehicleHistory = get("/api/vehicles/" + vehicleId + "/service-history", admin, 200).get("data");
    assertArrayContainsId(vehicleHistory, serviceRecordId);

    JsonNode offer = post("/api/offers", admin, Map.of(
        "customerId", customerId,
        "vehicleId", vehicleId,
        "title", "Brake inspection",
        "description", "Inspect brake system",
        "partsCost", new BigDecimal("700.00"),
        "laborCost", new BigDecimal("500.00"),
        "expiresOn", "2026-07-12"), 200).get("data");
    long offerId = offer.get("id").asLong();
    assertThat(offer.get("amount").decimalValue()).isEqualByComparingTo("1200.00");

    JsonNode sentOffer = post("/api/offers/" + offerId + "/send", admin, Map.of(), 200).get("data");
    assertThat(sentOffer.get("status").asText()).isEqualTo("SENT");
    assertThat(getRaw("/api/offers/" + offerId + "/pdf", admin, 200)).startsWith("%PDF");

    JsonNode generatedDocuments = get("/api/documents", admin, 200).get("data");
    long generatedDocumentId = findDocumentIdByServiceRecordId(generatedDocuments, serviceRecordId);
    post("/api/documents/" + generatedDocumentId + "/send", admin, Map.of(), 200);
    assertThat(getRaw("/api/documents/" + generatedDocumentId + "/pdf", admin, 200)).startsWith("%PDF");

    createAndReadId("/api/documents", admin, Map.of(
        "customerId", customerId,
        "serviceRecordId", serviceRecordId,
        "type", "INSPECTION",
        "fileName", "inspection-" + suffix + ".pdf",
        "contentType", "application/pdf",
        "storageKey", "documents/inspection-" + suffix + ".pdf"));

    JsonNode summary = get("/api/dashboard/summary", admin, 200).get("data");
    assertThat(summary.get("customers").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(summary.get("vehicles").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(summary.get("appointments").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(summary.get("serviceRecords").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(summary.get("offers").asInt()).isGreaterThanOrEqualTo(1);

    long deletedCustomerId = createAndReadId("/api/customers", admin, Map.of(
        "firstName", "Delete",
        "lastName", suffix,
        "email", "delete-" + suffix + "@carcare.test",
        "phone", "+38970999999",
        "address", "Delete Street 1"));
    delete("/api/customers/" + deletedCustomerId, admin, 200);
    get("/api/customers/" + deletedCustomerId, admin, 400);
  }

  private long createAndReadId(String path, TokenPair tokenPair, Map<String, Object> body) throws Exception {
    JsonNode response = post(path, tokenPair, body, 200);
    assertThat(response.get("success").asBoolean()).isTrue();
    return response.get("data").get("id").asLong();
  }

  private TokenPair login(String email, String password) throws Exception {
    JsonNode data = post("/api/auth/login", null, Map.of("email", email, "password", password), 200).get("data");
    assertThat(data.get("accessToken").asText()).isNotBlank();
    assertThat(data.get("refreshToken").asText()).isNotBlank();
    assertThat(data.get("user").get("email").asText()).isEqualTo(email);
    return new TokenPair(data.get("accessToken").asText(), data.get("refreshToken").asText());
  }

  private JsonNode get(String path, TokenPair tokenPair, int expectedStatus) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
    authorize(builder, tokenPair);
    return send(builder.build(), expectedStatus);
  }

  private JsonNode post(String path, TokenPair tokenPair, Object body, int expectedStatus) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json(body)));
    authorize(builder, tokenPair);
    return send(builder.build(), expectedStatus);
  }

  private JsonNode put(String path, TokenPair tokenPair, Object body, int expectedStatus) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(json(body)));
    authorize(builder, tokenPair);
    return send(builder.build(), expectedStatus);
  }

  private JsonNode delete(String path, TokenPair tokenPair, int expectedStatus) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).DELETE();
    authorize(builder, tokenPair);
    return send(builder.build(), expectedStatus);
  }

  private JsonNode send(HttpRequest request, int expectedStatus) throws IOException, InterruptedException {
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(expectedStatus);
    return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
  }

  private String getRaw(String path, TokenPair tokenPair, int expectedStatus) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
    authorize(builder, tokenPair);
    HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(expectedStatus);
    return response.body();
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private void authorize(HttpRequest.Builder builder, TokenPair tokenPair) {
    if (tokenPair != null) {
      builder.header(HttpHeaders.AUTHORIZATION, bearer(tokenPair.accessToken()));
    }
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private void assertArrayContainsId(JsonNode array, long id) {
    assertThat(array.isArray()).isTrue();
    boolean found = false;
    for (JsonNode item : array) {
      found = found || item.get("id").asLong() == id;
    }
    assertThat(found).isTrue();
  }

  private long findDocumentIdByServiceRecordId(JsonNode array, long serviceRecordId) {
    assertThat(array.isArray()).isTrue();
    for (JsonNode item : array) {
      if (item.hasNonNull("serviceRecordId") && item.get("serviceRecordId").asLong() == serviceRecordId) {
        return item.get("id").asLong();
      }
    }
    throw new AssertionError("Document for service record " + serviceRecordId + " was not found");
  }

  private record TokenPair(String accessToken, String refreshToken) {
  }
}
