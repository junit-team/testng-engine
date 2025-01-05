/*
 * Copyright 2021-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import java.util.List;

import org.testng.IAlterSuiteListener;
import org.testng.IClassListener;
import org.testng.IConfigurationListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

abstract class DefaultListener implements IClassListener, ITestListener, IConfigurationListener, IAlterSuiteListener {

	@Override
	public void alter(List<XmlSuite> suites) {
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
	}

	@Override
	public void onTestStart(ITestResult result) {
	}

	@Override
	public void onTestSuccess(ITestResult result) {
	}

	@Override
	public void onTestFailure(ITestResult result) {
	}

	@Override
	public void onTestSkipped(ITestResult result) {
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result) {
	}

	@Override
	public void onStart(ITestContext context) {
	}

	@Override
	public void onFinish(ITestContext context) {
	}

	@Override
	public void onConfigurationSuccess(ITestResult tr) {
	}

	@Override
	public void onConfigurationSuccess(ITestResult tr, ITestNGMethod tm) {
	}

	@Override
	public void onConfigurationFailure(ITestResult tr) {
	}

	@Override
	public void onConfigurationFailure(ITestResult tr, ITestNGMethod tm) {
	}

	@Override
	public void onConfigurationSkip(ITestResult tr) {
	}

	@Override
	public void onConfigurationSkip(ITestResult tr, ITestNGMethod tm) {
	}

	@Override
	public void beforeConfiguration(ITestResult tr) {
	}

	@Override
	public void beforeConfiguration(ITestResult tr, ITestNGMethod tm) {
	}
}
