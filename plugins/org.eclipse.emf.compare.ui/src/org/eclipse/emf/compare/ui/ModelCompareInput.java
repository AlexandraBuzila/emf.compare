/*******************************************************************************
 * Copyright (c) 2006, 2007 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.ui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.compare.diff.merge.api.AbstractMerger;
import org.eclipse.emf.compare.diff.merge.api.MergeFactory;
import org.eclipse.emf.compare.diff.metamodel.AttributeChange;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChange;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChange;
import org.eclipse.emf.compare.match.metamodel.Match2Elements;
import org.eclipse.emf.compare.match.metamodel.Match3Element;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.ui.util.EMFCompareConstants;
import org.eclipse.emf.compare.ui.util.EMFCompareEObjectUtils;
import org.eclipse.swt.graphics.Image;

/**
 * Input to be used for a 2 or 3-way comparison in a
 * {@link org.eclipse.emf.compare.ui.viewer.content.ModelContentMergeViewer ModelContentMergeViewer}.
 * 
 * @author Cedric Brun <a href="mailto:cedric.brun@obeo.fr">cedric.brun@obeo.fr</a>
 */
public class ModelCompareInput implements ICompareInput {
	/** {@link DiffModel} result of the underlying comparison. */
	private final DiffModel diff;

	/** Memorizes all listeners registered for this {@link ICompareInput compare input}. */
	private final List<ICompareInputChangeListener> inputChangeListeners = new ArrayList<ICompareInputChangeListener>();

	/** {@link MatchModel} result of the underlying comparison. */
	private final MatchModel match;

	/**
	 * Creates a CompareInput given the resulting
	 * {@link org.eclipse.emf.compare.match.diff.match.MatchModel match} and
	 * {@link org.eclipse.emf.compare.match.diff.diff.DiffModel diff} of the comparison.
	 * 
	 * @param matchModel
	 *            {@link org.eclipse.emf.compare.match.diff.match.MatchModel match} of the comparison.
	 * @param diffModel
	 *            {@link org.eclipse.emf.compare.match.diff.diff.DiffModel diff} of the comparison.
	 */
	public ModelCompareInput(MatchModel matchModel, DiffModel diffModel) {
		match = matchModel;
		diff = diffModel;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#addCompareInputChangeListener(ICompareInputChangeListener)
	 */
	public void addCompareInputChangeListener(ICompareInputChangeListener listener) {
		inputChangeListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#copy(boolean)
	 */
	public void copy(boolean leftToRight) {
		for (final DiffElement aDiff : getDiffAsList()) {
			// we might remove the diff from the list before merging it (eOpposite reference)
			if (aDiff.eContainer() != null)
				doCopy(aDiff, leftToRight);
		}
		fireCompareInputChanged();
	}

	/**
	 * Copies a single {@link DiffElement} or a {@link DiffGroup} in the given direction.
	 * 
	 * @param element
	 *            {@link DiffElement Element} to copy.
	 * @param leftToRight
	 *            Direction of the copy.
	 */
	@SuppressWarnings("unchecked")
	public void copy(DiffElement element, boolean leftToRight) {
		if (element instanceof DiffGroup) {
			final List<DiffElement> subDiffList = new LinkedList<DiffElement>(((DiffGroup)element)
					.getSubDiffElements());
			for (DiffElement subDiff : subDiffList) {
				if (subDiff instanceof DiffGroup)
					copy(subDiff, leftToRight);
				else
					doCopy(subDiff, leftToRight);
			}
		} else {
			doCopy(element, leftToRight);
		}
		fireCompareInputChanged();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#getAncestor()
	 */
	public ITypedElement getAncestor() {
		ITypedElement element = null;

		if (getMatch().getMatchedElements().get(0) instanceof Match3Element)
			element = new TypedElementWrapper(((Match3Element)getMatch().getMatchedElements().get(0))
					.getOriginElement());

		return element;
	}

	/**
	 * Returns this ModelCompareInput's DiffModel.
	 * 
	 * @return This ModelCompareInput's DiffModel.
	 */
	public DiffModel getDiff() {
		return diff;
	}

	/**
	 * Returns the {@link DiffElement} of the input {@link DiffModel} as a list. Doesn't take
	 * {@link DiffGroup}s into account.
	 * 
	 * @return The {@link DiffElement} of the input {@link DiffModel} as a list.
	 */
	public List<DiffElement> getDiffAsList() {
		final List<DiffElement> diffs = new LinkedList<DiffElement>();
		// We'll order the diffs by class (modelElementChange, attributechange then referenceChange)
		final List<ModelElementChange> modelElementDiffs = new LinkedList<ModelElementChange>();
		final List<AttributeChange> attributeChangeDiffs = new LinkedList<AttributeChange>();
		final List<ReferenceChange> referenceChangeDiffs = new LinkedList<ReferenceChange>();
		for (final TreeIterator iterator = getDiff().eAllContents(); iterator.hasNext(); ) {
			final DiffElement aDiff = (DiffElement)iterator.next();
			if (aDiff instanceof ModelElementChange)
				modelElementDiffs.add((ModelElementChange)aDiff);
			else if (aDiff instanceof AttributeChange)
				attributeChangeDiffs.add((AttributeChange)aDiff);
			else if (aDiff instanceof ReferenceChange)
				referenceChangeDiffs.add((ReferenceChange)aDiff);
		}
		diffs.addAll(modelElementDiffs);
		diffs.addAll(attributeChangeDiffs);
		diffs.addAll(referenceChangeDiffs);
		modelElementDiffs.clear();
		attributeChangeDiffs.clear();
		referenceChangeDiffs.clear();
		return diffs;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#getImage()
	 */
	public Image getImage() {
		Image image = null;

		if (getMatch() != null)
			image = EMFCompareEObjectUtils.computeObjectImage(getMatch());
		else if (getDiff() != null)
			image = EMFCompareEObjectUtils.computeObjectImage(getDiff());

		return image;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#getKind()
	 */
	public int getKind() {
		if (getAncestor() != null)
			return EMFCompareConstants.ENABLE_ANCESTOR;
		return EMFCompareConstants.NO_CHANGE;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#getLeft()
	 */
	public ITypedElement getLeft() {
		ITypedElement element = null;

		if (getMatch().getMatchedElements().get(0) instanceof Match2Elements)
			element = new TypedElementWrapper(((Match2Elements)getMatch().getMatchedElements().get(0))
					.getLeftElement());

		return element;
	}

	/**
	 * Returns this ModelCompareInput's MatchModel.
	 * 
	 * @return This ModelCompareInput's MatchModel.
	 */
	public MatchModel getMatch() {
		return match;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#getName()
	 */
	public String getName() {
		String name = null;

		if (getMatch() != null)
			name = EMFCompareEObjectUtils.computeObjectName(getMatch());
		else if (getDiff() != null)
			name = EMFCompareEObjectUtils.computeObjectName(getDiff());

		return name;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#getRight()
	 */
	public ITypedElement getRight() {
		ITypedElement element = null;

		if (getMatch().getMatchedElements().get(0) instanceof Match2Elements)
			element = new TypedElementWrapper(((Match2Elements)getMatch().getMatchedElements().get(0))
					.getRightElement());

		return element;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ICompareInput#removeCompareInputChangeListener(ICompareInputChangeListener)
	 */
	public void removeCompareInputChangeListener(ICompareInputChangeListener listener) {
		inputChangeListeners.remove(listener);
	}

	/**
	 * Applies the changes implied by a given {@link DiffElement} in the direction specified by
	 * <code>leftToRight</code>.
	 * 
	 * @param element
	 *            {@link DiffElement} containing the copy information.
	 * @param leftToRight
	 *            <code>True</code> if the changes must be applied from the left to the right model,
	 *            <code>False</code> otherwise.
	 */
	protected void doCopy(DiffElement element, boolean leftToRight) {
		final AbstractMerger merger = MergeFactory.createMerger(element);
		if (leftToRight && merger.canUndoInTarget()) {
			merger.undoInTarget();
		} else if (!leftToRight && merger.canApplyInOrigin()) {
			merger.applyInOrigin();
		}
	}

	/**
	 * Notifies all {@link ICompareInputChangeListener listeners} registered for this
	 * {@link ModelCompareInput input} that a change occured.
	 */
	protected void fireCompareInputChanged() {
		for (ICompareInputChangeListener listener : inputChangeListeners) {
			listener.compareInputChanged(this);
		}
	}
}
