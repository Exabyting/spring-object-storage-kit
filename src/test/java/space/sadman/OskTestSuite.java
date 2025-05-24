package space.sadman;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test suite to run all tests in the project.
 * This class uses JUnit 5's @Suite annotation to discover and run all tests
 * in the specified packages.
 */
@Suite
@SuiteDisplayName("OSK (Object Storage Kit) Test Suite")
@SelectPackages({
        "space.sadman.annotation",
        "space.sadman.core", 
        "space.sadman.properties",
        "space.sadman.validator"
})
public class OskTestSuite {
    // This class serves as the entry point for running all tests
    // The @SelectPackages annotation tells JUnit to scan for tests in the specified packages
}
