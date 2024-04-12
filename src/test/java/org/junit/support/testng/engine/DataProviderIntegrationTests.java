/*
 * Copyright 2021-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.EventConditions.uniqueIdSubstring;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.junit.support.testng.engine.TestContext.testNGVersion;

import example.dataproviders.*;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;

class DataProviderIntegrationTests extends AbstractIntegrationTests {

	@Test
	void discoversDataProviderTestMethods() {
		var request = request().selectors(selectClass(DataProviderMethodTestCase.class)).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));

		var classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(3);

		var methodDescriptors = classDescriptor.getChildren().stream() //
				.collect(toMap(TestDescriptor::getDisplayName, identity()));
		assertThat(methodDescriptors.keySet()).containsExactlyInAnyOrder("test", "test(java.lang.String)", "test(int)");
		methodDescriptors.forEach((displayName, methodDescriptor) -> {
			assertThat(methodDescriptor.getLegacyReportingName()).isEqualTo(displayName);
			assertThat(methodDescriptor.getChildren()).isEmpty();
		});

		assertThat(methodDescriptors.get("test").getType()) //
				.isEqualTo(TEST);
		assertThat(methodDescriptors.get("test").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", ""));
		assertThat(methodDescriptors.get("test").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test()");

		assertThat(methodDescriptors.get("test(java.lang.String)").getType()) //
				.isEqualTo(CONTAINER);
		assertThat(methodDescriptors.get("test(java.lang.String)").getSource()) //
				.contains(
					MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", String.class.getName()));
		assertThat(methodDescriptors.get("test(java.lang.String)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(java.lang.String)");

		assertThat(methodDescriptors.get("test(int)").getType()) //
				.isEqualTo(CONTAINER);
		assertThat(methodDescriptors.get("test(int)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", int.class.getName()));
		assertThat(methodDescriptors.get("test(int)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(int)");
	}

	@Test
	void executesDataProviderTestMethods() {
		var results = testNGEngine().selectors(selectClass(DataProviderMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(DataProviderMethodTestCase.class), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedWithFailure(message("parameterless"))), //
			event(testClass(DataProviderMethodTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(DataProviderMethodTestCase.class), started()), //
			event(container("method:test(java.lang.String)"), started()), //
			event(uniqueIdSubstring("method:test(java.lang.String)"), dynamicTestRegistered("invoc:0"),
				displayName("[0] a"), legacyReportingName("test(java.lang.String)[0]")), //
			event(uniqueIdSubstring("method:test(java.lang.String)"), test("invoc:0"), started()), //
			event(uniqueIdSubstring("method:test(java.lang.String)"), test("invoc:0"),
				finishedWithFailure(message("a"))), //
			event(uniqueIdSubstring("method:test(java.lang.String)"), dynamicTestRegistered("invoc:1"),
				displayName("[1] b"), legacyReportingName("test(java.lang.String)[1]")), //
			event(uniqueIdSubstring("method:test(java.lang.String)"), test("invoc:1"), started()), //
			event(uniqueIdSubstring("method:test(java.lang.String)"), test("invoc:1"),
				finishedWithFailure(message("b"))), //
			event(container("method:test(java.lang.String)"), finishedSuccessfully()), //
			event(testClass(DataProviderMethodTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(DataProviderMethodTestCase.class), started()), //
			event(container("method:test(int)"), started()), //
			event(uniqueIdSubstring("method:test(int)"), dynamicTestRegistered("invoc:0"), displayName("[0] 1"),
				legacyReportingName("test(int)[0]")), //
			event(uniqueIdSubstring("method:test(int)"), test("invoc:0"), started()), //
			event(uniqueIdSubstring("method:test(int)"), test("invoc:0"), finishedWithFailure(message("1"))), //
			event(uniqueIdSubstring("method:test(int)"), dynamicTestRegistered("invoc:1"), displayName("[1] 2"),
				legacyReportingName("test(int)[1]")), //
			event(uniqueIdSubstring("method:test(int)"), test("invoc:1"), started()), //
			event(uniqueIdSubstring("method:test(int)"), test("invoc:1"), finishedWithFailure(message("2"))), //
			event(container("method:test(int)"), finishedSuccessfully()), //
			event(testClass(DataProviderMethodTestCase.class), finishedSuccessfully()));
	}

	@Test
	void discoversFactoryWithDataProviderTestClass() {
		var request = request().selectors(selectClass(FactoryWithDataProviderTestCase.class)).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));

		var classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(4);

		var methodDescriptors = classDescriptor.getChildren().stream() //
				.collect(toMap(descriptor -> descriptor.getUniqueId().getLastSegment().getValue(), identity()));
		assertThat(methodDescriptors.keySet()).containsExactlyInAnyOrder("a()@0", "a()@1", "b()@0", "b()@1");
	}

	@Test
	void executesFactoryWithDataProviderTestClass() {
		var results = testNGEngine().selectors(selectClass(FactoryWithDataProviderTestCase.class)).execute();

		var capturesFactoryParameters = testNGVersion().compareTo(new ComparableVersion("7.0")) >= 0;
		var firstParamSuffix = capturesFactoryParameters ? "(a)" : "";
		var secondParamSuffix = capturesFactoryParameters ? "(b)" : "";

		results.containerEvents().assertEventsMatchLooselyInOrder( //
			event(engine(), started()), //
			event(testClass(FactoryWithDataProviderTestCase.class), started()), //
			event(testClass(FactoryWithDataProviderTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
		results.allEvents().debug().assertEventsMatchLooselyInOrder( //
			event(testClass(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:a()@0"), started(), displayName("a[0]" + firstParamSuffix)), //
			event(test("method:a()@0"), finishedWithFailure(message("a"))), //
			event(testClass(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:a()@1"), started(), displayName("a[1]" + secondParamSuffix)), //
			event(test("method:a()@1"), finishedWithFailure(message("b"))), //
			event(testClass(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:b()@0"), started(), displayName("b[0]" + firstParamSuffix)), //
			event(test("method:b()@0"), finishedWithFailure(message("a"))), //
			event(testClass(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:b()@1"), started(), displayName("b[1]" + secondParamSuffix)), //
			event(test("method:b()@1"), finishedWithFailure(message("b"))), //
			event(testClass(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
	}

	@Test
	void executesFactoryMethodTestClass() {
		var results = testNGEngine().selectors(selectClass(FactoryMethodTestCase.class)).execute();

		results.allEvents().debug();

		results.containerEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(testClass(FactoryMethodTestCase.class), started()), //
			event(testClass(FactoryMethodTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(FactoryMethodTestCase.class), started()), //
			event(test("method:test()@0"), displayName("test[0]"), started()), //
			event(test("method:test()@0"), finishedSuccessfully()), //
			event(testClass(FactoryMethodTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(FactoryMethodTestCase.class), started()), //
			event(test("method:test()@1"), displayName("test[1]"), started()), //
			event(test("method:test()@1"), finishedSuccessfully()), //
			event(testClass(FactoryMethodTestCase.class), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(maxExclusive = "7.6")
	void reportsExceptionInDataProviderMethodAsAborted() {
		var testClass = DataProviderMethodErrorHandlingTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:test(int)"), started()), //
			event(container("method:test(int)"), abortedWithReason(cause(message("exception in data provider")))), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	@RequiresTestNGVersion(min = "7.6")
	void reportsExceptionInDataProviderMethodAsFailed() {
		var testClass = DataProviderMethodErrorHandlingTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().debug().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:test(int)"), started()), //
			event(container("method:test(int)"), finishedWithFailure(cause(message("exception in data provider")))), //
			event(testClass(testClass), finishedSuccessfully()));
	}

	@Test
	void reportsNoEventsForDataProviderWithZeroInvocations() {
		var testClass = DataProviderMethodEmptyListTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void reportsParallelDataProviderCorrectly() {
		var testClass = ParallelDataProviderTestCase.class;

		var results = testNGEngine().selectors(selectClass(testClass)).execute();

		results.containerEvents().debug().assertEventsMatchLooselyInOrder( //
			event(testClass(testClass), started()), //
			event(container("method:test(java.lang.Integer)"), started()), //
			event(container("method:test(java.lang.Integer)"), finishedSuccessfully()), //
			event(testClass(testClass), finishedSuccessfully()));
		results.testEvents().assertStatistics(
			stats -> stats.dynamicallyRegistered(11).started(11).succeeded(11).finished(11));
	}
}
