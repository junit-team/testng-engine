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
import static java.util.Collections.singleton;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

class TestNGSelectorResolver implements SelectorResolver {

	private final Predicate<String> classNameFilter;

	TestNGSelectorResolver(Predicate<String> classNameFilter) {
		this.classNameFilter = classNameFilter;
	}

	@Override
	public Resolution resolve(ClassSelector selector, Context context) {
		if (!classNameFilter.test(selector.getClassName())) {
			return Resolution.unresolved();
		}
		return context.addToParent(parent -> Optional.of(createClassDescriptor(selector, parent))) //
				.map(classDescriptor -> Match.exact(classDescriptor, () -> {
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

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		UniqueId uniqueId = selector.getUniqueId();
		Segment lastSegment = uniqueId.getLastSegment();
		if (ClassDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
			return Resolution.selectors(singleton(selectClass(lastSegment.getValue())));
		}
		if (MethodDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
			String methodName = lastSegment.getValue();
			int i = methodName.indexOf('(');
			if (i != -1) {
				methodName = methodName.substring(0, i);
			}
			Segment previousSegment = uniqueId.removeLastSegment().getLastSegment();
			if (ClassDescriptor.SEGMENT_TYPE.equals(previousSegment.getType())) {
				String className = previousSegment.getValue();
				return Resolution.selectors(singleton(selectMethod(className, methodName)));
			}
		}
		return Resolution.unresolved();
	}

	private ClassDescriptor createClassDescriptor(ClassSelector selector, TestDescriptor parent) {
		UniqueId uniqueId = parent.getUniqueId().append(ClassDescriptor.SEGMENT_TYPE, selector.getClassName());
		return new ClassDescriptor(uniqueId, selector.getJavaClass());
	}
}
