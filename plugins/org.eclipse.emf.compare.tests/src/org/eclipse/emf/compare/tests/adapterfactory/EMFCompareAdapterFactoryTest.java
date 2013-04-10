/*******************************************************************************
 * Copyright (c) 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.tests.adapterfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.internal.adapterfactory.RankedAdapterFactoryDescriptorRegistryImpl;
import org.eclipse.emf.compare.internal.adapterfactory.RankedAdapterFactory;
import org.eclipse.emf.compare.internal.adapterfactory.RankedAdapterFactoryDescriptor;
import org.eclipse.emf.compare.provider.IItemStyledLabelProvider;
import org.eclipse.emf.compare.provider.spec.CompareItemProviderAdapterFactorySpec;
import org.eclipse.emf.compare.provider.spec.ComparisonItemProviderSpec;
import org.eclipse.emf.compare.provider.utils.ComposedStyledString;
import org.eclipse.emf.compare.provider.utils.IStyledString.IComposedStyledString;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.junit.Test;

/**
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
@SuppressWarnings({"restriction", "nls" })
public class EMFCompareAdapterFactoryTest {

	@Test
	public void testEMFCompareAdapterFactory() throws IOException {

		final RankedAdapterFactoryDescriptorRegistryImpl registry = new RankedAdapterFactoryDescriptorRegistryImpl(
				ComposedAdapterFactory.Descriptor.Registry.INSTANCE);

		final Collection<String> key = new ArrayList<String>();
		key.add("http://www.eclipse.org/emf/compare");
		key.add("org.eclipse.emf.compare.provider.IItemStyledLabelProvider");

		registry.put(key, new TestEMFCompareAdapterFactoryDescriptor(
				new CompareItemProviderAdapterFactorySpec2(), 10));
		registry.put(key, new TestEMFCompareAdapterFactoryDescriptor(
				new CompareItemProviderAdapterFactorySpec3(), 20));

		final Collection<String> keyIItemLabelProvider = new ArrayList<String>();
		keyIItemLabelProvider.add("http://www.eclipse.org/emf/compare");
		keyIItemLabelProvider.add("org.eclipse.emf.edit.provider.IItemLabelProvider");
		registry.put(keyIItemLabelProvider, new TestEMFCompareAdapterFactoryDescriptor(
				new CompareItemProviderAdapterFactorySpec2(), 30));

		final AdapterFactory fAdapterFactory = new ComposedAdapterFactory(registry);

		final Comparison comparison = CompareFactory.eINSTANCE.createComparison();
		Adapter adapter = fAdapterFactory.adapt(comparison, IItemStyledLabelProvider.class);

		assertTrue(adapter instanceof ComparisonItemProviderSpec3);

		IComposedStyledString styledText = ((IItemStyledLabelProvider)adapter).getStyledText(comparison);

		assertEquals("ComparisonItemProviderSpecRanking20", styledText.getString());

	}

	/**
	 * EMFCompareAdapterFactory.Descriptor used for test ranking.
	 */
	public class TestEMFCompareAdapterFactoryDescriptor implements RankedAdapterFactoryDescriptor {

		AdapterFactory adapterFactory;

		public TestEMFCompareAdapterFactoryDescriptor(AdapterFactory adapterFactory, int ranking) {
			this.adapterFactory = adapterFactory;
			((RankedAdapterFactory)adapterFactory).setRanking(ranking);
		}

		public AdapterFactory createAdapterFactory() {
			return adapterFactory;
		}

		public int getRanking() {
			return ((RankedAdapterFactory)adapterFactory).getRanking();
		}

	}

	/**
	 * Specialized CompareItemProviderAdapterFactorySpec, used for test ranking.
	 */
	public class CompareItemProviderAdapterFactorySpec2 extends CompareItemProviderAdapterFactorySpec implements RankedAdapterFactory {

		private int ranking;

		public CompareItemProviderAdapterFactorySpec2() {
			super();
		}

		public int getRanking() {
			return ranking;
		}

		public void setRanking(int ranking) {
			this.ranking = ranking;
		}

		@Override
		public Adapter createComparisonAdapter() {
			if (comparisonItemProvider == null) {
				comparisonItemProvider = new ComparisonItemProviderSpec2(this);
			}

			return comparisonItemProvider;
		}
	}

	/**
	 * Specialized ComparisonItemProviderSpec, used for test ranking.
	 */
	public class ComparisonItemProviderSpec2 extends ComparisonItemProviderSpec {

		public ComparisonItemProviderSpec2(AdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		@Override
		public IComposedStyledString getStyledText(Object object) {
			return new ComposedStyledString("ComparisonItemProviderSpecRanking10");
		}
	}

	/**
	 * Specialized CompareItemProviderAdapterFactorySpec, used for test ranking.
	 */
	public class CompareItemProviderAdapterFactorySpec3 extends CompareItemProviderAdapterFactorySpec implements RankedAdapterFactory {

		private int ranking;

		public CompareItemProviderAdapterFactorySpec3() {
			super();
		}

		public int getRanking() {
			return ranking;
		}

		public void setRanking(int ranking) {
			this.ranking = ranking;
		}

		@Override
		public Adapter createComparisonAdapter() {
			if (comparisonItemProvider == null) {
				comparisonItemProvider = new ComparisonItemProviderSpec3(this);
			}

			return comparisonItemProvider;
		}
	}

	/**
	 * Specialized ComparisonItemProviderSpec, used for test ranking.
	 */
	public class ComparisonItemProviderSpec3 extends ComparisonItemProviderSpec {

		public ComparisonItemProviderSpec3(AdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		@Override
		public IComposedStyledString getStyledText(Object object) {
			return new ComposedStyledString("ComparisonItemProviderSpecRanking20");
		}
	}

}