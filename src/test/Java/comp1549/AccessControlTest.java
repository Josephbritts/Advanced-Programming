package comp1549;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import comp1549.logging.AuditLog;
import comp1549.logging.LogEntry;
import comp1549.model.AccessScope;
import comp1549.model.Decision;
import comp1549.model.Role;
import comp1549.model.User;
import comp1549.model.resource.ExamPaper;
import comp1549.model.resource.LectureMaterial;
import comp1549.model.resource.PrinterResource;
import comp1549.model.resource.Resource;
import comp1549.security.capability.Capability;
import comp1549.security.policy.DefaultPolicy;
import comp1549.security.policy.Policy;

public class AccessControlTest {

    private Policy policy;
    private AuditLog auditLog;

    private User guest;
    private User student;
    private User staff;
    private User admin;

    private Resource publicLecture;
    private Resource internalPrinter;
    private Resource confidentialExam;

    @BeforeEach
    void setUp() {

        policy = new DefaultPolicy();
        auditLog = new AuditLog();

        guest   = new User("u1", "Guest",   Role.GUEST);
        student = new User("u2", "Student", Role.STUDENT);
        staff   = new User("u3", "Staff",   Role.STAFF);
        admin   = new User("u4", "Admin",   Role.ADMIN);

        publicLecture    = new LectureMaterial("r1", AccessScope.PUBLIC,      "Intro");
        internalPrinter  = new PrinterResource("r2", AccessScope.INTERNAL,    "Queue");
        confidentialExam = new ExamPaper(     "r3", AccessScope.CONFIDENTIAL, "Exam");
    }

    @Test
void guestCanReadPublicResource() {

    String content = publicLecture.read(
            guest, Capability.read(), policy, auditLog);

    assertEquals("Intro", content);
    assertEquals(1, auditLog.getEntries().size());

    LogEntry entry = auditLog.getEntries().get(0);

    assertEquals("READ", entry.getOperation());
    assertEquals(Decision.ALLOW, entry.getDecision());
    assertEquals(Role.GUEST, entry.getRole());
}
    @Test
    void studentCanReadInternalResource() {

        String content = internalPrinter.read(student, Capability.read(), policy, auditLog);

        assertEquals("Queue", content);
        assertEquals(1, auditLog.getEntries().size());

        assertEquals("ALLOW",
                auditLog.getEntries().get(0).getDecision().name());
    }

    @Test
    void studentCannotReadConfidentialResource() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> confidentialExam.read(student, Capability.read(), policy, auditLog));

        assertTrue(ex.getMessage().contains("Only STAFF/ADMIN"));

        assertEquals(1, auditLog.getEntries().size());

        var entry = auditLog.getEntries().get(0);

        assertEquals("READ", entry.getOperation());
        assertEquals("REFUSE", entry.getDecision().name());
        assertTrue(entry.getReason().contains("Only STAFF/ADMIN"));
    }

    @Test
    void staffCanWriteLectureMaterial() {

        publicLecture.write(staff, "Updated", Capability.write(), policy, auditLog);

        String content = publicLecture.read(staff, Capability.read(), policy, auditLog);

        assertEquals("Updated", content);
    }

    @Test
    void studentCannotWritePrinter() {

        SecurityException ex = assertThrows(SecurityException.class,
                () -> internalPrinter.write(student, "Print job", Capability.write(), policy, auditLog));

        assertNotNull(ex.getMessage());

        var entry = auditLog.getEntries().get(0);

        assertEquals("WRITE", entry.getOperation());
        assertEquals("REFUSE", entry.getDecision().name());
        assertTrue(entry.getReason().contains("STUDENT"));
    }

    @Test
    void adminCanWriteConfidentialExamPaper() {

        confidentialExam.write(admin, "Approved exam version",
                Capability.write(), policy, auditLog);

        String content = confidentialExam.read(admin,
                Capability.read(), policy, auditLog);

        assertEquals("Approved exam version", content);

        assertEquals("ALLOW",
                auditLog.getEntries().get(0).getDecision().name());
    }

    @Test
    void guestCannotReadInternalResource() {

        SecurityException ex = assertThrows(SecurityException.class,
                () -> internalPrinter.read(guest,
                        Capability.read(), policy, auditLog));

        assertNotNull(ex.getMessage());

        var entry = auditLog.getEntries().get(0);

        assertEquals("READ", entry.getOperation());
        assertEquals("REFUSE", entry.getDecision().name());
        assertTrue(entry.getReason().contains("GUEST"));
    }

    @Test
    void adminCanReadConfidentialResource() {

        String content = confidentialExam.read(admin,
                Capability.read(), policy, auditLog);

        assertEquals("Exam", content);

        var entry = auditLog.getEntries().get(0);

        assertEquals("READ", entry.getOperation());
        assertEquals("ALLOW", entry.getDecision().name());
    }

    @Test
    void capabilityFactoryReturnsCorrectOperationName() {

        Capability<?> readCap = Capability.read();
        Capability<?> writeCap = Capability.write();

        assertEquals("READ", readCap.getOperationName());
        assertEquals("WRITE", writeCap.getOperationName());
    }
    
}