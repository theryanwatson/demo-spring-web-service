package org.watson.demos;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@SuiteDisplayName(IntegrationTestSuite.TAG + " Test Suite")
@SelectPackages("org.watson.demos")
@IncludeTags(IntegrationTestSuite.TAG)
@IncludeClassNamePatterns(".*IntegrationTest.*")
@Suite
public class IntegrationTestSuite {
    public static final String TAG = "Integration";
}
