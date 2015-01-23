/**
 * 
 */
package org.eclipse.emf.compare.ide.ui.contentmergeviewer.richtext;

import org.eclipse.compare.contentmergeviewer.IMergeViewerContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

/**
 * @author Alexandra Buzila
 *
 */
public class RichTextContentMergeViewerContentProvider implements IMergeViewerContentProvider {

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		System.out.println("input changed ");
		if (newInput != null) {
			EMFCompareRichTextMergeViewer richTextViewer = (EMFCompareRichTextMergeViewer) viewer;
			richTextViewer.setInput(newInput);
		}
	}

	public Image getAncestorImage(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getAncestorContent(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean showAncestor(Object input) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getLeftLabel(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public Image getLeftImage(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getLeftContent(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isLeftEditable(Object input) {
		// TODO Auto-generated method stub
		return true;
	}

	public void saveLeftContent(Object input, byte[] bytes) {
		// TODO Auto-generated method stub
	}

	public String getRightLabel(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public Image getRightImage(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getRightContent(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRightEditable(Object input) {
		// TODO Auto-generated method stub
		return true;
	}

	public void saveRightContent(Object input, byte[] bytes) {
		// TODO Auto-generated method stub
	}

	public String getAncestorLabel(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

}
