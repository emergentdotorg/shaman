/*
 * Copyright 2010 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emergent.android.weave.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A test suite containing all tests for my application.
 *
 * @author Patrick Woodworth
 */
public class AllTests extends TestSuite {
  public static Test suite() {
//    return new android.test.suitebuilder.TestSuiteBuilder(AllTests.class).includeAllPackagesUnderHere().build();
    TestSuite retval = new TestSuite();
//    retval.addTestSuite(UserWeaveTest.class);
    return retval;
  }
}
