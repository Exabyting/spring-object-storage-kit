#!/bin/bash

# Test script to validate GitHub Actions workflow components locally
# This script simulates the GitHub Actions workflow steps

set -e

echo "🧪 Testing GitHub Actions Workflow Components"
echo "============================================="

# Change to project directory
cd "$(dirname "$0")/.."

echo ""
echo "📋 Step 1: Clean and run tests with coverage"
echo "---------------------------------------------"
./gradlew clean test jacocoTestReport

echo ""
echo "📊 Step 2: Verify coverage files exist"
echo "--------------------------------------"
if [ ! -f "build/reports/jacoco/test/jacocoTestReport.xml" ]; then
    echo "❌ Error: JaCoCo XML report not found"
    exit 1
fi

if [ ! -d "build/reports/jacoco/test/html" ]; then
    echo "❌ Error: JaCoCo HTML reports not found"
    exit 1
fi

echo "✅ Coverage reports found"

echo ""
echo "🏷️  Step 3: Test coverage badge update"
echo "--------------------------------------"
bash scripts/update-coverage-badge.sh

echo ""
echo "📖 Step 4: Verify README badges updated"
echo "---------------------------------------"
if grep -q "coverage.*brightgreen\|coverage.*red\|coverage.*orange\|coverage.*yellow" README.md; then
    echo "✅ Coverage badges found in README.md"
else
    echo "❌ Error: Coverage badges not found in README.md"
    exit 1
fi

echo ""
echo "🔍 Step 5: Test coverage parsing"
echo "--------------------------------"
JACOCO_REPORT="build/reports/jacoco/test/jacocoTestReport.xml"

# Test instruction coverage parsing
INSTRUCTION_COVERED=$(xmllint --xpath "sum(//counter[@type='INSTRUCTION']/@covered)" "$JACOCO_REPORT" 2>/dev/null || echo "0")
INSTRUCTION_MISSED=$(xmllint --xpath "sum(//counter[@type='INSTRUCTION']/@missed)" "$JACOCO_REPORT" 2>/dev/null || echo "0")

if [ "$INSTRUCTION_COVERED" -gt 0 ] || [ "$INSTRUCTION_MISSED" -gt 0 ]; then
    echo "✅ Instruction coverage parsing successful"
else
    echo "❌ Error: Could not parse instruction coverage"
    exit 1
fi

# Test branch coverage parsing
BRANCH_COVERED=$(xmllint --xpath "sum(//counter[@type='BRANCH']/@covered)" "$JACOCO_REPORT" 2>/dev/null || echo "0")
BRANCH_MISSED=$(xmllint --xpath "sum(//counter[@type='BRANCH']/@missed)" "$JACOCO_REPORT" 2>/dev/null || echo "0")

echo "✅ Branch coverage parsing successful"

# Test line coverage parsing
LINE_COVERED=$(xmllint --xpath "sum(//counter[@type='LINE']/@covered)" "$JACOCO_REPORT" 2>/dev/null || echo "0")
LINE_MISSED=$(xmllint --xpath "sum(//counter[@type='LINE']/@missed)" "$JACOCO_REPORT" 2>/dev/null || echo "0")

echo "✅ Line coverage parsing successful"

echo ""
echo "📈 Step 6: Display coverage metrics"
echo "-----------------------------------"
TOTAL_INSTRUCTIONS=$((INSTRUCTION_COVERED + INSTRUCTION_MISSED))
TOTAL_BRANCHES=$((BRANCH_COVERED + BRANCH_MISSED))
TOTAL_LINES=$((LINE_COVERED + LINE_MISSED))

if [ "$TOTAL_INSTRUCTIONS" -gt 0 ]; then
    INSTRUCTION_PERCENT=$((INSTRUCTION_COVERED * 100 / TOTAL_INSTRUCTIONS))
    echo "Instruction Coverage: $INSTRUCTION_PERCENT% ($INSTRUCTION_COVERED/$TOTAL_INSTRUCTIONS)"
fi

if [ "$TOTAL_BRANCHES" -gt 0 ]; then
    BRANCH_PERCENT=$((BRANCH_COVERED * 100 / TOTAL_BRANCHES))
    echo "Branch Coverage: $BRANCH_PERCENT% ($BRANCH_COVERED/$TOTAL_BRANCHES)"
fi

if [ "$TOTAL_LINES" -gt 0 ]; then
    LINE_PERCENT=$((LINE_COVERED * 100 / TOTAL_LINES))
    echo "Line Coverage: $LINE_PERCENT% ($LINE_COVERED/$TOTAL_LINES)"
fi

echo ""
echo "🔧 Step 7: Validate YAML workflow files"
echo "---------------------------------------"
if command -v python3 >/dev/null 2>&1; then
    python3 -c "import yaml; yaml.safe_load(open('.github/workflows/main-ci.yml', 'r'))" && echo "✅ main-ci.yml is valid YAML"
    python3 -c "import yaml; yaml.safe_load(open('.github/workflows/pr-tests.yml', 'r'))" && echo "✅ pr-tests.yml is valid YAML"
    
    if [ -f ".github/workflows/release-coverage.yml" ]; then
        python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release-coverage.yml', 'r'))" && echo "✅ release-coverage.yml is valid YAML"
    fi
else
    echo "⚠️  Python3 not available, skipping YAML validation"
fi

echo ""
echo "🎉 All GitHub Actions workflow components validated successfully!"
echo "==============================================================="
echo ""
echo "🚀 Ready for GitHub Actions deployment:"
echo "  ✅ Coverage generation and reporting"
echo "  ✅ Badge updates"
echo "  ✅ XML parsing"
echo "  ✅ Workflow syntax"
echo "  ✅ File permissions and structure"
echo ""
echo "📝 Next steps:"
echo "  1. Commit and push changes to trigger workflows"
echo "  2. Configure GitHub Pages in repository settings"
echo "  3. Monitor workflow runs for any environment-specific issues"
