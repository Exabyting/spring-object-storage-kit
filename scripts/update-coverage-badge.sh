#!/bin/bash

# Script to update coverage badge in README.md
# This script parses the JaCoCo XML report and updates the coverage badge

set -e

JACOCO_REPORT="build/reports/jacoco/test/jacocoTestReport.xml"
README_FILE="README.md"

if [ ! -f "$JACOCO_REPORT" ]; then
    echo "Error: JaCoCo report not found at $JACOCO_REPORT"
    echo "Please run: ./gradlew clean test jacocoTestReport"
    exit 1
fi

# Extract coverage percentage from JaCoCo XML report
# Look for the overall summary at the end of the file
INSTRUCTION_MISSED=$(grep 'type="INSTRUCTION"' "$JACOCO_REPORT" | grep -o 'missed="[0-9]*"' | tail -1 | grep -o '[0-9]*')
INSTRUCTION_COVERED=$(grep 'type="INSTRUCTION"' "$JACOCO_REPORT" | grep -o 'covered="[0-9]*"' | tail -1 | grep -o '[0-9]*')

# Also extract branch and line coverage
BRANCH_MISSED=$(grep 'type="BRANCH"' "$JACOCO_REPORT" | grep -o 'missed="[0-9]*"' | tail -1 | grep -o '[0-9]*')
BRANCH_COVERED=$(grep 'type="BRANCH"' "$JACOCO_REPORT" | grep -o 'covered="[0-9]*"' | tail -1 | grep -o '[0-9]*')

# Extract line coverage from the XML
LINE_MISSED=$(grep 'type="LINE"' "$JACOCO_REPORT" | grep -o 'missed="[0-9]*"' | tail -1 | grep -o '[0-9]*')
LINE_COVERED=$(grep 'type="LINE"' "$JACOCO_REPORT" | grep -o 'covered="[0-9]*"' | tail -1 | grep -o '[0-9]*')

if [ -z "$INSTRUCTION_MISSED" ] || [ -z "$INSTRUCTION_COVERED" ]; then
    echo "Error: Could not parse instruction coverage data from $JACOCO_REPORT"
    echo "This might indicate an issue with the JaCoCo report format or test execution."
    exit 1
fi

# Set defaults for missing values
BRANCH_MISSED=${BRANCH_MISSED:-0}
BRANCH_COVERED=${BRANCH_COVERED:-0}
LINE_MISSED=${LINE_MISSED:-0}
LINE_COVERED=${LINE_COVERED:-0}

# Calculate coverage percentages
TOTAL_INSTRUCTIONS=$((INSTRUCTION_MISSED + INSTRUCTION_COVERED))
INSTRUCTION_PERCENT=$((INSTRUCTION_COVERED * 100 / TOTAL_INSTRUCTIONS))

TOTAL_BRANCHES=$((BRANCH_MISSED + BRANCH_COVERED))
BRANCH_PERCENT=0
if [ "$TOTAL_BRANCHES" -gt 0 ]; then
    BRANCH_PERCENT=$((BRANCH_COVERED * 100 / TOTAL_BRANCHES))
fi

TOTAL_LINES=$((LINE_MISSED + LINE_COVERED))
LINE_PERCENT=0
if [ "$TOTAL_LINES" -gt 0 ]; then
    LINE_PERCENT=$((LINE_COVERED * 100 / TOTAL_LINES))
fi

echo "Instruction Coverage: $INSTRUCTION_PERCENT% ($INSTRUCTION_COVERED/$TOTAL_INSTRUCTIONS)"
echo "Branch Coverage: $BRANCH_PERCENT% ($BRANCH_COVERED/$TOTAL_BRANCHES)"
echo "Line Coverage: $LINE_PERCENT% ($LINE_COVERED/$TOTAL_LINES)"

# Function to determine badge color based on coverage
get_badge_color() {
    local percent=$1
    if [ "$percent" -ge 90 ]; then
        echo "brightgreen"
    elif [ "$percent" -ge 80 ]; then
        echo "green"
    elif [ "$percent" -ge 70 ]; then
        echo "yellow"
    elif [ "$percent" -ge 60 ]; then
        echo "orange"
    else
        echo "red"
    fi
}

INSTRUCTION_COLOR=$(get_badge_color $INSTRUCTION_PERCENT)
BRANCH_COLOR=$(get_badge_color $BRANCH_PERCENT)
LINE_COLOR=$(get_badge_color $LINE_PERCENT)

# Update badges in README.md
# Main coverage badge (instruction coverage)
sed -i.bak "s/coverage-[0-9]*%25-[a-z]*/coverage-${INSTRUCTION_PERCENT}%25-${INSTRUCTION_COLOR}/g" "$README_FILE"

# Line coverage badge
sed -i.bak "s/line%20coverage-[0-9]*%25-[a-z]*/line%20coverage-${LINE_PERCENT}%25-${LINE_COLOR}/g" "$README_FILE"

# Branch coverage badge
sed -i.bak "s/branch%20coverage-[0-9]*%25-[a-z]*/branch%20coverage-${BRANCH_PERCENT}%25-${BRANCH_COLOR}/g" "$README_FILE"

# Clean up backup file
rm -f "$README_FILE.bak"

echo "Updated coverage badges:"
echo "  - Instruction coverage: $INSTRUCTION_PERCENT% (${INSTRUCTION_COLOR})"
echo "  - Line coverage: $LINE_PERCENT% (${LINE_COLOR})"
echo "  - Branch coverage: $BRANCH_PERCENT% (${BRANCH_COLOR})"
echo "Changes made to $README_FILE"
