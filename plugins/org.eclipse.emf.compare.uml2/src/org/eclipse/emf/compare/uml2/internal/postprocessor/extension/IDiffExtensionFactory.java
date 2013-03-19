/*******************************************************************************
 * Copyright (c) 2011, 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.internal.postprocessor.extension;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.uml2.internal.UMLDiff;

/**
 * An {@link IDiffExtensionFactory} is a factory capable to create an {@link AbstractDiffExtension} from a
 * {@link DiffElement} if and only if this factory can {@link #handles(DiffElement) handle} the given
 * {@link DiffElement}.
 * <p>
 * A factory must be able to say in which parent a {@link AbstractDiffExtension} must be attached if it
 * handles the {@link DiffElement} from which it has been {@link #create(DiffElement) created}.
 */
public interface IDiffExtensionFactory {

	Class<? extends UMLDiff> getExtensionKind();

	/**
	 * Returns true if this factory handles the given kind of DiffElement, i.e., if it can create an
	 * {@link AbstractDiffExtension}.
	 * <p>
	 * <b>Performance note: </b> this method should return as quickly as possible as it will called on every
	 * {@link DiffElement} of the DiffModel.
	 * 
	 * @param input
	 *            the element to test
	 * @return true if this factory handles the given input, false otherwise.
	 */
	boolean handles(Diff input);

	/**
	 * Creates and returns an {@link AbstractDiffExtension} from the given {@link DiffElement}. The returned
	 * element MUST NOT be added to its parent, it will be done by the UML2DiffEngine.
	 * 
	 * @param input
	 *            The input difference element.
	 * @return The difference extension.
	 */
	Diff create(Diff input);

	/**
	 * Returns the {@link DiffElement} in which the {@link #create(DiffElement) created}
	 * {@link AbstractDiffExtension} will be added as a sub diff element. This {@link DiffElement} can be from
	 * the model or newly created.
	 * 
	 * @param input
	 *            The input difference element.
	 * @return The difference extension.
	 */
	Match getParentMatch(Diff input);

	/**
	 * Sets the required link of the difference extension created by the related factory.
	 * 
	 * @param diff
	 *            The difference extension.
	 * @param comaprison
	 *            The comparison.
	 */
	void fillRequiredDifferences(Comparison comparison, UMLDiff extension);
}