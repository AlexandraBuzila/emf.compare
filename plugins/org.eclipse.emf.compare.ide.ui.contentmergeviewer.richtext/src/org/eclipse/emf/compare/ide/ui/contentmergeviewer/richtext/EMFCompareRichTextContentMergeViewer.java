/**
 * 
 */
package org.eclipse.emf.compare.ide.ui.contentmergeviewer.richtext;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ide.ui.internal.configuration.EMFCompareConfiguration;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.text.EMFCompareTextMergeViewerContentProvider;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.CompareInputAdapter;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Alexandra Buzila
 *
 */
@SuppressWarnings("restriction")
public class EMFCompareRichTextContentMergeViewer extends EMFCompareContentMergeViewer {

	/**
	 * Bundle name of the property file containing all displayed strings.
	 */
	private static final String BUNDLE_NAME = EMFCompareRichTextContentMergeViewer.class.getName();

	protected EMFCompareRichTextContentMergeViewer(Composite parent, EMFCompareConfiguration configuration) {
		super(SWT.NONE, null, configuration);
		buildControl(parent);
		setContentProvider(new EMFCompareTextMergeViewerContentProvider(configuration));
	}

	@Override
	protected IMergeViewer createMergeViewer(Composite parent, MergeViewerSide side) {
		EMFCompareRichTextMergeViewer viewer = new EMFCompareRichTextMergeViewer(parent, side,
				getCompareConfiguration());
		viewer.setContentProvider(new RichTextContentMergeViewerContentProvider());
		viewer.getEditor().addModifyListener(new TextModifyListener(viewer));
		return viewer;
	}

	@Override
	protected void paintCenter(GC g) {
		// TODO Auto-generated method stub

	}

	@Override
	protected byte[] getContents(boolean isleft) {
		// EMFCompareRichTextMergeViewer v = isleft ? left : right;
		// if (v != null) {
		// String text = v.getText();
		// if (text != null)
		// return text.getBytes();
		// }
		return null;
	}

	private class TextModifyListener implements ModifyListener {

		private EMFCompareRichTextMergeViewer viewer;

		public TextModifyListener(EMFCompareRichTextMergeViewer viewer) {
			this.viewer = viewer;
		}

		public void modifyText(ModifyEvent e) {
			String text = viewer.getText();
			final Object oldInput = getInput();
			if (oldInput instanceof CompareInputAdapter) {
				final Diff diff = (Diff) ((CompareInputAdapter) oldInput).getComparisonObject();
				final Match match = diff.getMatch();
				updateModel(diff, text);
			}
		}
	}

	public void updateModel(Diff diff, String text) {
		System.out.println("Model update! New Value: \n"+text);
		
	}

}
