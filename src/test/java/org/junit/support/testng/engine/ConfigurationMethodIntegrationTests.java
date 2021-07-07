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

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.annotation.Retention;

import example.configuration.FailingAfterClassConfigurationMethodTestCase;
import example.configuration.FailingAfterMethodConfigurationMethodTestCase;
import example.configuration.FailingAfterSuiteConfigurationMethodTestCase;
import example.configuration.FailingAfterTestConfigurationMethodTestCase;
import example.configuration.FailingBeforeClassConfigurationMethodTestCase;
import example.configuration.FailingBeforeMethodConfigurationMethodTestCase;
import example.configuration.FailingBeforeSuiteConfigurationMethodTestCase;
import example.configuration.FailingBeforeTestConfigurationMethodTestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationMethodIntegrationTests extends AbstractIntegrationTests {

	@Test
	void reportsFailureFromBeforeClassMethod() {
		var results = testNGEngine().selectors(
			selectClass(FailingBeforeClassConfigurationMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(FailingBeforeClassConfigurationMethodTestCase.class), started()), //
			event(testClass(FailingBeforeClassConfigurationMethodTestCase.class),
				finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.10")
	void reportsFailureFromBeforeMethodConfigurationMethodAsAbortedWithoutThrowable() {
		var testClass = FailingBeforeMethodConfigurationMethodTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), started()), //
			event(test("method:b()"), abortedWithReason()), //
			event(testClass(testClass), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(min = "6.11")
	void reportsFailureFromBeforeMethodConfigurationMethodAsAbortedWithThrowable() {
		var testClass = FailingBeforeMethodConfigurationMethodTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), started()), //
			event(test("method:b()"), abortedWithReason(message("boom"))), //
			event(testClass(testClass), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.10")
	void reportsFailureFromBeforeTestMethodAsAbortedWithoutThrowable() {
		var testClass = FailingBeforeTestConfigurationMethodTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), abortedWithReason()), //
			event(testClass(testClass), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.9.13.6")
	void reportsFailureFromBeforeSuiteMethodAsSkipped() {
		var testClass = FailingBeforeSuiteConfigurationMethodTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:test()"), skippedWithReason(__ -> true)), //
			event(testClass(testClass), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@ParameterizedTest
	@TestCasesWithEarlyEngineLevelConfigurationMethodFailures
	@RequiresTestNGVersion(min = "6.10")
	void reportsFailureFromEarlyEngineLevelConfigurationMethodAsAbortedWithThrowable(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().debug().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), abortedWithReason(message("boom"))), //
			event(testClass(testClass), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@Test
	void reportsFailureFromAfterMethodConfigurationMethodAsClassLevelFailure() {
		var testClass = FailingAfterMethodConfigurationMethodTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedWithFailure(message("boom"))));
	}

	@ParameterizedTest
	@TestCasesWithLateEngineLevelConfigurationMethodFailures
	void reportsFailureFromLateEngineLevelConfigurationMethodAsEngineLevelFailure(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()), //
			event(engine(), finishedWithFailure(message("boom"))));
	}

	@Retention(RUNTIME)
	@ValueSource(classes = { FailingBeforeTestConfigurationMethodTestCase.class,
			FailingBeforeSuiteConfigurationMethodTestCase.class })
	@interface TestCasesWithEarlyEngineLevelConfigurationMethodFailures {
	}

	@Retention(RUNTIME)
	@ValueSource(classes = { FailingAfterClassConfigurationMethodTestCase.class,
			FailingAfterTestConfigurationMethodTestCase.class, FailingAfterSuiteConfigurationMethodTestCase.class })
	@interface TestCasesWithLateEngineLevelConfigurationMethodFailures {
	}

}
