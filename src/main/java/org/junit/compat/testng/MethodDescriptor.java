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

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

class MethodDescriptor extends AbstractTestDescriptor {

	protected MethodDescriptor(UniqueId uniqueId, String displayName, TestSource source) {
		super(uniqueId, displayName, source);
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}

	@Override
	public boolean mayRegisterTests() {
		return false;
	}
}
