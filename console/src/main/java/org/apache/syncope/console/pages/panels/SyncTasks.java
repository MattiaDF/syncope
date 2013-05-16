/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.RoleTemplateModalPage;
import org.apache.syncope.console.pages.SyncTaskModalPage;
import org.apache.syncope.console.pages.Tasks;
import org.apache.syncope.console.pages.Tasks.TasksProvider;
import org.apache.syncope.console.pages.UserTemplateModalPage;
import org.apache.syncope.console.rest.TaskRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SyncTasks extends Panel {

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 700;

    private static final long serialVersionUID = -8674781241465369244L;

    @SpringBean
    private TaskRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private int paginatorRows;

    private WebMarkupContainer container;

    private ModalWindow window;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    private final List<IColumn<TaskTO, String>> columns;

    private AjaxDataTablePanel<TaskTO, String> table;

    public SyncTasks(final String id, final PageReference pageRef) {
        super(id);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        window = new ModalWindow("taskWin");
        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setCookieName("view-task-win");
        add(window);

        ((Tasks) pageRef.getPage()).setWindowClosedCallback(window, container);

        paginatorRows = prefMan.getPaginatorRows(getWebRequest(), Constants.PREF_SYNC_TASKS_PAGINATOR_ROWS);

        columns = new ArrayList<IColumn<TaskTO, String>>();

        columns.add(new PropertyColumn<TaskTO, String>(
                new StringResourceModel("id", this, null), "id", "id"));
        columns.add(new PropertyColumn<TaskTO, String>(
                new StringResourceModel("name", this, null), "name", "name"));
        columns.add(new PropertyColumn<TaskTO, String>(
                new StringResourceModel("description", this, null), "description", "description"));
        columns.add(new PropertyColumn<TaskTO, String>(
                new StringResourceModel("resourceName", this, null), "resource", "resource"));
        columns.add(new DatePropertyColumn<TaskTO>(
                new StringResourceModel("lastExec", this, null), "lastExec", "lastExec"));
        columns.add(new DatePropertyColumn<TaskTO>(
                new StringResourceModel("nextExec", this, null), "nextExec", "nextExec"));
        columns.add(new PropertyColumn<TaskTO, String>(
                new StringResourceModel("latestExecStatus", this, null), "latestExecStatus", "latestExecStatus"));
        columns.add(new ActionColumn<TaskTO, String>(new StringResourceModel("actions", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel getActions(final String componentId, final IModel<TaskTO> model) {

                final SyncTaskTO taskTO = (SyncTaskTO) model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, pageRef);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new SyncTaskModalPage(window, taskTO, pageRef);
                            }
                        });

                        window.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Tasks");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new UserTemplateModalPage(pageRef, window, taskTO);
                            }
                        });

                        window.show(target);
                    }
                }, ActionLink.ActionType.USER_TEMPLATE, "Tasks");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new RoleTemplateModalPage(pageRef, window, taskTO);
                            }
                        });

                        window.show(target);
                    }
                }, ActionLink.ActionType.ROLE_TEMPLATE, "Tasks");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.startExecution(taskTO.getId(), false);
                            getSession().info(getString("operation_succeeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.add(container);
                        target.add(getPage().get("feedback"));
                    }
                }, ActionLink.ActionType.EXECUTE, "Tasks");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.startExecution(taskTO.getId(), true);
                            getSession().info(getString("operation_succeeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.add(container);
                        target.add(getPage().get("feedback"));
                    }
                }, ActionLink.ActionType.DRYRUN, "Tasks");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.delete(taskTO.getId(), SyncTaskTO.class);
                            info(getString("operation_succeeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.add(container);
                        target.add(getPage().get("feedback"));
                    }
                }, ActionLink.ActionType.DELETE, "Tasks");

                return panel;
            }

            @Override
            public Component getHeader(String componentId) {
                final ActionLinksPanel panel = new ActionLinksPanel(componentId, new Model(), pageRef);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        if (target != null) {
                            target.add(table);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, "Tasks", "list");

                return panel;
            }
        });

        table = Tasks.updateTaskTable(
                columns,
                new TasksProvider<SyncTaskTO>(restClient, paginatorRows, getId(), SyncTaskTO.class),
                container,
                0,
                pageRef,
                restClient);

        container.add(table);

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(), Constants.PREF_SYNC_TASKS_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));

                table = Tasks.updateTaskTable(
                        columns,
                        new TasksProvider<SyncTaskTO>(restClient, paginatorRows, getId(), SyncTaskTO.class),
                        container,
                        table == null ? 0 : (int) table.getCurrentPage(),
                        pageRef,
                        restClient);

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

        // create new user
        AjaxLink createLink = new ClearIndicatingAjaxLink("createLink", pageRef) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new SyncTaskModalPage(window, new SyncTaskTO(), pageRef);
                    }
                });

                window.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createLink, RENDER, xmlRolesReader.getAllAllowedRoles("Tasks", "create"));

        add(createLink);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AbstractSearchResultPanel.EventDataWrapper) {
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(container);
        }
    }
}
