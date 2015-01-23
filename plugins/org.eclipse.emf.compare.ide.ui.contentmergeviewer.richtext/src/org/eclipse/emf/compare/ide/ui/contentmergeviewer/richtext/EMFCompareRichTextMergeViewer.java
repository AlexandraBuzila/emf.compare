/**
 * 
 */
package org.eclipse.emf.compare.ide.ui.contentmergeviewer.richtext;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.accessor.AccessorAdapter;
import org.eclipse.emf.compare.rcp.ui.internal.configuration.IEMFCompareConfiguration;
import org.eclipse.emf.compare.rcp.ui.internal.mergeviewer.impl.AbstractMergeViewer;
import org.eclipse.epf.richtext.RichTextSelection;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.papyrus.infra.widgets.epf.richtext.extension.RichTextEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Alexandra Buzila
 *
 */
@SuppressWarnings("restriction")
public class EMFCompareRichTextMergeViewer extends AbstractMergeViewer {

	private RichTextEditor richTextEditor;
	private Control fControl;

	public EMFCompareRichTextMergeViewer(Composite parent, MergeViewerSide side,
			IEMFCompareConfiguration compareConfiguration) {
		super(side, compareConfiguration);
		fControl = createControl(parent);
		hookControl(fControl);
	}

	
	private Control createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(composite);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(composite);
		richTextEditor = new RichTextEditor(composite, SWT.NONE);
		return composite;
	}

	@Override
	public void setInput(Object input) {
		if (input instanceof AccessorAdapter) {
			AccessorAdapter adapter = (AccessorAdapter) input;
			String readString;
			try {
				readString = Utilities.readString(adapter);
				richTextEditor.setText(readString);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Control getControl() {
		return fControl;
	}

	@Override
	public ISelection getSelection() {
		final RichTextSelection selected = richTextEditor.getSelected();
		return new ISelection() {
			public boolean isEmpty() {
				if (selected == null)
					return true;
				if (selected.getText() == null)
					return true;
				return selected.getText().isEmpty();
			}
		};
	}

	@Override
	public void refresh() {
		// TODO
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		// TODO
	}

	public String getText() {
		return richTextEditor.getText();
	}
	
	public RichTextEditor getEditor(){
		return richTextEditor;
	}
}