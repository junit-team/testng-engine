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

import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.includedIf;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
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
import example.basics.SuccessPercentageTestCase;
import example.configuration.FailingBeforeClassConfigurationMethodTestCase;
import example.dataproviders.DataProviderMethodTestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.testng.SkipException;

class ReportingIntegrationTests extends AbstractIntegrationTests {

	@ParameterizedTest
	@ValueSource(classes = { SimpleTestCase.class, InheritingSubClassTestCase.class })
	void executesSuccessfulTests(Class<?> testClass) {
		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:successful()"), started()), //
			event(test("method:successful()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void executesFailingTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(SimpleTestCase.class), started()), //
			event(test("method:failing()"), started()), //
			event(test("method:failing()"), finishedWithFailure(message("boom"))), //
			event(testClass(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	void executesAbortedTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(SimpleTestCase.class), started()), //
			event(test("method:aborted()"), started()), //
			event(test("method:aborted()"), abortedWithReason(instanceOf(SkipException.class), message("not today"))), //
			event(testClass(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(max = "6.14.3")
	void reportsMethodsSkippedDueToFailingDependencyAsSkipped() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(SimpleTestCase.class), started()), //
			event(test("method:skippedDueToFailingDependency()"),
				skippedWithReason(it -> it.contains("depends on not successfully finished methods"))), //
			event(testClass(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(min = "7.0")
	void reportsMethodsSkippedDueToFailingDependencyAsAborted() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(SimpleTestCase.class), started()), //
			event(test("method:skippedDueToFailingDependency()"), started()), //
			event(test("method:skippedDueToFailingDependency()"),
				abortedWithReason(message(it -> it.contains("depends on not successfully finished methods")))), //
			event(testClass(SimpleTestCase.class), finishedSuccessfully()));
	}

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
	void executesNoTestWhenPostDiscoveryFilterExcludesEverything() {
		var testClass = SimpleTestCase.class;

		var results = testNGEngine() //
				.selectors(selectClass(testClass)) //
				.filters((PostDiscoveryFilter) descriptor -> excluded("not today")) //
				.execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void reportsPreviouslyExcludedTestsThatAreExecutedDueToHavingTheSameMethodNameAsDynamicTests() {
		PostDiscoveryFilter onlyParameterlessMethods = descriptor -> {
			var source = descriptor.getSource().orElse(null);
			return includedIf(
				!(source instanceof MethodSource) || isBlank(((MethodSource) source).getMethodParameterTypes()));
		};

		var results = testNGEngine() //
				.selectors(selectClass(DataProviderMethodTestCase.class)) //
				.filters(onlyParameterlessMethods) //
				.execute();

		results.containerEvents().assertStatistics(stats -> stats //
				.dynamicallyRegistered(2) //
				.started(1 + 1 + 2) //
				.succeeded(1 + 1 + 2) //
				.finished(1 + 1 + 2));

		results.testEvents().assertStatistics(stats -> stats //
				.dynamicallyRegistered(2 + 2) //
				.started(1 + 2 + 2) //
				.failed(1 + 2 + 2) //
				.succeeded(0) //
				.finished(1 + 2 + 2));
	}

	@Test
	void reportsSuccessPercentageTestCase() {
		var testClass = SuccessPercentageTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:test()"), started()), //
			event(dynamicTestRegistered("invoc:0"), displayName("[0]")), //
			event(test("invoc:0"), started()), //
			event(test("invoc:0"), finishedSuccessfully()), //
			event(dynamicTestRegistered("invoc:1"), displayName("[1]")), //
			event(test("invoc:1"), started()), //
			event(test("invoc:1"), finishedWithFailure(message("boom"))), //
			event(dynamicTestRegistered("invoc:2"), displayName("[2]")), //
			event(test("invoc:2"), started()), //
			event(test("invoc:2"), finishedSuccessfully()), //
			event(dynamicTestRegistered("invoc:3"), displayName("[3]")), //
			event(test("invoc:3"), started()), //
			event(test("invoc:3"), finishedSuccessfully()), //
			event(container("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void executesAllInvocationsForInvocationUniqueIdSelector() {
		var uniqueId = UniqueId.forEngine("testng") //
				.append(ClassDescriptor.SEGMENT_TYPE, SuccessPercentageTestCase.class.getName()) //
				.append(MethodDescriptor.SEGMENT_TYPE, "test()") //
				.append(InvocationDescriptor.SEGMENT_TYPE, "0");

		var results = testNGEngine().selectors(selectUniqueId(uniqueId)).execute();

		results.testEvents().assertStatistics(stats -> stats.finished(4));
	}

}
