package org.fadhel.waeiplatform;

import com.jayway.jsonpath.JsonPath;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationRegistrationIntegrationTests {

    private static final String ADMIN_EMAIL = "admin@waei.sa";
    private static final String ADMIN_PASSWORD = "Admin@12345";
    private static final String ORG_PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Value("${app.upload.dir}")
    private String uploadDirectory;

    @BeforeEach
    void cleanDatabaseAndUploads() throws IOException {
        for (Account account : accountRepository.findAll()) {
            if (!"PLATFORM_ADMIN".equals(account.getRole())) {
                accountRepository.delete(account);
            }
        }
        organizationRepository.deleteAll();

        Path directory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    void registrationCreatesPendingOrganizationAndAdminAccount() throws Exception {
        registerOrganization("org1@example.sa", "admin1@example.sa");

        Organization organization = organizationRepository.findOrganizationByEmail("org1@example.sa");
        assertNotNull(organization);
        assertEquals(OrganizationVerificationStatus.PENDING, organization.getVerificationStatus());
        assertNotNull(organization.getSubmittedAt());
        assertNotNull(organization.getDocumentStoredName());
        assertEquals("application/pdf", organization.getDocumentContentType());
        assertTrue(Files.exists(uploadFor(organization.getDocumentStoredName())));

        Account account = accountRepository.findAccountByEmail("admin1@example.sa");
        assertNotNull(account);
        assertEquals("ORG_ADMIN", account.getRole());
        assertEquals("PENDING", account.getStatus());
        assertEquals(organization.getId(), account.getOrganization().getId());
    }

    @Test
    void duplicateOrganizationEmailIsRejected() throws Exception {
        registerOrganization("duplicate@example.sa", "first-admin@example.sa");

        mockMvc.perform(registrationRequest("duplicate@example.sa", "second-admin@example.sa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization email already exists"));
    }

    @Test
    void invalidDocumentTypeIsRejected() throws Exception {
        MockMultipartFile invalidDocument = new MockMultipartFile(
                "document", "notes.txt", "text/plain", "not a document".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(registrationRequest("invalid@example.sa", "invalid-admin@example.sa", invalidDocument))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization document must be a PDF, JPEG or PNG file"));

        assertNull(organizationRepository.findOrganizationByEmail("invalid@example.sa"));
    }

    @Test
    void adminCanListRegistrationAndDownloadDocument() throws Exception {
        registerOrganization("review@example.sa", "review-admin@example.sa");
        Organization organization = organizationRepository.findOrganizationByEmail("review@example.sa");
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/organization/registrations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("review@example.sa"))
                .andExpect(jsonPath("$[0].verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].adminEmail").value("review-admin@example.sa"))
                .andExpect(jsonPath("$[0].documentOriginalName").value("registration.pdf"))
                .andExpect(jsonPath("$[0].documentDownloadUrl", notNullValue()));

        mockMvc.perform(get("/api/v1/organization/{id}/document", organization.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    void reviewEndpointsRequirePlatformAdmin() throws Exception {
        registerOrganization("secure@example.sa", "secure-admin@example.sa");
        Organization organization = organizationRepository.findOrganizationByEmail("secure@example.sa");

        mockMvc.perform(get("/api/v1/organization/registrations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/organization/{id}/document", organization.getId()))
                .andExpect(status().isUnauthorized());

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        verifyOrganization(organization.getId(), adminToken);

        String organizationToken = login("secure-admin@example.sa", ORG_PASSWORD);

        mockMvc.perform(get("/api/v1/organization/registrations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizationToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/organization/{id}/document", organization.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizationToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void verificationActivatesOrganizationAdminAccount() throws Exception {
        registerOrganization("verified@example.sa", "verified-admin@example.sa");
        Organization organization = organizationRepository.findOrganizationByEmail("verified@example.sa");
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(put("/api/v1/organization/{id}/verify", organization.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        Organization verifiedOrganization =
                organizationRepository.findOrganizationByEmail("verified@example.sa");
        assertEquals(OrganizationVerificationStatus.VERIFIED, verifiedOrganization.getVerificationStatus());
        assertNotNull(verifiedOrganization.getVerifiedAt());

        Account account = accountRepository.findAccountByEmail("verified-admin@example.sa");
        assertEquals("ACTIVE", account.getStatus());

        mockMvc.perform(loginRequest("verified-admin@example.sa", ORG_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId()))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void rejectedOrganizationCanResubmitAndBeVerified() throws Exception {
        registerOrganization("resubmit@example.sa", "resubmit-admin@example.sa");
        Organization organization = organizationRepository.findOrganizationByEmail("resubmit@example.sa");
        String oldDocument = organization.getDocumentStoredName();
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(put("/api/v1/organization/{id}/reject", organization.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Document is not readable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"));

        mockMvc.perform(loginRequest("resubmit-admin@example.sa", ORG_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("rejected")));

        mockMvc.perform(resubmissionRequest(organization.getId(), "WrongPassword1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid organization administrator credentials"));

        mockMvc.perform(resubmissionRequest(organization.getId(), ORG_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));

        Organization resubmitted = organizationRepository.findOrganizationByEmail("resubmit@example.sa");
        assertEquals(OrganizationVerificationStatus.PENDING, resubmitted.getVerificationStatus());
        assertNull(resubmitted.getRejectionReason());
        assertFalse(Files.exists(uploadFor(oldDocument)));
        assertTrue(Files.exists(uploadFor(resubmitted.getDocumentStoredName())));

        verifyOrganization(resubmitted.getId(), adminToken);
        login("resubmit-admin@example.sa", ORG_PASSWORD);
    }

    @Test
    void deletingOrganizationRemovesItsDocumentAndAccount() throws Exception {
        registerOrganization("delete@example.sa", "delete-admin@example.sa");
        Organization organization = organizationRepository.findOrganizationByEmail("delete@example.sa");
        String storedDocument = organization.getDocumentStoredName();
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(delete("/api/v1/organization/delete/{id}", organization.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        assertNull(organizationRepository.findOrganizationByEmail("delete@example.sa"));
        assertNull(accountRepository.findAccountByEmail("delete-admin@example.sa"));
        assertFalse(Files.exists(uploadFor(storedDocument)));
    }

    private void registerOrganization(String organizationEmail, String adminEmail) throws Exception {
        mockMvc.perform(registrationRequest(organizationEmail, adminEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    private MockMultipartHttpServletRequestBuilder registrationRequest(
            String organizationEmail, String adminEmail) {
        return registrationRequest(organizationEmail, adminEmail, document("registration.pdf"));
    }

    private MockMultipartHttpServletRequestBuilder registrationRequest(
            String organizationEmail, String adminEmail, MockMultipartFile file) {
        return multipart("/api/v1/organization/register")
                .file(file)
                .param("name", "Helping Hands Organization")
                .param("type", "BENEFICIARY")
                .param("sector", "Education")
                .param("email", organizationEmail)
                .param("phoneNumber", "0512345678")
                .param("documentReference", "CR-123456")
                .param("adminFullName", "Organization Administrator")
                .param("adminEmail", adminEmail)
                .param("adminPassword", ORG_PASSWORD);
    }

    private MockMultipartHttpServletRequestBuilder resubmissionRequest(
            Long organizationId, String password) {
        return multipart("/api/v1/organization/{id}/resubmit", organizationId)
                .file(document("updated-registration.pdf"))
                .param("name", "Helping Hands Organization")
                .param("type", "BENEFICIARY")
                .param("sector", "Healthcare")
                .param("email", "resubmit@example.sa")
                .param("phoneNumber", "0512345678")
                .param("documentReference", "CR-654321")
                .param("adminEmail", "resubmit-admin@example.sa")
                .param("password", password);
    }

    private MockMultipartFile document(String fileName) {
        return new MockMultipartFile(
                "document",
                fileName,
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 test organization document".getBytes(StandardCharsets.UTF_8));
    }

    private void verifyOrganization(Long organizationId, String adminToken) throws Exception {
        mockMvc.perform(put("/api/v1/organization/{id}/verify", organizationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(loginRequest(email, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private MockHttpServletRequestBuilder loginRequest(String email, String password) {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private Path uploadFor(String storedName) {
        return Paths.get(uploadDirectory).toAbsolutePath().normalize().resolve(storedName);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
