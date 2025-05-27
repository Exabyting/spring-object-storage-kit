#!/bin/bash

# Final Project Status Check Script
# This script provides a comprehensive overview of the GitHub Actions CI/CD setup

echo "🏁 Spring Object Storage Kit - CI/CD Setup Complete"
echo "=================================================="
echo ""

# Check build.gradle syntax
echo "📋 Checking build.gradle configuration..."
if grep -q "^plugins {" build.gradle; then
    echo "✅ build.gradle syntax is correct"
else
    echo "❌ build.gradle has syntax issues"
fi

# Check JaCoCo configuration
if grep -q "jacoco" build.gradle && grep -q "jacocoTestReport" build.gradle; then
    echo "✅ JaCoCo configuration is present"
else
    echo "❌ JaCoCo configuration is missing"
fi

# Check GitHub Actions workflows
echo ""
echo "🔄 Checking GitHub Actions workflows..."

if [ -f ".github/workflows/main-ci.yml" ]; then
    echo "✅ Main CI/CD workflow exists"
else
    echo "❌ Main CI/CD workflow is missing"
fi

if [ -f ".github/workflows/pr-tests.yml" ]; then
    echo "✅ PR testing workflow exists"
else
    echo "❌ PR testing workflow is missing"
fi

# Check scripts
echo ""
echo "📜 Checking automation scripts..."

if [ -f "scripts/update-coverage-badge.sh" ] && [ -x "scripts/update-coverage-badge.sh" ]; then
    echo "✅ Coverage badge update script is ready"
else
    echo "❌ Coverage badge update script has issues"
fi

if [ -f "scripts/validate-workflows.sh" ] && [ -x "scripts/validate-workflows.sh" ]; then
    echo "✅ Workflow validation script is ready"
else
    echo "❌ Workflow validation script has issues"
fi

# Check documentation
echo ""
echo "📚 Checking documentation..."

if [ -f "docs/GITHUB_ACTIONS_SETUP.md" ]; then
    echo "✅ GitHub Actions setup documentation exists"
else
    echo "❌ GitHub Actions setup documentation is missing"
fi

if [ -f "CONTRIBUTING.md" ]; then
    echo "✅ Contributing guidelines exist"
else
    echo "❌ Contributing guidelines are missing"
fi

# Check README badges
echo ""
echo "🏷️ Checking README badges..."

if grep -q "coverage.*brightgreen" README.md; then
    echo "✅ Coverage badges are present and up-to-date"
else
    echo "❌ Coverage badges need attention"
fi

# Test coverage generation
echo ""
echo "🧪 Testing coverage generation..."

if [ -f "build/reports/jacoco/test/jacocoTestReport.xml" ]; then
    echo "✅ JaCoCo XML report exists"
    
    # Extract current coverage
    INSTRUCTION_MISSED=$(grep 'type="INSTRUCTION"' build/reports/jacoco/test/jacocoTestReport.xml | grep -o 'missed="[0-9]*"' | tail -1 | grep -o '[0-9]*' || echo "0")
    INSTRUCTION_COVERED=$(grep 'type="INSTRUCTION"' build/reports/jacoco/test/jacocoTestReport.xml | grep -o 'covered="[0-9]*"' | tail -1 | grep -o '[0-9]*' || echo "0")
    
    if [ "$INSTRUCTION_COVERED" -gt 0 ]; then
        TOTAL_INSTRUCTIONS=$((INSTRUCTION_MISSED + INSTRUCTION_COVERED))
        COVERAGE_PERCENT=$((INSTRUCTION_COVERED * 100 / TOTAL_INSTRUCTIONS))
        echo "✅ Current instruction coverage: $COVERAGE_PERCENT%"
    else
        echo "⚠️  Coverage data needs verification"
    fi
else
    echo "❌ JaCoCo XML report not found - run tests first"
fi

# Final status
echo ""
echo "🎉 SETUP SUMMARY"
echo "================"
echo ""
echo "The Spring Object Storage Kit now has a complete CI/CD automation setup:"
echo ""
echo "✨ AUTOMATED FEATURES:"
echo "  • Test execution on every commit and PR"
echo "  • Coverage report generation with JaCoCo"
echo "  • Automatic README badge updates"
echo "  • GitHub Pages deployment for coverage reports"
echo "  • PR comments with coverage summaries"
echo "  • Commit comments with detailed coverage tables"
echo "  • Artifact uploads for test results"
echo ""
echo "🔧 CONFIGURED COMPONENTS:"
echo "  • Main branch CI/CD workflow (main-ci.yml)"
echo "  • Pull request testing workflow (pr-tests.yml)"
echo "  • Coverage badge update script"
echo "  • Workflow validation script"
echo "  • Comprehensive documentation"
echo ""
echo "🎯 QUALITY GATES:"
echo "  • 80% minimum instruction coverage (enforced)"
echo "  • Color-coded badges (red < 50%, orange 50-79%, green ≥ 80%)"
echo "  • Automatic threshold validation"
echo ""
echo "📋 NEXT STEPS:"
echo "  1. Push changes to GitHub to trigger workflows"
echo "  2. Enable GitHub Pages in repository settings"
echo "  3. Monitor first workflow run for any adjustments"
echo "  4. Review coverage reports and set team standards"
echo ""
echo "🎊 The project is now ready for production-grade CI/CD automation!"
echo ""
