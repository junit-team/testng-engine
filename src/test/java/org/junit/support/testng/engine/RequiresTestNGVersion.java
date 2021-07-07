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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.function.Predicate.isEqual;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.support.testng.engine.TestContext.testNGVersion;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

@Retention(RUNTIME)
@Target(METHOD)
@ExtendWith(RequiresTestNGVersion.Extension.class)
@interface RequiresTestNGVersion {

	String min() default "";

	String max() default "";

	String[] except() default {};

	class Extension implements ExecutionCondition {
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
			return AnnotationSupport.findAnnotation(context.getElement(), RequiresTestNGVersion.class).map(
				this::satisfiesRequirements).orElse(enabled("no TestNG version requirements"));
		}

		private ConditionEvaluationResult satisfiesRequirements(RequiresTestNGVersion requirements) {
			var actualVersion = testNGVersion();
			if (!requirements.max().isBlank()
					&& actualVersion.compareTo(new ComparableVersion(requirements.max())) > 0) {
				return disabled("max constraint not met: %s > %s".formatted(actualVersion, requirements.max()));
			}
			if (!requirements.min().isBlank()
					&& actualVersion.compareTo(new ComparableVersion(requirements.min())) < 0) {
				return disabled("min constraint not met: %s < %s".formatted(actualVersion, requirements.min()));
			}
			if (Arrays.stream(requirements.except()).map(ComparableVersion::new).anyMatch(isEqual(actualVersion))) {
				return disabled("except constraint not met: %s is contained in %s".formatted(actualVersion,
					Arrays.toString(requirements.except())));
			}
			return enabled("satisfies all TestNG version requirements");
		}
	}
}
