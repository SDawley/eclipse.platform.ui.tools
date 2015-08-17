/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 * Steven Spungin <steven@spungin.tv> - Bug 404166, 424730, 437951
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.component;

import javax.inject.Inject;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.emf.ui.common.IContributionClassCreator;
import org.eclipse.e4.tools.emf.ui.common.ImageTooltip;
import org.eclipse.e4.tools.emf.ui.common.Util;
import org.eclipse.e4.tools.emf.ui.common.component.AbstractComponentEditor;
import org.eclipse.e4.tools.emf.ui.internal.ResourceProvider;
import org.eclipse.e4.tools.emf.ui.internal.common.ModelEditor;
import org.eclipse.e4.tools.emf.ui.internal.common.VirtualEntry;
import org.eclipse.e4.tools.emf.ui.internal.common.component.ControlFactory.TextPasteHandler;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.ContributionClassDialog;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.PartDescriptorIconDialogEditor;
import org.eclipse.e4.ui.model.application.commands.impl.CommandsPackageImpl;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicPackageImpl;
import org.eclipse.e4.ui.model.application.impl.ApplicationPackageImpl;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFProperties;
import org.eclipse.emf.databinding.FeaturePath;
import org.eclipse.emf.databinding.edit.EMFEditProperties;
import org.eclipse.emf.databinding.edit.IEMFEditValueProperty;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.jface.databinding.swt.IWidgetValueProperty;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

public class PartDescriptorEditor extends AbstractComponentEditor {

	private Composite composite;
	private EMFDataBindingContext context;

	@Inject
	@Optional
	private IProject project;

	@Inject
	IEclipseContext eclipseContext;

	private final IListProperty PART__MENUS = EMFProperties.list(BasicPackageImpl.Literals.PART_DESCRIPTOR__MENUS);
	private final IListProperty HANDLER_CONTAINER__HANDLERS = EMFProperties
			.list(CommandsPackageImpl.Literals.HANDLER_CONTAINER__HANDLERS);
	private final IValueProperty PART__TOOLBAR = EMFProperties
			.value(BasicPackageImpl.Literals.PART_DESCRIPTOR__TOOLBAR);
	private Button createRemoveToolBar;
	private StackLayout stackLayout;

	@Inject
	public PartDescriptorEditor() {
		super();
	}

	@Override
	public Image getImage(Object element) {
		return getImage(element, ResourceProvider.IMG_PartDescriptor);
	}

	@Override
	public String getLabel(Object element) {
		return Messages.PartDescriptorEditor_Label;
	}

	@Override
	public String getDescription(Object element) {
		return Messages.PartDescriptorEditor_Descriptor;
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

		if (createRemoveToolBar != null) {
			createRemoveToolBar.setSelection(((MPartDescriptor) object).getToolbar() != null);
		}

		getMaster().setValue(object);
		enableIdGenerator(UiPackageImpl.Literals.UI_LABEL__LABEL,
				ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__ELEMENT_ID, null);
		return composite;
	}

	protected Composite createForm(Composite parent, EMFDataBindingContext context, IObservableValue master,
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
		ControlFactory.createTranslatedTextField(parent, Messages.PartDescriptorEditor_LabelLabel, master, context,
				textProp, EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_LABEL__LABEL),
				resourcePool, project);
		ControlFactory.createTranslatedTextField(parent, Messages.PartDescriptorEditor_Tooltip, master, context,
				textProp, EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_LABEL__TOOLTIP),
				resourcePool, project);

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartDescriptorEditor_IconURI);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			final Text t = new Text(parent, SWT.BORDER);
			TextPasteHandler.createFor(t);
			t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			context.bindValue(
					textProp.observeDelayed(200, t),
					EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_LABEL__ICON_URI).observeDetail(
							master));

			new ImageTooltip(t, Messages) {

				@Override
				protected URI getImageURI() {
					final MUILabel part = (MUILabel) getMaster().getValue();
					final String uri = part.getIconURI();
					if (uri == null || uri.trim().length() == 0) {
						return null;
					}
					return URI.createURI(part.getIconURI());
				}
			};

			final Button b = new Button(parent, SWT.PUSH | SWT.FLAT);
			b.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
			b.setImage(createImage(ResourceProvider.IMG_Obj16_zoom));
			b.setText(Messages.ModelTooling_Common_FindEllipsis);
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					final PartDescriptorIconDialogEditor dialog = new PartDescriptorIconDialogEditor(b.getShell(),
							eclipseContext, project, getEditingDomain(), (MPartDescriptor) getMaster().getValue(), Messages);
					dialog.open();
				}
			});
		}

		// ------------------------------------------------------------
		final Link lnk;
		{
			/*
			 * IContributionClassCreator accepts MContribitions but
			 * MPartDescriptor does not implement, so we need to be a bit
			 * creative here
			 */
			//
			final IContributionClassCreator c = getEditor().getContributionCreator(
					org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl.Literals.PART);
			if (project != null && c != null) {
				lnk = new Link(parent, SWT.NONE);
				lnk.setText("<A>" + Messages.PartEditor_ClassURI + "</A>"); //$NON-NLS-1$//$NON-NLS-2$
				lnk.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
				final IObservableValue masterFinal = master;
				lnk.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						final MPart dummyPart = MBasicFactory.INSTANCE.createPart();
						final String contributionURI = ((MPartDescriptor) getMaster().getValue()).getContributionURI();
						dummyPart.setContributionURI(contributionURI);
						c.createOpen(dummyPart, getEditingDomain(), project, lnk.getShell());
						((MPartDescriptor) masterFinal.getValue()).setContributionURI(dummyPart.getContributionURI());
					}
				});
			} else {
				// Dispose the lnk widget, which is unused in this else branch
				// and screws up the layout: see https://bugs.eclipse.org/421369
				lnk = null;
				final Label l = new Label(parent, SWT.NONE);
				l.setText(Messages.PartDescriptorEditor_ClassURI);
				l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			}

			final Text t = new Text(parent, SWT.BORDER);
			t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			context.bindValue(textProp.observeDelayed(200, t),
					EMFEditProperties
					.value(getEditingDomain(), BasicPackageImpl.Literals.PART_DESCRIPTOR__CONTRIBUTION_URI)
					.observeDetail(master));
			TextPasteHandler.createFor(t);

			final Button b = new Button(parent, SWT.PUSH | SWT.FLAT);
			b.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
			b.setImage(createImage(ResourceProvider.IMG_Obj16_zoom));
			b.setText(Messages.ModelTooling_Common_FindEllipsis);
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					final ContributionClassDialog dialog = new ContributionClassDialog(b.getShell(), eclipseContext,
							getEditingDomain(), (MPartDescriptor) getMaster().getValue(),
							BasicPackageImpl.Literals.PART_DESCRIPTOR__CONTRIBUTION_URI, Messages);
					dialog.open();
				}
			});
		}

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartEditor_ToolBar);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			createRemoveToolBar = new Button(parent, SWT.CHECK);
			createRemoveToolBar.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					final MPartDescriptor window = (MPartDescriptor) getMaster().getValue();
					if (window.getToolbar() == null) {
						addToolBar();
					} else {
						removeToolBar();
					}
				}
			});
			createRemoveToolBar.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
		}

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartDescriptorEditor_ContainerData);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			final Text t = new Text(parent, SWT.BORDER);
			final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			t.setLayoutData(gd);
			context.bindValue(textProp.observeDelayed(200, t),
					EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_ELEMENT__CONTAINER_DATA)
					.observeDetail(master));
		}

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartDescriptorEditor_Dirtyable);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			final Button checkbox = new Button(parent, SWT.CHECK);
			checkbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));

			final IEMFEditValueProperty mprop = EMFEditProperties.value(getEditingDomain(),
					BasicPackageImpl.Literals.PART_DESCRIPTOR__DIRTYABLE);
			final IWidgetValueProperty uiProp = WidgetProperties.selection();

			context.bindValue(uiProp.observe(checkbox), mprop.observeDetail(master));
		}

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartDescriptorEditor_Closeable);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			final Button checkbox = new Button(parent, SWT.CHECK);
			checkbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));

			final IEMFEditValueProperty mprop = EMFEditProperties.value(getEditingDomain(),
					BasicPackageImpl.Literals.PART_DESCRIPTOR__CLOSEABLE);
			final IWidgetValueProperty uiProp = WidgetProperties.selection();

			context.bindValue(uiProp.observe(checkbox), mprop.observeDetail(master));
		}

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartDescriptorEditor_Multiple);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			final Button checkbox = new Button(parent, SWT.CHECK);
			checkbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));

			final IEMFEditValueProperty mprop = EMFEditProperties.value(getEditingDomain(),
					BasicPackageImpl.Literals.PART_DESCRIPTOR__ALLOW_MULTIPLE);
			final IWidgetValueProperty uiProp = WidgetProperties.selection();

			context.bindValue(uiProp.observe(checkbox), mprop.observeDetail(master));
		}

		// ------------------------------------------------------------
		{
			final Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.PartDescriptorEditor_Category);
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			final Text t = new Text(parent, SWT.BORDER);
			t.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
			context.bindValue(textProp.observeDelayed(200, t),
					EMFEditProperties.value(getEditingDomain(), BasicPackageImpl.Literals.PART_DESCRIPTOR__CATEGORY)
					.observeDetail(master));
		}

		ControlFactory.createBindingContextWiget(parent, Messages, this, Messages.PartEditor_BindingContexts);

		item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.ModelTooling_Common_TabSupplementary);

		parent = createScrollableContainer(folder);
		item.setControl(parent.getParent());

		ControlFactory.createStringListWidget(parent, Messages, this, Messages.CategoryEditor_Tags,
				ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__TAGS, VERTICAL_LIST_WIDGET_INDENT);
		ControlFactory.createMapProperties(parent, Messages, this, Messages.ModelTooling_Contribution_PersistedState,
				ApplicationPackageImpl.Literals.APPLICATION_ELEMENT__PERSISTED_STATE, VERTICAL_LIST_WIDGET_INDENT);

		createContributedEditorTabs(folder, context, getMaster(), MPartDescriptor.class);

		folder.setSelection(0);

		return folder;
	}

	private void addToolBar() {
		final MToolBar menu = MMenuFactory.INSTANCE.createToolBar();
		setElementId(menu);

		final Command cmd = SetCommand.create(getEditingDomain(), getMaster().getValue(),
				BasicPackageImpl.Literals.PART_DESCRIPTOR__TOOLBAR, menu);
		if (cmd.canExecute()) {
			getEditingDomain().getCommandStack().execute(cmd);
		}
	}

	private void removeToolBar() {
		final Command cmd = SetCommand.create(getEditingDomain(), getMaster().getValue(),
				BasicPackageImpl.Literals.PART_DESCRIPTOR__TOOLBAR, null);
		if (cmd.canExecute()) {
			getEditingDomain().getCommandStack().execute(cmd);
		}
	}

	@Override
	public IObservableList getChildList(final Object element) {
		final WritableList list = new WritableList();

		if (getEditor().isModelFragment() && Util.isImport((EObject) element)) {
			return list;
		}

		list.add(new VirtualEntry<Object>(ModelEditor.VIRTUAL_PARTDESCRIPTOR_MENU, PART__MENUS, element,
				Messages.PartDescriptorEditor_Menus) {

			@Override
			protected boolean accepted(Object o) {
				return true;
			}

		});

		list.add(new VirtualEntry<Object>(ModelEditor.VIRTUAL_HANDLER, HANDLER_CONTAINER__HANDLERS, element,
				Messages.PartDescriptorEditor_Handlers) {

			@Override
			protected boolean accepted(Object o) {
				return true;
			}

		});

		final MPartDescriptor window = (MPartDescriptor) element;
		if (window.getToolbar() != null) {
			list.add(0, window.getToolbar());
		}

		PART__TOOLBAR.observe(element).addValueChangeListener(new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				if (event.diff.getOldValue() != null) {
					list.remove(event.diff.getOldValue());
					if (getMaster().getValue() == element && !createRemoveToolBar.isDisposed()) {
						createRemoveToolBar.setSelection(false);
					}

				}

				if (event.diff.getNewValue() != null) {
					list.add(0, event.diff.getNewValue());
					if (getMaster().getValue() == element && !createRemoveToolBar.isDisposed()) {
						createRemoveToolBar.setSelection(true);
					}
				}
			}
		});

		return list;
	}

	@Override
	public String getDetailLabel(Object element) {
		return getLocalizedLabel((MUILabel) element);
	}

	@Override
	public FeaturePath[] getLabelProperties() {
		return new FeaturePath[] { FeaturePath.fromList(UiPackageImpl.Literals.UI_LABEL__LABEL) };
	}
}