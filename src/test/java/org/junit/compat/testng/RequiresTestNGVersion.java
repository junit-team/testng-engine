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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.text.MessageFormat.format;
import static org.junit.compat.testng.TestContext.testNGVersion;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
				return disabled(format("max constraint not met: {0} > {1}", actualVersion, requirements.max()));
			}
			if (!requirements.min().isBlank()
					&& actualVersion.compareTo(new ComparableVersion(requirements.min())) < 0) {
				return disabled(format("min constraint not met: {0} < {1}", actualVersion, requirements.min()));
			}
			return enabled("satisfies all TestNG version requirements");
		}
	}
}
