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

import static org.apache.commons.lang3.StringUtils.removeEnd;

import org.apache.maven.artifact.versioning.ComparableVersion;

class TestContext {

	static ComparableVersion testNGVersion() {
		return new ComparableVersion(removeEnd(System.getProperty("testng.version", "7.9.0"), "-SNAPSHOT"));
	}
}
