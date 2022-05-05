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

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

class LoggingListener extends DefaultListener {

	static final LoggingListener INSTANCE = new LoggingListener();
	private static final Logger LOGGER = Logger.getLogger(LoggingListener.class.getName());

	private LoggingListener() {
	}

	@Override
	public void alter(List<XmlSuite> suites) {
		log(() -> "alter: " + suites);
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		log(() -> "onBeforeClass: " + testClass);
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		log(() -> "onAfterClass: " + testClass);
	}

	@Override
	public void onTestStart(ITestResult result) {
		log(() -> "onTestStart: " + result);
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		log(() -> "onTestSuccess: " + result);
	}

	@Override
	public void onTestFailure(ITestResult result) {
		log(() -> "onTestFailure: " + result);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		log(() -> "onTestSkipped: " + result);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		log(() -> "onTestFailedButWithinSuccessPercentage: " + result);
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result) {
		log(() -> "onTestFailedWithTimeout: " + result);
	}

	@Override
	public void onStart(ITestContext context) {
		log(() -> "onStart: " + context);
	}

	@Override
	public void onFinish(ITestContext context) {
		log(() -> "onFinish: " + context);
	}

	@Override
	public void onConfigurationSuccess(ITestResult tr) {
		log(() -> "onConfigurationSuccess: " + tr);
	}

	@Override
	public void onConfigurationSuccess(ITestResult tr, ITestNGMethod tm) {
		log(() -> "onConfigurationSuccess: " + tr + ", " + tm);
	}

	@Override
	public void onConfigurationFailure(ITestResult tr) {
		log(() -> "onConfigurationFailure: " + tr);
	}

	@Override
	public void onConfigurationFailure(ITestResult tr, ITestNGMethod tm) {
		log(() -> "onConfigurationFailure: " + tr + ", " + tm);
	}

	@Override
	public void onConfigurationSkip(ITestResult tr) {
		log(() -> "onConfigurationSkip: " + tr);
	}

	@Override
	public void onConfigurationSkip(ITestResult tr, ITestNGMethod tm) {
		log(() -> "onConfigurationSkip: " + tr + ", " + tm);
	}

	@Override
	public void beforeConfiguration(ITestResult tr) {
		log(() -> "beforeConfiguration: " + tr);
	}

	@Override
	public void beforeConfiguration(ITestResult tr, ITestNGMethod tm) {
		log(() -> "beforeConfiguration: " + tr + ", " + tm);
	}

	private void log(Supplier<String> message) {
		LOGGER.fine(message);
	}
}
