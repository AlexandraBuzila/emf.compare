/*******************************************************************************
 * Copyright (c) 2006, 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Cedric Notot - [374185] Performance issue
 *******************************************************************************/
package org.eclipse.emf.compare.diagram.papyrus;

import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.papyrus.infra.gmfdiag.common.editpolicies.IMaskManagedLabelEditPolicy;
import org.eclipse.papyrus.uml.diagram.activity.edit.parts.UMLEditPartFactory;
import org.eclipse.papyrus.uml.tools.utils.ICustomAppearence;
import org.eclipse.papyrus.uml.tools.utils.PropertyUtil;
import org.eclipse.uml2.uml.Property;

/**
 * View label provider for Activity.
 * @author Mickael Barbero <a href="mailto:mickael.barbero@obeo.fr">mickael.barbero@obeo.fr</a>
 */
public class ActivityViewLabelProvider extends AbstractUMLViewLabelProvider {

	/**
	 * The edit part factory.
	 */
	private static final UMLEditPartFactory EDIT_PART_FACTORY = new UMLEditPartFactory();
	
	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.emf.compare.diagram.provider.AbstractLabelProvider#createEditPart(org.eclipse.gmf.runtime.notation.View)
	 */
	@Override
	protected EditPart createEditPart(View view) {
		return EDIT_PART_FACTORY.createEditPart(null, view);
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.emf.compare.diagram.papyrus.AbstractUMLViewLabelProvider#getLabelSwitch(org.eclipse.papyrus.diagram.common.editpolicies.IMaskManagedLabelEditPolicy)
	 */
	protected LabelSwitch getLabelSwitch(IMaskManagedLabelEditPolicy labelEditPolicy) {
		return new ActivityLabelSwitch(labelEditPolicy);
	}
	
	/**
	 * Switch to return the label in relation to the kind of model object.
	 * @author Mickael Barbero <a href="mailto:mickael.barbero@obeo.fr">mickael.barbero@obeo.fr</a>
	 */
	class ActivityLabelSwitch extends LabelSwitch {

		/**
		 * Constructor.
		 * @param labelEditPolicy The label edit policy.
		 */
		ActivityLabelSwitch(IMaskManagedLabelEditPolicy labelEditPolicy) {
			super(labelEditPolicy);
		}
		
		/**
		 * {@inheritDoc}
		 *
		 * @see org.eclipse.emf.compare.diagram.papyrus.AbstractUMLViewLabelProvider.LabelSwitch#caseProperty(org.eclipse.uml2.uml.Property)
		 */
		@Override
		public String caseProperty(Property object) {
			if (object.getOwningAssociation() != null) {
				int displayValue = ICustomAppearence.DEFAULT_UML_RELATIONEND_PROPERTY;
				if (getLabelEditPolicy() != null) {
					displayValue = getLabelEditPolicy().getCurrentDisplayValue();
				}
				return PropertyUtil.getCustomLabel(object, displayValue);
			}
			return super.caseProperty(object);
		}
	}
}
