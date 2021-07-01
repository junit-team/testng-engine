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

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

class InvocationDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "invoc";
	private final int invocationIndex;

	InvocationDescriptor(UniqueId uniqueId, String displayName, MethodSource source, int invocationIndex) {
		super(uniqueId, displayName, source);
		this.invocationIndex = invocationIndex;
	}

	@Override
	public String getLegacyReportingName() {
		return String.format("%s[%d]", getParent().orElseThrow(IllegalStateException::new).getLegacyReportingName(),
			invocationIndex);
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}
}
