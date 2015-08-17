/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 * Steven Spungin <steven@spungin.tv> - Ongoing maintenance
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.emf.ui.common.Util;
import org.eclipse.e4.tools.emf.ui.common.component.AbstractComponentEditor;
import org.eclipse.e4.tools.emf.ui.internal.ResourceProvider;
import org.eclipse.e4.tools.emf.ui.internal.common.AbstractPickList.PickListFeatures;
import org.eclipse.e4.tools.emf.ui.internal.common.ComponentLabelProvider;
import org.eclipse.e4.tools.emf.ui.internal.common.E4PickList;
import org.eclipse.e4.tools.emf.ui.internal.common.uistructure.UIViewer;
import org.eclipse.e4.tools.emf.ui.internal.imp.ModelImportWizard;
import org.eclipse.e4.ui.model.application.impl.ApplicationPackageImpl;
import org.eclipse.e4.ui.model.application.ui.advanced.MAdvancedFactory;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFProperties;
import org.eclipse.emf.databinding.FeaturePath;
import org.eclipse.emf.databinding.IEMFListProperty;
import org.eclipse.emf.databinding.edit.EMFEditProperties;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.databinding.swt.IWidgetValueProperty;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class PerspectiveStackEditor extends AbstractComponentEditor {
	private Composite composite;
	private EMFDataBindingContext context;

	private final IListProperty ELEMENT_CONTAINER__CHILDREN = EMFProperties
			.list(UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN);
	private StackLayout stackLayout;
	private final List<Action> actions = new ArrayList<Action>();
	private final List<Action> actionsImport = new ArrayList<Action>();

	@Inject
	@Optional
	private IProject project;

	@Inject
	public PerspectiveStackEditor() {
		super();
	}

	@Inject
	Shell shell;

	@PostConstruct
	void init() {
		actions.add(new Action(Messages.PerspectiveStackEditor_AddPerspective,
				createImageDescriptor(ResourceProvider.IMG_Perspective)) {
			@Override
			public void run() {
				handleAddPerspective();
			}
		});

		// --- import ---
		actionsImport.add(new Action(Messages.PerspectiveStackEditor_AddPerspective,
				createImageDescriptor(ResourceProvider.IMG_Perspective)) {
			@Override
			public void run() {
				handleImportPerspective();
			}
		});
	}

	@Override
	public Image getImage(Object element) {
		return getImage(element, ResourceProvider.IMG_PerspectiveStack);
	}

	@Override
	public String getLabel(Object element) {
		return Messages.PerspectiveStackEditor_Label;
	}

	@Override
	public String getDetailLabel(Object element) {
		return null;
	}

	@Override
	public String getDescription(Object element) {
		return Messages.PerspectiveStackEditor_Description;
	}

	@Override
	public Composite doGetEditor(Composite parent, Object object) {
		if (composite == null) {
			context = new EMFDataBindingContext();
			if (getEditor().isModelFragment()) {
				composite = new Composite(parent, SWT.NONE);
				stackLayout = new StackLayout();
				composite.setLayout(stackLayout);
				createForm(composite, context, getMaster(), false);
				createForm(composite, context, getMaster(), true);
			} else {
				composite = createForm(parent, context, getMaster(), false);
			}
		}

		if (getEditor().isModelFragment()) {
			Control topControl;
			if (Util.isImport((EObject) object)) {
				topControl = composite.getChildren()[1];
			} else {
				topControl = composite.getChildren()[0];
			}

			if (stackLayout.topControl != topControl) {
				stackLayout.topControl = topControl;
				composite.layout(true, true);
			}
		}

		getMaster().setValue(object);
		return composite;
	}

	private Composite createForm(Composite parent, final EMFDataBindingContext context, WritableValue master,
			boolean isImport) {
		final CTabFolder folder = new CTabFolder(parent, SWT.BOTTOM);

		CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.ModelTooling_Common_TabDefault);

		parent = createScrollableContainer(folder);
		item.setControl(parent.getParent());

		if (getEditor().isShowXMIId() || getEditor().isLiveModel()) {
			ControlFactory.createXMIId(parent, this);
		}

		final IWidgetValueProperty textProp = WidgetProperties.text(SWT.Modify);

		if (isImport) {
			ControlFactory.createFindImport(parent, Messages, this, context);
			folder.setSelection(0);
			return folder;
		}

		ControlFactory.createTextField(parent, Messages.ModelTooling_Common_Id, master, context, textProp,
				EMFEditProperties
				.value(getEditingDomain(), ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__ELEMENT_ID));
		ControlFactory.createTextField(parent, Messages.ModelTooling_UIElement_AccessibilityPhrase, getMaster(),
				context, textProp,
				EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_ELEMENT__ACCESSIBILITY_PHRASE));
		ControlFactory.createSelectedElement(parent, this, context, Messages.PerspectiveStackEditor_SelectedElement);

		// ------------------------------------------------------------
		{

			final E4PickList pickList = new E4PickList(parent, SWT.NONE, Arrays.asList(PickListFeatures.NO_PICKER),
					Messages, this, UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN) {
				@Override
				protected void addPressed() {
					handleAddPerspective();
				}
			};
			pickList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			pickList.setText(Messages.PerspectiveStackEditor_Perspectives);

			final TableViewer viewer = pickList.getList();
			viewer.setContentProvider(new ObservableListContentProvider());
			viewer.setLabelProvider(new ComponentLabelProvider(getEditor(), Messages));
			final IEMFListProperty prop = EMFProperties.list(UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN);
			viewer.setInput(prop.observeDetail(getMaster()));
		}

		ControlFactory.createCheckBox(parent, Messages.ModelTooling_UIElement_ToBeRendered, getMaster(), context,
				WidgetProperties.selection(),
				EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_ELEMENT__TO_BE_RENDERED));
		ControlFactory.createCheckBox(parent, Messages.ModelTooling_UIElement_Visible, getMaster(), context,
				WidgetProperties.selection(),
				EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_ELEMENT__VISIBLE));

		item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.ModelTooling_Common_TabSupplementary);

		parent = createScrollableContainer(folder);
		item.setControl(parent.getParent());

		ControlFactory.createStringListWidget(parent, Messages, this, Messages.CategoryEditor_Tags,
				ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__TAGS, VERTICAL_LIST_WIDGET_INDENT);
		ControlFactory.createMapProperties(parent, Messages, this, Messages.ModelTooling_Contribution_PersistedState,
				ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__PERSISTED_STATE, VERTICAL_LIST_WIDGET_INDENT);

		if (project == null) {
			createUITreeInspection(folder);
		}

		createContributedEditorTabs(folder, context, getMaster(), MPerspectiveStack.class);

		folder.setSelection(0);

		return folder;
	}

	private void createUITreeInspection(CTabFolder folder) {
		final CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.ModelTooling_Common_RuntimeWidgetTree);
		final Composite container = new Composite(folder, SWT.NONE);
		container.setLayout(new GridLayout());
		item.setControl(container);

		final UIViewer objectViewer = new UIViewer();
		final TreeViewer viewer = objectViewer.createViewer(container, UiPackageImpl.Literals.UI_ELEMENT__WIDGET,
				getMaster(), resourcePool, Messages);
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	@Override
	public IObservableList getChildList(Object element) {
		return ELEMENT_CONTAINER__CHILDREN.observe(element);
	}

	@Override
	public FeaturePath[] getLabelProperties() {
		return new FeaturePath[] { FeaturePath.fromList(UiPackageImpl.Literals.UI_ELEMENT__TO_BE_RENDERED) };
	}

	protected void handleAddPerspective() {
		final MPerspective eObject = MAdvancedFactory.INSTANCE.createPerspective();
		addToModel(eObject);
	}

	protected void handleImportPerspective() {
		final ModelImportWizard wizard = new ModelImportWizard(MPerspective.class, this, resourcePool);
		final WizardDialog wizardDialog = new WizardDialog(shell, wizard);
		if (wizardDialog.open() == Window.OK) {
			final MPerspective[] elements = (MPerspective[]) wizard.getElements(MPerspective.class);
			for (final MPerspective category : elements) {
				addToModel(category);
			}
		}
	}

	private void addToModel(MPerspective perspective) {
		setElementId(perspective);
		final Command cmd = AddCommand.create(getEditingDomain(), getMaster().getValue(),
				UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN, perspective);
		if (cmd.canExecute()) {
			getEditingDomain().getCommandStack().execute(cmd);
			getEditor().setSelection(perspective);
		}
	}

	@Override
	public List<Action> getActions(Object element) {
		final ArrayList<Action> l = new ArrayList<Action>(super.getActions(element));
		l.addAll(actions);
		return l;
	}

	@Override
	public List<Action> getActionsImport(Object element) {
		final ArrayList<Action> l = new ArrayList<Action>(super.getActionsImport(element));
		l.addAll(actionsImport);
		return l;
	}
}
