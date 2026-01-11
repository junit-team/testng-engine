/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.basics;

import static org.testng.Assert.fail;

import example.dataproviders.DataProviders;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.Test;

@Test(retryAnalyzer = RetriedTestCase.MyRetryAnalyzer.class)
public class RetriedTestCase {

	int runs;

	public void test() {
		if (runs++ == 0) {
			fail("retry");
		}
	}

	@Test(retryAnalyzer = MyRetryAnalyzer.class, dataProvider = "ints", dataProviderClass = DataProviders.class)
	public void dataProviderTest(int value) {
		if (runs++ == 0) {
			fail("retry @ " + value);
		}
	}

	public static class MyRetryAnalyzer implements IRetryAnalyzer {
		@Override
		public boolean retry(ITestResult result) {
			return true;
		}
	}
}
