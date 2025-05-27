# Contributing to Spring Object Storage Kit (SpringOSK)

We welcome contributions to the Spring Object Storage Kit! This document provides guidelines and information for
contributors.

## 📋 Table of Contents

- [Code of Conduct](#-code-of-conduct)
- [Getting Started](#-getting-started)
- [Development Setup](#-development-setup)
- [Project Structure](#-project-structure)
- [Development Guidelines](#-development-guidelines)
- [Testing](#-testing)
- [Pull Request Process](#-pull-request-process)
- [Issue Guidelines](#-issue-guidelines)
- [Coding Standards](#-coding-standards)
- [Documentation](#-documentation)
- [Release Process](#-release-process)
- [Getting Help](#-getting-help)
- [Recognition](#-recognition)
- [License](#-license)

## 🤝 Code of Conduct

By participating in this project, you agree to abide by our code of conduct:

- Be respectful and considerate towards others
- Use welcoming and professional language
- Focus on what is best for the community
- Show empathy towards other community members
- Accept constructive criticism gracefully

## 🚀 Getting Started

### Prerequisites

- **Java 17+** - Required for building and running the project
- **Docker** - Required for integration tests with Testcontainers
- **Git** - For version control
- **IDE** - IntelliJ IDEA, Eclipse, or VS Code recommended

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/spring-object-storage-kit.git
   cd spring-object-storage-kit
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/exabyting/spring-object-storage-kit.git
   ```

## 🛠️ Development Setup

### Build the Project

```bash
# Clean build
./gradlew clean build

# Skip tests for faster build
./gradlew clean build -x test

# Build without daemon (for CI environments)
./gradlew clean build --no-daemon
```

### Run Tests

```bash
# Run tests and generate coverage report
./gradlew test jacocoTestReport

# Run tests with detailed output
./gradlew test --info

# Run tests and generate coverage report
./gradlew test jacocoTestReport

# Verify coverage meets minimum threshold (80%)
./gradlew jacocoTestCoverageVerification
```

### Local Development Environment

For local development with real services:

#### MinIO Setup

```bash
# Using Docker
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

#### LocalStack (AWS S3 emulation)

```bash
# Using Docker
docker run -p 4566:4566 localstack/localstack
```

## 📁 Project Structure

```
spring-object-storage-kit/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/exabyting/springosk/
│   │   │       ├── config/           # Spring configuration classes
│   │   │       ├── core/             # Core client implementations
│   │   │       ├── minio/            # MinIO-specific implementations
│   │   │       ├── s3/               # AWS S3-specific implementations
│   │   │       ├── properties/       # Configuration properties
│   │   │       ├── annotation/       # Custom annotations
│   │   │       └── validator/        # Validation logic
│   │   └── resources/
│   │       └── application.yml       # Default configuration
│   └── test/
│       └── java/
│           └── com/exabyting/springosk/
│               ├── config/           # Configuration tests
│               ├── core/             # Core functionality tests
│               ├── integration/      # Integration tests
│               ├── minio/            # MinIO-specific tests
│               ├── s3/               # S3-specific tests
│               ├── properties/       # Properties tests
│               └── validation/       # Validation tests
├── build.gradle                     # Build configuration
├── settings.gradle                  # Project settings
├── README.md                        # Project documentation
├── CONTRIBUTING.md                  # This file
└── LICENSE                          # MIT License
```

## 📝 Development Guidelines

### Branching Strategy

- `main` - Production-ready code
- `dev` - Integration branch for features
- `feature/feature-name` - New features
- `bugfix/bug-description` - Bug fixes
- `hotfix/critical-fix` - Critical production fixes

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
type(scope): description

feat(s3): add multipart upload support
fix(minio): resolve connection timeout issue
docs(readme): update configuration examples
test(integration): add LocalStack integration tests
refactor(core): simplify client interface
chore(deps): update AWS SDK to latest version
```

Types:

- `feat` - New features
- `fix` - Bug fixes
- `docs` - Documentation changes
- `test` - Test additions/modifications
- `refactor` - Code refactoring
- `chore` - Maintenance tasks
- `perf` - Performance improvements

### Code Organization

#### Package Guidelines

- `config` - Spring Boot configuration and bean definitions
- `core` - Main client interfaces and common functionality
- `s3` - AWS S3-specific implementations
- `minio` - MinIO-specific implementations
- `properties` - Configuration properties and validation
- `annotation` - Custom annotations
- `validator` - Validation logic

#### Class Naming Conventions

- Configuration classes: `*Config`
- Service implementations: `*Operations`
- Test classes: `*Test`
- Integration tests: `*IntegrationTest`
- Properties classes: `*Properties`

## 🧪 Testing

### Testing Strategy

We use a comprehensive testing approach:

1. **Unit Tests** - Test individual components in isolation
2. **Integration Tests** - Test with real services using Testcontainers
3. **Configuration Tests** - Test Spring Boot configuration
4. **Validation Tests** - Test property validation

### Testcontainers Integration

The project uses Testcontainers for integration testing:

#### MinIO Tests

```java

@Testcontainers
@SpringBootTest
class MinIOIntegrationTest {

    @Container
    static GenericContainer<?> minioContainer =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withExposedPorts(9000, 9001)
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withCommand("server", "/data", "--console-address", ":9001");
}
```

#### AWS S3 Tests (LocalStack)

```java

@Testcontainers
@SpringBootTest
class S3IntegrationTest {

    @Container
    static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                    .withServices(LocalStackContainer.Service.S3);
}
```

### Test Guidelines

1. **Test Naming**: Use descriptive names following the pattern `should_[ExpectedBehavior]_when_[StateUnderTest]`
2. **Test Organization**: Group related tests in nested classes
3. **Test Data**: Use meaningful test data that reflects real-world scenarios
4. **Cleanup**: Always clean up resources in `@AfterEach` methods
5. **Isolation**: Each test should be independent and not rely on others

### Test Configuration

Use `@DynamicPropertySource` for test-specific configuration:

```java

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("object-storage-kit.storage-type", () -> "minio");
    registry.add("object-storage-kit.endpoint", minioContainer::getS3URL);
    registry.add("object-storage-kit.access-key", minioContainer::getUserName);
    registry.add("object-storage-kit.secret-key", minioContainer::getPassword);
}
```

## 🔄 Pull Request Process

### Before Submitting

1. **Sync with upstream**:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes** following the coding standards

4. **Run tests locally**:
   ```bash
   ./gradlew clean test
   ```

5. **Update documentation** if needed

### Submitting the PR

1. **Push your branch**:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create the pull request** on GitHub with:
    - Clear title and description
    - Reference to related issues
    - Summary of changes
    - Testing instructions

### PR Requirements

- ✅ All tests pass
- ✅ Code follows project standards
- ✅ Documentation updated if needed
- ✅ No merge conflicts
- ✅ Signed commits (if required)
- ✅ Appropriate test coverage

### Review Process

1. **Automated checks** run (CI/CD pipeline)
2. **Code review** by maintainers
3. **Address feedback** and update the PR
4. **Final approval** and merge

## 🐛 Issue Guidelines

### Bug Reports

Include the following information:

```markdown
**Environment:**

- SpringOSK version:
- Spring Boot version:
- Java version:
- Storage provider: [S3/MinIO]

**Description:**
Brief description of the issue

**Steps to Reproduce:**

1. Step one
2. Step two
3. Step three

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Configuration:**

```yaml
object-storage-kit:
  storage-type: s3
  # your configuration
```

**Logs/Stack Trace:**

```
Paste relevant logs here
```

```

### Feature Requests

Use this template:

```markdown
**Feature Description:**
Brief description of the feature

**Use Case:**
Why is this feature needed?

**Proposed Solution:**
How should this be implemented?

**Alternatives Considered:**
Other approaches you've considered

**Additional Context:**
Any other relevant information
```

## 📏 Coding Standards

### Java Style Guide

Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with these additions:

#### Formatting

- **Line length**: 120 characters
- **Indentation**: 4 spaces (no tabs)
- **Braces**: Always use braces, even for single statements

#### Naming Conventions

- **Classes**: PascalCase (`ObjectStorageClient`)
- **Methods**: camelCase (`putObject`, `createBucket`)
- **Constants**: UPPER_SNAKE_CASE (`DEFAULT_TIMEOUT`)
- **Variables**: camelCase (`bucketName`, `accessKey`)

#### Documentation

- **Public APIs**: Always include Javadoc
- **Complex logic**: Add inline comments
- **Configuration**: Document all properties

### Code Quality

#### Required Annotations

```java
@Slf4j          // For logging
@RequiredArgsConstructor  // For constructor injection
@Validated      // For method parameter validation
@NonNull        // For null-safety
```

#### Exception Handling

```java
// Good: Specific exception with context
throw new ObjectStorageException("Failed to upload object to bucket: "+bucketName, e);

// Bad: Generic exception
throw new

RuntimeException(e);
```

#### Logging Guidelines

```java
// Use appropriate log levels
log.debug("Processing object: {}",objectKey);
log.

info("Successfully uploaded object: {} to bucket: {}",objectKey, bucketName);
log.

warn("Retrying failed operation: {}",operation);
log.

error("Failed to connect to storage provider",exception);
```

### Configuration Properties

Follow these patterns:

```java

@ConfigurationProperties(prefix = "object-storage-kit")
@Validated
@Data
public class OskProperties {

    @NotNull
    @ValidEnum(enumClass = StorageType.class)
    private StorageType storageType;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]{1,61}[a-z0-9]$")
    private String defaultBucket;
}
```

## 📚 Documentation

### Code Documentation

- **Public APIs**: Complete Javadoc with examples
- **Configuration**: Document all properties in application.yml
- **README**: Keep examples up-to-date

### Examples

Provide working examples for common use cases:

```java
/**
 * Uploads a file to the specified bucket.
 *
 * @param bucketName the name of the bucket
 * @param objectKey the key for the object
 * @param data the file content as byte array
 * @return true if upload was successful, false otherwise
 * @throws ObjectStorageException if upload fails
 *
 * @example
 * <pre>
 * byte[] fileContent = "Hello World".getBytes();
 * boolean success = client.putObject("my-bucket", "hello.txt", fileContent);
 * </pre>
 */
Boolean putObject(String bucketName, String objectKey, byte[] data);
```

## 🚀 Release Process

### Version Management

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Release Steps

1. **Update version** in `build.gradle`
2. **Update CHANGELOG.md** with release notes
3. **Create release branch**: `release/v1.2.0`
4. **Final testing** and bug fixes
5. **Merge to main** and tag release
6. **Publish artifacts** to Maven Central
7. **Create GitHub release** with release notes

### Pre-release Checklist

- [ ] All tests pass
- [ ] Documentation updated
- [ ] Version bumped appropriately
- [ ] CHANGELOG.md updated
- [ ] No breaking changes in minor releases
- [ ] Migration guide for major releases

## 🆘 Getting Help

### Communication Channels

- **GitHub Issues** - Bug reports and feature requests
- **GitHub Discussions** - Questions and community support
- **Email** - support@exabyting.com for private inquiries

### Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [Testcontainers Documentation](https://www.testcontainers.org/)

## 🙏 Recognition

Contributors are recognized in several ways:

- **CONTRIBUTORS.md** - Listed as project contributors
- **Release Notes** - Mentioned in release announcements
- **GitHub** - Contributors shown on repository page

## 📄 License

By contributing to Spring Object Storage Kit, you agree that your contributions will be licensed under
the [MIT License](LICENSE).

---

Thank you for contributing to Spring Object Storage Kit! 🎉

For questions about contributing, please open an issue or contact the maintainers.
