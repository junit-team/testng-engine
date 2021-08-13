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

import example.basics.CustomAttributeTestCase;
import example.basics.DisabledClassTestCase;
import example.basics.ExpectedExceptionsTestCase;
import example.basics.InheritingSubClassTestCase;
import example.basics.ParallelExecutionTestCase;
import example.basics.RetriedTestCase;
import example.basics.SimpleTestCase;
import example.basics.SuccessPercentageTestCase;
import example.basics.TimeoutTestCase;
import example.configuration.methods.FailingBeforeClassConfigurationMethodTestCase;
import example.dataproviders.DataProviderMethodTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.testng.SkipException;
import org.testng.internal.thread.ThreadTimeoutException;

import java.util.Map;

import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.includedIf;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

class ReportingIntegrationTests extends AbstractIntegrationTests {

	@ParameterizedTest
	@ValueSource(classes = { SimpleTestCase.class, InheritingSubClassTestCase.class })
	void executesSuccessfulTests(Class<?> testClass) {
		var results = testNGEngine().selectors(selectMethod(testClass, "successful")).execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(testClass), started()), //
			event(test("method:successful()"), started()), //
			event(test("method:successful()"), reportEntry(Map.of("description", "a test that passes"))), //
			event(test("method:successful()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
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

	@Test
	void reportsRetriedTestsCorrectly() {
		var testClass = RetriedTestCase.class;

		var results = testNGEngine().selectors(selectMethod(testClass, "test")).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:test()"), started()), //
			event(dynamicTestRegistered("invoc:0"), displayName("[0]")), //
			event(test("invoc:0"), started()), //
			event(test("invoc:0"), abortedWithReason(message("retry"))), //
			event(dynamicTestRegistered("invoc:1"), displayName("[1]")), //
			event(test("invoc:1"), started()), //
			event(test("invoc:1"), finishedSuccessfully()), //
			event(container("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void reportsRetriedTestsWithDataProvidersCorrectly() {
		var testClass = RetriedTestCase.class;

		var results = testNGEngine().selectors(selectMethod(testClass, "dataProviderTest")).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:dataProviderTest(int)"), started()), //
			event(dynamicTestRegistered("invoc:0"), displayName("[0] 1")), //
			event(test("invoc:0"), started()), //
			event(test("invoc:0"), abortedWithReason(message("retry @ 1"))), //
			event(dynamicTestRegistered("invoc:1"), displayName("[1] 1")), //
			event(test("invoc:1"), started()), //
			event(test("invoc:1"), finishedSuccessfully()), //
			event(dynamicTestRegistered("invoc:2"), displayName("[2] 2")), //
			event(test("invoc:2"), started()), //
			event(test("invoc:2"), finishedSuccessfully()), //
			event(container("method:dataProviderTest(int)"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "timeOut", "invocationTimeOut" })
	void reportsTimedOutTestsAsFailures(String methodName) {
		var testClass = TimeoutTestCase.class;

		var results = testNGEngine().selectors(selectMethod(testClass, methodName)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:%s()".formatted(methodName)), started()), //
			event(test("method:%s()".formatted(methodName)),
				finishedWithFailure(instanceOf(ThreadTimeoutException.class))), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void reportsTestThrowingExpectedExceptionAsSuccessful() {
		var testClass = ExpectedExceptionsTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(min = "7.0") // introduced in 7.0
	void reportsCustomAttributesAsReportEntries() {
		var testClass = CustomAttributeTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), reportEntry(Map.of("foo", "bar, baz"))), //
			event(test("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void reportsParallelInvocations() {
		var testClass = ParallelExecutionTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.containerEvents().assertStatistics(stats -> stats.started(3).finished(3));
		results.testEvents().assertStatistics(stats -> stats.started(10).finished(10));

		results.allEvents().debug().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:test()"), started()), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(test("method:test()"), dynamicTestRegistered("invoc")), //
			event(container("method:test()"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void reportsDisabledTestsMethodsAsSkipped() {
		var results = testNGEngine().selectors(selectClass(SimpleTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
				event(testClass(SimpleTestCase.class), started()), //
				event(test("method:disabled()"), skippedWithReason(__ -> true)), //
				event(testClass(SimpleTestCase.class), finishedSuccessfully()));
	}

	@Test
	void reportsTestMethodsInDisabledClassesAsSkipped() {
		var results = testNGEngine().selectors(selectClass(DisabledClassTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
				event(testClass(SimpleTestCase.class), started()), //
				event(test("method:test()"), skippedWithReason(__ -> true)), //
				event(testClass(SimpleTestCase.class), finishedSuccessfully()));
	}

}
