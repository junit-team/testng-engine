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
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import example.basics.FailingBeforeClassTestCase;
import example.basics.FailingBeforeMethodTestCase;
import example.basics.InheritingSubClass;
import example.basics.SimpleTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testng.SkipException;

class ReportingIntegrationTests extends AbstractIntegrationTests {

	@ParameterizedTest
	@ValueSource(classes = { SimpleTest.class, InheritingSubClass.class })
	void executesSuccessfulTests(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(testClass), started()), //
			event(test("method:successful()"), started()), //
			event(test("method:successful()"), finishedSuccessfully()), //
			event(container(testClass), finishedSuccessfully()));
	}

	@Test
	void executesFailingTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTest.class), started()), //
			event(test("method:failing()"), started()), //
			event(test("method:failing()"), finishedWithFailure(message("boom"))), //
			event(container(SimpleTest.class), finishedSuccessfully()));
	}

	@Test
	void executesAbortedTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTest.class), started()), //
			event(test("method:aborted()"), started()), //
			event(test("method:aborted()"), abortedWithReason(instanceOf(SkipException.class), message("not today"))), //
			event(container(SimpleTest.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(max = "6.14.3")
	void reportsMethodsSkippedDueToFailingDependencyAsSkipped() {
		var results = testNGEngine().selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTest.class), started()), //
			event(test("method:skippedDueToFailingDependency()"),
				skippedWithReason(it -> it.contains("depends on not successfully finished methods"))), //
			event(container(SimpleTest.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(min = "7.0")
	void reportsMethodsSkippedDueToFailingDependencyAsAborted() {
		var results = testNGEngine().selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTest.class), started()), //
			event(test("method:skippedDueToFailingDependency()"), started()), //
			event(test("method:skippedDueToFailingDependency()"),
				abortedWithReason(message(it -> it.contains("depends on not successfully finished methods")))), //
			event(container(SimpleTest.class), finishedSuccessfully()));
	}

	@Test
	void reportsFailureFromBeforeClassMethod() {
		var results = testNGEngine().selectors(selectClass(FailingBeforeClassTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeClassTestCase.class), started()), //
			event(container(FailingBeforeClassTestCase.class), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(max = "6.10")
	void reportsFailureFromBeforeMethodMethodAsAbortedWithoutThrowable() {
		var results = testNGEngine().selectors(selectClass(FailingBeforeMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeMethodTestCase.class), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), started()), //
			event(test("method:b()"), abortedWithReason()), //
			event(container(FailingBeforeMethodTestCase.class), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(min = "6.11", max = "6.14.3")
	void reportsFailureFromBeforeMethodMethodAsSkipped() {
		var results = testNGEngine().selectors(selectClass(FailingBeforeMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeMethodTestCase.class), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), skippedWithReason("boom")), //
			event(container(FailingBeforeMethodTestCase.class), finishedWithFailure(message("boom"))));
	}

	@Test
	@RequiresTestNGVersion(min = "7.0")
	void reportsFailureFromBeforeMethodMethodAsAbortedWithThrowable() {
		var results = testNGEngine().selectors(selectClass(FailingBeforeMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeMethodTestCase.class), started()), //
			event(test("method:a()"), started()), //
			event(test("method:a()"), finishedSuccessfully()), //
			event(test("method:b()"), started()), //
			event(test("method:b()"), abortedWithReason(message("boom"))), //
			event(container(FailingBeforeMethodTestCase.class), finishedWithFailure(message("boom"))));
	}

}
