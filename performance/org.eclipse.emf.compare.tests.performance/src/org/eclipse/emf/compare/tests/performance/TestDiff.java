/*******************************************************************************
 * Copyright (c) 2012, 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.tests.performance;

import java.io.IOException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import data.models.Data;
import data.models.LargeInputData;
import data.models.NominalInputData;
import data.models.NominalSplitInputData;
import data.models.SmallInputData;
import data.models.SmallSplitInputData;
import fr.obeo.performance.api.PerformanceMonitor;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDiff extends AbstractEMFComparePerformanceTest {

	
	/** 
	 * {@inheritDoc}
	 * @see org.eclipse.emf.compare.tests.performance.AbstractEMFComparePerformanceTest#setSUTName()
	 */
	@Override
	protected void setSUTName() {
		getPerformance().getSystemUnderTest().setName(TestDiff.class.getSimpleName());
	}

	@Test
	public void a_diffUMLSmall() {
		PerformanceMonitor monitor = getPerformance().createMonitor("diffUMLSmall");
		final Data data = new SmallInputData();
		data.match();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.diff();
			}
		});
		data.dispose();
	}
	
	@Test
	public void b_diffUMLNominal() throws IOException {
		PerformanceMonitor monitor = getPerformance().createMonitor("diffUMLNominal");
		final Data data = new NominalInputData();
		data.match();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.diff();
			}
		});
		data.dispose();
	}
	
	@Test
	public void c_diffUMLSmallSplit() {
		PerformanceMonitor monitor = getPerformance().createMonitor("diffUMLSmallSplit");
		final Data data = new SmallSplitInputData();
		data.match();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.diff();
			}
		});
		data.dispose();
	}
	
	@Test
	public void d_diffUMLNominalSplit() {
		PerformanceMonitor monitor = getPerformance().createMonitor("diffUMLNominalSplit");
		final Data data = new NominalSplitInputData();
		data.match();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.diff();
			}
		});
		data.dispose();
	}
	
//	@Test
	public void e_diffUMLLarge() throws IOException {
		PerformanceMonitor monitor = getPerformance().createMonitor("diffUMLLarge");
		final Data data = new LargeInputData();
		data.match();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.diff();
			}
		});
		data.dispose();
	}
}
