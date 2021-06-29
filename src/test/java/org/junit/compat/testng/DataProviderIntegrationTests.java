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
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import example.dataproviders.DataProviderMethodTestCase;
import example.dataproviders.FactoryWithDataProviderTestCase;

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
				.contains(MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", ""));
		assertThat(methodDescriptors.get("test").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test()");
		assertThat(methodDescriptors.get("test[0](a)").getSource()) //
				.contains(
					MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", String.class.getName()));
		assertThat(methodDescriptors.get("test[0](a)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(java.lang.String)_0");
		assertThat(methodDescriptors.get("test[1](b)").getSource()) //
				.contains(
					MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", String.class.getName()));
		assertThat(methodDescriptors.get("test[1](b)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(java.lang.String)_1");
		assertThat(methodDescriptors.get("test[0](1)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", int.class.getName()));
		assertThat(methodDescriptors.get("test[0](1)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(int)_0");
		assertThat(methodDescriptors.get("test[1](2)").getSource()) //
				.contains(MethodSource.from(DataProviderMethodTestCase.class.getName(), "test", int.class.getName()));
		assertThat(methodDescriptors.get("test[1](2)").getUniqueId().getLastSegment().getValue()) //
				.isEqualTo("test(int)_1");
	}

	@Test
	void executesDataProviderTestMethods() {
		var results = testNGEngine().selectors(selectClass(DataProviderMethodTestCase.class)).execute();

		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(DataProviderMethodTestCase.class), started()), //
			event(test("method:test()"), started()), //
			event(test("method:test()"), finishedWithFailure(message("parameterless"))), //
			event(container(DataProviderMethodTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(DataProviderMethodTestCase.class), started()), //
			event(test("method:test(java.lang.String)_0"), started()), //
			event(test("method:test(java.lang.String)_0"), finishedWithFailure(message("a"))), //
			event(test("method:test(java.lang.String)_1"), started()), //
			event(test("method:test(java.lang.String)_1"), finishedWithFailure(message("b"))), //
			event(container(DataProviderMethodTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(DataProviderMethodTestCase.class), started()), //
			event(test("method:test(int)_0"), started()), //
			event(test("method:test(int)_0"), finishedWithFailure(message("1"))), //
			event(test("method:test(int)_1"), started()), //
			event(test("method:test(int)_1"), finishedWithFailure(message("2"))), //
			event(container(DataProviderMethodTestCase.class), finishedSuccessfully()));
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

		results.containerEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(FactoryWithDataProviderTestCase.class), started()), //
			event(container(FactoryWithDataProviderTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:a()@0"), started()), //
			event(test("method:a()@0"), finishedWithFailure(message("a"))), //
			event(container(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:a()@1"), started()), //
			event(test("method:a()@1"), finishedWithFailure(message("b"))), //
			event(container(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:b()@0"), started()), //
			event(test("method:b()@0"), finishedWithFailure(message("a"))), //
			event(container(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
		results.allEvents().assertEventsMatchLooselyInOrder( //
			event(container(FactoryWithDataProviderTestCase.class), started()), //
			event(test("method:b()@1"), started()), //
			event(test("method:b()@1"), finishedWithFailure(message("b"))), //
			event(container(FactoryWithDataProviderTestCase.class), finishedSuccessfully()));
	}
}
