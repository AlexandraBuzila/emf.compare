/**
 * 
 */
package org.eclipse.emf.compare.ide.ui.contentmergeviewer.richtext;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.emf.compare.ide.ui.internal.configuration.EMFCompareConfiguration;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Alexandra Buzila
 *
 */
public class EMFCompareRichTextContentMergeViewerCreator implements IViewerCreator {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.compare.IViewerCreator#createViewer(org.eclipse.swt.widgets
	 * .Composite, org.eclipse.compare.CompareConfiguration)
	 */
	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		return new EMFCompareRichTextContentMergeViewer(parent, new EMFCompareConfiguration(config));

	}

}
