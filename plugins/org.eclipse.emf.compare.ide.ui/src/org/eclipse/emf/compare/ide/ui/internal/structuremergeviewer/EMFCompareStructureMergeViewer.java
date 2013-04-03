/*******************************************************************************
 * Copyright (c) 2012, 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.command.ICompareCopyCommand;
import org.eclipse.emf.compare.domain.ICompareEditingDomain;
import org.eclipse.emf.compare.domain.impl.EMFCompareEditingDomain;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareConstants;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareIDEUIPlugin;
import org.eclipse.emf.compare.ide.ui.internal.actions.save.SaveComparisonModelAction;
import org.eclipse.emf.compare.ide.ui.internal.editor.ComparisonScopeInput;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.ComparisonNode;
import org.eclipse.emf.compare.ide.ui.logical.EMFSynchronizationModel;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.actions.FilterActionMenu;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.actions.GroupActionMenu;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.filters.IDifferenceFilter;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.filters.IDifferenceFilterSelectionChangeEvent;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.filters.StructureMergeViewerFilter;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.IDifferenceGroupProvider;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.StructureMergeViewerGrouper;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.impl.DefaultGroupProvider;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.internal.ui.mapping.ModelCompareEditorInput;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class EMFCompareStructureMergeViewer extends DiffTreeViewer implements CommandStackListener {

	private final ICompareInputChangeListener fCompareInputChangeListener;

	private final ComposedAdapterFactory fAdapterFactory;

	private final CompareViewerSwitchingPane fParent;

	private Object fRoot;

	private ICompareEditingDomain editingDomain;

	/** The comparator used for this structure merge viewer */
	private EMFCompareStructureMergeViewerComparator structureMergeViewerComparator;

	/**
	 * The difference filter that will be applied to the structure viewer. Note that this will be initialized
	 * from {@link #createToolItems(ToolBarManager)} since that method is called from the super-constructor
	 * and we cannot init ourselves beforehand.
	 */
	private StructureMergeViewerFilter structureMergeViewerFilter;

	/**
	 * This will be used by our adapter factory in order to group together the differences located under the
	 * Comparison. Note that this will be initialized from {@link #createToolItems(ToolBarManager)} since that
	 * method is called from the super-constructor and we cannot init ourselves beforehand.
	 */
	private StructureMergeViewerGrouper structureMergeViewerGrouper;

	private MenuManager groupsMenuManager;

	private MenuManager filtersMenuManager;

	private GroupActionMenu groupActionMenu;

	private DefaultGroupProvider defaultGroupProvider;

	private FilterActionMenu filterActionMenu;

	private SaveComparisonModelAction saveAction;

	private EventBus eventBus;

	/**
	 * @param parent
	 * @param configuration
	 */
	public EMFCompareStructureMergeViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);

		fAdapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		fAdapterFactory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());
		fAdapterFactory.addAdapterFactory(new ResourceItemProviderAdapterFactory());

		structureMergeViewerComparator = new EMFCompareStructureMergeViewerComparator();

		setLabelProvider(new DelegatingStyledCellLabelProvider(
				new EMFCompareStructureMergeViewerLabelProvider(fAdapterFactory, this)));
		setContentProvider(new EMFCompareStructureMergeViewerContentProvider(fAdapterFactory,
				structureMergeViewerGrouper, structureMergeViewerFilter, configuration));

		if (parent instanceof CompareViewerSwitchingPane) {
			fParent = (CompareViewerSwitchingPane)parent;
		} else {
			fParent = null;
		}

		fCompareInputChangeListener = new ICompareInputChangeListener() {
			public void compareInputChanged(ICompareInput input) {
				EMFCompareStructureMergeViewer.this.compareInputChanged(input);
			}
		};

		// Wrap the defined comparer in our own.
		setComparer(new DiffNodeComparer(super.getComparer()));

		if (eventBus == null) {
			eventBus = new EventBus();
			eventBus.register(this);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#getComparator()
	 */
	@Override
	public ViewerComparator getComparator() {
		return structureMergeViewerComparator;
	}

	@SuppressWarnings("unchecked")
	@Subscribe
	public void recordFilterSelectionChange(IDifferenceFilterSelectionChangeEvent event) {
		final Object property = getCompareConfiguration().getProperty(EMFCompareConstants.SELECTED_FILTERS);
		final Collection<IDifferenceFilter> selectedFilters;
		if (property == null) {
			selectedFilters = new HashSet<IDifferenceFilter>();
		} else {
			selectedFilters = (Collection<IDifferenceFilter>)property;
		}
		switch (event.getAction()) {
			case ACTIVATE:
				selectedFilters.add(event.getFilter());
				break;
			case DEACTIVATE:
				selectedFilters.remove(event.getFilter());
				break;
			default:
				throw new IllegalStateException();
		}
		getCompareConfiguration().setProperty(EMFCompareConstants.SELECTED_FILTERS, selectedFilters);
	}

	@Subscribe
	public void recordGroupProviderSelectionChange(IDifferenceGroupProvider differenceGroupProvider) {
		getCompareConfiguration().setProperty(EMFCompareConstants.SELECTED_GROUP, differenceGroupProvider);
	}

	/**
	 * Triggered by fCompareInputChangeListener
	 */
	void compareInputChanged(ICompareInput input) {
		if (input == null) {
			// When closing, we don't need a progress monitor to handle the input change
			compareInputChanged((ICompareInput)null, new NullProgressMonitor());
			return;
		}
		CompareConfiguration cc = getCompareConfiguration();
		// The compare configuration is nulled when the viewer is disposed
		if (cc != null) {
			BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
				public void run() {
					try {
						inputChangedTask.run(new NullProgressMonitor());
					} catch (InvocationTargetException e) {
						EMFCompareIDEUIPlugin.getDefault().log(e.getTargetException());
					} catch (InterruptedException e) {
						// Ignore
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#getRoot()
	 */
	@Override
	protected Object getRoot() {
		return fRoot;
	}

	private IRunnableWithProgress inputChangedTask = new IRunnableWithProgress() {
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask("Computing Structure Differences", 100);
			compareInputChanged((ICompareInput)getInput(), new SubProgressMonitor(monitor, 100));
			monitor.done();
		}
	};

	/**
	 * Team left us with absolutely no way to determine whether our supplied input is the result of a
	 * synchronization or not.
	 * <p>
	 * In order to properly resolve the logical model of the resource currently being compared we need to know
	 * what "other" resources were part of its logical model, and we need to know the revisions of these
	 * resources we are to load. All of this has already been computed by Team, but it would not let us know.
	 * This method uses discouraged means to get around this "black box" locking from Team.
	 * </p>
	 * <p>
	 * The basic need here is to retrieve the Subscriber from this point. We have a lot of accessible
	 * variables, the two most important being the CompareConfiguration and ICompareInput... I could find no
	 * way around the privileged access to the private ModelCompareEditorInput.participant field. There does
	 * not seem to be any adapter (or Platform.getAdapterManager().getAdapter(...)) that would allow for this,
	 * so I'm taking the long way 'round.
	 * </p>
	 * 
	 * @return The subscriber used for this comparison if any could be found, <code>null</code> otherwise.
	 */
	@SuppressWarnings("restriction")
	private Subscriber getSubscriber() {
		if (getCompareConfiguration().getContainer() instanceof ModelCompareEditorInput) {
			final ModelCompareEditorInput modelInput = (ModelCompareEditorInput)getCompareConfiguration()
					.getContainer();
			ISynchronizeParticipant participant = null;
			try {
				final Field field = ModelCompareEditorInput.class.getDeclaredField("participant"); //$NON-NLS-1$
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						field.setAccessible(true);
						return null;
					}
				});
				participant = (ISynchronizeParticipant)field.get(modelInput);
			} catch (NoSuchFieldException e) {
				// Swallow this, this private field was there at least from 3.5 to 4.3
			} catch (IllegalArgumentException e) {
				// Cannot happen
			} catch (IllegalAccessException e) {
				// "Should" not happen, but ignore it anyway
			}
			if (participant instanceof ModelSynchronizeParticipant
					&& ((ModelSynchronizeParticipant)participant).getContext() instanceof SubscriberMergeContext) {
				return ((SubscriberMergeContext)((ModelSynchronizeParticipant)participant).getContext())
						.getSubscriber();
			}
		}
		return null;
	}

	void compareInputChanged(ICompareInput input, IProgressMonitor monitor) {
		if (input != null) {
			if (input instanceof ComparisonNode) {
				compareInputChanged((ComparisonNode)input, monitor);
			} else if (input instanceof ComparisonScopeInput) {
				compareInputChanged((ComparisonScopeInput)input, monitor);
			} else {
				final ITypedElement left = input.getLeft();
				final ITypedElement right = input.getRight();
				final ITypedElement origin = input.getAncestor();

				final Subscriber subscriber = getSubscriber();
				final EMFSynchronizationModel syncModel = EMFSynchronizationModel.createSynchronizationModel(
						subscriber, left, right, origin);

				// Double check : git allows modification of the index file ... but we cannot
				final CompareConfiguration config = getCompareConfiguration();
				if (!syncModel.isLeftEditable()) {
					config.setLeftEditable(false);
					saveAction.setEnabled(false);
				}
				if (!syncModel.isRightEditable()) {
					config.setRightEditable(false);
					saveAction.setEnabled(false);
				}

				final IComparisonScope scope = syncModel.createMinimizedScope();
				final Comparison compareResult = EMFCompare
						.builder()
						.setMatchEngineFactoryRegistry(
								EMFCompareRCPPlugin.getDefault().getMatchEngineFactoryRegistry())
						.setPostProcessorRegistry(EMFCompareRCPPlugin.getDefault().getPostProcessorRegistry())
						.build().compare(scope, BasicMonitor.toMonitor(monitor));

				final ResourceSet leftResourceSet = (ResourceSet)scope.getLeft();
				final ResourceSet rightResourceSet = (ResourceSet)scope.getRight();
				final ResourceSet originResourceSet = (ResourceSet)scope.getOrigin();

				editingDomain = (ICompareEditingDomain)getCompareConfiguration().getProperty(
						EMFCompareConstants.EDITING_DOMAIN);
				if (editingDomain == null) {
					editingDomain = EMFCompareEditingDomain.create(leftResourceSet, rightResourceSet,
							originResourceSet);
					getCompareConfiguration().setProperty(EMFCompareConstants.EDITING_DOMAIN, editingDomain);
				}

				editingDomain.getCommandStack().addCommandStackListener(this);

				compareInputChanged(scope, compareResult);
			}
		} else {
			ResourceSet leftResourceSet = null;
			ResourceSet rightResourceSet = null;
			ResourceSet originResourceSet = null;

			if (fRoot != null) {
				Comparison comparison = (Comparison)((Adapter)fRoot).getTarget();
				Iterator<Match> matchIt = comparison.getMatches().iterator();
				if (comparison.isThreeWay()) {
					while (matchIt.hasNext()
							&& (leftResourceSet == null || rightResourceSet == null || originResourceSet == null)) {
						Match match = matchIt.next();
						if (leftResourceSet == null) {
							leftResourceSet = getResourceSet(match.getLeft());
						}
						if (rightResourceSet == null) {
							rightResourceSet = getResourceSet(match.getRight());
						}
						if (originResourceSet == null) {
							originResourceSet = getResourceSet(match.getOrigin());
						}
					}
				} else {
					while (matchIt.hasNext() && (leftResourceSet == null || rightResourceSet == null)) {
						Match match = matchIt.next();
						if (leftResourceSet == null) {
							leftResourceSet = getResourceSet(match.getLeft());
						}
						if (rightResourceSet == null) {
							rightResourceSet = getResourceSet(match.getRight());
						}
					}
				}
			}

			if (editingDomain != null) {
				editingDomain.getCommandStack().removeCommandStackListener(this);
				getCompareConfiguration().setProperty(EMFCompareConstants.EDITING_DOMAIN, null);
				editingDomain = null;
			}

			// FIXME: should unload only if input.getLeft/Right/Ancestor (previously stored in field) are
			// instanceof ResourceNode
			unload(leftResourceSet);
			unload(rightResourceSet);
			unload(originResourceSet);

			getCompareConfiguration().setProperty(EMFCompareConstants.COMPARE_RESULT, null);
			getCompareConfiguration().setProperty(EMFCompareConstants.SELECTED_FILTERS, null);
			getCompareConfiguration().setProperty(EMFCompareConstants.SELECTED_GROUP, null);
			fRoot = null;
		}
	}

	void compareInputChanged(ComparisonNode input, IProgressMonitor monitor) {
		editingDomain = (ICompareEditingDomain)getCompareConfiguration().getProperty(
				EMFCompareConstants.EDITING_DOMAIN);
		editingDomain.getCommandStack().addCommandStackListener(this);

		compareInputChanged(null, input.getTarget());
	}

	void compareInputChanged(ComparisonScopeInput input, IProgressMonitor monitor) {
		editingDomain = (ICompareEditingDomain)getCompareConfiguration().getProperty(
				EMFCompareConstants.EDITING_DOMAIN);
		editingDomain.getCommandStack().addCommandStackListener(this);

		EMFCompare comparator = (EMFCompare)getCompareConfiguration().getProperty(
				EMFCompareConstants.COMPARATOR);
		Comparison comparison = comparator.compare(input.getComparisonScope(), BasicMonitor
				.toMonitor(monitor));
		compareInputChanged(null, comparison);
	}

	private static void unload(ResourceSet resourceSet) {
		if (resourceSet != null) {
			for (Resource resource : resourceSet.getResources()) {
				resource.unload();
			}
			resourceSet.getResources().clear();
		}
	}

	private static ResourceSet getResourceSet(EObject eObject) {
		if (eObject != null) {
			Resource eResource = eObject.eResource();
			if (eResource != null) {
				return eResource.getResourceSet();
			}
		}
		return null;
	}

	void compareInputChanged(final IComparisonScope scope, final Comparison comparison) {
		fRoot = fAdapterFactory.adapt(comparison, ICompareInput.class);
		groupActionMenu.createActions(scope, comparison);
		filterActionMenu.createActions(scope, comparison);
		getCompareConfiguration().setProperty(EMFCompareConstants.COMPARE_RESULT, comparison);

		getCompareConfiguration().getContainer().runAsynchronously(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				String message = null;
				if (comparison.getDifferences().isEmpty()) {
					message = "No Differences";
				}

				if (Display.getCurrent() != null) {
					refreshAfterDiff(message, fRoot);
				} else {
					final String theMessage = message;
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							refreshAfterDiff(theMessage, fRoot);
						}
					});
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#isExpandable(java.lang.Object)
	 */
	@Override
	public boolean isExpandable(Object parent) {
		if (hasFilters()) {
			// workaround for 65762
			return hasFilteredChildren(parent);
		}
		return super.isExpandable(parent);
	}

	/**
	 * Public method to test if a element has any children that passed the filters
	 * 
	 * @param parent
	 *            the element to test
	 * @return return <code>true</code> if the element has at least a child that passed the filters
	 */
	public final boolean hasFilteredChildren(Object parent) {
		Object[] rawChildren = getRawChildren(parent);
		return containsNonFiltered(rawChildren, parent);
	}

	private boolean containsNonFiltered(Object[] elements, Object parent) {
		if (elements.length == 0) {
			return false;
		}
		if (!hasFilters()) {
			return true;
		}
		ViewerFilter[] filters = getFilters();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			if (!isFiltered(object, parent, filters)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * All element filter tests must go through this method. Can be overridden by subclasses.
	 * 
	 * @param object
	 *            the object to filter
	 * @param parent
	 *            the parent
	 * @param filters
	 *            the filters to apply
	 * @return true if the element is filtered
	 */
	protected boolean isFiltered(Object object, Object parent, ViewerFilter[] filters) {
		for (int i = 0; i < filters.length; i++) {
			ViewerFilter filter = filters[i];
			if (!filter.select(this, parent, object)) {
				return true;
			}
		}
		return false;
	}

	private void refreshAfterDiff(String message, Object root) {
		if (getControl().isDisposed()) {
			return;
		}

		if (fParent != null) {
			fParent.setTitleArgument(message);
		}

		refresh(root);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#setComparer(org.eclipse.jface.viewers.IElementComparer)
	 */
	@Override
	public void setComparer(IElementComparer comparer) {
		// Wrap this new comparer in our own
		super.setComparer(new DiffNodeComparer(comparer));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.structuremergeviewer.DiffTreeViewer#createToolItems(org.eclipse.jface.action.ToolBarManager)
	 */
	@Override
	protected void createToolItems(ToolBarManager toolbarManager) {
		super.createToolItems(toolbarManager);
		groupActionMenu = new GroupActionMenu(getStructureMergeViewerGrouper(), getGroupsMenuManager(),
				getDefaultGroupProvider());
		filterActionMenu = new FilterActionMenu(getStructureMergeViewerFilter(), getFiltersMenuManager());
		saveAction = new SaveComparisonModelAction(getCompareConfiguration());
		toolbarManager.add(saveAction);
		toolbarManager.add(groupActionMenu);
		toolbarManager.add(filterActionMenu);
	}

	/**
	 * Returns the viewer filter that is to be applied on the structure viewer.
	 * <p>
	 * Note that this will be called from {@link #createToolItems(ToolBarManager)}, which is called from the
	 * super-constructor, when we have had no time to initialize the {@link #structureMergeViewerFilter}
	 * field.
	 * </p>
	 * 
	 * @return The difference filter that is to be applied on the structure viewer.
	 */
	protected StructureMergeViewerFilter getStructureMergeViewerFilter() {
		if (structureMergeViewerFilter == null) {
			if (eventBus == null) {
				eventBus = new EventBus();
				eventBus.register(this);
			}
			structureMergeViewerFilter = new StructureMergeViewerFilter(eventBus);
			structureMergeViewerFilter.install(this);
		}
		return structureMergeViewerFilter;
	}

	/**
	 * Returns the viewer grouper that is to be applied on the structure viewer.
	 * <p>
	 * Note that this will be called from {@link #createToolItems(ToolBarManager)}, which is called from the
	 * super-constructor, when we have had no time to initialize the {@link #structureMergeViewerGrouper}
	 * field.
	 * </p>
	 * 
	 * @return The viewer grouper grouper that is to be applied on the structure viewer.
	 */
	protected StructureMergeViewerGrouper getStructureMergeViewerGrouper() {
		if (structureMergeViewerGrouper == null) {
			if (eventBus == null) {
				eventBus = new EventBus();
				eventBus.register(this);
			}
			structureMergeViewerGrouper = new StructureMergeViewerGrouper(eventBus);
			structureMergeViewerGrouper.install(this);
		}
		return structureMergeViewerGrouper;
	}

	/**
	 * Returns the menu manager that is to be applied to groups on the structure viewer.
	 * 
	 * @return The menu manager that is to be applied to groups on the structure viewer.
	 */
	public MenuManager getGroupsMenuManager() {
		if (groupsMenuManager == null) {
			groupsMenuManager = new MenuManager();
		}
		return groupsMenuManager;
	}

	/**
	 * Returns the menu manager that is to be applied to filters on the structure viewer.
	 * 
	 * @return The menu manager that is to be applied to filters on the structure viewer.
	 */
	public MenuManager getFiltersMenuManager() {
		if (filtersMenuManager == null) {
			filtersMenuManager = new MenuManager();
		}
		return filtersMenuManager;
	}

	/**
	 * Returns the default group provider that is to be applied on the structure viewer.
	 * 
	 * @return The default group provider that is to be applied on the structure viewer.
	 */
	public DefaultGroupProvider getDefaultGroupProvider() {
		if (defaultGroupProvider == null) {
			defaultGroupProvider = new DefaultGroupProvider();
		}
		return defaultGroupProvider;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.structuremergeviewer.DiffTreeViewer#inputChanged(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		if (oldInput instanceof ICompareInput) {
			ICompareInput old = (ICompareInput)oldInput;
			old.removeCompareInputChangeListener(fCompareInputChangeListener);
		}
		if (input instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput)input;
			ci.addCompareInputChangeListener(fCompareInputChangeListener);
			compareInputChanged(ci);
			if (input != oldInput) {
				initialSelection();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.structuremergeviewer.DiffTreeViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		Object input = getInput();
		if (input instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput)input;
			ci.removeCompareInputChangeListener(fCompareInputChangeListener);
		}
		compareInputChanged((ICompareInput)null);
		fAdapterFactory.dispose();

		super.handleDispose(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.command.CommandStackListener#commandStackChanged(java.util.EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		Command mostRecentCommand = editingDomain.getCommandStack().getMostRecentCommand();
		if (mostRecentCommand instanceof ICompareCopyCommand) {
			Collection<?> affectedObjects = mostRecentCommand.getAffectedObjects();
			refresh(true);
			if (!affectedObjects.isEmpty()) {
				List<Object> adaptedAffectedObject = newArrayList();
				for (Object affectedObject : affectedObjects) {
					adaptedAffectedObject.add(fAdapterFactory.adapt(affectedObject, ICompareInput.class));
				}
				setSelection(new StructuredSelection(adaptedAffectedObject), true);
			}
		} else {
			// FIXME, should recompute the difference, something happened outside of this compare editor
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#getSortedChildren(java.lang.Object)
	 */
	@Override
	protected Object[] getSortedChildren(Object parentElementOrTreePath) {
		Object[] result = super.getSortedChildren(parentElementOrTreePath);
		if (parentElementOrTreePath instanceof Adapter
				&& ((Adapter)parentElementOrTreePath).getTarget() instanceof Conflict) {

			Collections.sort(Arrays.asList(result), new Comparator<Object>() {
				public int compare(Object o1, Object o2) {
					return getValue(o1) - getValue(o2);
				}

				public int getValue(Object o) {
					int value = 0;
					if (o instanceof Adapter && ((Adapter)o).getTarget() instanceof Diff) {
						if (((Diff)((Adapter)o).getTarget()).getSource() == DifferenceSource.LEFT) {
							value = 1;
						} else {
							value = 2;
						}
					}
					return value;
				}
			});

		}
		return result;
	}

	/**
	 * Use our own {@link ViewerComparator} in order to manage {@link IStyledLabelProvider} and
	 * {@link DelegatingStyledCellLabelProvider} cases.
	 */
	private static class EMFCompareStructureMergeViewerComparator extends ViewerComparator {

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer,
		 *      java.lang.Object, java.lang.Object)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int cat1 = category(e1);
			int cat2 = category(e2);

			if (cat1 != cat2) {
				return cat1 - cat2;
			}

			String name1 = getLabel(viewer, e1);
			String name2 = getLabel(viewer, e2);

			// use the comparator to compare the strings
			return getComparator().compare(name1, name2);
		}

		/**
		 * Returns the appropriate label of the given object based on the label provider of the given viewer.
		 * 
		 * @param viewer
		 *            The given {@link Viewer}.
		 * @param e1
		 *            The given object for which we want the label.
		 * @return The appropriate label based on the label provider of the given viewer.
		 */
		private String getLabel(Viewer viewer, Object e1) {
			String name1;
			if (viewer == null || !(viewer instanceof ContentViewer)) {
				name1 = e1.toString();
			} else {
				IBaseLabelProvider prov = ((ContentViewer)viewer).getLabelProvider();
				if (prov instanceof ILabelProvider) {
					ILabelProvider lprov = (ILabelProvider)prov;
					name1 = lprov.getText(e1);
				} else if (prov instanceof IStyledLabelProvider) {
					name1 = ((IStyledLabelProvider)prov).getStyledText(e1).getString();
				} else if (prov instanceof DelegatingStyledCellLabelProvider) {
					name1 = ((DelegatingStyledCellLabelProvider)prov).getStyledStringProvider()
							.getStyledText(e1).getString();
				} else {
					name1 = e1.toString();
				}
			}
			if (name1 == null) {
				name1 = "";//$NON-NLS-1$
			}
			return name1;
		}
	}
}
