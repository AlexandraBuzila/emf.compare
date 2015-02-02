/**
 * 
 */
package org.eclipse.emf.compare.ide.ui.contentmergeviewer.richtext;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.command.ICompareCopyCommand;
import org.eclipse.emf.compare.ide.ui.internal.configuration.EMFCompareConfiguration;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.text.EMFCompareTextMergeViewerContentProvider;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.CompareInputAdapter;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.uml2.internal.OpaqueElementBodyChange;
import org.eclipse.emf.compare.utils.IEqualityHelper;
import org.eclipse.emf.compare.utils.ReferenceUtil;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.change.util.ChangeRecorder;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.ChangeCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.UMLPackage;

import com.google.common.collect.ImmutableSet;

/**
 * @author Alexandra Buzila
 *
 */
@SuppressWarnings("restriction")
public class EMFCompareRichTextContentMergeViewer extends EMFCompareContentMergeViewer {

	/**
	 * Bundle name of the property file containing all displayed strings.
	 */
	private static final String BUNDLE_NAME = EMFCompareRichTextContentMergeViewer.class.getName();

	protected EMFCompareRichTextContentMergeViewer(Composite parent, EMFCompareConfiguration configuration) {
		super(SWT.NONE, null, configuration);
		buildControl(parent);
		setContentProvider(new EMFCompareTextMergeViewerContentProvider(configuration));
	}

	@Override
	protected IMergeViewer createMergeViewer(Composite parent, MergeViewerSide side) {
		EMFCompareRichTextMergeViewer viewer = new EMFCompareRichTextMergeViewer(parent, side,
				getCompareConfiguration());
		viewer.setContentProvider(new RichTextContentMergeViewerContentProvider());
		viewer.getEditor().addModifyListener(new TextModifyListener(viewer));
		return viewer;
	}

	@Override
	protected void paintCenter(GC g) {
		// TODO Auto-generated method stub

	}

	@Override
	protected byte[] getContents(boolean isleft) {
		// EMFCompareRichTextMergeViewer v = isleft ? left : right;
		// if (v != null) {
		// String text = v.getText();
		// if (text != null)
		// return text.getBytes();
		// }
		return null;
	}

	private class TextModifyListener implements ModifyListener {

		private EMFCompareRichTextMergeViewer viewer;

		public TextModifyListener(EMFCompareRichTextMergeViewer viewer) {
			this.viewer = viewer;
		}

		public void modifyText(ModifyEvent e) {
			String text = viewer.getText();
			final Object oldInput = getInput();
			if (oldInput instanceof CompareInputAdapter) {
				if (!Diff.class.isInstance(((CompareInputAdapter) oldInput).getComparisonObject())) {
					return;
				}
				final Diff diff = (Diff) ((CompareInputAdapter) oldInput).getComparisonObject();
				EAttribute eAttribute = getAttributeFromDiff(diff);
				if (eAttribute == null) {
					return;
				}

				updateModel(diff, eAttribute, viewer.getSide(), text);
			}
		}
	}

	public void updateModel(final Diff diff, final EAttribute eAttribute, MergeViewerSide mergeViewerSide,
			String newValue) {

		EObject eObject = null;
		if (mergeViewerSide == MergeViewerSide.LEFT) {
			eObject = diff.getMatch().getLeft();
		} else if (mergeViewerSide == MergeViewerSide.RIGHT) {
			eObject = diff.getMatch().getRight();
		}

		if (eObject == null) {
			return;
		}

		final String oldValue = getOldStringValue(diff, eObject, eAttribute);
		final IEqualityHelper equalityHelper = diff.getMatch().getComparison().getEqualityHelper();
		final boolean oldAndNewEquals = equalityHelper.matchingAttributeValues(newValue, oldValue);
		if (!oldAndNewEquals && isSideEditable(mergeViewerSide)) {
			// Save the change
			getCompareConfiguration()
					.getEditingDomain()
					.getCommandStack()
					.execute(
							new UpdateModelAndRejectDiffCommand(getCompareConfiguration().getEditingDomain()
									.getChangeRecorder(), eObject, eAttribute, oldValue, newValue, diff,
									(mergeViewerSide == MergeViewerSide.LEFT)));
		}
	}

	private String getOldStringValue(Diff diff, EObject eObject, EAttribute eAttribute) {
		if (diff instanceof AttributeChange) {
			return getStringValue(eObject, eAttribute);
		}
		if (diff instanceof OpaqueElementBodyChange && eObject instanceof OpaqueAction) {
			String language = OpaqueElementBodyChange.class.cast(diff).getLanguage();
			int index = OpaqueAction.class.cast(eObject).getLanguages().indexOf(language);
			return OpaqueAction.class.cast(eObject).getBodies().get(index);
		}
		return null;
	}

	private String getStringValue(final EObject eObject, final EAttribute eAttribute) {
		final EDataType eAttributeType = eAttribute.getEAttributeType();
		final Object value;
		if (eObject == null) {
			value = null;
		} else {
			value = ReferenceUtil.safeEGet(eObject, eAttribute);
		}
		return EcoreUtil.convertToString(eAttributeType, value);
	}

	private EAttribute getAttributeFromDiff(Diff diff) {
		if (diff instanceof AttributeChange) {
			return AttributeChange.class.cast(diff).getAttribute();
		}
		if (diff instanceof OpaqueElementBodyChange) {
			return UMLPackage.eINSTANCE.getOpaqueAction_Body();
		}
		return null;
	}

	private boolean isSideEditable(MergeViewerSide mergeViewerSide) {
		if (mergeViewerSide == MergeViewerSide.LEFT) {
			return getCompareConfiguration().isLeftEditable();
		}
		if (mergeViewerSide == MergeViewerSide.RIGHT) {
			return getCompareConfiguration().isRightEditable();
		}
		return false;
	}

	/**
	 * Command to directly modify the semantic model and reject the related
	 * difference.
	 * 
	 * @author cnotot
	 */
	private static class UpdateModelAndRejectDiffCommand extends ChangeCommand implements ICompareCopyCommand {

		private boolean isLeft;

		private Diff difference;

		private Object oldValue;

		private Object newValue;

		private EStructuralFeature feature;

		private EObject owner;

		public UpdateModelAndRejectDiffCommand(ChangeRecorder changeRecorder, EObject owner,
				EStructuralFeature feature, Object oldValue, Object newValue, Diff difference, boolean isLeft) {
			super(changeRecorder, ImmutableSet.<Notifier> builder().add(owner).addAll(getAffectedDiff(difference))
					.build());
			this.owner = owner;
			this.feature = feature;
			this.oldValue = oldValue;
			this.newValue = newValue;
			this.difference = difference;
			this.isLeft = isLeft;
		}

		@Override
		public void doExecute() {
			if (feature.isMany()) {
				@SuppressWarnings("unchecked")
				List<Object> elements = (List<Object>) owner.eGet(feature);
				int index = elements.indexOf(oldValue);
				elements.set(index, newValue);
			} else {
				owner.eSet(feature, newValue);
			}
			for (Diff affectedDiff : getAffectedDiff(difference)) {
				affectedDiff.setState(DifferenceState.DISCARDED);
			}
		}

		private static Set<Diff> getAffectedDiff(Diff diff) {
			EList<Conflict> conflicts = diff.getMatch().getComparison().getConflicts();
			for (Conflict conflict : conflicts) {
				EList<Diff> conflictualDifferences = conflict.getDifferences();
				if (conflictualDifferences.contains(diff)) {
					return ImmutableSet.copyOf(conflictualDifferences);
				}
			}
			return ImmutableSet.of(diff);
		}

		public boolean isLeftToRight() {
			return !isLeft;
		}

	}

}
