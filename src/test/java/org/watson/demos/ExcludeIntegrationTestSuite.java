package org.watson.demos;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Execution(ExecutionMode.CONCURRENT)
@SelectPackages("org.watson.demos")
@ExcludeTags(IntegrationTestSuite.TAG)
@ExcludeClassNamePatterns(".*IntegrationTest.*")
@Suite
public class ExcludeIntegrationTestSuite {
}
