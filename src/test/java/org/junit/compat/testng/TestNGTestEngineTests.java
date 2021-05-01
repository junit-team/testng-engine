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
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.nio.file.Path;
import java.util.Map;

import example.DataProviderMethodTest;
import example.FactoryWithDataProviderTest;
import example.SimpleTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.testng.SkipException;

public class TestNGTestEngineTests {

	@TempDir
	Path tempDir;

	@Test
	void discoversTestMethods() {
		var request = request().selectors(selectClass(SimpleTest.class)).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));

		assertThat(rootDescriptor.getUniqueId()).isEqualTo(UniqueId.forEngine("testng"));
		assertThat(rootDescriptor.getChildren()).hasSize(1);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getDisplayName()).isEqualTo(SimpleTest.class.getSimpleName());
		assertThat(classDescriptor.getLegacyReportingName()).isEqualTo(SimpleTest.class.getName());
		assertThat(classDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(SimpleTest.class));
		assertThat(classDescriptor.getChildren()).hasSize(4);

		Map<String, TestDescriptor> methodDescriptors = classDescriptor.getChildren().stream() //
				.collect(toMap(TestDescriptor::getDisplayName, identity()));
		assertThat(methodDescriptors.keySet()).containsExactlyInAnyOrder("successful", "failing", "aborted",
			"skippedDueToFailingDependency");
		methodDescriptors.forEach((methodName, methodDescriptor) -> {
			assertThat(methodDescriptor.getLegacyReportingName()).isEqualTo(methodName);
			assertThat(methodDescriptor.getType()).isEqualTo(TEST);
			assertThat(methodDescriptor.getTags()).contains(TestTag.create("foo"));
			assertThat(methodDescriptor.getSource()).contains(
				MethodSource.from(SimpleTest.class.getName(), methodName, ""));
			assertThat(methodDescriptor.getChildren()).isEmpty();
		});
		assertThat(methodDescriptors.get("successful").getTags()) //
				.containsExactlyInAnyOrder(TestTag.create("foo"), TestTag.create("bar"));
	}

	@Test
	void discoversDataProviderTestMethods() {
		var request = request().selectors(selectClass(DataProviderMethodTest.class)).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));

		var classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(5);

		var methodDescriptors = classDescriptor.getChildren().stream() //
				.collect(toMap(TestDescriptor::getDisplayName, identity()));
		assertThat(methodDescriptors.keySet()).containsExactlyInAnyOrder("test", "test[0](a)", "test[1](b)",
			"test[0](1)", "test[1](2)");
		methodDescriptors.forEach((displayName, methodDescriptor) -> {
			assertThat(methodDescriptor.getLegacyReportingName()).isEqualTo(displayName);
			assertThat(methodDescriptor.getType()).isEqualTo(TEST);
			assertThat(methodDescriptor.getChildren()).isEmpty();
		});

		assertThat(methodDescriptors.get("test").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTest.class.getName(), "test", ""));
		assertThat(methodDescriptors.get("test").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test()");
		assertThat(methodDescriptors.get("test[0](a)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTest.class.getName(), "test", String.class.getName()));
		assertThat(methodDescriptors.get("test[0](a)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(java.lang.String)_0");
		assertThat(methodDescriptors.get("test[1](b)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTest.class.getName(), "test", String.class.getName()));
		assertThat(methodDescriptors.get("test[1](b)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(java.lang.String)_1");
		assertThat(methodDescriptors.get("test[0](1)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTest.class.getName(), "test", int.class.getName()));
		assertThat(methodDescriptors.get("test[0](1)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(int)_0");
		assertThat(methodDescriptors.get("test[1](2)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTest.class.getName(), "test", int.class.getName()));
		assertThat(methodDescriptors.get("test[1](2)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(int)_1");
	}

	@Test
	void executesDataProviderTestMethods() {
		var results = testNGEngine().selectors(selectClass(DataProviderMethodTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(DataProviderMethodTest.class), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedWithFailure(message("parameterless"))), //
			event(container(DataProviderMethodTest.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(DataProviderMethodTest.class), started()), //
			event(test("method:test(java.lang.String)_0"), started()), //
			event(test("method:test(java.lang.String)_0"), finishedWithFailure(message("a"))), //
			event(test("method:test(java.lang.String)_1"), started()), //
			event(test("method:test(java.lang.String)_1"), finishedWithFailure(message("b"))), //
			event(container(DataProviderMethodTest.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(DataProviderMethodTest.class), started()), //
			event(test("method:test(int)_0"), started()), //
			event(test("method:test(int)_0"), finishedWithFailure(message("1"))), //
			event(test("method:test(int)_1"), started()), //
			event(test("method:test(int)_1"), finishedWithFailure(message("2"))), //
			event(container(DataProviderMethodTest.class), finishedSuccessfully()));
	}

	@Test
	void discoversFactoryWithDataProviderTestClass() {
		var request = request().selectors(selectClass(FactoryWithDataProviderTest.class)).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));

		var classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(4);

		var methodDescriptors = classDescriptor.getChildren().stream() //
				.collect(toMap(descriptor -> descriptor.getUniqueId().getLastSegment().getValue(), identity()));
		assertThat(methodDescriptors.keySet()).containsExactlyInAnyOrder("a()@0", "a()@1", "b()@0", "b()@1");
	}

	@Test
	void executesFactoryWithDataProviderTestClass() {
		var results = testNGEngine().selectors(selectClass(FactoryWithDataProviderTest.class)).execute();

		results.containerEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(FactoryWithDataProviderTest.class), started()), //
			event(container(FactoryWithDataProviderTest.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTest.class), started()), //
			event(test("method:a()@0"), started()), //
			event(test("method:a()@0"), finishedWithFailure(message("a"))), //
			event(container(FactoryWithDataProviderTest.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTest.class), started()), //
			event(test("method:a()@1"), started()), //
			event(test("method:a()@1"), finishedWithFailure(message("b"))), //
			event(container(FactoryWithDataProviderTest.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTest.class), started()), //
			event(test("method:b()@0"), started()), //
			event(test("method:b()@0"), finishedWithFailure(message("a"))), //
			event(container(FactoryWithDataProviderTest.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTest.class), started()), //
			event(test("method:b()@1"), started()), //
			event(test("method:b()@1"), finishedWithFailure(message("b"))), //
			event(container(FactoryWithDataProviderTest.class), finishedSuccessfully()));
	}

	@Test
	void executesSuccessfulTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(SimpleTest.class), started()), //
			event(test("method:successful()"), started()), //
			event(test("method:successful()"), finishedSuccessfully()), //
			event(container(SimpleTest.class), finishedSuccessfully()));
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

	private EngineTestKit.Builder testNGEngine() {
		return EngineTestKit.engine("testng") //
				.configurationParameter("testng.verbose", "10") //
				.configurationParameter("testng.useDefaultListeners", "false") //
				.configurationParameter("testng.outputDirectory", tempDir.toString());
	}
}
