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
package org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.or;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.ofKind;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.onFeature;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.valueIs;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.command.impl.CopyCommand;
import org.eclipse.emf.compare.diagram.DiagramDiff;
import org.eclipse.emf.compare.diagram.ide.ui.internal.accessor.IDiagramDiffAccessor;
import org.eclipse.emf.compare.diagram.ide.ui.internal.accessor.IDiagramNodeAccessor;
import org.eclipse.emf.compare.domain.ICompareEditingDomain;
import org.eclipse.emf.compare.ide.EMFCompareIDEPlugin;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.tree.TreeContentMergeViewerContentProvider;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.RedoAction;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.UndoAction;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.utils.DiffUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.emf.transaction.TransactionalCommandStack;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gmf.runtime.notation.BasicCompartment;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;

/**
 * Specialized {@link org.eclipse.compare.contentmergeviewer.ContentMergeViewer} that uses
 * {@link org.eclipse.jface.viewers.TreeViewer} to display left, right and ancestor {@link EObject}.
 * 
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
public class DiagramContentMergeViewer extends EMFCompareContentMergeViewer {

	/**
	 * Interface for the management of decorators.
	 * 
	 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
	 */
	private interface IDecoratorManager {

		/**
		 * It hides the revealed decorators.
		 */
		void hideAll();

		/**
		 * From a given difference, it hides the related decorators.
		 * 
		 * @param difference
		 *            The difference.
		 */
		void hideDecorators(Diff difference);

		/**
		 * From a given difference, it reveals the related decorators.
		 * 
		 * @param difference
		 *            The difference.
		 */
		void revealDecorators(Diff difference);
	}

	/**
	 * Decorator manager to create, hide or reveal decorator figures related to deleted or added graphical
	 * objects.
	 * 
	 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
	 */
	private abstract class AbstractDecoratorManager implements IDecoratorManager {

		/**
		 * Decorator represented by a <code>figure</code> on a <code>layer</code>, from the given
		 * <code>side</code> of the merge viewer. An edit part may be linked to the <code>figure</code> in
		 * some cases.<br>
		 * The decorator is related to a <code>difference</code> and it is binded with the reference view and
		 * figure.
		 * 
		 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
		 */
		protected abstract class AbstractDecorator {

			/** The reference <code>View</code> for this decorator. */
			protected View fOriginView;

			/** The reference <code>IFigure</code> for this phantom. */
			protected IFigure fOriginFigure;

			/** The <code>IFigure</code> representing this phantom. */
			protected IFigure fFigure;

			/** The layer on which the <code>figure</code> has to be drawn. */
			protected IFigure fLayer;

			/** The side of the merge viewer on which the <code>figure</code> has to be drawn. */
			protected MergeViewerSide fSide;

			/** The difference related to this phantom. */
			protected Diff fDifference;

			/** The edit part of the figure representing this phantom. May be null. */
			protected EditPart fEditPart;

			/**
			 * Getter.
			 * 
			 * @return the originView {@link Phantom#fOriginView}.
			 */
			public View getOriginView() {
				return fOriginView;
			}

			/**
			 * Setter.
			 * 
			 * @param originView
			 *            {@link Phantom#fOriginView}.
			 */
			public void setOriginView(View originView) {
				this.fOriginView = originView;
			}

			/**
			 * Getter.
			 * 
			 * @return the originFigure {@link Phantom#fOriginFigure}.
			 */
			public IFigure getOriginFigure() {
				return fOriginFigure;
			}

			/**
			 * Setter.
			 * 
			 * @param originFigure
			 *            {@link Phantom#fOriginFigure}.
			 */
			public void setOriginFigure(IFigure originFigure) {
				this.fOriginFigure = originFigure;
			}

			/**
			 * Getter.
			 * 
			 * @return the figure {@link Phantom#fFigure}.
			 */
			public IFigure getFigure() {
				return fFigure;
			}

			/**
			 * Setter.
			 * 
			 * @param figure
			 *            {@link Phantom#fFigure}.
			 */
			public void setFigure(IFigure figure) {
				this.fFigure = figure;
			}

			/**
			 * Getter.
			 * 
			 * @return the layer {@link Phantom#fLayer}.
			 */
			public IFigure getLayer() {
				return fLayer;
			}

			/**
			 * Setter.
			 * 
			 * @param layer
			 *            {@link Phantom#fLayer}.
			 */
			public void setLayer(IFigure layer) {
				this.fLayer = layer;
			}

			/**
			 * Getter.
			 * 
			 * @return the side {@link Phantom#fSide}.
			 */
			public MergeViewerSide getSide() {
				return fSide;
			}

			/**
			 * Setter.
			 * 
			 * @param side
			 *            {@link Phantom#fSide}.
			 */
			public void setSide(MergeViewerSide side) {
				this.fSide = side;
			}

			/**
			 * Getter.
			 * 
			 * @return the difference {@link Phantom#fDifference}.
			 */
			public Diff getDifference() {
				return fDifference;
			}

			/**
			 * Setter.
			 * 
			 * @param difference
			 *            {@link Phantom#fDifference}.
			 */
			public void setDifference(Diff difference) {
				this.fDifference = difference;
			}

			/**
			 * Getter.
			 * 
			 * @return the editPart {@link Phantom#fEditPart}.
			 */
			public EditPart getEditPart() {
				return fEditPart;
			}

			/**
			 * Setter.
			 * 
			 * @param editPart
			 *            {@link Phantom#fEditPart}.
			 */
			public void setEditPart(EditPart editPart) {
				this.fEditPart = editPart;
			}

		}

		/**
		 * From a given difference, it hides the related decorators.
		 * 
		 * @param difference
		 *            The difference.
		 */
		public void hideDecorators(Diff difference) {
			List<? extends AbstractDecorator> oldDecorators = getDecorators(difference);
			if (oldDecorators != null && !oldDecorators.isEmpty() && getComparison() != null) {
				handleDecorators(oldDecorators, false, true);
			}
		}

		/**
		 * From a given difference, it reveals the related decorators.
		 * 
		 * @param difference
		 *            The difference.
		 */
		public void revealDecorators(Diff difference) {

			List<? super AbstractDecorator> decorators = (List<? super AbstractDecorator>)getDecorators(difference);

			// Create phantoms only if they do not already exist and if the related difference is an ADD or
			// DELETE
			if ((decorators == null || decorators.isEmpty()) && isGoodCandidate(difference)) {

				DiagramDiff diagramDiff = (DiagramDiff)difference;

				List<View> referenveViews = getReferenceViews(diagramDiff);

				for (View referenceView : referenveViews) {
					IFigure referenceFigure = getFigure(referenceView);

					MergeViewerSide targetSide = getTargetSide(getComparison().getMatch(referenceView),
							referenceView);

					if (decorators == null) {
						decorators = new ArrayList();
					}

					decorators.add(createAndRegisterDecorator(difference, referenceView, referenceFigure,
							targetSide));
				}

			}

			// The selected difference is an ADD or DELETE and decorators exist for it
			if (decorators != null && !decorators.isEmpty()) {
				revealDecorators((List<? extends AbstractDecorator>)decorators);
			}
		}

		/**
		 * It reveals the given decorators.
		 * 
		 * @param decorators
		 *            The main decorators.
		 */
		protected void revealDecorators(List<? extends AbstractDecorator> decorators) {
			handleDecorators(decorators, true, true);
		}

		/**
		 * Get the figure related to the given view.
		 * 
		 * @param view
		 *            The view.
		 * @return the figure.
		 */
		protected IFigure getFigure(View view) {
			MergeViewerSide side = getSide(view);
			GraphicalEditPart originEditPart = (GraphicalEditPart)getViewer(side).getEditPart(view);
			return originEditPart.getFigure();
		}

		/**
		 * It manages the display of the given decorators.
		 * 
		 * @param decorators
		 *            The decorators to handle.
		 * @param isAdd
		 *            True if it has to be revealed, False otherwise.
		 * @param areMain
		 *            It indicates if the given decorators to handle are considered as the main ones (the ones
		 *            directly linked to the selected difference).
		 */
		protected void handleDecorators(List<? extends AbstractDecorator> decorators, boolean isAdd,
				boolean areMain) {
			for (AbstractDecorator decorator : decorators) {
				handleDecorator(decorator, isAdd, areMain);
			}
		}

		/**
		 * It manages the display of the given decorator.
		 * 
		 * @param decorator
		 *            The decorator to handle.
		 * @param isAdd
		 *            True if it has to be revealed, False otherwise.
		 * @param isMain
		 *            It indicates if the given decorator to handle is considered as the main one (the one
		 *            directly linked to the selected difference).
		 */
		protected void handleDecorator(AbstractDecorator decorator, boolean isAdd, boolean isMain) {
			IFigure layer = decorator.getLayer();
			IFigure figure = decorator.getFigure();
			EditPart editpart = decorator.getEditPart();
			if (editpart == null) {
				if (isAdd && !layer.getChildren().contains(figure)) {
					handleAddDecorator(decorator, layer, figure, isMain);
				} else if (layer.getChildren().contains(figure)) {
					handleDeleteDecorator(decorator, layer, figure, isMain);
				}
			} else {
				if (isAdd && !editpart.isActive()) {
					editpart.activate();
					handleAddDecorator(decorator, layer, figure, isMain);
				} else if (editpart.isActive()) {
					editpart.deactivate();
					handleDeleteDecorator(decorator, layer, figure, isMain);
				}
			}

		}

		/**
		 * It manages the reveal of the given decorator.
		 * 
		 * @param decorator
		 *            The decorator.
		 * @param parent
		 *            The parent figure which has to get the figure to reveal (<code>toAdd</code>)
		 * @param toAdd
		 *            The figure to reveal.
		 * @param isMain
		 *            It indicates if the given decorator to reveal is considered as the main one (the one
		 *            directly linked to the selected difference).
		 */
		protected void handleAddDecorator(AbstractDecorator decorator, IFigure parent, IFigure toAdd,
				boolean isMain) {
			parent.add(toAdd);
		}

		/**
		 * It manages the hiding of the given decorator.
		 * 
		 * @param decorator
		 *            The decorator.
		 * @param parent
		 *            The parent figure which has to get the figure to hide (<code>toDelete</code>)
		 * @param toDelete
		 *            The figure to hide.
		 * @param isMain
		 *            It indicates if the given decorator to hide is considered as the main one (the one
		 *            directly linked to the selected difference).
		 */
		protected void handleDeleteDecorator(AbstractDecorator decorator, IFigure parent, IFigure toDelete,
				boolean isMain) {
			parent.remove(toDelete);
		}

		/**
		 * It checks if the given difference is a good candidate to manage decorators.<br>
		 * 
		 * @see {@link PhantomManager#goodCandidate()}.
		 * @param difference
		 *            The difference.
		 * @return True if it is a good candidate, False otherwise.
		 */
		private boolean isGoodCandidate(Diff difference) {
			return goodCandidate().apply(difference);
		}

		/**
		 * Get the layer on the given side, from the reference view.<br>
		 * 
		 * @see @ link PhantomManager#getIDLayer(View)} .
		 * @param referenceView
		 *            The reference view.
		 * @param side
		 *            The side where the layer has to be found.
		 * @return The layer figure.
		 */
		protected IFigure getLayer(View referenceView, MergeViewerSide side) {
			Diagram referenceDiagram = referenceView.getDiagram();
			Diagram targetDiagram = (Diagram)getMatchView(referenceDiagram, side);
			DiagramMergeViewer targetViewer = getViewer(side);
			IFigure targetLayer = LayerManager.Helper.find(targetViewer.getEditPart(targetDiagram)).getLayer(
					getIDLayer(referenceView));
			return targetLayer;
		}

		/**
		 * Get the layer ID to use from the reference view.<br>
		 * If the reference view is an edge, it is the {@link LayerConstants.CONNECTION_LAYER} which is used,
		 * {@link LayerConstants.SCALABLE_LAYERS} otherwise.
		 * 
		 * @param referenceView
		 *            The reference view.
		 * @return The ID of te layer.
		 */
		protected Object getIDLayer(View referenceView) {
			if (referenceView instanceof Edge) {
				return LayerConstants.CONNECTION_LAYER;
			} else {
				return LayerConstants.SCALABLE_LAYERS;
			}
		}

		/**
		 * It translates the coordinates of the given bounds, from the reference figure and the root of this
		 * one, to absolute coordinates.
		 * 
		 * @param referenceFigure
		 *            The reference figure.
		 * @param rootReferenceFigure
		 *            The root of the reference figure.
		 * @param boundsToTranslate
		 *            The bounds to translate.
		 */
		protected void translateCoordinates(IFigure referenceFigure, IFigure rootReferenceFigure,
				Rectangle boundsToTranslate) {
			IFigure referenceParentFigure = referenceFigure.getParent();
			if (referenceParentFigure != null && referenceFigure != rootReferenceFigure) {
				if (referenceParentFigure.isCoordinateSystem()) {
					boundsToTranslate.x += referenceParentFigure.getBounds().x;
					boundsToTranslate.y += referenceParentFigure.getBounds().y;
				}
				translateCoordinates(referenceParentFigure, rootReferenceFigure, boundsToTranslate);
			}
		}

		/**
		 * Get the predicate to know the differences concerned by the display of decorators.
		 * 
		 * @return The predicate.
		 */
		protected abstract Predicate<Diff> goodCandidate();

		/**
		 * Get the views which have to be used as reference to build the related decorators from the given
		 * difference of the value concerned by the difference.<br>
		 * 
		 * @param difference
		 *            The difference.
		 * @return The list of reference views.
		 */
		protected abstract List<View> getReferenceViews(DiagramDiff difference);

		/**
		 * Get the side where decorators have to be drawn, according to the given reference view and its
		 * match.<br>
		 * 
		 * @param match
		 *            The match of the reference view.
		 * @param referenceView
		 *            The reference view.
		 * @return The side for phantoms.
		 */
		protected abstract MergeViewerSide getTargetSide(Match match, View referenceView);

		/**
		 * It creates new decorators and registers them.
		 * 
		 * @param diff
		 *            The related difference used as index for the main decorator.
		 * @param referenceView
		 *            The reference view as base for creation of the decorator.
		 * @param referenceFigure
		 *            The reference figure as base for creation of the decorator.
		 * @param targetSide
		 *            The side where the decorator has to be created.
		 * @return The list of main decorators.
		 */
		protected abstract AbstractDecorator createAndRegisterDecorator(Diff diff, View referenceView,
				IFigure referenceFigure, MergeViewerSide targetSide);

		/**
		 * Get the main decorators related to the given difference.
		 * 
		 * @param difference
		 *            The difference.
		 * @return The list of main decorators.
		 */
		protected abstract List<? extends AbstractDecorator> getDecorators(Diff difference);

	}

	/**
	 * Phantom manager to create, hide or reveal phantom figures related to deleted or added graphical
	 * objects.
	 * 
	 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
	 */
	private class PhantomManager extends AbstractDecoratorManager {

		/**
		 * Phantom represented by a <code>figure</code> on a <code>layer</code>, from the given
		 * <code>side</code> of the merge viewer. An edit part may be linked to the <code>figure</code> in
		 * some cases.<br>
		 * The phantom is related to a <code>difference</code> and it is binded with the reference view and
		 * figure.
		 * 
		 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
		 */
		private class Phantom extends AbstractDecorator {

			/**
			 * Constructor.
			 * 
			 * @param layer
			 *            {@link Phantom#fLayer}.
			 * @param side
			 *            {@link Phantom#fSide}.
			 * @param originView
			 *            {@link Phantom#fOriginView}.
			 * @param originFigure
			 *            {@link Phantom#fOriginFigure}.
			 * @param diff
			 *            {@link Phantom#fDifference}.
			 */
			public Phantom(IFigure layer, MergeViewerSide side, View originView, IFigure originFigure,
					Diff diff) {
				setLayer(layer);
				setSide(side);
				setOriginView(originView);
				setOriginFigure(originFigure);
				setDifference(diff);
			}

			/**
			 * Get the decorator dependencies of this one. The dependencies are the decorator ancestors plus
			 * the extremities of an edge decorator.
			 * 
			 * @return The list of found decorators.
			 */
			public List<? extends AbstractDecorator> getDependencies() {
				List<AbstractDecorator> result = new ArrayList<AbstractDecorator>();
				result.addAll(getAncestors());
				if (fOriginView instanceof Edge) {
					View source = ((Edge)fOriginView).getSource();
					View target = ((Edge)fOriginView).getTarget();
					result.addAll(getOrCreateRelatedDecorators(source));
					result.addAll(getOrCreateRelatedDecorators(target));
				}
				return result;
			}

			/**
			 * From the given view, get or create the related phantoms.
			 * 
			 * @param referenceView
			 *            The given view.
			 * @return The list of phantoms.
			 */
			private List<Phantom> getOrCreateRelatedDecorators(EObject referenceView) {
				List<Phantom> result = new ArrayList<Phantom>();
				Collection<Diff> changes = Collections2.filter(getComparison().getDifferences(referenceView),
						goodCandidate());
				for (Diff change : changes) {
					Phantom phantom = fPhantomRegistry.get(change);
					if (phantom == null) {
						phantom = createAndRegisterDecorator(change, (View)referenceView, PhantomManager.this
								.getFigure((View)referenceView), fSide);
					}
					result.add(phantom);
				}
				return result;
			}

			/**
			 * Get the ancestor decorators of this one.
			 * 
			 * @return The list of the ancestors.
			 */
			private List<? extends AbstractDecorator> getAncestors() {
				List<AbstractDecorator> result = new ArrayList<AbstractDecorator>();
				EObject parentOriginView = fOriginView.eContainer();
				while (parentOriginView != null) {
					result.addAll(getOrCreateRelatedDecorators(parentOriginView));
					parentOriginView = parentOriginView.eContainer();
				}
				return result;
			}
		}

		/** Registry of created phantoms, indexed by difference. */
		private final Map<Diff, Phantom> fPhantomRegistry = new HashMap<Diff, Phantom>();

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#goodCandidate()<br>
		 *      Only the diagram differences ADD or DELETE are concerned by this display.
		 */
		@Override
		protected Predicate<Diff> goodCandidate() {
			return new Predicate<Diff>() {
				public boolean apply(Diff difference) {
					return and(instanceOf(DiagramDiff.class),
							or(ofKind(DifferenceKind.ADD), ofKind(DifferenceKind.DELETE))).apply(difference);
				}
			};
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#getReferenceViews(org.eclipse.emf.compare.diagram.DiagramDiff)
		 */
		@Override
		protected List<View> getReferenceViews(DiagramDiff difference) {
			List<View> result = new ArrayList<View>();

			Match match = getComparison().getMatch(difference.getView());

			EObject originObj = match.getOrigin();
			EObject leftObj = match.getLeft();
			EObject rightObj = match.getRight();

			if (leftObj instanceof View || rightObj instanceof View) {
				result.add(getReferenceView((View)originObj, (View)leftObj, (View)rightObj));
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#getTargetSide(org.eclipse.emf.compare.Match,
		 *      org.eclipse.gmf.runtime.notation.View)<br>
		 *      If the left object is null, a phantom should be drawn instead. Else, it means that the right
		 *      object is null and a phantom should be displayed on the right side.
		 */
		@Override
		protected MergeViewerSide getTargetSide(Match match, View referenceView) {
			MergeViewerSide targetSide = null;
			if (match.getLeft() == null) {
				targetSide = MergeViewerSide.LEFT;
			} else {
				targetSide = MergeViewerSide.RIGHT;
			}
			return targetSide;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#createAndRegisterDecorator(org.eclipse.emf.compare.Diff,
		 *      org.eclipse.gmf.runtime.notation.View, org.eclipse.draw2d.IFigure,
		 *      org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide)
		 */
		@Override
		protected Phantom createAndRegisterDecorator(Diff diff, View referenceView, IFigure referenceFigure,
				MergeViewerSide targetSide) {
			Phantom phantom = createPhantom(diff, referenceView, referenceFigure, targetSide);
			fPhantomRegistry.put(diff, phantom);
			return phantom;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#getDecorators(org.eclipse.emf.compare.Diff)
		 */
		@Override
		protected List<Phantom> getDecorators(Diff difference) {
			List<Phantom> result = new ArrayList<DiagramContentMergeViewer.PhantomManager.Phantom>();
			Phantom phantom = fPhantomRegistry.get(difference);
			if (phantom != null) {
				result.add(phantom);
			}
			return result;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#handleDecorator(org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager.AbstractDecorator,
		 *      boolean)
		 */
		@Override
		protected void handleDecorator(AbstractDecorator decorator, boolean isAdd, boolean isMain) {
			super.handleDecorator(decorator, isAdd, isMain);
			// Display the dependencies (context) of this decorator
			for (AbstractDecorator ancestor : ((Phantom)decorator).getDependencies()) {
				super.handleDecorator(ancestor, isAdd, false);
			}
		}

		@Override
		protected void handleAddDecorator(AbstractDecorator decorator, IFigure parent, IFigure toAdd,
				boolean isMain) {
			super.handleAddDecorator(decorator, parent, toAdd, isMain);
			// Set the highlight of the figure
			Color strokeColor = null;
			if (isMain) {
				((Shape)toAdd).setLineWidth(((Shape)toAdd).getLineWidth() + 1);
				strokeColor = getCompareColor().getStrokeColor(decorator.getDifference(), isThreeWay(),
						false, true);
				// figure.setBorder(new FocusBorder());

				// FIXME: Find a way to set the focus on this figure.
				getViewer(decorator.getSide()).getGraphicalViewer().reveal(toAdd);
			} else {
				strokeColor = getCompareColor().getStrokeColor(decorator.getDifference(), isThreeWay(),
						false, false);

			}
			toAdd.setForegroundColor(strokeColor);
		}

		@Override
		protected void handleDeleteDecorator(AbstractDecorator decorator, IFigure parent, IFigure toDelete,
				boolean isMain) {
			super.handleDeleteDecorator(decorator, parent, toDelete, isMain);
			// Re-initialize the highlight of the figure
			if (isMain) {
				((Shape)toDelete).setLineWidth(((Shape)toDelete).getLineWidth() - 1);
				Color strokeColor = getCompareColor().getStrokeColor(decorator.getDifference(), isThreeWay(),
						false, false);
				toDelete.setForegroundColor(strokeColor);
				// figure.setBorder(null);
			}
		}

		/**
		 * Get the view which has to be used as reference to build a phantom.<br>
		 * The reference is the non null object among the given objects. In case of delete object, in the
		 * context of three-way comparison, the reference will be the ancestor one (<code>originObj</code>).
		 * 
		 * @param originObj
		 *            The ancestor object.
		 * @param leftView
		 *            The left object.
		 * @param rightView
		 *            The right object.
		 * @return The reference object.
		 */
		private View getReferenceView(View originObj, View leftView, View rightView) {
			View referenceView;
			if (originObj != null) {
				referenceView = originObj;
			} else if (leftView != null) {
				referenceView = leftView;
			} else {
				referenceView = rightView;
			}
			return referenceView;
		}

		/**
		 * It creates a new phantom from the given difference, view and figure.
		 * 
		 * @param diff
		 *            The related difference used as index for the main phantom.
		 * @param referenceView
		 *            The reference view as base for creation of the phantom.
		 * @param referenceFigure
		 *            The reference figure as base for creation of the phantom.
		 * @param side
		 *            The side where the phantom has to be created.
		 * @return The phantom.
		 */
		private Phantom createPhantom(Diff diff, View referenceView, IFigure referenceFigure,
				MergeViewerSide side) {

			MergeViewerSide referenceSide = getSide(referenceView);

			Rectangle rect = referenceFigure.getBounds().getCopy();

			IFigure referenceLayer = getLayer(referenceView, referenceSide);
			translateCoordinates(referenceFigure, referenceLayer, rect);

			IFigure ghost = null;

			IFigure targetLayer = getLayer(referenceView, side);
			Phantom phantom = new Phantom(targetLayer, side, referenceView, referenceFigure, diff);

			// Container "list" case
			if (referenceView.eContainer() instanceof BasicCompartment) {
				ghost = new Polyline();

				Diff refiningDiff = Iterators.find(diff.getRefinedBy().iterator(), and(
						valueIs(referenceView), onFeature(NotationPackage.Literals.VIEW__PERSISTED_CHILDREN
								.getName())));

				// FIXME:
				// - It has to manage visible views.
				// - What about transient children ?
				int index = DiffUtil.findInsertionIndex(getComparison(), refiningDiff,
						side == MergeViewerSide.LEFT);

				IFigure referenceParentFigure = referenceFigure.getParent();
				Rectangle referenceParentBounds = referenceParentFigure.getBounds().getCopy();
				translateCoordinates(referenceParentFigure, referenceLayer, referenceParentBounds);

				View parentView = (View)getMatchView(referenceView.eContainer(), side);
				if (parentView != null) {
					int nbElements = getVisibleViews(parentView).size();
					if (index > nbElements) {
						index = nbElements;
					}
				}

				// FIXME: The add of decorators modifies the physical coordinates of elements
				// FIXME: Compute position from the y position of the first child + sum of height of the
				// children.
				int pos = rect.height * index + referenceParentBounds.y + 1;

				((Polyline)ghost).setEndpoints(new Point(rect.x, pos), new Point(rect.x + rect.width, pos));

				// Edge case
			} else if (referenceView instanceof Edge) {
				// If the edge phantom ties shapes where their coordinates changed
				if (hasAnExtremityChange((Edge)referenceView, side)) {
					EditPart edgeEditPart = createEdgeEditPart((Edge)referenceView, referenceSide, side);
					if (edgeEditPart instanceof GraphicalEditPart) {
						phantom.setEditPart(edgeEditPart);
						ghost = ((GraphicalEditPart)edgeEditPart).getFigure();
						ghost.getChildren().clear();
					}
					// Else, it creates only a polyline connection figure with the same properties as the
					// reference
				} else {
					if (referenceFigure instanceof PolylineConnection) {
						ghost = new PolylineConnection();
						ghost.setBounds(rect);
						((PolylineConnection)ghost).setPoints(((PolylineConnection)referenceFigure)
								.getPoints().getCopy());
					}
				}
			}

			// Default case: Nodes
			if (ghost == null) {
				ghost = new RectangleFigure();
				ghost.setBounds(rect);
			}

			if (ghost instanceof Shape) {
				((Shape)ghost).setFill(false);
			}

			phantom.setFigure(ghost);

			translateWhenInsideContainerChange(phantom);

			return phantom;
		}

		/**
		 * Get the visible view under the given parent view.
		 * 
		 * @param parent
		 *            The parent view.
		 * @return The list of views.
		 */
		private List<View> getVisibleViews(View parent) {
			return (List<View>)Lists.newArrayList(Iterators.filter(parent.getChildren().iterator(),
					new Predicate<Object>() {
						public boolean apply(Object input) {
							return input instanceof View && ((View)input).isVisible();
						}
					}));
		}

		/**
		 * It translates and resizes the figure of the given phantom when this one is nested in a container
		 * which is subjected to a coordinates change.
		 * 
		 * @param phantom
		 *            The phantom.
		 */
		private void translateWhenInsideContainerChange(Phantom phantom) {
			Collection<Diff> changes = Collections2.filter(phantom.getDifference().getMatch()
					.getDifferences(), new Predicate<Diff>() {

				public boolean apply(Diff difference) {
					// FIXME: it will be changed to CHANGE (change coordinates (or dimension))
					return difference.getKind() == DifferenceKind.MOVE;
				}

			});
			if (changes.size() > 0) {
				View referenceView = phantom.getOriginView();
				View parentReferenceView = (View)referenceView.eContainer();
				if (parentReferenceView != null) {
					View parentView = (View)getMatchView(parentReferenceView, phantom.getSide());
					IFigure parentFigure = getFigure(parentView);
					Rectangle parentRect = parentFigure.getBounds().getCopy();
					translateCoordinates(parentFigure, getLayer(parentReferenceView, getSide(parentView)),
							parentRect);

					IFigure parentReferenceFigure = getFigure(parentReferenceView);
					Rectangle parentReferenceRect = parentReferenceFigure.getBounds().getCopy();
					translateCoordinates(parentReferenceFigure, getLayer(parentReferenceView,
							getSide(parentReferenceView)), parentReferenceRect);

					int deltaX = parentRect.x - parentReferenceRect.x;
					int deltaY = parentRect.y - parentReferenceRect.y;
					int deltaWidth = parentRect.width - parentReferenceRect.width;
					int deltaHeight = parentRect.height - parentReferenceRect.height;

					IFigure figure = phantom.getFigure();

					Rectangle rect = figure.getBounds().getCopy();
					rect.x += deltaX;
					rect.y += deltaY;
					rect.width += deltaWidth;
					if (!(figure instanceof Polyline)) {
						rect.height += deltaHeight;
					}
					figure.setBounds(rect);

					if (figure instanceof Polyline) {

						Point firstPoint = ((Polyline)figure).getPoints().getFirstPoint().getCopy();
						Point lastPoint = ((Polyline)figure).getPoints().getLastPoint().getCopy();

						firstPoint.x += deltaX;
						firstPoint.y += deltaY;

						lastPoint.x += deltaX + deltaWidth;
						lastPoint.y += deltaY;

						((Polyline)figure).setEndpoints(firstPoint, lastPoint);

					}

				}
			}

		}

		/**
		 * It checks that the given edge is linked to graphical objects subjected to coordinate changes, on
		 * the given side.
		 * 
		 * @param edge
		 *            The edge to check.
		 * @param targetSide
		 *            The side to check extremities (side of the phantom).
		 * @return True if an extremity at least changed its location, False otherwise.
		 */
		private boolean hasAnExtremityChange(Edge edge, MergeViewerSide targetSide) {
			View referenceSource = edge.getSource();
			View referenceTarget = edge.getTarget();
			return hasChange(referenceSource, targetSide) || hasChange(referenceTarget, targetSide);
		}

		/**
		 * It checks that the coordinates of the given view changed between left and right, from the given
		 * side.
		 * 
		 * @param referenceView
		 *            The view to check.
		 * @param targetSide
		 *            The side to focus.
		 * @return True if the view changed its location, False otherwise.
		 */
		private boolean hasChange(View referenceView, MergeViewerSide targetSide) {
			DifferenceKind lookup = DifferenceKind.MOVE; // FIXME: it will be change to CHANGE (change
															// coordinates)
			View extremity = (View)getMatchView(referenceView, targetSide);
			// Look for a related change coordinates on the extremity of the edge reference.
			Collection<Diff> diffs = Collections2.filter(getComparison().getDifferences(referenceView), and(
					instanceOf(DiagramDiff.class), ofKind(lookup)));
			if (diffs.isEmpty()) {
				// Look for a related change coordinates on the matching extremity (other side) of the edge
				// reference.
				diffs = Collections2.filter(getComparison().getDifferences(extremity), and(
						instanceOf(DiagramDiff.class), ofKind(lookup)));
			}
			return !diffs.isEmpty();
		}

		/**
		 * It creates and returns a new edit part from the given edge. This edit part listens the reference
		 * edge but is attached to the controllers of the target (phantom) side.
		 * 
		 * @param referenceEdge
		 *            The edge as base of the edit part.
		 * @param referenceSide
		 *            The side of this edge.
		 * @param targetSide
		 *            The side where the edit part has to be created to draw the related phantom.
		 * @return The new edit part.
		 */
		private EditPart createEdgeEditPart(Edge referenceEdge, MergeViewerSide referenceSide,
				MergeViewerSide targetSide) {
			EditPart edgeEditPartReference = getViewer(referenceSide).getEditPart(referenceEdge);
			EditPart edgeEditPart = null;
			if (edgeEditPartReference instanceof ConnectionEditPart) {
				edgeEditPart = createEditPartForPhantoms(referenceEdge, referenceSide, targetSide);
				if (edgeEditPart instanceof ConnectionEditPart) {
					View source = (View)((ConnectionEditPart)edgeEditPartReference).getSource().getModel();
					if (source == null) {
						source = referenceEdge.getSource();
					}
					View target = (View)((ConnectionEditPart)edgeEditPartReference).getTarget().getModel();
					if (target == null) {
						target = referenceEdge.getTarget();
					}
					EditPart sourceEp = createEditPartForPhantoms(source, referenceSide, targetSide);
					((AbstractGraphicalEditPart)sourceEp).activate();
					((AbstractGraphicalEditPart)sourceEp).getFigure();
					((ConnectionEditPart)edgeEditPart).setSource(sourceEp);
					EditPart targetEp = createEditPartForPhantoms(target, referenceSide, targetSide);
					((AbstractGraphicalEditPart)targetEp).activate();
					((AbstractGraphicalEditPart)targetEp).getFigure();
					((ConnectionEditPart)edgeEditPart).setTarget(targetEp);
				}
			}
			return edgeEditPart;
		}

		/**
		 * It creates and returns a new edit part from the given view. This edit part listens the reference
		 * view but is attached to the controllers of the target (phantom) side.
		 * 
		 * @param referenceView
		 *            The view as base of the edit part.
		 * @param referenceSide
		 *            The side of this view.
		 * @param targetSide
		 *            The side where the edit part has to be created to draw the related phantom.
		 * @return The new edit part.
		 */
		private EditPart createEditPartForPhantoms(EObject referenceView, MergeViewerSide referenceSide,
				MergeViewerSide targetSide) {
			EditPart editPartParent = null;
			EditPart editPart = null;
			EditPart editPartReference = getViewer(referenceSide).getEditPart(referenceView);
			EditPart editPartReferenceParent = editPartReference.getParent();
			Object referenceViewParent = editPartReferenceParent.getModel();
			if (!(referenceViewParent instanceof EObject)) {
				referenceViewParent = referenceView.eContainer();
			}
			View viewParent = (View)getMatchView((EObject)referenceViewParent, targetSide);
			if (viewParent != null) {
				editPartParent = getViewer(targetSide).getEditPart(viewParent);
			}
			if (editPartParent == null) {
				editPartParent = createEditPartForPhantoms((EObject)referenceViewParent, referenceSide,
						targetSide);

			}
			if (editPartParent != null) {
				View view = (View)getMatchView(referenceView, targetSide);
				if (view != null) {
					editPart = getViewer(targetSide).getEditPart(view);
				}
				if (editPart == null) {
					editPart = getViewer(targetSide).getGraphicalViewer().getEditPartFactory()
							.createEditPart(editPartParent, referenceView);
					editPart.setParent(editPartParent);
					getViewer(targetSide).getGraphicalViewer().getEditPartRegistry().put(referenceView,
							editPart);
				}

			}
			return editPart;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.IDecoratorManager#hideAll()
		 */
		public void hideAll() {
			Iterator<Phantom> visiblePhantoms = Iterators.filter(fPhantomRegistry.values().iterator(),
					new Predicate<Phantom>() {
						public boolean apply(Phantom phantom) {
							return phantom.getFigure().getParent() != null;
						}
					});
			while (visiblePhantoms.hasNext()) {
				Phantom phantom = (Phantom)visiblePhantoms.next();
				handleDecorator(phantom, false, true);
			}
		}
	}

	/**
	 * Marker manager to create, hide or reveal marker figures related to deleted or added graphical objects.
	 * 
	 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
	 */
	private class MarkerManager extends AbstractDecoratorManager {

		/**
		 * Marker represented by a <code>figure</code> on a <code>layer</code>, from the given
		 * <code>side</code> of the merge viewer. An edit part may be linked to the <code>figure</code> in
		 * some cases.<br>
		 * The marker is related to a <code>difference</code> and it is binded with the reference view and
		 * figure.
		 * 
		 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
		 */
		private class Marker extends AbstractDecorator {

			/** Thickness of the marker. */
			public static final int THICKNESS = 6;

			/** The alpha number for the figure. */
			public static final int ALPHA = 30;

			/**
			 * Constructor.
			 * 
			 * @param layer
			 *            {@link Marker#fLayer}.
			 * @param side
			 *            {@link Marker#fSide}.
			 * @param originView
			 *            {@link Marker#fOriginView}.
			 * @param originFigure
			 *            {@link Marker#fOriginFigure}.
			 * @param diff
			 *            {@link Marker#fDifference}.
			 */
			public Marker(IFigure layer, MergeViewerSide side, View originView, IFigure originFigure,
					Diff diff) {
				setLayer(layer);
				setSide(side);
				setOriginView(originView);
				setOriginFigure(originFigure);
				setDifference(diff);
			}
		}

		/** Registry of created markers, indexed by difference. */
		private Map<Diff, List<Marker>> fMarkerRegistry = new HashMap<Diff, List<Marker>>();

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#getReferenceViews(org.eclipse.emf.compare.diagram.DiagramDiff)
		 */
		@Override
		protected List<View> getReferenceViews(DiagramDiff difference) {
			List<View> result = new ArrayList<View>();
			Match matchValue = getComparison().getMatch(difference.getView());
			if (matchValue.getLeft() != null) {
				result.add((View)matchValue.getLeft());
			}
			if (matchValue.getRight() != null) {
				result.add((View)matchValue.getRight());
			}
			if (getComparison().isThreeWay()) {
				switch (difference.getKind()) {
					case DELETE:
					case CHANGE:
					case MOVE:
						result.add((View)matchValue.getOrigin());
						break;
					default:
						break;
				}
			}
			return result;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#getTargetSide(org.eclipse.emf.compare.Match,
		 *      org.eclipse.gmf.runtime.notation.View)
		 */
		@Override
		protected MergeViewerSide getTargetSide(Match match, View referenceView) {
			return getSide(referenceView);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#createAndRegisterDecorator(org.eclipse.emf.compare.Diff,
		 *      org.eclipse.gmf.runtime.notation.View, org.eclipse.draw2d.IFigure,
		 *      org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide)
		 */
		@Override
		protected Marker createAndRegisterDecorator(Diff diff, View referenceView, IFigure referenceFigure,
				MergeViewerSide targetSide) {
			Marker marker = createMarker(diff, referenceView, referenceFigure, targetSide);
			List<Marker> markers = fMarkerRegistry.get(diff);
			if (markers == null) {
				markers = new ArrayList<Marker>();
				fMarkerRegistry.put(diff, markers);
			}
			markers.add(marker);
			return marker;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#getDecorators(org.eclipse.emf.compare.Diff)
		 */
		@Override
		protected List<Marker> getDecorators(Diff difference) {
			return fMarkerRegistry.get(difference);
		}

		@Override
		protected void handleAddDecorator(AbstractDecorator decorator, IFigure parent, IFigure toAdd,
				boolean isMain) {
			super.handleAddDecorator(decorator, parent, toAdd, isMain);
			DiagramMergeViewer viewer = getViewer(decorator.getSide());
			viewer.getGraphicalViewer().reveal(viewer.getEditPart(decorator.getOriginView()));
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.AbstractDecoratorManager#goodCandidate()<br>
		 *      All graphical differences are concerned.
		 */
		@Override
		protected Predicate<Diff> goodCandidate() {
			return new Predicate<Diff>() {
				public boolean apply(Diff difference) {
					return instanceOf(DiagramDiff.class).apply(difference);
				}
			};
		}

		/**
		 * It creates a new marker from the given difference, view and figure.
		 * 
		 * @param diff
		 *            The related difference used as index for the main marker.
		 * @param referenceView
		 *            The reference view as base for creation of the marker.
		 * @param referenceFigure
		 *            The reference figure as base for creation of the marker.
		 * @param side
		 *            The side where the marker has to be created.
		 * @return The phantom.
		 */
		private Marker createMarker(Diff diff, View referenceView, IFigure referenceFigure,
				MergeViewerSide side) {

			Rectangle referenceBounds = referenceFigure.getBounds().getCopy();
			IFigure referenceLayer = getLayer(referenceView, side);
			translateCoordinates(referenceFigure, referenceLayer, referenceBounds);

			IFigure markerFigure = null;

			IFigure targetLayer = getLayer(referenceView, side);
			Marker marker = new Marker(targetLayer, side, referenceView, referenceFigure, diff);

			if (referenceView.eContainer() instanceof BasicCompartment) {

				markerFigure = new RectangleFigure();
				markerFigure.setBounds(referenceBounds);

			} else if (referenceView instanceof Edge) {

				if (referenceFigure instanceof PolylineConnection) {

					markerFigure = new PolylineConnection();

					markerFigure.setBounds(referenceBounds);

					((PolylineConnection)markerFigure).setPoints(((PolylineConnection)referenceFigure)
							.getPoints().getCopy());

					int oldWidth = ((Shape)referenceFigure).getLineWidth();
					int newWidth = oldWidth + Marker.THICKNESS * 2;
					((PolylineConnection)markerFigure).setLineWidth(newWidth);
				}

			}

			// Default case: Nodes
			if (markerFigure == null) {

				markerFigure = new RectangleFigure();

				referenceBounds.x -= Marker.THICKNESS;
				referenceBounds.y -= Marker.THICKNESS;
				referenceBounds.width += Marker.THICKNESS * 2;
				referenceBounds.height += Marker.THICKNESS * 2;

				markerFigure.setBounds(referenceBounds);
			}

			Color strokeColor = getCompareColor().getStrokeColor(diff, isThreeWay(), false, true);
			markerFigure.setForegroundColor(strokeColor);
			markerFigure.setBackgroundColor(strokeColor);
			// markerFigure.setBorder(new FocusBorder());
			((Shape)markerFigure).setAlpha(Marker.ALPHA);

			marker.setFigure(markerFigure);

			return marker;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.IDecoratorManager#hideAll()
		 */
		public void hideAll() {
			Iterator<Marker> visibleMarkers = Iterators.filter(Iterables.concat(fMarkerRegistry.values())
					.iterator(), new Predicate<Marker>() {
				public boolean apply(Marker marker) {
					return marker.getFigure().getParent() != null;
				}
			});
			while (visibleMarkers.hasNext()) {
				Marker marker = (Marker)visibleMarkers.next();
				handleDecorator(marker, false, true);
			}
		}

	}

	/**
	 * Decorator manager to create, hide or reveal all decorator figures related to graphical changes.
	 * 
	 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
	 */
	private class DecoratorsManager implements IDecoratorManager {
		/** Phantoms manager. */
		private IDecoratorManager fPhantomManager = new PhantomManager();

		/** Markers manager. */
		private IDecoratorManager fMarkerManager = new MarkerManager();

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.IDecoratorManager#hideDecorators(org.eclipse.emf.compare.Diff)
		 */
		public void hideDecorators(Diff difference) {
			fMarkerManager.hideDecorators(difference);
			fPhantomManager.hideDecorators(difference);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.IDecoratorManager#revealDecorators(org.eclipse.emf.compare.Diff)
		 */
		public void revealDecorators(Diff difference) {
			fMarkerManager.revealDecorators(difference);
			fPhantomManager.revealDecorators(difference);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram.DiagramContentMergeViewer.IDecoratorManager#hideAll()
		 */
		public void hideAll() {
			fMarkerManager.hideAll();
			fPhantomManager.hideAll();
		}

	}

	/**
	 * Bundle name of the property file containing all displayed strings.
	 */
	private static final String BUNDLE_NAME = DiagramContentMergeViewer.class.getName();

	/** The editing domain. */
	private ICompareEditingDomain fEditingDomain;

	/** Listener to manage the update of the decorators on events about the command stack. */
	private CommandStackListener fDecoratorsCommandStackListener;

	/** The phantom manager to use in the context of this viewer. */
	private final DecoratorsManager fDecoratorsManager = new DecoratorsManager();

	/** The current "opened" difference. */
	private Diff fCurrentSelectedDiff;

	/**
	 * The adapter factory used to create the content and label provider for ancestor, left and right
	 * {@link DiagramMergeViewer}.
	 */
	private final ComposedAdapterFactory fAdapterFactory;

	/**
	 * Creates a new {@link DiagramContentMergeViewer} by calling the super constructor with the given
	 * parameters.
	 * <p>
	 * It calls {@link #buildControl(Composite)} as stated in its javadoc.
	 * <p>
	 * {@link #setContentProvider(org.eclipse.jface.viewers.IContentProvider) content provider} to properly
	 * display ancestor, left and right parts.
	 * 
	 * @param parent
	 *            the parent composite to build the UI in
	 * @param config
	 *            the {@link CompareConfiguration}
	 */
	public DiagramContentMergeViewer(Composite parent, CompareConfiguration config) {
		super(SWT.NONE, ResourceBundle.getBundle(BUNDLE_NAME), config);

		fAdapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		fAdapterFactory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());
		fAdapterFactory.addAdapterFactory(new ResourceItemProviderAdapterFactory());

		buildControl(parent);
		setContentProvider(new TreeContentMergeViewerContentProvider(config, getComparison()));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		fEditingDomain.getCommandStack().removeCommandStackListener(fDecoratorsCommandStackListener);
		fAdapterFactory.dispose();
		super.handleDispose(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#getAncestorMergeViewer()
	 */
	@SuppressWarnings("unchecked")
	// see createMergeViewer() to see it is safe
	@Override
	public DiagramMergeViewer getAncestorMergeViewer() {
		return (DiagramMergeViewer)super.getAncestorMergeViewer();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#getLeftMergeViewer()
	 */
	@SuppressWarnings("unchecked")
	// see createMergeViewer() to see it is safe
	@Override
	public DiagramMergeViewer getLeftMergeViewer() {
		return (DiagramMergeViewer)super.getLeftMergeViewer();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#getRightMergeViewer()
	 */
	@SuppressWarnings("unchecked")
	// see createMergeViewer() to see it is safe
	@Override
	public DiagramMergeViewer getRightMergeViewer() {
		return (DiagramMergeViewer)super.getRightMergeViewer();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#copyDiff(boolean)
	 */
	@Override
	protected void copyDiff(boolean leftToRight) {
		/*
		 * FIXME change this! For the moment we always do a new setInput() on the content viewer whenever we
		 * select a Diagram Difference. This is meant to change so that we use selection synchronization
		 * instead. This code will break whenever we implement that change.
		 */
		if (fCurrentSelectedDiff != null) {
			final Command command = getEditingDomain().createCopyCommand(fCurrentSelectedDiff, leftToRight,
					EMFCompareIDEPlugin.getDefault().getMergerRegistry());
			getEditingDomain().getCommandStack().execute(command);

			if (leftToRight) {
				setRightDirty(true);
			} else {
				setLeftDirty(true);
			}
			// refresh();
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#getContents(boolean)
	 */
	@Override
	protected byte[] getContents(boolean left) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.DiagramCompareContentMergeViewer#createMergeViewer(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide)
	 */
	@Override
	protected IMergeViewer createMergeViewer(Composite parent, MergeViewerSide side) {
		final DiagramMergeViewer diagramMergeViewer = new DiagramMergeViewer(parent, side);
		return diagramMergeViewer;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.DiagramCompareContentMergeViewer#paintCenter(org.eclipse.swt.graphics.GC)
	 */
	@Override
	protected void paintCenter(GC g) {

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.DiagramCompareContentMergeViewer#updateContent(java.lang.Object,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void updateContent(Object ancestor, Object left, Object right) {
		initStackListenerAndUpdateContent(ancestor, left, right);

		getLeftMergeViewer().getGraphicalViewer().flush();
		getRightMergeViewer().getGraphicalViewer().flush();
		getAncestorMergeViewer().getGraphicalViewer().flush();

		if (left instanceof IDiagramDiffAccessor) {
			IDiagramDiffAccessor input = (IDiagramDiffAccessor)left;

			// initialization: reset the current difference selection hiding potential visible phantoms
			if (fCurrentSelectedDiff != null && fCurrentSelectedDiff.getState() != DifferenceState.MERGED) {
				fDecoratorsManager.hideDecorators(fCurrentSelectedDiff);
			}

			Diff diff = input.getDiff(); // equivalent to getInput().getTarget()
			fCurrentSelectedDiff = diff;

			if (diff.getState() != DifferenceState.MERGED) {
				fDecoratorsManager.revealDecorators(diff);
			}

			// FIXME use the decorator manager to refresh decorators after a merge and using undo/redo
			// actions.
		} else if (left instanceof IDiagramNodeAccessor) {
			if (fCurrentSelectedDiff != null && fCurrentSelectedDiff.getState() != DifferenceState.MERGED) {
				fDecoratorsManager.hideDecorators(fCurrentSelectedDiff);
			}
			fCurrentSelectedDiff = null;
		}

		updateToolItems();

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#installCommandStackListener(org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.UndoAction,
	 *      org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.RedoAction)
	 */
	@Override
	protected CommandStackListener installCommandStackListener(UndoAction undoAction, RedoAction redoAction) {
		CommandStackListener cmdStackListener = super.installCommandStackListener(undoAction, redoAction);
		fEditingDomain = getEditingDomain();
		fDecoratorsCommandStackListener = new CommandStackListener() {

			public void commandStackChanged(EventObject event) {
				Object source = event.getSource();
				if (source instanceof TransactionalCommandStack) {
					Command command = ((TransactionalCommandStack)source).getMostRecentCommand();
					if (command instanceof CopyCommand) {
						Iterator<DiagramDiff> diffs = Iterators.filter(command.getAffectedObjects()
								.iterator(), DiagramDiff.class);
						while (diffs.hasNext()) {
							DiagramDiff diagramDiff = diffs.next();
							if (diagramDiff.getState() != DifferenceState.UNRESOLVED) {
								fDecoratorsManager.hideDecorators(diagramDiff);
							}
						}
					}
				}
			}
		};
		fEditingDomain.getCommandStack().addCommandStackListener(fDecoratorsCommandStackListener);
		return cmdStackListener;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// No selection synchronization (content to structure merge viewer).
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#getDiffFrom(org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer)
	 */
	@Override
	protected Diff getDiffFrom(IMergeViewer viewer) {
		return fCurrentSelectedDiff;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#createControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createControls(Composite composite) {
		super.createControls(composite);
		getAncestorMergeViewer().removeSelectionChangedListener(this);
		getLeftMergeViewer().removeSelectionChangedListener(this);
		getRightMergeViewer().removeSelectionChangedListener(this);
	}

	/**
	 * Utility method to retrieve the {@link DiagramMergeViewer} from the given side.
	 * 
	 * @param side
	 *            The side to focus.
	 * @return The viewer.
	 */
	private DiagramMergeViewer getViewer(MergeViewerSide side) {
		DiagramMergeViewer result = null;
		switch (side) {
			case LEFT:
				result = getLeftMergeViewer();
				break;
			case RIGHT:
				result = getRightMergeViewer();
				break;
			case ANCESTOR:
				result = getAncestorMergeViewer();
				break;
			default:
		}
		return result;
	}

	/**
	 * Utility method to know the side where is located the given view.
	 * 
	 * @param view
	 *            The view.
	 * @return The side of the view.
	 */
	private MergeViewerSide getSide(View view) {
		MergeViewerSide result = null;
		Match match = getComparison().getMatch(view);
		if (match.getLeft() == view) {
			result = MergeViewerSide.LEFT;
		} else if (match.getRight() == view) {
			result = MergeViewerSide.RIGHT;
		} else if (match.getOrigin() == view) {
			result = MergeViewerSide.ANCESTOR;
		}
		return result;
	}

	/**
	 * Utility method to get the object matching with the given one, to the given side.
	 * 
	 * @param object
	 *            The object as base of the lookup.
	 * @param side
	 *            The side where the potential matching object has to be retrieved.
	 * @return The matching object.
	 */
	private EObject getMatchView(EObject object, MergeViewerSide side) {
		Match match = getComparison().getMatch(object);
		return getMatchView(match, side);
	}

	/**
	 * Utility method to get the object in the given side from the given match.
	 * 
	 * @param match
	 *            The match.
	 * @param side
	 *            The side where the potential matching object has to be retrieved.
	 * @return The matching object.
	 */
	private EObject getMatchView(Match match, MergeViewerSide side) {
		EObject result = null;
		switch (side) {
			case LEFT:
				result = match.getLeft();
				break;
			case RIGHT:
				result = match.getRight();
				break;
			case ANCESTOR:
				result = match.getOrigin();
				break;
			default:
		}
		return result;
	}

}
