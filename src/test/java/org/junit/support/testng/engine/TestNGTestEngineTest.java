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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.support.testng.engine.TestNGTestEngine.Phase;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.testng.TestNG;

@MockitoSettings(strictness = LENIENT)
public class TestNGTestEngineTest {

	TestNG testNG = new TestNG();

	@Test
	void configuresListenersFromConfigurationParameter(@Mock ConfigurationParameters configurationParameters) {
		when(configurationParameters.get("testng.listeners")) //
				.thenReturn(Optional.of(MyTestListener.class.getName() + " , " + AnotherTestListener.class.getName()));

		Phase.EXECUTION.configure(testNG, configurationParameters);

		assertThat(testNG.getTestListeners()) //
				.hasAtLeastOneElementOfType(MyTestListener.class) //
				.hasAtLeastOneElementOfType(AnotherTestListener.class);
	}

	@Test
	void throwsExceptionForMissingClasses(@Mock ConfigurationParameters configurationParameters) {
		when(configurationParameters.get("testng.listeners")) //
				.thenReturn(Optional.of("acme.MissingClass"));

		assertThatThrownBy(() -> Phase.EXECUTION.configure(testNG, configurationParameters)) //
				.hasMessage("Failed to load custom listener class") //
				.hasRootCauseExactlyInstanceOf(ClassNotFoundException.class) //
				.hasRootCauseMessage("acme.MissingClass");
	}

	@Test
	void throwsExceptionForClassesOfWrongType(@Mock ConfigurationParameters configurationParameters) {
		when(configurationParameters.get("testng.listeners")) //
				.thenReturn(Optional.of(Object.class.getName()));

		assertThatThrownBy(() -> Phase.EXECUTION.configure(testNG, configurationParameters)) //
				.hasMessage("Custom listener class must implement org.testng.ITestNGListener: java.lang.Object");
	}

	static class MyTestListener extends DefaultListener {
	}

	static class AnotherTestListener extends DefaultListener {
	}
}
