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

import static java.util.Collections.emptySet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.Optional;

import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

class TestNGSelectorResolver implements SelectorResolver {

	@Override
	public Resolution resolve(ClassSelector selector, Context context) {
		return context.addToParent(parent -> Optional.of(createClassDescriptor(selector, parent))).map(
			classDescriptor -> Match.exact(classDescriptor, () -> {
				classDescriptor.selectEntireClass();
				return emptySet();
			})) //
				.map(Resolution::match) //
				.orElse(Resolution.unresolved());
	}

	@Override
	public Resolution resolve(MethodSelector selector, Context context) {
		return context.resolve(selectClass(selector.getJavaClass())) //
				.map(parent -> {
					((ClassDescriptor) parent).includeTestMethod(selector.getMethodName());
					return parent;
				}) //
				.map(Match::partial) //
				.map(Resolution::match) //
				.orElse(Resolution.unresolved());
	}

	private ClassDescriptor createClassDescriptor(ClassSelector selector,
			org.junit.platform.engine.TestDescriptor parent) {
		return new ClassDescriptor(parent.getUniqueId().append("class", selector.getClassName()),
			selector.getJavaClass());
	}
}
