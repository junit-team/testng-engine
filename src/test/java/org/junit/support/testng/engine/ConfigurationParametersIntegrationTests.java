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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;

import example.configuration.parameters.DataProviderThreadCountTestCase;
import example.configuration.parameters.InvocationTrackingListener;
import example.configuration.parameters.ParallelMethodsTestCase;
import example.configuration.parameters.PreserveOrderTestCase;
import example.configuration.parameters.ReturnValuesTestCase;
import example.configuration.parameters.SystemPropertyProvidingListener;
import example.configuration.parameters.SystemPropertyReadingTestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationParametersIntegrationTests extends AbstractIntegrationTests {

	@Test
	void registersCustomListeners() {
		var testClass = SystemPropertyReadingTestCase.class;
		InvocationTrackingListener.invoked = false;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.listeners", SystemPropertyProvidingListener.class.getName() + " , "
						+ InvocationTrackingListener.class.getName()) //
				.execute();

		assertThat(InvocationTrackingListener.invoked).isTrue();
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void executesTestMethodsWithReturnValuesWhenEnabledViaConfigurationParameter() {
		var testClass = ReturnValuesTestCase.class;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.allowReturnValues", "true") //
				.execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void configuresPreserveOrderOnXmlTest(boolean preserveOrder) {
		var testClass = PreserveOrderTestCase.class;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.preserveOrder", String.valueOf(preserveOrder)) //
				.execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:test(org.testng.ITestContext)"), started()), //
			event(test("method:test(org.testng.ITestContext)"),
				preserveOrder ? finishedSuccessfully() : finishedWithFailure()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void configuresParallelMode() {
		var testClass = ParallelMethodsTestCase.class;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.parallel", "methods") //
				.execute();

		results.testEvents().debug().assertStatistics(stats -> stats.succeeded(2));
	}

	@Test
	void configuresThreadCount() {
		var testClass = ParallelMethodsTestCase.class;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.parallel", "methods") //
				.configurationParameter("testng.threadCount", "1") //
				.execute();

		results.testEvents().debug().assertStatistics(stats -> stats.succeeded(1).failed(1));
	}

	@Test
	void configuresDataProviderThreadCount() {
		var testClass = DataProviderThreadCountTestCase.class;
		var numInvocations = DataProviderThreadCountTestCase.NUM_INVOCATIONS;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.dataProviderThreadCount", String.valueOf(numInvocations)) //
				.execute();

		results.testEvents().debug().assertStatistics(stats -> stats.succeeded(numInvocations));
	}
}
