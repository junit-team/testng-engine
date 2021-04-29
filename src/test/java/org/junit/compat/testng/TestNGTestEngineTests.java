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
import static org.junit.platform.testkit.engine.EngineTestKit.engine;
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

import java.util.Map;

import example.SimpleTest;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.testng.SkipException;

public class TestNGTestEngineTests {

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
			assertThat(methodDescriptor.getSource()).contains(
				MethodSource.from(SimpleTest.class.getName(), methodName, ""));
			assertThat(methodDescriptor.getChildren()).isEmpty();
		});
	}

	@Test
	void executesSuccessfulTests() {
		var results = testNGEngine().selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder(event(container(SimpleTest.class), started()), //
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

		results.allEvents().assertEventsMatchLooselyInOrder(event(container(SimpleTest.class), started()), //
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

	private static EngineTestKit.Builder testNGEngine() {
		return engine("testng").configurationParameter("testng.verbose", "10");
	}
}
