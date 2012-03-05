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
package org.eclipse.emf.compare.uml2.diff.internal.extension.profile;

import org.eclipse.emf.compare.diff.metamodel.AbstractDiffExtension;
import org.eclipse.emf.compare.diff.metamodel.AttributeChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.uml2.diff.UML2DiffEngine;
import org.eclipse.emf.compare.uml2.diff.internal.extension.AbstractDiffExtensionFactory;
import org.eclipse.emf.compare.uml2diff.UML2DiffFactory;
import org.eclipse.emf.compare.uml2diff.UMLStereotypeAttributeChangeLeftTarget;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.util.UMLUtil;

/**
 * Factory for UMLStereotypeAttributeChangeLeftTarget.
 * 
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class UMLStereotypeAttributeChangeLeftTargetFactory extends AbstractDiffExtensionFactory {

	/**
	 * Constructor.
	 * 
	 * @param engine
	 *            The UML2 difference engine.
	 */
	public UMLStereotypeAttributeChangeLeftTargetFactory(UML2DiffEngine engine) {
		super(engine);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.uml2.diff.internal.extension.IDiffExtensionFactory#handles(org.eclipse.emf.compare.diff.metamodel.DiffElement)
	 */
	public boolean handles(DiffElement input) {
		if (input instanceof AttributeChangeLeftTarget) {
			final EObject left = ((AttributeChangeLeftTarget)input).getLeftElement();
			final EObject right = ((AttributeChangeLeftTarget)input).getRightElement();
			final EObject leftBase = UMLUtil.getBaseElement(left);
			final EObject rightBase = UMLUtil.getBaseElement(right);
			return leftBase != null && rightBase != null;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.uml2.diff.internal.extension.IDiffExtensionFactory#create(org.eclipse.emf.compare.diff.metamodel.DiffElement,
	 *      org.eclipse.emf.ecore.util.EcoreUtil.CrossReferencer)
	 */
	public AbstractDiffExtension create(DiffElement input, EcoreUtil.CrossReferencer crossReferencer) {
		final AttributeChangeLeftTarget attributeChangeLeftTarget = (AttributeChangeLeftTarget)input;
		final EObject leftElement = attributeChangeLeftTarget.getLeftElement();
		final EObject rightElement = attributeChangeLeftTarget.getRightElement();
		final EObject leftBase = UMLUtil.getBaseElement(leftElement);
		final EObject rightBase = UMLUtil.getBaseElement(rightElement);

		final UMLStereotypeAttributeChangeLeftTarget ret = UML2DiffFactory.eINSTANCE
				.createUMLStereotypeAttributeChangeLeftTarget();

		ret.setStereotype(UMLUtil.getStereotype(leftElement));
		ret.setRemote(input.isRemote());

		ret.setAttribute(attributeChangeLeftTarget.getAttribute());
		ret.setLeftElement(leftBase);
		ret.setRightElement(rightBase);
		ret.setLeftTarget(attributeChangeLeftTarget.getLeftTarget());

		ret.getHideElements().add(input);
		ret.getRequires().add(input);

		return ret;
	}

	@Override
	public DiffElement getParentDiff(DiffElement input, EcoreUtil.CrossReferencer crossReferencer) {
		final EObject right = ((AttributeChangeLeftTarget)input).getRightElement();
		final EObject rightBase = UMLUtil.getBaseElement(right);

		final DiffModel rootDiffGroup = (DiffModel)EcoreUtil.getRootContainer(input);

		return findOrCreateDiffGroup(rootDiffGroup, rightBase, crossReferencer);
	}
}
