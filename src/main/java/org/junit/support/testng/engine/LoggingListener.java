/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import java.util.logging.Logger;

import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

class LoggingListener extends DefaultListener {

	static final LoggingListener INSTANCE = new LoggingListener();
	private static final Logger LOGGER = Logger.getLogger(LoggingListener.class.getName());

	private LoggingListener() {
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		LOGGER.fine(() -> "onBeforeClass: " + testClass);
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		LOGGER.fine(() -> "onAfterClass: " + testClass);
	}

	@Override
	public void onTestStart(ITestResult result) {
		LOGGER.fine(() -> "onTestStart: " + result);
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		LOGGER.fine(() -> "onTestSuccess: " + result);
	}

	@Override
	public void onTestFailure(ITestResult result) {
		LOGGER.fine(() -> "onTestFailure: " + result);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		LOGGER.fine(() -> "onTestSkipped: " + result);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		LOGGER.fine(() -> "onTestFailedButWithinSuccessPercentage: " + result);
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result) {
		LOGGER.fine(() -> "onTestFailedWithTimeout: " + result);
	}

	@Override
	public void onStart(ITestContext context) {
		LOGGER.fine(() -> "onStart: " + context);
	}

	@Override
	public void onFinish(ITestContext context) {
		LOGGER.fine(() -> "onFinish: " + context);
	}

	@Override
	public void onConfigurationSuccess(ITestResult tr) {
		LOGGER.fine(() -> "onConfigurationSuccess: " + tr);
	}

	@Override
	public void onConfigurationSuccess(ITestResult tr, ITestNGMethod tm) {
		LOGGER.fine(() -> "onConfigurationSuccess: " + tr + ", " + tm);
	}

	@Override
	public void onConfigurationFailure(ITestResult tr) {
		LOGGER.fine(() -> "onConfigurationFailure: " + tr);
	}

	@Override
	public void onConfigurationFailure(ITestResult tr, ITestNGMethod tm) {
		LOGGER.fine(() -> "onConfigurationFailure: " + tr + ", " + tm);
	}

	@Override
	public void onConfigurationSkip(ITestResult tr) {
		LOGGER.fine(() -> "onConfigurationSkip: " + tr);
	}

	@Override
	public void onConfigurationSkip(ITestResult tr, ITestNGMethod tm) {
		LOGGER.fine(() -> "onConfigurationSkip: " + tr + ", " + tm);
	}

	@Override
	public void beforeConfiguration(ITestResult tr) {
		LOGGER.fine(() -> "beforeConfiguration: " + tr);
	}

	@Override
	public void beforeConfiguration(ITestResult tr, ITestNGMethod tm) {
		LOGGER.fine(() -> "beforeConfiguration: " + tr + ", " + tm);
	}
}
