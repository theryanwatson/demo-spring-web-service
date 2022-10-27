package org.watson.demos;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@SuiteDisplayName(IntegrationSuite.TAG + " Test Suite")
@SelectPackages("org.watson.demos")
@IncludeTags(IntegrationSuite.TAG)
@ExcludeClassNamePatterns(".*Suite.*")
@IncludeClassNamePatterns(".*IntegrationTest.*")
@Suite
public class IntegrationSuite {
    public static final String TAG = "Integration";
}
