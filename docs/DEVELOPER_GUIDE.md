# Developer Quick Reference Guide

## 🚀 Running Tests and Coverage Locally

### Basic Test Execution

```bash
# Run all tests
./gradlew test

# Run tests with coverage report
./gradlew clean test jacocoTestReport

# Run tests and check coverage thresholds
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification
```

### Coverage Reports

```bash
# Generate and view coverage reports
./gradlew jacocoTestReport

# View HTML coverage report
open build/reports/jacoco/test/html/index.html

# Check XML coverage data
cat build/reports/jacoco/test/jacocoTestReport.xml
```

### Update Coverage Badges

```bash
# Update README badges manually
bash scripts/update-coverage-badge.sh

# Check project status
bash scripts/project-status.sh

# Validate all workflow components
bash scripts/validate-workflows.sh
```

## 📊 Coverage Thresholds

| Metric      | Minimum | Target | Color Coding              |
|-------------|---------|--------|---------------------------|
| Instruction | 80%     | 90%+   | 🔴 <50% 🟠 50-79% 🟢 80%+ |
| Branch      | 70%     | 80%+   | 🔴 <50% 🟠 50-79% 🟢 80%+ |
| Line        | 80%     | 90%+   | 🔴 <50% 🟠 50-79% 🟢 80%+ |

## 🔧 GitHub Actions Workflows

### Main Branch Workflow (main-ci.yml)

**Triggers**: Push to `main`, manual dispatch

- ✅ Runs full test suite
- ✅ Generates coverage reports
- ✅ Updates README badges automatically
- ✅ Deploys reports to GitHub Pages
- ✅ Comments coverage on commits

### PR Workflow (pr-tests.yml)

**Triggers**: PR to `main` (opened, sync, reopened)

- ✅ Runs full test suite
- ✅ Validates coverage thresholds
- ✅ Comments coverage summary on PR
- ✅ Uploads test artifacts

## 🛠️ Common Development Tasks

### Adding New Tests

1. Create test files in `src/test/java/`
2. Run `./gradlew test` to verify
3. Check coverage with `./gradlew jacocoTestReport`
4. Commit changes to trigger CI

### Improving Coverage

1. Identify low-coverage areas in HTML report
2. Add missing test cases
3. Verify improvements: `./gradlew clean test jacocoTestReport`
4. Update badges: `bash scripts/update-coverage-badge.sh`

### Debugging Test Failures

```bash
# Run specific test class
./gradlew test --tests "ClassName"

# Run with debug output
./gradlew test --debug

# Check test results
ls build/test-results/test/
```

## 🚨 Troubleshooting

### Coverage Not Updating

- Ensure tests are passing: `./gradlew test`
- Regenerate reports: `./gradlew clean jacocoTestReport`
- Check XML report exists: `ls build/reports/jacoco/test/`

### Badge Update Failures

- Verify script permissions: `ls -la scripts/update-coverage-badge.sh`
- Test script manually: `bash scripts/update-coverage-badge.sh`
- Check XML report format

### GitHub Actions Issues

- Check workflow syntax: `yamllint .github/workflows/*.yml`
- Verify repository permissions in Settings → Actions
- Review workflow run logs in GitHub Actions tab

## 📝 Quality Standards

### Code Coverage

- All new features must include tests
- Minimum 80% instruction coverage required
- Branch coverage should be >70%
- Aim for 90%+ overall coverage

### Test Guidelines

- Use descriptive test method names
- Include both positive and negative test cases
- Test edge cases and error conditions
- Use proper assertions and verify behavior

### CI/CD Best Practices

- Keep workflows fast (<5 minutes)
- Use artifact uploads for debugging
- Monitor coverage trends over time
- Review coverage reports in PRs

## 🔗 Useful Links

- **Coverage Reports**: Available in GitHub Pages after CI runs
- **Workflow Runs**: GitHub → Actions tab
- **Test Results**: Check artifacts in workflow runs
- **JaCoCo Documentation**: https://www.jacoco.org/jacoco/

## 📞 Getting Help

If you encounter issues:

1. Check this guide first
2. Review GitHub Actions workflow logs
3. Run diagnostic scripts locally
4. Check the comprehensive setup documentation in `docs/GITHUB_ACTIONS_SETUP.md`

---
*This guide is automatically maintained with the CI/CD setup. Last updated: May 27, 2025*
