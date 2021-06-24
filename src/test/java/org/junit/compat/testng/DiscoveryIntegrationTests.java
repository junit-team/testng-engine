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
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.Map;

import example.SimpleTest;
import example.TwoTestMethods;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

class DiscoveryIntegrationTests extends AbstractIntegrationTests {

	@Test
	void discoversAllTestMethodsForClassSelector() {
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
	void discoversSingleTestMethodsForMethodSelector() {
		var request = request().selectors(selectMethod(SimpleTest.class, "successful")).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));

		assertThat(rootDescriptor.getUniqueId()).isEqualTo(UniqueId.forEngine("testng"));
		assertThat(rootDescriptor.getChildren()).hasSize(1);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getDisplayName()).isEqualTo(SimpleTest.class.getSimpleName());
		assertThat(classDescriptor.getLegacyReportingName()).isEqualTo(SimpleTest.class.getName());
		assertThat(classDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(SimpleTest.class));
		assertThat(classDescriptor.getChildren()).hasSize(1);

		TestDescriptor methodDescriptor = getOnlyElement(classDescriptor.getChildren());
		assertThat(methodDescriptor.getLegacyReportingName()).isEqualTo("successful");
		assertThat(methodDescriptor.getType()).isEqualTo(TEST);
		assertThat(methodDescriptor.getTags()).contains(TestTag.create("foo"));
		assertThat(methodDescriptor.getSource()).contains(
			MethodSource.from(SimpleTest.class.getName(), "successful", ""));
		assertThat(methodDescriptor.getChildren()).isEmpty();
		assertThat(methodDescriptor.getTags()) //
				.containsExactlyInAnyOrder(TestTag.create("foo"), TestTag.create("bar"));

		var results = testNGEngine().selectors(selectMethod(SimpleTest.class, "successful")).execute();
		results.testEvents().assertStatistics(stats -> stats.started(1).finished(1));
	}

	@Test
	void supportsDiscoveryOfClassAndMethodSelector() {
		DiscoverySelector[] selectors = { //
				selectClass(TwoTestMethods.class), //
				selectMethod(TwoTestMethods.class, "one") //
		};
		var request = request().selectors(selectors).build();

		var rootDescriptor = new TestNGTestEngine().discover(request, UniqueId.forEngine("testng"));
		assertThat(rootDescriptor.getChildren()).hasSize(1);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(2);

		var results = testNGEngine().selectors(selectors).execute();
		results.testEvents().assertStatistics(stats -> stats.started(2).finished(2));
	}
}
