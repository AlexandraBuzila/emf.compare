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
package org.eclipse.emf.compare.diagram.internal.factories.extensions;

import java.util.Collection;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.diagram.internal.extensions.DiagramDiff;
import org.eclipse.emf.compare.diagram.internal.extensions.ExtensionsFactory;
import org.eclipse.emf.compare.diagram.internal.extensions.NodeChange;
import org.eclipse.emf.compare.diagram.internal.factories.AbstractDiagramChangeFactory;
import org.eclipse.emf.compare.utils.ReferenceUtil;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.NotationPackage;

/**
 * Factory of node changes.
 * 
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
public class NodeChangeFactory extends AbstractDiagramChangeFactory {

	/**
	 * Constructor.
	 */
	public NodeChangeFactory() {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#getExtensionKind()
	 */
	@Override
	public Class<? extends Diff> getExtensionKind() {
		return NodeChange.class;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#createExtension()
	 */
	@Override
	public DiagramDiff createExtension() {
		return ExtensionsFactory.eINSTANCE.createNodeChange();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#setRefiningChanges(org.eclipse.emf.compare.diagram.internal.extensions.DiagramDiff,
	 *      org.eclipse.emf.compare.DifferenceKind, org.eclipse.emf.compare.Diff)
	 */
	@Override
	public void setRefiningChanges(Diff extension, DifferenceKind extensionKind, Diff refiningDiff) {
		// Macroscopic change on a node is refined by the unit main change and unit children related changes.
		extension.getRefinedBy().add(refiningDiff);
		if (extensionKind != DifferenceKind.MOVE) {
			if (refiningDiff instanceof ReferenceChange) {
				extension.getRefinedBy().addAll(getAllContainedDifferences((ReferenceChange)refiningDiff));
			}
		}
	}

	/**
	 * Get all the changes for the object containing them, from one of them: the given one.
	 * 
	 * @param input
	 *            one of the changes to get.
	 * @return The list of the related changes.
	 */
	protected Collection<Diff> getAllDifferencesForChange(Diff input) {
		return input.getMatch().getDifferences();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#isRelatedToAnExtensionAdd(org.eclipse.emf.compare.ReferenceChange)
	 */
	@Override
	protected boolean isRelatedToAnExtensionAdd(ReferenceChange input) {
		return isExclusive() && isRelatedToAnAddNode(input);
	}

	/**
	 * It checks that the given reference change concerns the add of a node.
	 * 
	 * @param input
	 *            The reference change.
	 * @return True if it concerns the add of a node, False otherwise.
	 */
	protected static boolean isRelatedToAnAddNode(ReferenceChange input) {
		return isContainmentOnSemanticNode(input) && input.getKind() == DifferenceKind.ADD;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#isRelatedToAnExtensionDelete(org.eclipse.emf.compare.ReferenceChange)
	 */
	@Override
	protected boolean isRelatedToAnExtensionDelete(ReferenceChange input) {
		return isExclusive() && isRelatedToADeleteNode(input);
	}

	/**
	 * It checks that the given reference change concerns the delete of a node.
	 * 
	 * @param input
	 *            The reference change.
	 * @return True if it concerns the delete of a node, False otherwise.
	 */
	protected static boolean isRelatedToADeleteNode(ReferenceChange input) {
		return isContainmentOnSemanticNode(input) && input.getKind() == DifferenceKind.DELETE;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#isRelatedToAnExtensionMove(org.eclipse.emf.compare.ReferenceChange)
	 */
	@Override
	protected boolean isRelatedToAnExtensionMove(ReferenceChange input) {
		return isExclusive() && isRelatedToAMoveNode(input);
	}

	/**
	 * It checks that the given reference change concerns the move of a node.
	 * 
	 * @param input
	 *            The reference change.
	 * @return True if it concerns the move of a node, False otherwise.
	 */
	protected static boolean isRelatedToAMoveNode(ReferenceChange input) {
		return isContainmentOnSemanticNode(input) && input.getKind() == DifferenceKind.MOVE;
	}

	/**
	 * It checks that the predicate applies on only this factory and not on potential children factories.
	 * 
	 * @return true if the predicate applies only on this factory.
	 */
	protected boolean isExclusive() {
		return getExtensionKind() == NodeChange.class;
	}

	/**
	 * It checks that the given difference is on a containment link to a Node attached to a semantic object.
	 * 
	 * @param input
	 *            The difference.
	 * @return True if the difference matches with the predicate.
	 */
	private static boolean isContainmentOnSemanticNode(ReferenceChange input) {
		return input.getReference().isContainment() && input.getValue() instanceof Node
				&& ReferenceUtil.safeEGet(input.getValue(), NotationPackage.Literals.VIEW__ELEMENT) != null;
	}

}
