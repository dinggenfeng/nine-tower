# Phase 1: Project Scaffold + Authentication — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up the full project scaffold (backend Spring Boot + frontend React) with a working user authentication system (register / login / me / logout) including unit tests, integration tests, and code quality tooling.

**Architecture:** Monorepo with `backend/` (Spring Boot 3.3.5, Java 21, Maven, single module) and `frontend/` (React 18, TypeScript, Vite, Ant Design 5). Backend exposes REST API with JWT Bearer token auth. User data stored in PostgreSQL via Spring Data JPA. Integration tests use Testcontainers (real PostgreSQL container). Code quality enforced via Checkstyle, PMD, SpotBugs, OWASP Dependency-Check.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Security 6, jjwt 0.12.6, PostgreSQL 16, Testcontainers 1.19, JUnit 5, Mockito, React 18, TypeScript 5, Vite 5, Ant Design 5, Zustand, Axios, ESLint, Prettier, Vitest

---

## File Map

### Backend

| File | Responsibility |
|------|---------------|
| `backend/pom.xml` | Maven dependencies + plugin config |
| `backend/checkstyle.xml` | Checkstyle rules |
| `backend/src/main/resources/application.yml` | App config |
| `backend/src/main/resources/application-test.yml` | Test config placeholder |
| `backend/src/main/java/com/ansible/AnsibleApplication.java` | Spring Boot entry point |
| `backend/src/main/java/com/ansible/common/Result.java` | Unified API response wrapper |
| `backend/src/main/java/com/ansible/common/GlobalExceptionHandler.java` | Global exception → Result mapping |
| `backend/src/main/java/com/ansible/common/enums/ProjectRole.java` | Enum: PROJECT_ADMIN, PROJECT_MEMBER |
| `backend/src/main/java/com/ansible/common/enums/VariableScope.java` | Enum: PROJECT, HOSTGROUP, ENVIRONMENT |
| `backend/src/main/java/com/ansible/common/BaseEntity.java` | Base JPA entity (id, createdAt, updatedAt, createdBy) |
| `backend/src/main/java/com/ansible/user/entity/User.java` | User JPA entity |
| `backend/src/main/java/com/ansible/user/repository/UserRepository.java` | User Spring Data repository |
| `backend/src/main/java/com/ansible/user/dto/RegisterRequest.java` | Registration request DTO |
| `backend/src/main/java/com/ansible/user/dto/LoginRequest.java` | Login request DTO |
| `backend/src/main/java/com/ansible/user/dto/TokenResponse.java` | JWT response DTO |
| `backend/src/main/java/com/ansible/user/dto/UserResponse.java` | Public user info DTO |
| `backend/src/main/java/com/ansible/user/dto/UpdateUserRequest.java` | Update user request DTO |
| `backend/src/main/java/com/ansible/security/JwtTokenProvider.java` | JWT generate / validate / parse |
| `backend/src/main/java/com/ansible/security/JwtAuthenticationFilter.java` | JWT filter — extract token from header |
| `backend/src/main/java/com/ansible/security/UserDetailsServiceImpl.java` | Load user from DB for Spring Security |
| `backend/src/main/java/com/ansible/security/SecurityConfig.java` | Spring Security filter chain |
| `backend/src/main/java/com/ansible/user/service/AuthService.java` | Register, login business logic |
| `backend/src/main/java/com/ansible/user/service/UserService.java` | Get user, update user, delete user |
| `backend/src/main/java/com/ansible/user/controller/AuthController.java` | POST /api/auth/* endpoints |
| `backend/src/main/java/com/ansible/user/controller/UserController.java` | GET/PUT/DELETE /api/users/* endpoints |
| `backend/src/test/java/com/ansible/AbstractIntegrationTest.java` | Testcontainers base class |
| `backend/src/test/java/com/ansible/user/service/AuthServiceTest.java` | AuthService unit tests |
| `backend/src/test/java/com/ansible/user/service/UserServiceTest.java` | UserService unit tests |
| `backend/src/test/java/com/ansible/user/controller/AuthControllerTest.java` | AuthController integration tests |
| `backend/src/test/java/com/ansible/user/controller/UserControllerTest.java` | UserController integration tests |

### Frontend

| File | Responsibility |
|------|---------------|
| `frontend/package.json` | npm dependencies |
| `frontend/vite.config.ts` | Vite config + proxy |
| `frontend/tsconfig.json` | TypeScript strict config |
| `frontend/.eslintrc.cjs` | ESLint rules |
| `frontend/.prettierrc` | Prettier config |
| `frontend/src/main.tsx` | React entry point |
| `frontend/src/App.tsx` | Router config + auth guard |
| `frontend/src/types/entity/User.ts` | User TypeScript type |
| `frontend/src/api/request.ts` | Axios instance + JWT interceptor + 401 redirect |
| `frontend/src/api/auth.ts` | register / login / logout / me API calls |
| `frontend/src/api/user.ts` | getUsers / updateUser / deleteUser API calls |
| `frontend/src/stores/authStore.ts` | Zustand: currentUser, token, login, logout |
| `frontend/src/pages/auth/Login.tsx` | Login form page |
| `frontend/src/pages/auth/Register.tsx` | Register form page |
| `frontend/src/components/Layout/MainLayout.tsx` | Authenticated layout shell with header |

---

## Task 1: Initialize backend project scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/ansible/AnsibleApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p backend/src/main/java/com/ansible
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/ansible
mkdir -p frontend
```

- [ ] **Step 2: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>

  <groupId>com.ansible</groupId>
  <artifactId>ansible-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>ansible-backend</name>

  <properties>
    <java.version>21</java.version>
    <jjwt.version>0.12.6</jjwt.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>${jjwt.version}</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>${jjwt.version}</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>${jjwt.version}</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
      <!-- Spotless: auto-format with Google Java Style -->
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>2.43.0</version>
        <configuration>
          <java>
            <googleJavaFormat>
              <version>1.22.0</version>
              <style>GOOGLE</style>
            </googleJavaFormat>
          </java>
        </configuration>
      </plugin>
      <!-- Checkstyle: enforce style rules -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.3.1</version>
        <configuration>
          <configLocation>checkstyle.xml</configLocation>
          <failsOnError>true</failsOnError>
          <consoleOutput>true</consoleOutput>
          <includeTestSourceDirectory>true</includeTestSourceDirectory>
        </configuration>
      </plugin>
      <!-- PMD: static analysis -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-pmd-plugin</artifactId>
        <version>3.22.0</version>
        <configuration>
          <failOnViolation>true</failOnViolation>
          <printFailingErrors>true</printFailingErrors>
          <targetJdk>${java.version}</targetJdk>
        </configuration>
      </plugin>
      <!-- SpotBugs: bug and security scan -->
      <plugin>
        <groupId>com.github.spotbugs</groupId>
        <artifactId>spotbugs-maven-plugin</artifactId>
        <version>4.8.3.1</version>
        <configuration>
          <effort>Max</effort>
          <threshold>Medium</threshold>
          <failOnError>true</failOnError>
        </configuration>
      </plugin>
      <!-- OWASP: dependency vulnerability scan (run explicitly: mvn dependency-check:check) -->
      <plugin>
        <groupId>org.owasp</groupId>
        <artifactId>dependency-check-maven</artifactId>
        <version>9.2.0</version>
        <configuration>
          <failBuildOnCVSS>7</failBuildOnCVSS>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Create `backend/src/main/java/com/ansible/AnsibleApplication.java`**

```java
package com.ansible;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnsibleApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnsibleApplication.class, args);
  }
}
```

- [ ] **Step 4: Create `backend/src/main/resources/application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ansible_dev
    username: ${DB_USERNAME:ansible}
    password: ${DB_PASSWORD:ansible}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

app:
  jwt:
    secret: ${JWT_SECRET:ansible-playbook-system-jwt-secret-key-must-be-at-least-256bits}
    expiration-ms: 28800000

server:
  port: 8080
```

- [ ] **Step 5: Create `backend/checkstyle.xml`** (simplified Google style)

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
  <property name="charset" value="UTF-8"/>
  <property name="severity" value="error"/>
  <module name="FileTabCharacter">
    <property name="eachLine" value="true"/>
  </module>
  <module name="TreeWalker">
    <module name="OuterTypeFilename"/>
    <module name="IllegalTokenText">
      <property name="tokens" value="STRING_LITERAL, CHAR_LITERAL"/>
      <property name="format" value="\\u00(09|0[aA]|0[dD]|22|27|5[cC])|\\(0[0-7]|[^btnfr&quot;&apos;\\])"/>
      <property name="message" value="Consider using special escape sequence instead of octal value or Unicode escaped value."/>
    </module>
    <module name="AvoidStarImport"/>
    <module name="OneTopLevelClass"/>
    <module name="NoLineWrap"/>
    <module name="EmptyBlock">
      <property name="option" value="TEXT"/>
      <property name="tokens" value="LITERAL_TRY, LITERAL_FINALLY, LITERAL_IF, LITERAL_ELSE, LITERAL_SWITCH"/>
    </module>
    <module name="NeedBraces"/>
    <module name="LeftCurly"/>
    <module name="RightCurly">
      <property name="id" value="RightCurlySame"/>
      <property name="tokens" value="LITERAL_TRY, LITERAL_CATCH, LITERAL_FINALLY, LITERAL_IF, LITERAL_ELSE, LITERAL_DO"/>
    </module>
    <module name="WhitespaceAround">
      <property name="allowEmptyConstructors" value="true"/>
      <property name="allowEmptyMethods" value="true"/>
      <property name="allowEmptyTypes" value="true"/>
      <property name="allowEmptyLoops" value="true"/>
    </module>
    <module name="OneStatementPerLine"/>
    <module name="MultipleVariableDeclarations"/>
    <module name="MissingSwitchDefault"/>
    <module name="FallThrough"/>
    <module name="UpperEll"/>
    <module name="ModifierOrder"/>
    <module name="EmptyLineSeparator">
      <property name="allowNoEmptyLineBetweenFields" value="true"/>
    </module>
    <module name="SeparatorWrap">
      <property name="id" value="SeparatorWrapDot"/>
      <property name="tokens" value="DOT"/>
      <property name="option" value="nl"/>
    </module>
    <module name="PackageName">
      <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
    </module>
    <module name="TypeName"/>
    <module name="MemberName">
      <property name="format" value="^[a-z][a-z0-9][a-zA-Z0-9]*$"/>
    </module>
    <module name="ParameterName">
      <property name="format" value="^[a-z]([a-z0-9][a-zA-Z0-9]*)?$"/>
    </module>
    <module name="LocalVariableName">
      <property name="format" value="^[a-z]([a-z0-9][a-zA-Z0-9]*)?$"/>
    </module>
    <module name="ClassTypeParameterName">
      <property name="format" value="(^[A-Z][0-9]?)$|([A-Z][a-zA-Z0-9]*[T]$)"/>
    </module>
    <module name="MethodTypeParameterName">
      <property name="format" value="(^[A-Z][0-9]?)$|([A-Z][a-zA-Z0-9]*[T]$)"/>
    </module>
    <module name="NoFinalizer"/>
    <module name="AbbreviationAsWordInName">
      <property name="ignoreFinal" value="false"/>
      <property name="allowedAbbreviationLength" value="1"/>
    </module>
    <module name="OverloadMethodsDeclarationOrder"/>
    <module name="VariableDeclarationUsageDistance"/>
    <module name="MethodParamPad"/>
    <module name="ParenPad"/>
    <module name="OperatorWrap">
      <property name="option" value="NL"/>
      <property name="tokens" value="BAND, BOR, BSR, BXOR, DIV, EQUAL, GE, GT, LAND, LE, LITERAL_INSTANCEOF, LOR, LT, MINUS, MOD, NOT_EQUAL, PLUS, QUESTION, SL, SR, STAR, METHOD_REF"/>
    </module>
    <module name="AnnotationLocation">
      <property name="id" value="AnnotationLocationMostCases"/>
      <property name="tokens" value="CLASS_DEF, INTERFACE_DEF, ENUM_DEF, METHOD_DEF, CTOR_DEF"/>
    </module>
    <module name="AnnotationLocation">
      <property name="id" value="AnnotationLocationVariables"/>
      <property name="tokens" value="VARIABLE_DEF"/>
      <property name="allowSamelineMultipleAnnotations" value="true"/>
    </module>
    <module name="NonEmptyAtclauseDescription"/>
    <module name="InvalidJavadocPosition"/>
    <module name="JavadocTagContinuationIndentation"/>
    <module name="SummaryJavadocCheck">
      <property name="forbiddenSummaryFragments" value="^@return the *|^This method returns |^A [{]@code [a-zA-Z0-9]+[}]( is a )"/>
    </module>
    <module name="AtclauseOrder">
      <property name="tagOrder" value="@param, @return, @throws, @deprecated"/>
      <property name="target" value="CLASS_DEF, INTERFACE_DEF, ENUM_DEF, METHOD_DEF, CTOR_DEF, VARIABLE_DEF"/>
    </module>
    <module name="MethodName">
      <property name="format" value="^[a-z][a-z0-9][a-zA-Z0-9_]*$"/>
    </module>
  </module>
</module>
```

- [ ] **Step 6: Create root `.gitignore`**

```
# Backend
backend/target/
backend/*.iml
backend/.mvn/

# Frontend
frontend/node_modules/
frontend/dist/
frontend/.env.local

# IDE
.idea/
*.iml
.vscode/
*.DS_Store
```

- [ ] **Step 7: Verify Maven can resolve dependencies**

```bash
cd backend && mvn dependency:resolve -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 8: Commit**

```bash
git init
git add backend/pom.xml backend/checkstyle.xml backend/src/main/resources/application.yml \
        backend/src/main/java/com/ansible/AnsibleApplication.java .gitignore
git commit -m "chore: initialize backend scaffold with Spring Boot 3.3.5"
```

---

## Task 2: Create common utilities

**Files:**
- Create: `backend/src/main/java/com/ansible/common/Result.java`
- Create: `backend/src/main/java/com/ansible/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/ansible/common/BaseEntity.java`
- Create: `backend/src/main/java/com/ansible/common/enums/ProjectRole.java`
- Create: `backend/src/main/java/com/ansible/common/enums/VariableScope.java`

- [ ] **Step 1: Create `Result.java`**

```java
package com.ansible.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Result<T> {

  private int code;
  private String message;
  private T data;

  public static <T> Result<T> success(T data) {
    return new Result<>(200, "success", data);
  }

  public static <T> Result<T> success() {
    return new Result<>(200, "success", null);
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null);
  }
}
```

- [ ] **Step 2: Create `GlobalExceptionHandler.java`**

```java
package com.ansible.common;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return Result.error(400, message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
    return Result.error(400, ex.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Result<Void> handleSecurity(SecurityException ex) {
    return Result.error(403, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<Void> handleGeneral(Exception ex) {
    return Result.error(500, "Internal server error");
  }
}
```

- [ ] **Step 3: Create `BaseEntity.java`**

```java
package com.ansible.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false, updatable = false)
  private Long createdBy;

  public void setCreatedBy(Long userId) {
    this.createdBy = userId;
  }

  @PrePersist
  void onCreate() {
    createdAt = updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

- [ ] **Step 4: Create `ProjectRole.java`**

```java
package com.ansible.common.enums;

public enum ProjectRole {
  PROJECT_ADMIN,
  PROJECT_MEMBER
}
```

- [ ] **Step 5: Create `VariableScope.java`**

```java
package com.ansible.common.enums;

public enum VariableScope {
  PROJECT,
  HOSTGROUP,
  ENVIRONMENT
}
```

- [ ] **Step 6: Format code**

```bash
cd backend && mvn spotless:apply
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/ansible/common/
git commit -m "feat: add common utilities (Result, BaseEntity, GlobalExceptionHandler, enums)"
```

---

## Task 3: Create User entity and repository

**Files:**
- Create: `backend/src/main/java/com/ansible/user/entity/User.java`
- Create: `backend/src/main/java/com/ansible/user/repository/UserRepository.java`

- [ ] **Step 1: Create `User.java`**

Note: `User` does not extend `BaseEntity` because users self-register (no `createdBy`).

```java
package com.ansible.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false, length = 50)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(unique = true, nullable = false, length = 100)
  private String email;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
```

- [ ] **Step 2: Create `UserRepository.java`**

```java
package com.ansible.user.repository;

import com.ansible.user.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query(
      "SELECT u FROM User u WHERE "
          + "(:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}
```

- [ ] **Step 3: Format code**

```bash
cd backend && mvn spotless:apply
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/user/
git commit -m "feat: add User entity and UserRepository"
```

---

## Task 4: Create DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/user/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/ansible/user/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/ansible/user/dto/TokenResponse.java`
- Create: `backend/src/main/java/com/ansible/user/dto/UserResponse.java`
- Create: `backend/src/main/java/com/ansible/user/dto/UpdateUserRequest.java`

- [ ] **Step 1: Create `RegisterRequest.java`**

```java
package com.ansible.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  private String password;

  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  @Size(max = 100)
  private String email;
}
```

- [ ] **Step 2: Create `LoginRequest.java`**

```java
package com.ansible.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "Password is required")
  private String password;
}
```

- [ ] **Step 3: Create `TokenResponse.java`**

```java
package com.ansible.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {

  private String token;
  private String tokenType;
  private long expiresIn;
  private UserResponse user;
}
```

- [ ] **Step 4: Create `UserResponse.java`**

```java
package com.ansible.user.dto;

import com.ansible.user.entity.User;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserResponse {

  private final Long id;
  private final String username;
  private final String email;
  private final LocalDateTime createdAt;

  public UserResponse(User user) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.createdAt = user.getCreatedAt();
  }
}
```

- [ ] **Step 5: Create `UpdateUserRequest.java`**

```java
package com.ansible.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  private String password;

  @Email(message = "Email format is invalid")
  @Size(max = 100)
  private String email;
}
```

- [ ] **Step 6: Format and commit**

```bash
cd backend && mvn spotless:apply
git add backend/src/main/java/com/ansible/user/dto/
git commit -m "feat: add user DTOs (RegisterRequest, LoginRequest, TokenResponse, UserResponse, UpdateUserRequest)"
```

---

## Task 5: Create JWT infrastructure

**Files:**
- Create: `backend/src/main/java/com/ansible/security/JwtTokenProvider.java`
- Create: `backend/src/main/java/com/ansible/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/ansible/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/com/ansible/security/SecurityConfig.java`

- [ ] **Step 1: Create `JwtTokenProvider.java`**

```java
package com.ansible.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${app.jwt.expiration-ms}")
  private long jwtExpirationMs;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtExpirationMs);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key())
        .compact();
  }

  public Long getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    return Long.valueOf(claims.getSubject());
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
```

- [ ] **Step 2: Create `JwtAuthenticationFilter.java`**

```java
package com.ansible.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsServiceImpl userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractToken(request);
    if (token != null && jwtTokenProvider.validateToken(token)) {
      Long userId = jwtTokenProvider.getUserIdFromToken(token);
      UserDetails userDetails = userDetailsService.loadUserById(userId);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
```

- [ ] **Step 3: Create `UserDetailsServiceImpl.java`**

```java
package com.ansible.security;

import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    return buildUserDetails(user);
  }

  public UserDetails loadUserById(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
    return buildUserDetails(user);
  }

  private UserDetails buildUserDetails(User user) {
    return new org.springframework.security.core.userdetails.User(
        String.valueOf(user.getId()),
        user.getPassword(),
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
```

- [ ] **Step 4: Create `SecurityConfig.java`**

```java
package com.ansible.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/register", "/api/auth/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
```

- [ ] **Step 5: Format code**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/security/
git commit -m "feat: add JWT infrastructure and Spring Security config"
```

---

## Task 6: AuthService — TDD

**Files:**
- Create: `backend/src/test/java/com/ansible/user/service/AuthServiceTest.java`
- Create: `backend/src/main/java/com/ansible/user/service/AuthService.java`

- [ ] **Step 1: Write failing unit tests**

Create `backend/src/test/java/com/ansible/user/service/AuthServiceTest.java`:

```java
package com.ansible.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.security.JwtTokenProvider;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private AuthService authService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("encoded_password");
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void register_success() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("new@example.com");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtTokenProvider.generateToken(1L)).thenReturn("jwt_token");

    TokenResponse response = authService.register(request);

    assertThat(response.getToken()).isEqualTo("jwt_token");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getUser().getUsername()).isEqualTo("testuser");
  }

  @Test
  void register_fails_when_username_taken() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("testuser");
    request.setPassword("password123");
    request.setEmail("other@example.com");

    when(userRepository.existsByUsername("testuser")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username already taken");

    verify(userRepository, never()).save(any());
  }

  @Test
  void register_fails_when_email_taken() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("test@example.com");

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email already registered");

    verify(userRepository, never()).save(any());
  }

  @Test
  void login_success() {
    LoginRequest request = new LoginRequest();
    request.setUsername("testuser");
    request.setPassword("password123");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
    when(jwtTokenProvider.generateToken(1L)).thenReturn("jwt_token");

    TokenResponse response = authService.login(request);

    assertThat(response.getToken()).isEqualTo("jwt_token");
    assertThat(response.getUser().getUsername()).isEqualTo("testuser");
  }

  @Test
  void login_fails_when_user_not_found() {
    LoginRequest request = new LoginRequest();
    request.setUsername("unknown");
    request.setPassword("password123");

    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid username or password");
  }

  @Test
  void login_fails_when_password_wrong() {
    LoginRequest request = new LoginRequest();
    request.setUsername("testuser");
    request.setPassword("wrongpassword");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid username or password");
  }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
cd backend && mvn test -pl . -Dtest=AuthServiceTest 2>&1 | tail -20
```

Expected: `COMPILATION ERROR` — `AuthService` does not exist yet.

- [ ] **Step 3: Create `AuthService.java`**

```java
package com.ansible.user.service;

import com.ansible.security.JwtTokenProvider;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Value("${app.jwt.expiration-ms}")
  private long jwtExpirationMs;

  @Transactional
  public TokenResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("Username already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setEmail(request.getEmail());
    User saved = userRepository.save(user);
    String token = jwtTokenProvider.generateToken(saved.getId());
    return new TokenResponse(token, "Bearer", jwtExpirationMs, new UserResponse(saved));
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid username or password");
    }
    String token = jwtTokenProvider.generateToken(user.getId());
    return new TokenResponse(token, "Bearer", jwtExpirationMs, new UserResponse(user));
  }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd backend && mvn test -Dtest=AuthServiceTest
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Format code**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/user/service/AuthService.java \
        backend/src/test/java/com/ansible/user/service/AuthServiceTest.java
git commit -m "feat: add AuthService with register/login — unit tests passing"
```

---

## Task 7: UserService — TDD

**Files:**
- Create: `backend/src/test/java/com/ansible/user/service/UserServiceTest.java`
- Create: `backend/src/main/java/com/ansible/user/service/UserService.java`

- [ ] **Step 1: Write failing unit tests**

Create `backend/src/test/java/com/ansible/user/service/UserServiceTest.java`:

```java
package com.ansible.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("encoded");
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void getUser_success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    UserResponse response = userService.getUser(1L);

    assertThat(response.getUsername()).isEqualTo("testuser");
    assertThat(response.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void getUser_notFound_throws() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void updateUser_updates_email() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userService.updateUser(1L, request, 1L);

    verify(userRepository).save(testUser);
    assertThat(testUser.getEmail()).isEqualTo("new@example.com");
  }

  @Test
  void updateUser_forbidden_when_not_owner() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("new@example.com");

    assertThatThrownBy(() -> userService.updateUser(1L, request, 2L))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("You can only modify your own account");
  }

  @Test
  void updateUser_fails_when_email_taken() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("taken@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(() -> userService.updateUser(1L, request, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email already registered");
  }

  @Test
  void deleteUser_success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    userService.deleteUser(1L, 1L);

    verify(userRepository).delete(testUser);
  }

  @Test
  void deleteUser_forbidden_when_not_owner() {
    assertThatThrownBy(() -> userService.deleteUser(1L, 2L))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("You can only delete your own account");
  }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
cd backend && mvn test -Dtest=UserServiceTest 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` — `UserService` does not exist.

- [ ] **Step 3: Create `UserService.java`**

```java
package com.ansible.user.service;

import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public UserResponse getUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return new UserResponse(user);
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
    return userRepository.searchUsers(keyword, pageable).map(UserResponse::new);
  }

  @Transactional
  public UserResponse updateUser(Long userId, UpdateUserRequest request, Long currentUserId) {
    if (!userId.equals(currentUserId)) {
      throw new SecurityException("You can only modify your own account");
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (StringUtils.hasText(request.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("Email already registered");
      }
      user.setEmail(request.getEmail());
    }
    if (StringUtils.hasText(request.getPassword())) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    return new UserResponse(userRepository.save(user));
  }

  @Transactional
  public void deleteUser(Long userId, Long currentUserId) {
    if (!userId.equals(currentUserId)) {
      throw new SecurityException("You can only delete your own account");
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    userRepository.delete(user);
  }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd backend && mvn test -Dtest=UserServiceTest
```

Expected: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Format and commit**

```bash
cd backend && mvn spotless:apply
git add backend/src/main/java/com/ansible/user/service/UserService.java \
        backend/src/test/java/com/ansible/user/service/UserServiceTest.java
git commit -m "feat: add UserService with get/update/delete — unit tests passing"
```

---

## Task 8: Integration test base + AuthController — TDD

**Files:**
- Create: `backend/src/test/java/com/ansible/AbstractIntegrationTest.java`
- Create: `backend/src/test/java/com/ansible/user/controller/AuthControllerTest.java`
- Create: `backend/src/main/java/com/ansible/user/controller/AuthController.java`

- [ ] **Step 1: Create `AbstractIntegrationTest.java`**

```java
package com.ansible;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("ansible_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
  }
}
```

- [ ] **Step 2: Write failing integration tests for AuthController**

Create `backend/src/test/java/com/ansible/user/controller/AuthControllerTest.java`:

```java
package com.ansible.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void register_returns_token() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("alice");
    request.setPassword("password123");
    request.setEmail("alice@example.com");

    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(request),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getToken()).isNotBlank();
    assertThat(response.getBody().getData().getUser().getUsername()).isEqualTo("alice");
  }

  @Test
  void register_fails_duplicate_username() {
    RegisterRequest first = new RegisterRequest();
    first.setUsername("alice");
    first.setPassword("password123");
    first.setEmail("alice@example.com");
    restTemplate.postForEntity("/api/auth/register", first, Object.class);

    RegisterRequest second = new RegisterRequest();
    second.setUsername("alice");
    second.setPassword("password456");
    second.setEmail("other@example.com");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(second),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void login_returns_token() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("bob");
    reg.setPassword("password123");
    reg.setEmail("bob@example.com");
    restTemplate.postForEntity("/api/auth/register", reg, Object.class);

    LoginRequest login = new LoginRequest();
    login.setUsername("bob");
    login.setPassword("password123");

    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(login),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getToken()).isNotBlank();
  }

  @Test
  void login_fails_wrong_password() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("charlie");
    reg.setPassword("password123");
    reg.setEmail("charlie@example.com");
    restTemplate.postForEntity("/api/auth/register", reg, Object.class);

    LoginRequest login = new LoginRequest();
    login.setUsername("charlie");
    login.setPassword("wrongpassword");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(login),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void me_returns_current_user() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("dave");
    reg.setPassword("password123");
    reg.setEmail("dave@example.com");
    ResponseEntity<Result<TokenResponse>> regResponse =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    String token = regResponse.getBody().getData().getToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Result<UserResponse>> response =
        restTemplate.exchange(
            "/api/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getUsername()).isEqualTo("dave");
  }
}
```

- [ ] **Step 3: Run tests — expect compilation failure**

```bash
cd backend && mvn test -Dtest=AuthControllerTest 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` — `AuthController` does not exist.

- [ ] **Step 4: Create `AuthController.java`**

```java
package com.ansible.user.controller;

import com.ansible.common.Result;
import com.ansible.security.JwtAuthenticationFilter;
import com.ansible.user.dto.LoginRequest;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.service.AuthService;
import com.ansible.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UserService userService;

  @PostMapping("/register")
  public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
    return Result.success(authService.register(request));
  }

  @PostMapping("/login")
  public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return Result.success(authService.login(request));
  }

  @GetMapping("/me")
  public Result<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(userService.getUser(userId));
  }
}
```

- [ ] **Step 5: Run integration tests — expect PASS**

```bash
cd backend && mvn test -Dtest=AuthControllerTest
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

Note: First run downloads the PostgreSQL Docker image — may take 1-2 minutes.

- [ ] **Step 6: Format and commit**

```bash
cd backend && mvn spotless:apply
git add backend/src/main/java/com/ansible/user/controller/AuthController.java \
        backend/src/test/java/com/ansible/AbstractIntegrationTest.java \
        backend/src/test/java/com/ansible/user/controller/AuthControllerTest.java
git commit -m "feat: add AuthController with register/login/me — integration tests passing"
```

---

## Task 9: UserController — TDD

**Files:**
- Create: `backend/src/test/java/com/ansible/user/controller/UserControllerTest.java`
- Create: `backend/src/main/java/com/ansible/user/controller/UserController.java`

- [ ] **Step 1: Write failing integration tests**

Create `backend/src/test/java/com/ansible/user/controller/UserControllerTest.java`:

```java
package com.ansible.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UserControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;

  private String token;
  private Long userId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("alice");
    reg.setPassword("password123");
    reg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = response.getBody().getData().getToken();
    userId = response.getBody().getData().getUser().getId();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  @Test
  void getUser_success() {
    ResponseEntity<Result<UserResponse>> response =
        restTemplate.exchange(
            "/api/users/" + userId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getUsername()).isEqualTo("alice");
  }

  @Test
  void getUser_unauthorized_without_token() {
    ResponseEntity<Object> response =
        restTemplate.getForEntity("/api/users/" + userId, Object.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void updateUser_email_success() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setEmail("newalice@example.com");

    ResponseEntity<Result<UserResponse>> response =
        restTemplate.exchange(
            "/api/users/" + userId,
            HttpMethod.PUT,
            new HttpEntity<>(request, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getEmail()).isEqualTo("newalice@example.com");
  }

  @Test
  void deleteUser_success() {
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/users/" + userId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(userRepository.findById(userId)).isEmpty();
  }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
cd backend && mvn test -Dtest=UserControllerTest 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` — `UserController` does not exist.

- [ ] **Step 3: Create `UserController.java`**

```java
package com.ansible.user.controller;

import com.ansible.common.Result;
import com.ansible.user.dto.UpdateUserRequest;
import com.ansible.user.dto.UserResponse;
import com.ansible.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public Result<Page<UserResponse>> searchUsers(
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    return Result.success(userService.searchUsers(keyword, pageable));
  }

  @GetMapping("/{id}")
  public Result<UserResponse> getUser(@PathVariable Long id) {
    return Result.success(userService.getUser(id));
  }

  @PutMapping("/{id}")
  public Result<UserResponse> updateUser(
      @PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(userService.updateUser(id, request, currentUserId));
  }

  @DeleteMapping("/{id}")
  public Result<Void> deleteUser(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    userService.deleteUser(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 4: Enable Spring Data Web support** — add to `AnsibleApplication.java`:

```java
package com.ansible;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class AnsibleApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnsibleApplication.class, args);
  }
}
```

- [ ] **Step 5: Run integration tests — expect PASS**

```bash
cd backend && mvn test -Dtest=UserControllerTest
```

Expected: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Run all tests**

```bash
cd backend && mvn test
```

Expected: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 7: Format and commit**

```bash
cd backend && mvn spotless:apply
git add backend/src/main/java/com/ansible/user/controller/UserController.java \
        backend/src/main/java/com/ansible/AnsibleApplication.java \
        backend/src/test/java/com/ansible/user/controller/UserControllerTest.java
git commit -m "feat: add UserController with get/update/delete — integration tests passing"
```

---

## Task 10: Backend code quality checks

- [ ] **Step 1: Run Checkstyle**

```bash
cd backend && mvn checkstyle:check
```

Fix any reported violations. Common fixes: import order, line length, brace placement.

Expected: `BUILD SUCCESS` — no violations.

- [ ] **Step 2: Run PMD**

```bash
cd backend && mvn pmd:check
```

Fix any reported violations. Common fixes: unused imports, empty catch blocks.

Expected: `BUILD SUCCESS` — no violations.

- [ ] **Step 3: Run SpotBugs**

```bash
cd backend && mvn spotbugs:check
```

Fix any bugs reported. Common fixes: null checks, resource leaks.

Expected: `BUILD SUCCESS` — no bugs.

- [ ] **Step 4: Run OWASP Dependency Check**

Note: First run downloads the NVD vulnerability database — takes 5-10 minutes.

```bash
cd backend && mvn dependency-check:check
```

Expected: `BUILD SUCCESS` — no CVEs above severity 7.

- [ ] **Step 5: Commit if any fixes were applied**

```bash
git add -A
git commit -m "fix: resolve code quality scan violations for auth module"
```

---

## Task 11: Initialize frontend project

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/.eslintrc.cjs`
- Create: `frontend/.prettierrc`
- Create: `frontend/src/main.tsx`

- [ ] **Step 1: Scaffold with Vite**

```bash
cd /c/Users/dingg/code/nine-tower
npm create vite@latest frontend -- --template react-ts
cd frontend
```

- [ ] **Step 2: Install dependencies**

```bash
npm install antd @ant-design/icons axios zustand react-router-dom
npm install -D eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser \
              eslint-plugin-react eslint-plugin-react-hooks prettier vitest \
              @testing-library/react @testing-library/jest-dom @vitejs/plugin-react
```

- [ ] **Step 3: Update `frontend/vite.config.ts`**

```typescript
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
  },
});
```

- [ ] **Step 4: Create `frontend/src/test/setup.ts`**

```typescript
import "@testing-library/jest-dom";
```

- [ ] **Step 5: Create `frontend/.eslintrc.cjs`**

```javascript
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react-hooks/recommended",
  ],
  ignorePatterns: ["dist", ".eslintrc.cjs"],
  parser: "@typescript-eslint/parser",
  plugins: ["react-refresh"],
  rules: {
    "react-refresh/only-export-components": [
      "warn",
      { allowConstantExport: true },
    ],
    "@typescript-eslint/no-explicit-any": "error",
    "no-console": ["warn", { allow: ["warn", "error"] }],
  },
};
```

- [ ] **Step 6: Create `frontend/.prettierrc`**

```json
{
  "semi": true,
  "singleQuote": false,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100
}
```

- [ ] **Step 7: Add scripts to `frontend/package.json`**

Add to the `scripts` section:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "format": "prettier --write src",
    "test": "vitest run",
    "test:watch": "vitest"
  }
}
```

- [ ] **Step 8: Verify lint passes**

```bash
cd frontend && npm run lint
```

Expected: no errors.

- [ ] **Step 9: Commit**

```bash
cd /c/Users/dingg/code/nine-tower
git add frontend/
git commit -m "chore: initialize frontend with Vite + React 18 + TypeScript + Ant Design"
```

---

## Task 12: Frontend types, API layer, auth store

**Files:**
- Create: `frontend/src/types/entity/User.ts`
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/api/user.ts`
- Create: `frontend/src/stores/authStore.ts`

- [ ] **Step 1: Create `frontend/src/types/entity/User.ts`**

```typescript
export interface User {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

export interface TokenResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

- [ ] **Step 2: Create `frontend/src/api/request.ts`**

```typescript
import axios from "axios";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 403 || error.response?.status === 401) {
      localStorage.removeItem("token");
      window.location.href = "/login";
    }
    return Promise.reject(error.response?.data ?? error);
  }
);

export default request;
```

- [ ] **Step 3: Create `frontend/src/api/auth.ts`**

```typescript
import request from "./request";
import type { TokenResponse } from "../types/entity/User";

export interface RegisterPayload {
  username: string;
  password: string;
  email: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export const authApi = {
  register: (payload: RegisterPayload): Promise<ApiResult<TokenResponse>> =>
    request.post("/auth/register", payload),

  login: (payload: LoginPayload): Promise<ApiResult<TokenResponse>> =>
    request.post("/auth/login", payload),

  me: (): Promise<ApiResult<TokenResponse["user"]>> => request.get("/auth/me"),
};
```

- [ ] **Step 4: Create `frontend/src/api/user.ts`**

```typescript
import request from "./request";
import type { User, PageResponse } from "../types/entity/User";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface UpdateUserPayload {
  email?: string;
  password?: string;
}

export const userApi = {
  searchUsers: (
    keyword?: string,
    page = 0,
    size = 20
  ): Promise<ApiResult<PageResponse<User>>> =>
    request.get("/users", { params: { keyword, page, size } }),

  getUser: (id: number): Promise<ApiResult<User>> => request.get(`/users/${id}`),

  updateUser: (id: number, payload: UpdateUserPayload): Promise<ApiResult<User>> =>
    request.put(`/users/${id}`, payload),

  deleteUser: (id: number): Promise<ApiResult<void>> => request.delete(`/users/${id}`),
};
```

- [ ] **Step 5: Create `frontend/src/stores/authStore.ts`**

```typescript
import { create } from "zustand";
import type { User } from "../types/entity/User";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem("token"),
  isAuthenticated: !!localStorage.getItem("token"),

  login: (token: string, user: User) => {
    localStorage.setItem("token", token);
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem("token");
    set({ token: null, user: null, isAuthenticated: false });
  },
}));
```

- [ ] **Step 6: Run lint and format**

```bash
cd frontend && npm run format && npm run lint
```

Expected: no errors.

- [ ] **Step 7: Commit**

```bash
cd /c/Users/dingg/code/nine-tower
git add frontend/src/types/ frontend/src/api/ frontend/src/stores/
git commit -m "feat: add frontend types, API layer, and auth store"
```

---

## Task 13: Login and Register pages

**Files:**
- Create: `frontend/src/pages/auth/Login.tsx`
- Create: `frontend/src/pages/auth/Register.tsx`
- Create: `frontend/src/components/Layout/MainLayout.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`

- [ ] **Step 1: Create `frontend/src/pages/auth/Login.tsx`**

```tsx
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Card, Form, Input, message, Typography } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { authApi, LoginPayload } from "../../api/auth";
import { useAuthStore } from "../../stores/authStore";

const { Title } = Typography;

export default function Login() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<LoginPayload>();

  const onFinish = async (values: LoginPayload) => {
    try {
      const res = await authApi.login(values);
      login(res.data.token, res.data.user);
      message.success("Login successful");
      navigate("/projects");
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? "Login failed");
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f0f2f5",
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={2} style={{ textAlign: "center", marginBottom: 32 }}>
          Ansible Playbook System
        </Title>
        <Form form={form} onFinish={onFinish} layout="vertical" size="large">
          <Form.Item
            name="username"
            rules={[{ required: true, message: "Please enter your username" }]}
          >
            <Input prefix={<UserOutlined />} placeholder="Username" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: "Please enter your password" }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Login
            </Button>
          </Form.Item>
          <div style={{ textAlign: "center" }}>
            Don&apos;t have an account? <Link to="/register">Register</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Create `frontend/src/pages/auth/Register.tsx`**

```tsx
import { LockOutlined, MailOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Card, Form, Input, message, Typography } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { authApi, RegisterPayload } from "../../api/auth";
import { useAuthStore } from "../../stores/authStore";

const { Title } = Typography;

export default function Register() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<RegisterPayload>();

  const onFinish = async (values: RegisterPayload) => {
    try {
      const res = await authApi.register(values);
      login(res.data.token, res.data.user);
      message.success("Registration successful");
      navigate("/projects");
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? "Registration failed");
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f0f2f5",
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={2} style={{ textAlign: "center", marginBottom: 32 }}>
          Create Account
        </Title>
        <Form form={form} onFinish={onFinish} layout="vertical" size="large">
          <Form.Item
            name="username"
            rules={[
              { required: true, message: "Please enter a username" },
              { min: 3, message: "Username must be at least 3 characters" },
              { max: 50, message: "Username must be at most 50 characters" },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="Username" />
          </Form.Item>
          <Form.Item
            name="email"
            rules={[
              { required: true, message: "Please enter your email" },
              { type: "email", message: "Please enter a valid email" },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="Email" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[
              { required: true, message: "Please enter a password" },
              { min: 8, message: "Password must be at least 8 characters" },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Register
            </Button>
          </Form.Item>
          <div style={{ textAlign: "center" }}>
            Already have an account? <Link to="/login">Login</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Create `frontend/src/components/Layout/MainLayout.tsx`**

```tsx
import { LogoutOutlined, ProjectOutlined } from "@ant-design/icons";
import { Avatar, Dropdown, Layout, Menu, MenuProps, Space, Typography } from "antd";
import { useNavigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";

const { Header, Content } = Layout;

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const userMenuItems: MenuProps["items"] = [
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "Logout",
      onClick: () => {
        logout();
        navigate("/login");
      },
    },
  ];

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          background: "#fff",
          padding: "0 24px",
          borderBottom: "1px solid #f0f0f0",
        }}
      >
        <Space
          style={{ cursor: "pointer" }}
          onClick={() => navigate("/projects")}
        >
          <ProjectOutlined style={{ fontSize: 20 }} />
          <Typography.Text strong style={{ fontSize: 16 }}>
            Ansible Playbook System
          </Typography.Text>
        </Space>
        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space style={{ cursor: "pointer" }}>
            <Avatar>{user?.username?.[0]?.toUpperCase()}</Avatar>
            <Typography.Text>{user?.username}</Typography.Text>
          </Space>
        </Dropdown>
      </Header>
      <Content style={{ padding: "24px", background: "#f0f2f5" }}>
        <Outlet />
      </Content>
    </Layout>
  );
}
```

- [ ] **Step 4: Create `frontend/src/App.tsx`**

```tsx
import { Navigate, Route, Routes } from "react-router-dom";
import MainLayout from "./components/Layout/MainLayout";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import { useAuthStore } from "./stores/authStore";

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/projects" replace />} />
        <Route
          path="projects"
          element={<div style={{ padding: 24 }}>Projects — coming in Plan 2</div>}
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
```

- [ ] **Step 5: Update `frontend/src/main.tsx`**

```tsx
import { App } from "antd";
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import AppRoutes from "./App";
import "antd/dist/reset.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App>
        <AppRoutes />
      </App>
    </BrowserRouter>
  </React.StrictMode>
);
```

- [ ] **Step 6: Run lint**

```bash
cd frontend && npm run lint
```

Expected: no errors.

- [ ] **Step 7: Verify app builds**

```bash
cd frontend && npm run build
```

Expected: `dist/` created with no errors.

- [ ] **Step 8: Commit**

```bash
cd /c/Users/dingg/code/nine-tower
git add frontend/src/
git commit -m "feat: add Login, Register pages and MainLayout with auth routing"
```

---

## Task 14: Final end-to-end verification

- [ ] **Step 1: Start PostgreSQL** (if not running)

```bash
docker run -d --name ansible-db -e POSTGRES_DB=ansible_dev \
  -e POSTGRES_USER=ansible -e POSTGRES_PASSWORD=ansible \
  -p 5432:5432 postgres:16
```

- [ ] **Step 2: Start backend**

```bash
cd backend && mvn spring-boot:run
```

Expected: `Started AnsibleApplication` log line.

- [ ] **Step 3: Start frontend**

```bash
cd frontend && npm run dev
```

Expected: `Local: http://localhost:5173/`

- [ ] **Step 4: Manual smoke test**

1. Open `http://localhost:5173/`
2. Redirected to `/login` — verify login page renders
3. Click "Register" — verify register page renders
4. Register a new user — verify redirect to `/projects`
5. Logout from header dropdown — verify redirect to `/login`
6. Login with registered credentials — verify success

- [ ] **Step 5: Run full backend test suite**

```bash
cd backend && mvn verify
```

Expected: `BUILD SUCCESS`, all 15 tests pass.

- [ ] **Step 6: Run frontend tests**

```bash
cd frontend && npm run test
```

Expected: all tests pass (setup file only at this stage — component tests added in later plans).

- [ ] **Step 7: Tag Phase 1 complete**

```bash
git tag phase1-complete
```

---

## Quality Checklist (before declaring Phase 1 done)

- [ ] `cd backend && mvn spotless:check` — no formatting issues
- [ ] `cd backend && mvn checkstyle:check` — no style violations
- [ ] `cd backend && mvn pmd:check` — no rule violations
- [ ] `cd backend && mvn spotbugs:check` — no bugs found
- [ ] `cd backend && mvn dependency-check:check` — no critical CVEs
- [ ] `cd backend && mvn test` — all 15 tests pass
- [ ] `cd frontend && npm run lint` — no ESLint errors
- [ ] `cd frontend && npm run build` — clean build

---

## What comes next

This plan delivers a working authentication foundation. Subsequent plans follow the same pattern (TDD → unit test → integration test → quality checks → commit):

| Plan | Scope |
|------|-------|
| Plan 2 | Project CRUD + Member Management |
| Plan 3 | HostGroup + Host CRUD |
| Plan 4 | Role CRUD |
| Plan 5 | Task + Handler CRUD |
| Plan 6 | Template + File Management |
| Plan 7 | Variable Management (all scopes) |
| Plan 8 | Tag + Environment Management |
| Plan 9 | Playbook Builder + YAML Export |
