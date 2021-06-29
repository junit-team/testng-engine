/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.compat.testng;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.stream.Stream;

import example.configuration.FailingBeforeClassConfigurationMethodTestCase;
import example.configuration.FailingBeforeMethodConfigurationMethodTestCase;
import example.configuration.FailingBeforeSuiteConfigurationMethodTestCase;
import example.configuration.FailingBeforeTestConfigurationMethodTestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigurationMethodIntegrationTests extends AbstractIntegrationTests {

	@Test
	void reportsFailureFromBeforeClassMethod() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeClassConfigurationMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeClassConfigurationMethodTestCase.class), started()), //
			event(container(FailingBeforeClassConfigurationMethodTestCase.class),
				finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.10")
	void reportsFailureFromBeforeMethodMethodAsAbortedWithoutThrowable() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeMethodConfigurationMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeMethodConfigurationMethodTestCase.class), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), started()), //
			event(test("method:b()"), abortedWithReason()), //
			event(container(FailingBeforeMethodConfigurationMethodTestCase.class),
				finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(min = "6.11", max = "6.14.3")
	void reportsFailureFromBeforeMethodMethodAsSkipped() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeMethodConfigurationMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeMethodConfigurationMethodTestCase.class), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), skippedWithReason("boom")), //
			event(container(FailingBeforeMethodConfigurationMethodTestCase.class),
				finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(min = "7.0")
	void reportsFailureFromBeforeMethodMethodAsAbortedWithThrowable() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeMethodConfigurationMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeMethodConfigurationMethodTestCase.class), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), started()), //
			event(test("method:b()"), abortedWithReason(message("boom"))), //
			event(container(FailingBeforeMethodConfigurationMethodTestCase.class),
				finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.10")
	void reportsFailureFromBeforeTestMethodAsAbortedWithoutThrowable() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeTestConfigurationMethodTestCase.class)).execute();

		results.allEvents().debug().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(FailingBeforeTestConfigurationMethodTestCase.class), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), abortedWithReason()), //
			event(container(FailingBeforeTestConfigurationMethodTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.10")
	void reportsFailureFromBeforeSuiteMethodAsSkipped() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeSuiteConfigurationMethodTestCase.class)).execute();

		results.allEvents().debug().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(FailingBeforeSuiteConfigurationMethodTestCase.class), started()), //
			event(test("method:test()"), skippedWithReason(__ -> true)), //
			event(container(FailingBeforeSuiteConfigurationMethodTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@ParameterizedTest
	@MethodSource("testCasesWithEngineLevelConfigurationMethodFailures")
	@RequiresTestNGVersion(min = "6.11", max = "6.14.3")
	void reportsFailureFromEngineLevelConfigurationMethodAsSkipped(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().debug().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("method:test()"), skippedWithReason("boom")), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@ParameterizedTest
	@MethodSource("testCasesWithEngineLevelConfigurationMethodFailures")
	@RequiresTestNGVersion(min = "7.0")
	void reportsFailureFromEngineLevelConfigurationMethodAsAbortedWithThrowable(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().debug().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), abortedWithReason(message("boom"))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	static Stream<Class<?>> testCasesWithEngineLevelConfigurationMethodFailures() {
		return Stream.of(FailingBeforeTestConfigurationMethodTestCase.class,
			FailingBeforeSuiteConfigurationMethodTestCase.class);
	}

}
