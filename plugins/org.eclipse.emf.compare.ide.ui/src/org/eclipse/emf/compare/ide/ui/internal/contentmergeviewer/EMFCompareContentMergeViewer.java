/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer;

import java.util.EventObject;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.compare.internal.CompareHandlerService;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.domain.ICompareEditingDomain;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareConstants;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.DynamicObject;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.EMFCompareColor;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.RedoAction;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.UndoAction;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.ICompareColor;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.ICompareColorProvider;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewerItem;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.accessor.IStructuralFeatureAccessor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.actions.ActionFactory;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public abstract class EMFCompareContentMergeViewer extends ContentMergeViewer implements ISelectionChangedListener, ICompareColorProvider, IAdaptable {

	private static final String HANDLER_SERVICE = "fHandlerService";

	protected static final int HORIZONTAL = 1;

	protected static final int VERTICAL = 2;

	protected static final double HSPLIT = 0.5;

	protected static final double VSPLIT = 0.3;

	/**
	 * Width of center bar
	 */
	protected static final int CENTER_WIDTH = 34;

	private IMergeViewer fAncestor;

	private IMergeViewer fLeft;

	private IMergeViewer fRight;

	private ActionContributionItem fCopyDiffLeftToRightItem;

	private ActionContributionItem fCopyDiffRightToLeftItem;

	private final Comparison fComparison;

	private final AtomicBoolean fSyncingSelections = new AtomicBoolean(false);

	private EMFCompareColor fColors;

	private final ICompareEditingDomain fEditingDomain;

	private final DynamicObject fDynamicObject;

	private CommandStackListener fCommandStackListener;

	/**
	 * @param style
	 * @param bundle
	 * @param cc
	 */
	protected EMFCompareContentMergeViewer(int style, ResourceBundle bundle, CompareConfiguration cc) {
		super(style, bundle, cc);
		fDynamicObject = new DynamicObject(this);

		fComparison = (Comparison)cc.getProperty(EMFCompareConstants.COMPARE_RESULT);

		fEditingDomain = (ICompareEditingDomain)getCompareConfiguration().getProperty(
				EMFCompareConstants.EDITING_DOMAIN);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.mergeviewer.ICompareColorProvider#getCompareColor()
	 */
	public ICompareColor getCompareColor() {
		return fColors;
	}

	/**
	 * @return the fEditingDomain
	 */
	protected final ICompareEditingDomain getEditingDomain() {
		return fEditingDomain;
	}

	/**
	 * @return the fComparison
	 */
	protected final Comparison getComparison() {
		return fComparison;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overriden to enhance visibility.
	 * </p>
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#isThreeWay()
	 */
	@Override
	public boolean isThreeWay() {
		// enhances visibility
		return super.isThreeWay();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#updateContent(java.lang.Object,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void updateContent(Object ancestor, Object left, Object right) {
		fAncestor.setInput(ancestor);
		fLeft.setInput(left);
		fRight.setInput(right);
		// must update selection after the three viewers input has been set
		// to avoid some NPE/AssertionError (they are calling each other on selectionChanged event to
		// synchronize their selection)

		IMergeViewerItem leftInitialItem = null;
		if (left instanceof IStructuralFeatureAccessor) {
			leftInitialItem = ((IStructuralFeatureAccessor)left).getInitialItem();
		}

		ISelection leftSelection = createSelectionOrEmpty(leftInitialItem);
		fLeft.setSelection(leftSelection, true); // others will synchronize on this one :)

		getCenterControl().redraw();
	}

	private ISelection createSelectionOrEmpty(final Object o) {
		final ISelection selection;
		if (o != null) {
			selection = new StructuredSelection(o);
		} else {
			selection = StructuredSelection.EMPTY;
		}
		return selection;
	}

	/**
	 * Inhibits this method to avoid asking to save on each input change!!
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#doSave(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	protected boolean doSave(Object newInput, Object oldInput) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#createControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createControls(Composite composite) {
		fAncestor = createMergeViewer(composite, MergeViewerSide.ANCESTOR);
		fAncestor.addSelectionChangedListener(this);

		fLeft = createMergeViewer(composite, MergeViewerSide.LEFT);
		fLeft.addSelectionChangedListener(this);

		fRight = createMergeViewer(composite, MergeViewerSide.RIGHT);
		fRight.addSelectionChangedListener(this);

		fColors = new EMFCompareColor(this, null, getCompareConfiguration());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#createToolItems(org.eclipse.jface.action.ToolBarManager)
	 */
	@Override
	protected void createToolItems(ToolBarManager toolBarManager) {
		// Copy actions
		CompareConfiguration cc = getCompareConfiguration();
		if (cc.isRightEditable()) {
			Action copyLeftToRight = new Action() {
				@Override
				public void run() {
					copyDiff(true);
					// Select next diff
					navigate(true);
				}
			};
			Utilities.initAction(copyLeftToRight, getResourceBundle(), "action.CopyDiffLeftToRight."); //$NON-NLS-1$
			copyLeftToRight.setEnabled(false);
			fCopyDiffLeftToRightItem = new ActionContributionItem(copyLeftToRight);
			fCopyDiffLeftToRightItem.setVisible(true);
			toolBarManager.appendToGroup("merge", fCopyDiffLeftToRightItem); //$NON-NLS-1$
			getHandlerService().registerAction(copyLeftToRight, "org.eclipse.compare.copyLeftToRight"); //$NON-NLS-1$
		}

		if (cc.isLeftEditable()) {
			Action copyRightToLeft = new Action() {
				@Override
				public void run() {
					copyDiff(false);
					// Select next diff
					navigate(true);
				}
			};
			Utilities.initAction(copyRightToLeft, getResourceBundle(), "action.CopyDiffRightToLeft."); //$NON-NLS-1$
			copyRightToLeft.setEnabled(false);
			fCopyDiffRightToLeftItem = new ActionContributionItem(copyRightToLeft);
			fCopyDiffRightToLeftItem.setVisible(true);
			toolBarManager.appendToGroup("merge", fCopyDiffRightToLeftItem); //$NON-NLS-1$
			getHandlerService().registerAction(copyRightToLeft, "org.eclipse.compare.copyRightToLeft"); //$NON-NLS-1$
		}

		// Navigation
		final Action nextDiff = new Action() {
			@Override
			public void run() {
				navigate(true);
			}
		};
		Utilities.initAction(nextDiff, getResourceBundle(), "action.NextDiff.");
		ActionContributionItem contributionNextDiff = new ActionContributionItem(nextDiff);
		contributionNextDiff.setVisible(true);
		toolBarManager.appendToGroup("navigation", contributionNextDiff);

		final Action previousDiff = new Action() {
			@Override
			public void run() {
				navigate(false);
			}
		};
		Utilities.initAction(previousDiff, getResourceBundle(), "action.PrevDiff.");
		ActionContributionItem contributionPreviousDiff = new ActionContributionItem(previousDiff);
		contributionPreviousDiff.setVisible(true);
		toolBarManager.appendToGroup("navigation", contributionPreviousDiff);

		// Undo/Redo
		final UndoAction undoAction = new UndoAction(fEditingDomain);
		final RedoAction redoAction = new RedoAction(fEditingDomain);

		if (fCommandStackListener == null) {
			fCommandStackListener = new CommandStackListener() {
				public void commandStackChanged(EventObject event) {
					undoAction.update();
					redoAction.update();
					setLeftDirty(fEditingDomain.getCommandStack().isLeftSaveNeeded());
					setRightDirty(fEditingDomain.getCommandStack().isRightSaveNeeded());
					refresh();
				}
			};
		}
		fEditingDomain.getCommandStack().addCommandStackListener(fCommandStackListener);

		getHandlerService().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		getHandlerService().setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#copy(boolean)
	 */
	@Override
	protected void copy(boolean leftToRight) {
		EList<Diff> differences = getComparison().getDifferences();

		final Command copyCommand = getEditingDomain().createCopyAllNonConflictingCommand(differences,
				leftToRight);

		getEditingDomain().getCommandStack().execute(copyCommand);

		// if (leftToRight) {
		// setRightDirty(true);
		// } else {
		// setLeftDirty(true);
		// }
		refresh();
	}

	/**
	 * Called by the framework to navigate to the next (or previous) difference. This will open the content
	 * viewer for the next (or previous) diff displayed in the structure viewer.
	 * 
	 * @param next
	 *            <code>true</code> if we are to open the next structure viewer's diff, <code>false</code> if
	 *            we should go to the previous instead.
	 */
	protected void navigate(boolean next) {
		final Control control = getControl();
		if (control != null && !control.isDisposed()) {
			final ICompareNavigator navigator = getCompareConfiguration().getContainer().getNavigator();
			if (navigator instanceof CompareNavigator && ((CompareNavigator)navigator).hasChange(next)) {
				navigator.selectChange(next);
			}
		}
	}

	/**
	 * 
	 */
	protected abstract void copyDiff(boolean leftToRight);

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#handleResizeAncestor(int, int, int, int)
	 */
	@Override
	protected void handleResizeAncestor(int x, int y, int width, int height) {
		if (width > 0) {
			getAncestorMergeViewer().getControl().setVisible(true);
			getAncestorMergeViewer().getControl().setBounds(x, y, width, height);
		} else {
			getAncestorMergeViewer().getControl().setVisible(false);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#handleResizeLeftRight(int, int, int,
	 *      int, int, int)
	 */
	@Override
	protected void handleResizeLeftRight(int x, int y, int width1, int centerWidth, int width2, int height) {
		fLeft.getControl().setBounds(x, y, width1, height);
		fRight.getControl().setBounds(x + width1 + centerWidth, y, width2, height);
	}

	protected abstract IMergeViewer createMergeViewer(Composite parent, MergeViewerSide side);

	@Override
	protected final int getCenterWidth() {
		return CENTER_WIDTH;
	}

	protected final CompareHandlerService getHandlerService() {
		return (CompareHandlerService)fDynamicObject.get(HANDLER_SERVICE);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#getControl()
	 */
	@Override
	public Composite getControl() {
		return (Composite)super.getControl();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#createCenterControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createCenterControl(Composite parent) {
		final Control ret = super.createCenterControl(parent);

		final PaintListener paintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {
				paintCenter(e.gc);
			}
		};
		ret.addPaintListener(paintListener);

		ret.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ret.removePaintListener(paintListener);
			}
		});

		return ret;
	}

	protected abstract void paintCenter(GC g);

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == CompareHandlerService.class) {
			return getHandlerService();
		}
		if (adapter == CompareHandlerService[].class) {
			return new CompareHandlerService[] {getHandlerService(), };
		}
		return null;
	}

	/**
	 * @return the fAncestor
	 */
	protected IMergeViewer getAncestorMergeViewer() {
		return fAncestor;
	}

	/**
	 * @return the fLeft
	 */
	protected IMergeViewer getLeftMergeViewer() {
		return fLeft;
	}

	/**
	 * @return the fRight
	 */
	protected IMergeViewer getRightMergeViewer() {
		return fRight;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		if (fSyncingSelections.compareAndSet(false, true)) { // prevents stack overflow :)
			try {
				ISelectionProvider selectionProvider = event.getSelectionProvider();
				ISelection selection = event.getSelection();
				synchronizeSelection(selectionProvider, selection);
				updateToolItems();
			} finally {
				fSyncingSelections.set(false);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#updateToolItems()
	 */
	@Override
	protected void updateToolItems() {
		super.updateToolItems();

		Diff diff = getDiffFrom(getRightMergeViewer());
		if (diff == null) {
			diff = getDiffFrom(getLeftMergeViewer());
		}
		boolean enableCopy = false;
		if (diff != null) {
			enableCopy = true;
		}

		if (fCopyDiffLeftToRightItem != null) {
			fCopyDiffLeftToRightItem.getAction().setEnabled(enableCopy);
		}
		if (fCopyDiffRightToLeftItem != null) {
			fCopyDiffRightToLeftItem.getAction().setEnabled(enableCopy);
		}
	}

	/**
	 * Checks the element selected in the given viewer in order to determine whether it can be adapted into a
	 * Diff.
	 * 
	 * @param viewer
	 *            The viewer which selection is to be checked.
	 * @return The first of the Diffs selected in the given viewer, if any.
	 */
	protected Diff getDiffFrom(IMergeViewer viewer) {
		Diff diff = null;
		final ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			final Iterator<?> selectedElements = ((IStructuredSelection)selection).iterator();
			while (diff == null && selectedElements.hasNext()) {
				final Object element = selectedElements.next();
				if (element instanceof IMergeViewerItem) {
					diff = ((IMergeViewerItem)element).getDiff();
				}
			}
		}
		return diff;
	}

	private void synchronizeSelection(final ISelectionProvider selectionProvider, final ISelection selection) {
		if (selectionProvider == fLeft) {
			fRight.setSelection(selection, true);
			fAncestor.setSelection(selection, true);
		} else if (selectionProvider == fRight) {
			fLeft.setSelection(selection, true);
			fAncestor.setSelection(selection, true);
		} else { // selectionProvider == fAncestor
			fLeft.setSelection(selection, true);
			fRight.setSelection(selection, true);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		if (fCommandStackListener != null) {
			fEditingDomain.getCommandStack().removeCommandStackListener(fCommandStackListener);
		}
		super.handleDispose(event);
	}
}
