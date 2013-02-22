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
package org.eclipse.emf.compare.ide.ui.internal.handler;

import com.google.common.collect.Iterators;

import java.util.Iterator;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.domain.ICompareEditingDomain;
import org.eclipse.emf.compare.domain.impl.EMFCompareEditingDomain;
import org.eclipse.emf.compare.ide.EMFCompareIDE;
import org.eclipse.emf.compare.ide.ui.internal.editor.ComparisonScopeEditorInput;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.provider.AdapterFactoryUtil;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public abstract class AbstractCompareHandler extends AbstractHandler {

	protected static CompareEditorInput createCompareEditorInput(IWorkbenchPart part,
			AdapterFactory adapterFactory, Notifier left, Notifier right, Notifier origin) {
		CompareEditorInput input = null;

		ICompareEditingDomain editingDomain = createEMFCompareEditingDomain(part, left, right, origin);

		final CompareConfiguration configuration = new CompareConfiguration();
		// never use id in EObjects comparison
		IEObjectMatcher eObjectMatcher = DefaultMatchEngine.createDefaultEObjectMatcher(UseIdentifiers.NEVER);
		IMatchEngine matchEngine = new MatchEObjectEngine(eObjectMatcher, new DefaultComparisonFactory(
				new DefaultEqualityHelperFactory()));
		EMFCompare comparator = EMFCompareIDE.builder().setMatchEngine(matchEngine).build();
		IComparisonScope scope = EMFCompare.createDefaultScope(left, right, origin);
		input = new ComparisonScopeEditorInput(configuration, editingDomain, adapterFactory, comparator,
				scope);

		input.setTitle("Compare ('" + AdapterFactoryUtil.getText(adapterFactory, left) + "' - '"
				+ AdapterFactoryUtil.getText(adapterFactory, right) + "')");

		configuration.setContainer(input);
		return input;
	}

	private static ICompareEditingDomain createEMFCompareEditingDomain(final IWorkbenchPart activePart,
			final Notifier left, final Notifier right, Notifier origin) {

		EditingDomain delegatingEditingDomain = getDelegatingEditingDomain(activePart, left, right);
		CommandStack delegatingCommandStack = null;
		if (delegatingEditingDomain != null) {
			delegatingCommandStack = delegatingEditingDomain.getCommandStack();
		}

		return EMFCompareEditingDomain.create(left, right, origin, delegatingCommandStack);
	}

	private static EditingDomain getDelegatingEditingDomain(final IWorkbenchPart activePart, Notifier left,
			Notifier right) {
		EditingDomain delegatingEditingDomain = null;
		if (activePart instanceof IEditingDomainProvider) {
			delegatingEditingDomain = ((IEditingDomainProvider)activePart).getEditingDomain();
		} else if (AdapterFactoryEditingDomain.getEditingDomainFor(left) != null) {
			EditingDomain leftEditingDomain = AdapterFactoryEditingDomain.getEditingDomainFor(left);
			EditingDomain rightEditingDomain = AdapterFactoryEditingDomain.getEditingDomainFor(right);
			// do not use a delegating editing domain if those two are different.
			if (leftEditingDomain == rightEditingDomain) {
				delegatingEditingDomain = AdapterFactoryEditingDomain.getEditingDomainFor(left);
			}
		}
		return delegatingEditingDomain;
	}

	private static class MatchEObjectEngine extends DefaultMatchEngine {
		/**
		 * @param matcher
		 * @param comparisonFactory
		 */
		public MatchEObjectEngine(IEObjectMatcher matcher, IComparisonFactory comparisonFactory) {
			super(matcher, comparisonFactory);
		}

		@Override
		protected void match(Comparison comparison, IComparisonScope scope, EObject left, EObject right,
				EObject origin, Monitor monitor) {
			if (left == null || right == null) {
				throw new IllegalArgumentException();
			}

			Match rootMatch = CompareFactory.eINSTANCE.createMatch();
			rootMatch.setLeft(left);
			rootMatch.setRight(right);
			rootMatch.setOrigin(origin);
			comparison.getMatches().add(rootMatch);

			final Iterator<? extends EObject> leftEObjects = scope.getChildren(left);
			final Iterator<? extends EObject> rightEObjects = scope.getChildren(right);
			final Iterator<? extends EObject> originEObjects;
			if (origin != null) {
				originEObjects = scope.getChildren(origin);
			} else {
				originEObjects = Iterators.emptyIterator();
			}

			getEObjectMatcher().createMatches(comparison, leftEObjects, rightEObjects, originEObjects);

		}
	}
}
