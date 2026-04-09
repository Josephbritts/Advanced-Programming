# COMP1549 Advanced Programming — Task 2: Security Library

Complete Project Manual

## 1. Project Overview

This project implements Task 2 of the COMP1549 coursework: a Java-based access-controlled security library. The application enforces security rules by design, preventing incorrect access to resources at compile time where possible. It models a realistic access-controlled system with users, roles, resources, access scopes, typed capabilities using Java Generics, policy enforcement, and audit logging.

### What the Application Does

The system lets you define users with different roles (GUEST, STUDENT, STAFF, ADMIN), resources with different sensitivity levels (PUBLIC, INTERNAL, CONFIDENTIAL), and then enforces access control rules that determine who can read or write each resource. Every access attempt — whether allowed or refused — is recorded in an audit log with a timestamp.

### Mark Breakdown (Source Code: 50 marks total)

| Component | Marks |
|---|---:|
| Concepts implementation (users, roles, resources) | 6 |
| Access rules implementation (policy allow/refuse) | 6 |
| Scope (PUBLIC, INTERNAL, CONFIDENTIAL) | 6 |
| Capabilities (Read/Write with Generics) | 6 |
| Log (audit trail with correct details) | 6 |
| Use of Java Generics | 8 |
| Use of design patterns | 6 |
| JUnit based testing | 6 |

## 2. Project Structure

```text
Advanced-Programming/
├── pom.xml                          # Maven build configuration
├── .vscode/settings.json            # VS Code settings
├── src/
│   ├── main/java/comp1549/
│   │   ├── demo/
│   │   │   ├── DemoApp.java         # Main entry point — runs the demo
│   │   │   └── DemoDataFactory.java # Factory to create sample users & resources
│   │   ├── logging/
│   │   │   ├── AuditLog.java        # Thread-safe audit log (Singleton-like)
│   │   │   └── LogEntry.java        # Immutable log record
│   │   ├── model/
│   │   │   ├── AccessScope.java     # Enum: PUBLIC, INTERNAL, CONFIDENTIAL
│   │   │   ├── Decision.java        # Enum: ALLOW, REFUSE
│   │   │   ├── Role.java            # Enum: GUEST, STUDENT, STAFF, ADMIN
│   │   │   ├── User.java            # Immutable user with ID, name, role
│   │   │   └── resource/
│   │   │       ├── Resource.java        # Abstract base class for all resources
│   │   │       ├── ExamPaper.java       # Concrete resource: exam paper
│   │   │       ├── LectureMaterial.java # Concrete resource: lecture slides
│   │   │       └── PrinterResource.java # Concrete resource: printer
│   │   └── security/
│   │       ├── capability/
│   │       │   ├── Operation.java       # Marker interface for operation types
│   │       │   ├── ReadPermission.java  # Marker class — "Read" operation type
│   │       │   ├── WritePermission.java # Marker class — "Write" operation type
│   │       │   └── Capability.java      # Generic capability token Capability<T>
│   │       └── policy/
│   │           ├── Policy.java          # Interface for access control policies
│   │           ├── DefaultPolicy.java   # Concrete policy with all the rules
│   │           └── AccessDecision.java  # Result object: ALLOW/REFUSE + reason
│   └── test/java/comp1549/
│       └── AccessControlTest.java   # JUnit 5 tests (9 test cases)
└── target/                          # Maven build output (generated)
```

## 3. Build Configuration (pom.xml)

The project uses Maven as its build tool, configured for Java 17.

```xml
<groupId>comp1549</groupId>
<artifactId>task2-security-library</artifactId>
<version>1.0-SNAPSHOT</version>
```

### Key Properties

- Java version: 17 (source and target)
- Encoding: UTF-8
- JUnit version: 5.10.2

### Dependencies

- JUnit Jupiter 5.10.2 — for unit testing (test scope only)

### Plugins

- Maven Surefire Plugin 3.2.5 — runs JUnit tests during the test phase

### How to Build and Run

```bash
# Compile the project
mvn compile

# Run the tests
mvn test

# Run the demo application
mvn exec:java -Dexec.mainClass="comp1549.demo.DemoApp"

# OR compile and run manually:
mvn compile
java -cp target/classes comp1549.demo.DemoApp
```

## 4. Enums — The Foundation Types

### 4.1 Role.java

```java
public enum Role {
    GUEST, STUDENT, STAFF, ADMIN
}
```

Defines the four user roles in the system. Each role has different permissions:

- GUEST — lowest privilege. Can only read PUBLIC resources.
- STUDENT — can read PUBLIC and INTERNAL resources. Cannot read CONFIDENTIAL. Cannot write anything.
- STAFF — can read PUBLIC, INTERNAL, and CONFIDENTIAL resources. Can write to specific resources (LectureMaterial, PrinterResource).
- ADMIN — full access. Can read and write everything.

### 4.2 AccessScope.java

```java
public enum AccessScope {
    PUBLIC, INTERNAL, CONFIDENTIAL
}
```

Defines the sensitivity level of each resource:

- PUBLIC — anyone can read (all roles).
- INTERNAL — only STUDENT, STAFF, and ADMIN can read. GUEST is locked out.
- CONFIDENTIAL — only STAFF and ADMIN can read. GUEST and STUDENT are locked out.

### 4.3 Decision.java

```java
public enum Decision {
    ALLOW, REFUSE
}
```

A simple two-value enum used by the audit log to record whether an access attempt was allowed or refused.

## 5. The Model Layer

### 5.1 User.java — The User Class

Package: `comp1549.model`

```java
public final class User {
    private final String id;
    private final String name;
    private final Role role;
    ...
}
```

#### Key Details

- Immutable — all fields are final. Once created, a User cannot change.
- Validation — the constructor rejects null/blank IDs and names, and null roles. It trims whitespace from ID and name.
- `withRole(Role newRole)` — creates a new User with the same ID and name but a different role. This is a functional-style method that preserves immutability (instead of a setter, you get a new object).
- `equals()` / `hashCode()` — equality is based on `id` only (two users with the same ID are considered the same user, regardless of name or role).
- `toString()` — returns `User{id='guest1', name='Guest User', role=GUEST}`.

#### Why It Matters

The `final` class keyword prevents subclassing. Combined with all-final fields, this makes `User` a fully immutable value object — safe to pass across threads without synchronisation.

### 5.2 Resource.java — The Abstract Resource Base Class

Package: `comp1549.model.resource`

```java
public abstract class Resource {
    private final String id;
    private final String name;
    private final AccessScope scope;
    private String content;   // mutable — can be written to
    ...
}
```

This is the core class that ties together the entire access control system.

#### Fields

| Field | Type | Mutable? | Purpose |
|---|---|---|---|
| `id` | String | No | Unique resource identifier (e.g. "R1") |
| `name` | String | No | Human-readable name (e.g. "Exam Paper") |
| `scope` | AccessScope | No | Sensitivity level (PUBLIC/INTERNAL/CONFIDENTIAL) |
| `content` | String | Yes | The actual data stored in the resource |

#### The `read()` Method — Step by Step

```java
public synchronized String read(User user,
                                Capability<ReadPermission> capability,
                                Policy policy,
                                AuditLog auditLog)
```

1. Check permission: Calls `policy.decide(user, this, capability)` to get an `AccessDecision`.
2. Log the attempt: Calls `auditLog.record(...)` regardless of whether access is allowed or refused. Every attempt is logged.
3. Enforce the decision: If `decision.isAllowed()` is false, throws a `SecurityException` with the reason.
4. Return content: If allowed, returns the resource's content string.

The `synchronized` keyword means only one thread can read/write a resource at a time. This prevents race conditions when multiple users access the same resource concurrently.

Compile-time safety: The parameter `Capability<ReadPermission>` means you physically cannot pass a write capability to the `read` method. The Java compiler rejects it. This is the key use of Generics for security enforcement.

#### The `write()` Method — Step by Step

```java
public synchronized void write(User user,
                               String newContent,
                               Capability<WritePermission> capability,
                               Policy policy,
                               AuditLog auditLog)
```

Same flow as `read()` but:

- Requires `Capability<WritePermission>` instead of `Capability<ReadPermission>`.
- On success, replaces `this.content` with `newContent`.
- Returns `void` (nothing to return — write is a side effect).

#### The `isStaffWritable()` Method

```java
public boolean isStaffWritable() { return false; }
```

Returns false by default. Subclasses override this to true if STAFF should be allowed to write that resource. This is how the policy knows whether a STAFF user can write a specific resource (see DefaultPolicy section).

### 5.3 ExamPaper.java

```java
public class ExamPaper extends Resource {
    public ExamPaper(String id, AccessScope scope, String content) {
        super(id, "Exam Paper", scope, content);
    }
}
```

- Inherits all behaviour from `Resource`.
- Sets the name to `"Exam Paper"` automatically.
- Does NOT override `isStaffWritable()` — so it returns false. Only ADMIN can write exam papers.

### 5.4 LectureMaterial.java

```java
public class LectureMaterial extends Resource {
    public LectureMaterial(String id, AccessScope scope, String content) {
        super(id, "Lecture Material", scope, content);
    }

    @Override
    public boolean isStaffWritable() { return true; }
}
```

- Sets the name to `"Lecture Material"`.
- Overrides `isStaffWritable()` to return true — STAFF can write lecture material (in addition to ADMIN).

### 5.5 PrinterResource.java

```java
public class PrinterResource extends Resource {
    public PrinterResource(String id, AccessScope scope, String content) {
        super(id, "Printer", scope, content);
    }

    @Override
    public boolean isStaffWritable() { return true; }
}
```

- Sets the name to `"Printer"`.
- Overrides `isStaffWritable()` to return true — STAFF can write to the printer (in addition to ADMIN).

## 6. The Capability System (Java Generics)

This is the most technically interesting part of the project and the primary use of Java Generics.

### 6.1 Operation.java — Marker Interface

```java
public interface Operation { }
```

An empty (marker) interface. Its only purpose is to serve as a type bound — all operation types must implement this.

### 6.2 ReadPermission.java and WritePermission.java

```java
public final class ReadPermission implements Operation { }
public final class WritePermission implements Operation { }
```

These are marker classes — they have no fields or methods. Their entire purpose is to exist as distinct Java types so that the generic type system can distinguish between read and write capabilities at compile time.

### 6.3 Capability.java — The Generic Capability Token

```java
public final class Capability<T extends Operation> {
    private final Class<T> operationType;

    private Capability(Class<T> operationType) {
        this.operationType = Objects.requireNonNull(operationType);
    }

    public static Capability<ReadPermission> read() {
        return new Capability<>(ReadPermission.class);
    }

    public static Capability<WritePermission> write() {
        return new Capability<>(WritePermission.class);
    }
    ...
}
```

#### How It Works

The generic type parameter `<T extends Operation>` means T must be either `ReadPermission` or `WritePermission` (or any future `Operation` subtype).

Factory methods are the only way to create `Capability` objects (the constructor is private):

- `Capability.read()` returns `Capability<ReadPermission>`
- `Capability.write()` returns `Capability<WritePermission>`

#### Why This Is Compile-Time Safe

Because `Resource.read()` requires `Capability<ReadPermission>` and `Resource.write()` requires `Capability<WritePermission>`.

```java
// This compiles:
resource.read(user, Capability.read(), policy, log);

// This does NOT compile — type mismatch:
resource.read(user, Capability.write(), policy, log); // COMPILER ERROR
```

This is the "security by design" that the coursework specification asks for — incorrect access is restricted at compile time, not just at runtime.

#### Other Methods

- `getOperationType()` — returns the `Class<T>` object (e.g. `ReadPermission.class`).
- `getOperationName()` — derives the name from the class: strips `"Permission"` and uppercases. So `ReadPermission` → `"READ"`, `WritePermission` → `"WRITE"`.
- `equals()` / `hashCode()` — based on `operationType` so two `Capability.read()` instances are considered equal.
- `toString()` — returns e.g. `"Capability[READ]"`.

## 7. The Policy System

### 7.1 Policy.java — The Strategy Interface

```java
public interface Policy {
    AccessDecision decide(User user, Resource resource,
                          Capability<? extends Operation> capability);
}
```

This is the Strategy pattern. The interface defines a single method: given a user, a resource, and a capability, decide whether access should be allowed or refused.

The wildcard `Capability<? extends Operation>` means the policy can accept any capability type (read or write) and inspect it at runtime.

### 7.2 AccessDecision.java — The Decision Object

```java
public class AccessDecision {
    private final Decision decision;   // ALLOW or REFUSE
    private final String reason;       // Human-readable explanation
    ...
}
```

#### Factory Methods

- `AccessDecision.allow("reason")` — creates an ALLOW decision.
- `AccessDecision.refuse("reason")` — creates a REFUSE decision.

#### Key Methods

- `isAllowed()` — returns `true` if `decision == Decision.ALLOW`.
- `getDecision()` — returns the `Decision` enum value.
- `getReason()` — returns the explanation string (used in log and error messages).

### 7.3 DefaultPolicy.java — The Concrete Rules

This class contains ALL the access control logic. It implements `Policy`.

#### The `decide()` Method

```java
public AccessDecision decide(User user, Resource resource,
                             Capability<? extends Operation> capability) {
    if (capability.getOperationType().equals(ReadPermission.class)) {
        return decideRead(user, resource);
    }
    if (capability.getOperationType().equals(WritePermission.class)) {
        return decideWrite(user, resource);
    }
    return AccessDecision.refuse("Unknown capability");
}
```

Inspects the capability type at runtime and delegates to the appropriate method.

#### Read Rules (`decideRead`)

| Resource Scope | GUEST | STUDENT | STAFF | ADMIN |
|---|---|---|---|---|
| PUBLIC | ALLOW | ALLOW | ALLOW | ALLOW |
| INTERNAL | REFUSE | ALLOW | ALLOW | ALLOW |
| CONFIDENTIAL | REFUSE | REFUSE | ALLOW | ALLOW |

Implemented as a switch expression on the `AccessScope`:

- PUBLIC → always allow for everyone.
- INTERNAL → allow for STUDENT, STAFF, ADMIN; refuse for GUEST.
- CONFIDENTIAL → allow for STAFF and ADMIN only; refuse for GUEST and STUDENT.

#### Write Rules (`decideWrite`)

| User Role | Condition | Decision |
|---|---|---|
| ADMIN | Always | ALLOW — "ADMIN can write all resources" |
| STAFF | Only if `resource.isStaffWritable()` is true | ALLOW — "STAFF can write selected resources" |
| STAFF | If `resource.isStaffWritable()` is false | REFUSE |
| STUDENT | Always | REFUSE |
| GUEST | Always | REFUSE |

This means:

- ADMIN can write any resource (`ExamPaper`, `LectureMaterial`, `PrinterResource`).
- STAFF can write `LectureMaterial` and `PrinterResource` (they override `isStaffWritable()` to return true), but NOT `ExamPaper`.
- STUDENT and GUEST cannot write anything.

## 8. The Logging System

### 8.1 LogEntry.java — Immutable Log Record

```java
public class LogEntry {
    private final LocalDateTime timestamp;
    private final String userId;
    private final Role role;
    private final String resourceName;
    private final String operation;    // "READ" or "WRITE"
    private final Decision decision;   // ALLOW or REFUSE
    private final String reason;       // Explanation
    ...
}
```

Every field is final — `LogEntry` is immutable once created.

#### `toString()` Output Format

```text
31-03-2026 15:30, student1, STUDENT, Exam Paper, READ, REFUSE (Only STAFF/ADMIN can read CONFIDENTIAL resources)
```

This matches the format specified in the coursework:

```text
date, userId, role, resourceName, operation, decision (reason)
```

The `DateTimeFormatter` uses pattern `"dd-MM-yyyy HH:mm"`.

### 8.2 AuditLog.java — Thread-Safe Log Collection

```java
public class AuditLog {
    private final List<LogEntry> entries = new ArrayList<>();

    public synchronized void record(User user, Resource resource,
                                    String operation, AccessDecision decision) { ... }
    public synchronized List<LogEntry> getEntries() { ... }
    public synchronized void printAll() { ... }
}
```

#### Thread Safety

All three methods are synchronized, meaning only one thread can add or read log entries at a time. This is essential because the demo runs concurrent access attempts.

#### The `record()` Method

1. Validates all parameters (null checks using `Objects.requireNonNull`).
2. Creates a new `LogEntry` with the current timestamp (`LocalDateTime.now()`).
3. Adds it to the internal list.

#### The `getEntries()` Method

Returns a defensive copy wrapped in an unmodifiable list:

```java
return Collections.unmodifiableList(new ArrayList<>(entries));
```

This means callers cannot modify the original list — they get a read-only snapshot.

#### The `printAll()` Method

Iterates through all entries and prints each one using `System.out.println(entry)` (which calls `LogEntry.toString()`).

## 9. The Demo Layer

### 9.1 DemoDataFactory.java — Factory Method Pattern

```java
public final class DemoDataFactory {
    private DemoDataFactory() { } // Private constructor — cannot instantiate

    public static Map<String, User> createUsers() { ... }
    public static Map<String, Resource> createResources() { ... }
}
```

This is the Factory Method pattern. It provides a clean way to create test data without scattering object creation throughout the code.

#### Created Users

| Key | ID | Name | Role |
|---|---|---|---|
| `"guest1"` | guest1 | Guest User | GUEST |
| `"student1"` | student1 | Student User | STUDENT |
| `"staff1"` | staff1 | Staff User | STAFF |
| `"admin1"` | admin1 | Admin User | ADMIN |

#### Created Resources

| Key | ID | Type | Scope | Initial Content |
|---|---|---|---|---|
| `"lecture"` | R1 | LectureMaterial | PUBLIC | `"Week 1 slides"` |
| `"printer"` | R2 | PrinterResource | INTERNAL | `"Printer queue"` |
| `"exam"` | R3 | ExamPaper | CONFIDENTIAL | `"Exam paper draft"` |

Uses `LinkedHashMap` to preserve insertion order.

### 9.2 DemoApp.java — Main Entry Point

The `main()` method runs the entire demonstration in three phases.

#### Phase 1: Sequential Access Attempts

Six individual access attempts:

| # | User | Resource | Operation | Expected Result |
|---:|---|---|---|---|
| 1 | Guest | Lecture (PUBLIC) | READ | ALLOW — anyone can read PUBLIC |
| 2 | Student | Printer (INTERNAL) | READ | ALLOW — STUDENT can read INTERNAL |
| 3 | Student | Exam (CONFIDENTIAL) | READ | REFUSE — STUDENT cannot read CONFIDENTIAL |
| 4 | Staff | Lecture (PUBLIC) | WRITE `"Updated lecture slides"` | ALLOW — STAFF can write LectureMaterial |
| 5 | Student | Printer (INTERNAL) | WRITE `"Print request"` | REFUSE — STUDENT cannot write anything |
| 6 | Admin | Exam (CONFIDENTIAL) | WRITE `"Final approved exam paper"` | ALLOW — ADMIN can write anything |

#### Phase 2: Concurrent Access — Different Resources

Three threads run simultaneously, each accessing a different resource:

- Thread 1: Guest reads Lecture (PUBLIC) → ALLOW
- Thread 2: Student writes Printer (INTERNAL) → REFUSE
- Thread 3: Admin writes Exam (CONFIDENTIAL) → ALLOW

This demonstrates thread safety — the synchronized methods on `Resource` and `AuditLog` prevent data corruption.

#### Phase 3: Concurrent Access — Same Resource

Three threads all access the same Exam resource:

- Thread 1: Guest reads Exam (CONFIDENTIAL) → REFUSE
- Thread 2: Student reads Exam (CONFIDENTIAL) → REFUSE
- Thread 3: Admin writes Exam (CONFIDENTIAL) → ALLOW

After all threads finish, it prints the final exam content to show it was correctly updated.

#### Helper Methods

- `attemptRead()` — wraps `resource.read()` in a try-catch. Prints `"READ OK"` or `"READ DENIED"`.
- `attemptWrite()` — wraps `resource.write()` in a try-catch. Prints `"WRITE OK"` or `"WRITE DENIED"`.
- `joinQuietly()` — calls `thread.join()` and handles `InterruptedException` by re-setting the interrupt flag.

#### Phase 4: Print Audit Log

At the end, `auditLog.printAll()` prints every logged access attempt with timestamps, showing a complete audit trail.

## 10. Actual Output — Line-by-Line Explanation

Below is the real output from running `DemoApp`, with every line explained.

### Phase 1: Sequential Access Attempts (6 operations)

```text
READ OK -> User{id='guest1', name='Guest User', role=GUEST} read Lecture Material: Week 1 slides
```

Line 1 — Guest reads Lecture Material (PUBLIC scope). Since PUBLIC resources are readable by everyone, the policy returns ALLOW. The content `"Week 1 slides"` is returned and printed.

```text
READ OK -> User{id='student1', name='Student User', role=STUDENT} read Printer: Printer queue
```

Line 2 — Student reads Printer (INTERNAL scope). STUDENT is allowed to read INTERNAL resources (only GUEST is blocked), so the policy returns ALLOW. The content `"Printer queue"` is returned.

```text
READ DENIED -> User{id='student1', name='Student User', role=STUDENT} on Exam Paper: Only STAFF/ADMIN can read CONFIDENTIAL resources
```

Line 3 — Student tries to read Exam Paper (CONFIDENTIAL scope). The policy refuses because only STAFF and ADMIN can read CONFIDENTIAL resources. A `SecurityException` is thrown, caught by `attemptRead()`, and the denial reason is printed.

```text
WRITE OK -> User{id='staff1', name='Staff User', role=STAFF} wrote Lecture Material
```

Line 4 — Staff writes to Lecture Material. The policy checks: is the user ADMIN? No. Is the user STAFF and `isStaffWritable()` true? Yes — `LectureMaterial` overrides `isStaffWritable()` to return true. So the write is allowed. The content is updated to `"Updated lecture slides"`.

```text
WRITE DENIED -> User{id='student1', name='Student User', role=STUDENT} on Printer: STUDENT is not allowed to write resource Printer
```

Line 5 — Student tries to write to Printer. The policy checks: is the user ADMIN? No. Is the user STAFF? No — the user is STUDENT. So the write is refused. The reason message includes the role name and resource name.

```text
WRITE OK -> User{id='admin1', name='Admin User', role=ADMIN} wrote Exam Paper
```

Line 6 — Admin writes to Exam Paper (CONFIDENTIAL scope). The policy immediately allows it because ADMIN can write all resources. The content is updated to `"Final approved exam paper"`.

### Phase 2: Concurrent Demo — Different Resources

```text
=== CONCURRENT DEMO: DIFFERENT RESOURCES ===
READ OK -> User{id='guest1', name='Guest User', role=GUEST} read Lecture Material: Updated lecture slides
WRITE OK -> User{id='admin1', name='Admin User', role=ADMIN} wrote Exam Paper
WRITE DENIED -> User{id='student1', name='Student User', role=STUDENT} on Printer: STUDENT is not allowed to write resource Printer
```

Three threads run simultaneously, each accessing a different resource:

- Thread 1: Guest reads Lecture Material → ALLOW. Notice the content is now `"Updated lecture slides"` (not `"Week 1 slides"`) because Staff wrote to it in Phase 1, Line 4. This proves the write in Phase 1 actually modified the resource.
- Thread 2: Admin writes to Exam Paper → ALLOW (`"Concurrent exam update"`).
- Thread 3: Student writes to Printer → REFUSE (STUDENT cannot write).

The order of these three lines may vary between runs because threads execute concurrently. The synchronized keyword on `Resource.read()` and `Resource.write()` ensures that even though threads run in parallel, each individual resource operation completes atomically without data corruption.

### Phase 3: Concurrent Demo — Same Resource

```text
=== CONCURRENT DEMO: SAME RESOURCE ===
READ DENIED -> User{id='guest1', name='Guest User', role=GUEST} on Exam Paper: Only STAFF/ADMIN can read CONFIDENTIAL resources
WRITE OK -> User{id='admin1', name='Admin User', role=ADMIN} wrote Exam Paper
READ DENIED -> User{id='student1', name='Student User', role=STUDENT} on Exam Paper: Only STAFF/ADMIN can read CONFIDENTIAL resources
FINAL EXAM CONTENT -> Final approved exam paper
```

Three threads all access the same Exam Paper simultaneously:

- Thread 1: Guest reads Exam Paper → REFUSE (GUEST cannot read CONFIDENTIAL).
- Thread 2: Student reads Exam Paper → REFUSE (STUDENT cannot read CONFIDENTIAL).
- Thread 3: Admin writes Exam Paper → ALLOW (content updated to `"Final approved exam paper"`).

After all three threads finish (`thread.join()`), the program prints `FINAL EXAM CONTENT -> Final approved exam paper` to confirm the write completed correctly despite concurrent access. This is the key thread-safety demonstration — multiple threads hitting the same resource don't cause crashes or corrupted data.

Again, the order of the three access lines may vary between runs.

### Phase 4: Final Audit Log

```text
=== FINAL LOG ===
```

This prints every access attempt across all four phases. Each line follows the format:

```text
date, userId, role, resourceName, operation, decision (reason)
```

| # | Log Entry | Explanation |
|---:|---|---|
| 1 | `31-03-2026 15:02, guest1, GUEST, Lecture Material, READ, ALLOW (PUBLIC resources are readable by everyone)` | Phase 1, Line 1 — Guest reads PUBLIC lecture |
| 2 | `31-03-2026 15:02, student1, STUDENT, Printer, READ, ALLOW (INTERNAL resource readable by STUDENT/STAFF/ADMIN)` | Phase 1, Line 2 — Student reads INTERNAL printer |
| 3 | `31-03-2026 15:02, student1, STUDENT, Exam Paper, READ, REFUSE (Only STAFF/ADMIN can read CONFIDENTIAL resources)` | Phase 1, Line 3 — Student denied from CONFIDENTIAL exam |
| 4 | `31-03-2026 15:02, staff1, STAFF, Lecture Material, WRITE, ALLOW (STAFF can write selected resources)` | Phase 1, Line 4 — Staff writes lecture material |
| 5 | `31-03-2026 15:02, student1, STUDENT, Printer, WRITE, REFUSE (STUDENT is not allowed to write resource Printer)` | Phase 1, Line 5 — Student denied write to printer |
| 6 | `31-03-2026 15:02, admin1, ADMIN, Exam Paper, WRITE, ALLOW (ADMIN can write all resources)` | Phase 1, Line 6 — Admin writes exam paper |
| 7 | `31-03-2026 15:02, guest1, GUEST, Lecture Material, READ, ALLOW (PUBLIC resources are readable by everyone)` | Phase 2 — Guest reads lecture (concurrent) |
| 8 | `31-03-2026 15:02, admin1, ADMIN, Exam Paper, WRITE, ALLOW (ADMIN can write all resources)` | Phase 2 — Admin writes exam (concurrent) |
| 9 | `31-03-2026 15:02, student1, STUDENT, Printer, WRITE, REFUSE (STUDENT is not allowed to write resource Printer)` | Phase 2 — Student denied printer write (concurrent) |
| 10 | `31-03-2026 15:02, guest1, GUEST, Exam Paper, READ, REFUSE (Only STAFF/ADMIN can read CONFIDENTIAL resources)` | Phase 3 — Guest denied from exam (concurrent, same resource) |
| 11 | `31-03-2026 15:02, admin1, ADMIN, Exam Paper, WRITE, ALLOW (ADMIN can write all resources)` | Phase 3 — Admin writes exam (concurrent, same resource) |
| 12 | `31-03-2026 15:02, student1, STUDENT, Exam Paper, READ, REFUSE (Only STAFF/ADMIN can read CONFIDENTIAL resources)` | Phase 3 — Student denied from exam (concurrent, same resource) |

Key observations about the log:

- All 12 entries are present — every access attempt (allowed or refused) is logged.
- Timestamps are all identical (15:02) because the program runs in under a second.
- REFUSE entries include the specific reason explaining why access was denied.
- The log is thread-safe — entries from concurrent phases (7-12) are not corrupted or missing.
- The format matches the coursework specification: `date, userId, role, resourceName, operation, decision (reason)`.

### JUnit Test Output

```text
Running comp1549.AccessControlTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.064 s
BUILD SUCCESS
```

All 9 tests passed with zero failures or errors. Maven Surefire detected the JUnit 5 test class automatically and ran all `@Test` methods. The 0.064 second runtime confirms the tests are lightweight and fast.

## 11. JUnit Tests (AccessControlTest.java)

The test class contains 9 test cases that validate the core access control rules.

### Test Setup (`@BeforeEach`)

Before each test, a fresh set of objects is created:

- Policy: `new DefaultPolicy()`
- AuditLog: `new AuditLog()`
- Users: guest (GUEST), student (STUDENT), staff (STAFF), admin (ADMIN)
- Resources:
  - `publicLecture` — `LectureMaterial`, PUBLIC scope, content `"Intro"`
  - `internalPrinter` — `PrinterResource`, INTERNAL scope, content `"Queue"`
  - `confidentialExam` — `ExamPaper`, CONFIDENTIAL scope, content `"Exam"`

### Test-by-Test Breakdown

#### Test 1: `guestCanReadPublicResource()`

- What it does: Guest reads the public lecture.
- Assertions: Content is `"Intro"`, log has 1 entry, operation is `"READ"`, decision is ALLOW, role is GUEST.
- Validates: PUBLIC resources are accessible to everyone.

#### Test 2: `studentCanReadInternalResource()`

- What it does: Student reads the internal printer.
- Assertions: Content is `"Queue"`, log has 1 entry, decision is ALLOW.
- Validates: INTERNAL resources are accessible to STUDENT.

#### Test 3: `studentCannotReadConfidentialResource()`

- What it does: Student tries to read the confidential exam.
- Assertions: `assertThrows(SecurityException.class, ...)`, exception message contains `"Only STAFF/ADMIN"`, log has 1 entry, operation is `"READ"`, decision is REFUSE, reason contains `"Only STAFF/ADMIN"`.
- Validates: CONFIDENTIAL resources are denied to STUDENT.

#### Test 4: `staffCanWriteLectureMaterial()`

- What it does: Staff writes `"Updated"` to the public lecture, then reads it back.
- Assertions: Content after write is `"Updated"`.
- Validates: STAFF can write to LectureMaterial (`isStaffWritable = true`).

#### Test 5: `studentCannotWritePrinter()`

- What it does: Student tries to write to the printer.
- Assertions: `assertThrows(SecurityException.class, ...)`, exception message is not null, log entry shows WRITE, REFUSE, reason contains `"STUDENT"`.
- Validates: STUDENT cannot write to any resource.

#### Test 6: `adminCanWriteConfidentialExamPaper()`

- What it does: Admin writes `"Approved exam version"` to the exam, then reads it back.
- Assertions: Content after write is `"Approved exam version"`, first log entry is ALLOW.
- Validates: ADMIN can write to any resource including CONFIDENTIAL.

#### Test 7: `guestCannotReadInternalResource()`

- What it does: Guest tries to read the internal printer.
- Assertions: `assertThrows(SecurityException.class, ...)`, log entry shows READ, REFUSE, reason contains `"GUEST"`.
- Validates: GUEST cannot read INTERNAL resources.

#### Test 8: `adminCanReadConfidentialResource()`

- What it does: Admin reads the confidential exam.
- Assertions: Content is `"Exam"`, log entry shows READ, ALLOW.
- Validates: ADMIN can read CONFIDENTIAL resources.

#### Test 9: `capabilityFactoryReturnsCorrectOperationName()`

- What it does: Creates read and write capabilities, checks their operation names.
- Assertions: `Capability.read()` name is `"READ"`, `Capability.write()` name is `"WRITE"`.
- Validates: The Capability generic class correctly derives operation names from the type parameter.

## 12. Design Patterns Used

### 12.1 Strategy Pattern

- Where: `Policy` interface + `DefaultPolicy` class.
- How: The `Policy` interface defines the contract (`decide()`), and `DefaultPolicy` provides one specific implementation. The `Resource.read()` and `Resource.write()` methods accept any `Policy` implementation, so the access control rules can be swapped at runtime without changing any other code.
- Why: Allows different policies to be plugged in (e.g. a `"PermissivePolicy"` for testing, a `"StrictPolicy"` for production) without modifying `Resource`, `User`, or any other class.

### 12.2 Factory Method Pattern

- Where: `DemoDataFactory` class, `Capability.read()` / `Capability.write()` static methods, `AccessDecision.allow()` / `AccessDecision.refuse()` static methods.
- How: `DemoDataFactory` has static methods `createUsers()` and `createResources()` that encapsulate object creation. `Capability` has a private constructor and only exposes `read()` and `write()` factory methods.
- Why: Centralises object creation, ensures consistency (e.g. `Capability` can only be created with valid types), and makes the demo code cleaner.

### 12.3 Template Method Pattern

- Where: `Resource` abstract class with `read()` / `write()` methods and the `isStaffWritable()` hook.
- How: The `read()` and `write()` methods define the algorithm (check policy → log → enforce → act). Subclasses only override the `isStaffWritable()` hook to customise behaviour.
- Why: Avoids code duplication — the access-check-then-act flow is written once in `Resource` and inherited by all subclasses.

## 13. Java Generics Usage

### 13.1 `Capability<T extends Operation>` — Typed Capabilities

The primary and most important use of generics. The bounded type parameter `<T extends Operation>` ensures that:

- `Capability<ReadPermission>` and `Capability<WritePermission>` are distinct types.
- The compiler prevents mixing them up (passing a write capability to a read method).
- Future operation types (e.g. `DeletePermission`) can be added by simply creating a new class that implements `Operation`.

### 13.2 `Capability<? extends Operation>` — Wildcard in Policy

The `Policy.decide()` method uses a wildcard:

```java
Capability<? extends Operation> capability
```

This means the policy can accept any Capability type and inspect it at runtime using `capability.getOperationType()`. This is necessary because the policy needs to handle both reads and writes in a single method.