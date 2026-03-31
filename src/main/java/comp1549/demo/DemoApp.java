package comp1549.demo;

import java.util.Map;

import comp1549.logging.AuditLog;
import comp1549.model.User;
import comp1549.model.resource.Resource;
import comp1549.security.capability.Capability;
import comp1549.security.policy.DefaultPolicy;
import comp1549.security.policy.Policy;

public class DemoApp {

    public static void main(String[] args) {
        Policy policy = new DefaultPolicy();
        AuditLog auditLog = new AuditLog();

        Map<String, User> users = DemoDataFactory.createUsers();
        Map<String, Resource> resources = DemoDataFactory.createResources();

        User guest = users.get("guest1");
        User student = users.get("student1");
        User staff = users.get("staff1");
        User admin = users.get("admin1");

        Resource lecture = resources.get("lecture");
        Resource printer = resources.get("printer");
        Resource exam = resources.get("exam");

        attemptRead(guest, lecture, policy, auditLog);
        attemptRead(student, printer, policy, auditLog);
        attemptRead(student, exam, policy, auditLog);
        attemptWrite(staff, lecture, "Updated lecture slides", policy, auditLog);
        attemptWrite(student, printer, "Print request", policy, auditLog);
        attemptWrite(admin, exam, "Final approved exam paper", policy, auditLog);

        runConcurrentDifferentResources(policy, auditLog, users, resources);
        runConcurrentSameResource(policy, auditLog, users, resources);

        System.out.println("\n=== FINAL LOG ===");
        auditLog.printAll();
    }

    private static void attemptRead(User user, Resource resource, Policy policy, AuditLog auditLog) {
        try {
            String content = resource.read(user, Capability.read(), policy, auditLog);
            System.out.println("READ OK -> " + user + " read " + resource.getName() + ": " + content);
        } catch (SecurityException e) {
            System.out.println("READ DENIED -> " + user + " on " + resource.getName() + ": " + e.getMessage());
        }
    }

    private static void attemptWrite(User user,
                                     Resource resource,
                                     String content,
                                     Policy policy,
                                     AuditLog auditLog) {
        try {
            resource.write(user, content, Capability.write(), policy, auditLog);
            System.out.println("WRITE OK -> " + user + " wrote " + resource.getName());
        } catch (SecurityException e) {
            System.out.println("WRITE DENIED -> " + user + " on " + resource.getName() + ": " + e.getMessage());
        }
    }

    private static void runConcurrentDifferentResources(Policy policy,
                                                        AuditLog auditLog,
                                                        Map<String, User> users,
                                                        Map<String, Resource> resources) {
        System.out.println("\n=== CONCURRENT DEMO: DIFFERENT RESOURCES ===");

        User guest = users.get("guest1");
        User student = users.get("student1");
        User admin = users.get("admin1");

        Resource lecture = resources.get("lecture");
        Resource printer = resources.get("printer");
        Resource exam = resources.get("exam");

        Thread t1 = new Thread(() -> attemptRead(guest, lecture, policy, auditLog));
        Thread t2 = new Thread(() -> attemptWrite(student, printer, "Concurrent print update", policy, auditLog));
        Thread t3 = new Thread(() -> attemptWrite(admin, exam, "Concurrent exam update", policy, auditLog));

        t1.start();
        t2.start();
        t3.start();

        joinQuietly(t1);
        joinQuietly(t2);
        joinQuietly(t3);
    }

    private static void runConcurrentSameResource(Policy policy,
                                                  AuditLog auditLog,
                                                  Map<String, User> users,
                                                  Map<String, Resource> resources) {
        System.out.println("\n=== CONCURRENT DEMO: SAME RESOURCE ===");

        User guest = users.get("guest1");
        User student = users.get("student1");
        User admin = users.get("admin1");

        Resource exam = resources.get("exam");

        Thread t1 = new Thread(() -> attemptRead(guest, exam, policy, auditLog));
        Thread t2 = new Thread(() -> attemptRead(student, exam, policy, auditLog));
        Thread t3 = new Thread(() -> attemptWrite(admin, exam, "Final approved exam paper", policy, auditLog));

        t1.start();
        t2.start();
        t3.start();

        joinQuietly(t1);
        joinQuietly(t2);
        joinQuietly(t3);

        System.out.println("FINAL EXAM CONTENT -> " + exam.getContent());
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}