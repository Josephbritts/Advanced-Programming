package comp1549.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import comp1549.model.AccessScope;
import comp1549.model.Role;
import comp1549.model.User;
import comp1549.model.resource.ExamPaper;
import comp1549.model.resource.LectureMaterial;
import comp1549.model.resource.PrinterResource;
import comp1549.model.resource.Resource;

public final class DemoDataFactory {

    private DemoDataFactory() {
    }

    public static Map<String, User> createUsers() {
        Map<String, User> users = new LinkedHashMap<>();
        users.put("guest1", new User("guest1", "Guest User", Role.GUEST));
        users.put("student1", new User("student1", "Student User", Role.STUDENT));
        users.put("staff1", new User("staff1", "Staff User", Role.STAFF));
        users.put("admin1", new User("admin1", "Admin User", Role.ADMIN));
        return users;
    }

    public static Map<String, Resource> createResources() {
        Map<String, Resource> resources = new LinkedHashMap<>();
        resources.put("lecture", new LectureMaterial("R1", AccessScope.PUBLIC, "Week 1 slides"));
        resources.put("printer", new PrinterResource("R2", AccessScope.INTERNAL, "Printer queue"));
        resources.put("exam", new ExamPaper("R3", AccessScope.CONFIDENTIAL, "Exam paper draft"));
        return resources;
    }
}