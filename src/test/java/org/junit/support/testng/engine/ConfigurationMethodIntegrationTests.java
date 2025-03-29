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
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import example.configuration.methods.FailingAfterClassConfigurationMethodTestCase;
import example.configuration.methods.FailingAfterMethodConfigurationMethodTestCase;
import example.configuration.methods.FailingAfterSuiteConfigurationMethodTestCase;
import example.configuration.methods.FailingAfterTestConfigurationMethodTestCase;
import example.configuration.methods.FailingBeforeClassConfigurationMethodTestCase;
import example.configuration.methods.FailingBeforeMethodConfigurationMethodTestCase;
import example.configuration.methods.FailingBeforeSuiteConfigurationMethodTestCase;
import example.configuration.methods.FailingBeforeTestConfigurationMethodTestCase;
import example.configuration.methods.GroupsConfigurationMethodsTestCase;

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

	@ParameterizedTest
	@ValueSource(classes = { FailingBeforeTestConfigurationMethodTestCase.class,
			FailingBeforeSuiteConfigurationMethodTestCase.class })
	void reportsFailureFromEarlyEngineLevelConfigurationMethodAsAborted(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().debug().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), abortedWithReason(message("boom"))), //
			event(testClass(testClass), abortedWithReason()), //
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
	@ValueSource(classes = { FailingAfterClassConfigurationMethodTestCase.class,
			FailingAfterTestConfigurationMethodTestCase.class, FailingAfterSuiteConfigurationMethodTestCase.class })
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

	@Test
	void runsGroupsIncludingConfigurationMethods() {
		Class<?> testClass = GroupsConfigurationMethodsTestCase.class;
		GroupsConfigurationMethodsTestCase.EVENTS.clear();

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.configurationParameter("testng.groups", "group1") //
				.configurationParameter("testng.excludedGroups", "group2") //
				.execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:testGroup1()"), started()), //
			event(test("method:testGroup1()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		assertThat(GroupsConfigurationMethodsTestCase.EVENTS) //
				.containsExactly("beforeGroup1", "testGroup1", "afterGroup1");
	}

}
