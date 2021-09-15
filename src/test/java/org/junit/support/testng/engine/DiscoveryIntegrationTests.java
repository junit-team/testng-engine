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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.TagFilter.includeTags;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.Map;
import java.util.regex.Pattern;

import example.basics.DryRunTestCase;
import example.basics.IgnoredTestCase;
import example.basics.InheritedClassLevelOnlyAnnotationTestCase;
import example.basics.InheritingSubClassTestCase;
import example.basics.JUnitTestCase;
import example.basics.NestedTestClass;
import example.basics.SimpleTestCase;
import example.basics.SuccessPercentageTestCase;
import example.basics.TwoMethodsTestCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

class DiscoveryIntegrationTests extends AbstractIntegrationTests {

	private final TestNGTestEngine testEngine = new TestNGTestEngine();
	private final UniqueId engineId = UniqueId.forEngine(testEngine.getId());

	@Test
	void discoveryDoesNotRunTests() {
		var testClass = DryRunTestCase.class;
		DryRunTestCase.INVOCATIONS = 0;
		var request = request().selectors(selectClass(testClass)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(DryRunTestCase.INVOCATIONS).isEqualTo(0);
		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		TestDescriptor methodDescriptor = getOnlyElement(classDescriptor.getChildren());
		assertThat(methodDescriptor.getChildren()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(classes = { SimpleTestCase.class, InheritingSubClassTestCase.class })
	void discoversAllTestMethodsForClassSelector(Class<?> testClass) {
		var request = request().selectors(selectClass(testClass)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(rootDescriptor.getUniqueId()).isEqualTo(engineId);
		assertThat(rootDescriptor.getChildren()).hasSize(1);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getDisplayName()).isEqualTo(testClass.getSimpleName());
		assertThat(classDescriptor.getLegacyReportingName()).isEqualTo(testClass.getName());
		assertThat(classDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(classDescriptor.getTags()).contains(TestTag.create("foo"));
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(testClass));
		assertThat(classDescriptor.getChildren()).hasSize(4);

		Map<String, TestDescriptor> methodDescriptors = classDescriptor.getChildren().stream() //
				.collect(toMap(TestDescriptor::getDisplayName, identity()));
		assertThat(methodDescriptors.keySet()).containsExactlyInAnyOrder("successful", "failing", "aborted",
			"skippedDueToFailingDependency");
		methodDescriptors.forEach((methodName, methodDescriptor) -> {
			assertThat(methodDescriptor.getLegacyReportingName()).isEqualTo(methodName);
			assertThat(methodDescriptor.getType()).isEqualTo(TEST);
			assertThat(methodDescriptor.getTags()).contains(TestTag.create("foo"));
			assertThat(methodDescriptor.getSource()).contains(MethodSource.from(testClass.getName(), methodName, ""));
			assertThat(methodDescriptor.getChildren()).isEmpty();
		});
		assertThat(methodDescriptors.get("successful").getTags()) //
				.containsExactlyInAnyOrder(TestTag.create("foo"), TestTag.create("bar"));
	}

	@Test
	void discoversSingleTestMethodsForMethodSelector() {
		var request = request().selectors(selectMethod(SimpleTestCase.class, "successful")).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(rootDescriptor.getUniqueId()).isEqualTo(engineId);
		assertThat(rootDescriptor.getChildren()).hasSize(1);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getDisplayName()).isEqualTo(SimpleTestCase.class.getSimpleName());
		assertThat(classDescriptor.getLegacyReportingName()).isEqualTo(SimpleTestCase.class.getName());
		assertThat(classDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(SimpleTestCase.class));
		assertThat(classDescriptor.getChildren()).hasSize(1);

		TestDescriptor methodDescriptor = getOnlyElement(classDescriptor.getChildren());
		assertThat(methodDescriptor.getLegacyReportingName()).isEqualTo("successful");
		assertThat(methodDescriptor.getType()).isEqualTo(TEST);
		assertThat(methodDescriptor.getTags()).contains(TestTag.create("foo"));
		assertThat(methodDescriptor.getSource()).contains(
			MethodSource.from(SimpleTestCase.class.getName(), "successful", ""));
		assertThat(methodDescriptor.getChildren()).isEmpty();
		assertThat(methodDescriptor.getTags()) //
				.containsExactlyInAnyOrder(TestTag.create("foo"), TestTag.create("bar"));

		var results = testNGEngine().selectors(selectMethod(SimpleTestCase.class, "successful")).execute();
		results.testEvents().assertStatistics(stats -> stats.started(1).finished(1));
	}

	@Test
	void supportsDiscoveryOfClassAndMethodSelector() {
		DiscoverySelector[] selectors = { //
				selectClass(TwoMethodsTestCase.class), //
				selectMethod(TwoMethodsTestCase.class, "one") //
		};
		var request = request().selectors(selectors).build();

		var rootDescriptor = testEngine.discover(request, engineId);
		assertThat(rootDescriptor.getChildren()).hasSize(1);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getChildren()).hasSize(2);

		var results = testNGEngine().selectors(selectors).execute();
		results.testEvents().assertStatistics(stats -> stats.started(2).finished(2));
	}

	@Test
	void ignoredNonTestNGClasses() {
		var request = request().selectors(selectClass(Object.class)).build();

		var rootDescriptor = testEngine.discover(request, engineId);
		assertThat(rootDescriptor.getChildren()).isEmpty();
	}

	@Test
	void discoversAllTestMethodsForClassUniqueSelector() {
		var uniqueId = engineId //
				.append("class", SimpleTestCase.class.getName());
		var request = request().selectors(selectUniqueId(uniqueId)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		assertThat(classDescriptor.getUniqueId()).isEqualTo(uniqueId);
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(SimpleTestCase.class));
		assertThat(classDescriptor.getChildren()).hasSize(4);
	}

	@Test
	void discoversSingleTestMethodsForMethodUniqueIdSelector() {
		var uniqueId = engineId //
				.append("class", SimpleTestCase.class.getName()) //
				.append("method", "successful()");
		var request = request().selectors(selectUniqueId(uniqueId)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		TestDescriptor methodDescriptor = getOnlyElement(classDescriptor.getChildren());
		assertThat(methodDescriptor.getUniqueId()).isEqualTo(uniqueId);
		assertThat(methodDescriptor.getSource()).contains(
			MethodSource.from(SimpleTestCase.class.getName(), "successful", ""));
		assertThat(methodDescriptor.getChildren()).isEmpty();
	}

	@Test
	void discoversAllClassesViaPackageSelector() {
		var packageName = SimpleTestCase.class.getPackageName();
		var request = request().selectors(selectPackage(packageName)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(rootDescriptor.getChildren()) //
				.extracting(TestDescriptor::getDisplayName) //
				.contains(SimpleTestCase.class.getSimpleName(),
					InheritedClassLevelOnlyAnnotationTestCase.class.getSimpleName());
		assertThat(rootDescriptor.getChildren()) //
				.extracting(
					descriptor -> ((ClassSource) descriptor.getSource().orElseThrow()).getJavaClass().getPackageName()) //
				.containsOnly(packageName);
	}

	@Test
	void supportsPostDiscoveryFilters() {
		var request = request().selectors(selectClass(SimpleTestCase.class)).filters(includeTags("bar")).build();
		var launcher = LauncherFactory.create(
			LauncherConfig.builder().enableTestEngineAutoRegistration(false).addTestEngines(testEngine).build());
		var listener = new SummaryGeneratingListener();

		var testPlan = launcher.discover(request);
		launcher.execute(testPlan, listener);

		var rootIdentifier = getOnlyElement(testPlan.getRoots());
		var classIdentifier = getOnlyElement(testPlan.getChildren(rootIdentifier));
		var methodIdentifier = getOnlyElement(testPlan.getChildren(classIdentifier));
		assertThat(methodIdentifier.getDisplayName()).isEqualTo("successful");
		assertThat(listener.getSummary().getTestsStartedCount()).isEqualTo(1);
		assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(1);
	}

	@Test
	void supportsClassNameFilters() {
		var request = request() //
				.selectors(selectClass(SimpleTestCase.class)) //
				.filters(includeClassNamePatterns(Pattern.quote(TwoMethodsTestCase.class.getName()))) //
				.build();

		var rootDescriptor = testEngine.discover(request, engineId);
		assertThat(rootDescriptor.getChildren()).isEmpty();

		request = request() //
				.selectors(selectPackage(SimpleTestCase.class.getPackageName())) //
				.filters(includeClassNamePatterns(Pattern.quote(TwoMethodsTestCase.class.getName()))) //
				.build();

		rootDescriptor = testEngine.discover(request, engineId);
		assertThat(rootDescriptor.getChildren()).hasSize(1);
	}

	@Test
	void doesNotDiscoverJUnit4TestClasses() {
		var request = request().selectors(selectClass(JUnitTestCase.class)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(rootDescriptor.getChildren()).isEmpty();
	}

	@Test
	void discoversTestMethodsWithMultipleInvocationsAsContainers() {
		var testClass = SuccessPercentageTestCase.class;
		var request = request().selectors(selectClass(testClass)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		TestDescriptor methodDescriptor = getOnlyElement(classDescriptor.getChildren());
		assertThat(methodDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(methodDescriptor.getChildren()).isEmpty();
		assertThat(methodDescriptor.mayRegisterTests()).isTrue();
	}

	@Test
	void ignoresIgnoredTests() {
		var request = request().selectors(selectClass(IgnoredTestCase.class)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		TestDescriptor classDescriptor = getOnlyElement(rootDescriptor.getChildren());
		TestDescriptor methodDescriptor = getOnlyElement(classDescriptor.getChildren());
		assertThat(methodDescriptor.getDisplayName()).isEqualTo("test");
	}

	@ParameterizedTest
	@ValueSource(classes = { InterfaceTestCase.class, AbstractTestCase.class, RecordTestCase.class,
			EnumTestCase.class })
	void doesNotThrowExceptionWhenNonExecutableTypeOfClassIsSelected(Class<?> testClass) {
		var request = request().selectors(selectClass(testClass)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(rootDescriptor.getChildren()).isEmpty();
	}

	@Test
	void discoversNestedTestClasses() {
		var selectedTestClass = NestedTestClass.class;
		var request = request().selectors(selectClass(selectedTestClass)).build();

		var rootDescriptor = testEngine.discover(request, engineId);

		assertThat(rootDescriptor.getUniqueId()).isEqualTo(engineId);
		assertThat(rootDescriptor.getChildren()).hasSize(2);

		Map<String, TestDescriptor> classDescriptors = rootDescriptor.getChildren().stream() //
				.collect(toMap(TestDescriptor::getDisplayName, identity()));

		TestDescriptor classDescriptor = classDescriptors.get("A");
		assertThat(classDescriptor.getLegacyReportingName()).isEqualTo(NestedTestClass.A.class.getName());
		assertThat(classDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(NestedTestClass.A.class));
		assertThat(classDescriptor.getChildren()).hasSize(1);

		classDescriptor = classDescriptors.get("B");
		assertThat(classDescriptor.getLegacyReportingName()).isEqualTo(NestedTestClass.B.class.getName());
		assertThat(classDescriptor.getType()).isEqualTo(CONTAINER);
		assertThat(classDescriptor.getSource()).contains(ClassSource.from(NestedTestClass.B.class));
		assertThat(classDescriptor.getChildren()).hasSize(1);
	}

	interface InterfaceTestCase {
	}

	static abstract class AbstractTestCase {
	}

	record RecordTestCase() {
	}

	enum EnumTestCase {
	}
}
