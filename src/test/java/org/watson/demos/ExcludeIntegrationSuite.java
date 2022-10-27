package org.watson.demos;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@SuiteDisplayName("Exclude " + IntegrationSuite.TAG + " Test Suite")
@Execution(ExecutionMode.CONCURRENT)
@SelectPackages("org.watson.demos")
@ExcludeTags(IntegrationSuite.TAG)
@ExcludeClassNamePatterns({".*IntegrationTest.*", ".*Suite.*"})
@Suite
public class ExcludeIntegrationSuite {
}
