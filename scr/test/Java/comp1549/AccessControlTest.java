package comp1549.task2;

import comp1549.task2.logging.AuditLog;
import comp1549.task2.model.AccessScope;
import comp1549.task2.model.Role;
import comp1549.task2.model.User;
import comp1549.task2.model.resource.ExamPaper;
import comp1549.task2.model.resource.LectureMaterial;
import comp1549.task2.model.resource.PrinterResource;
import comp1549.task2.model.resource.Resource;
import comp1549.task2.security.capability.Capability;
import comp1549.task2.security.policy.DefaultPolicy;
import comp1549.task2.security.policy.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        guest = new User("u1", "Guest", Role.GUEST);
        student = new User("u2", "Student", Role.STUDENT);
        staff = new User("u3", "Staff", Role.STAFF);
        admin = new User("u4", "Admin", Role.ADMIN);

        publicLecture = new LectureMaterial("r1", AccessScope.PUBLIC, "Intro");
        internalPrinter = new PrinterResource("r2", AccessScope.INTERNAL, "Queue");
        confidentialExam = new ExamPaper("r3", AccessScope.CONFIDENTIAL, "Exam");
    }

    @Test
    void guestCanReadPublicResource() {
        String content = publicLecture.read(guest, Capability.read(), policy, auditLog);
        assertEquals("Intro", content);
        assertEquals(1, auditLog.getEntries().size());
        assertEquals("READ", auditLog.getEntries().get(0).getOperation());
    }

    @Test
    void studentCanReadInternalResource() {
        String content = internalPrinter.read(student, Capability.read(), policy, auditLog);
        assertEquals("Queue", content);
        assertEquals(1, auditLog.getEntries().size());
    }

    @Test
    void studentCannotReadConfidentialResource() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> confidentialExam.read(student, Capability.read(), policy, auditLog));
        assertTrue(ex.getMessage().contains("Only STAFF/ADMIN"));
        assertEquals(1, auditLog.getEntries().size());
        assertEquals("REFUSE", auditLog.getEntries().get(0).getDecision().name());
    }

    @Test
    void staffCanWriteLectureMaterial() {
        publicLecture.write(staff, "Updated", Capability.write(), policy, auditLog);
        String content = publicLecture.read(staff, Capability.read(), policy, auditLog);
        assertEquals("Updated", content);
    }

    @Test
    void studentCannotWritePrinter() {
        assertThrows(SecurityException.class,
                () -> internalPrinter.write(student, "Print job", Capability.write(), policy, auditLog));
        assertEquals(1, auditLog.getEntries().size());
        assertEquals("WRITE", auditLog.getEntries().get(0).getOperation());
    }

    @Test
    void adminCanWriteConfidentialExamPaper() {
        confidentialExam.write(admin, "Approved exam version", Capability.write(), policy, auditLog);
        String content = confidentialExam.read(admin, Capability.read(), policy, auditLog);
        assertEquals("Approved exam version", content);
    }
}