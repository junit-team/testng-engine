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

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

class InvocationDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "invoc";
	private final String legacyReportingName;

	InvocationDescriptor(UniqueId uniqueId, String displayName, String legacyReportingName, MethodSource source) {
		super(uniqueId, displayName, source);
		this.legacyReportingName = legacyReportingName;
	}

	@Override
	public String getLegacyReportingName() {
		return legacyReportingName;
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}
}
