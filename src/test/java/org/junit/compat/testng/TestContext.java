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

import org.apache.maven.artifact.versioning.ComparableVersion;

class TestContext {

	static ComparableVersion testNGVersion() {
		return new ComparableVersion(System.getProperty("testng.version"));
	}
}
