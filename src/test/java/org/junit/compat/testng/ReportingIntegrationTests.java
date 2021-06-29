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

import example.basics.InheritingSubClassTestCase;
import example.basics.SimpleTestCase;
import example.configuration.FailingBeforeClassMethodTestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testng.SkipException;

class ReportingIntegrationTests extends AbstractIntegrationTests {

	@ParameterizedTest
	@ValueSource(classes = { SimpleTestCase.class, InheritingSubClassTestCase.class })
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
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTestCase.class), started()), //
			event(test("method:failing()"), started()), //
			event(test("method:failing()"), finishedWithFailure(message("boom"))), //
			event(container(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	void executesAbortedTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTestCase.class), started()), //
			event(test("method:aborted()"), started()), //
			event(test("method:aborted()"), abortedWithReason(instanceOf(SkipException.class), message("not today"))), //
			event(container(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(max = "6.14.3")
	void reportsMethodsSkippedDueToFailingDependencyAsSkipped() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTestCase.class), started()), //
			event(test("method:skippedDueToFailingDependency()"),
				skippedWithReason(it -> it.contains("depends on not successfully finished methods"))), //
			event(container(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(min = "7.0")
	void reportsMethodsSkippedDueToFailingDependencyAsAborted() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTestCase.class), started()), //
			event(test("method:skippedDueToFailingDependency()"), started()), //
			event(test("method:skippedDueToFailingDependency()"),
				abortedWithReason(message(it -> it.contains("depends on not successfully finished methods")))), //
			event(container(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	void reportsFailureFromBeforeClassMethod() {
		var results = testNGEngine().selectors(selectClass(FailingBeforeClassMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FailingBeforeClassMethodTestCase.class), started()), //
			event(container(FailingBeforeClassMethodTestCase.class), finishedWithFailure(message("boom"))));
	}

}
