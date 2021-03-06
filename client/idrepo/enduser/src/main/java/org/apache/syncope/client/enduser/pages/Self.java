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
package org.apache.syncope.client.enduser.pages;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Self extends BaseEnduserWebPage implements IEventSource {

    private static final long serialVersionUID = 164651008547631054L;

    @SpringBean
    private ConfParamOps confParamOps;

    private AjaxWizardBuilder<AnyWrapper<UserTO>> wizardBuilder;

    protected static final String WIZARD_ID = "wizard";

    public Self(final PageParameters parameters) {
        super(parameters);

        body.add(buildWizard(SyncopeEnduserSession.get().isAuthenticated()
                ? SyncopeEnduserSession.get().getSelfTO()
                : buildNewUserTO(parameters),
                SyncopeEnduserSession.get().isAuthenticated()
                ? AjaxWizard.Mode.EDIT
                : AjaxWizard.Mode.CREATE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                @SuppressWarnings("unchecked")
                final Class<? extends WebPage> beforeLogout = (Class<? extends WebPage>) SyncopeEnduserSession.get().
                        getAttribute(Constants.BEFORE_LOGOUT_PAGE);
                if (beforeLogout == null) {
                    SyncopeEnduserSession.get().invalidate();
                    setResponsePage(getApplication().getHomePage());
                } else {
                    setResponsePage(beforeLogout);
                }
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeEnduserSession.get().invalidate();

                final PageParameters parameters = new PageParameters();
                parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.wizard.success"));
                setResponsePage(getApplication().getHomePage(), parameters);
            }
        }
        super.onEvent(event);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        navbar.setActiveNavItem(getClass().getSimpleName().toLowerCase());
    }

    protected final AjaxWizard<AnyWrapper<UserTO>> buildWizard(final UserTO userTO, final AjaxWizard.Mode mode) {
        final String formLayoutConfParam = confParamOps.get(
                SyncopeEnduserSession.get().getDomain(),
                "enduser.form.layout.info",
                FormLayoutInfoUtils.getDefaultValue(),
                String.class);

        final UserFormLayoutInfo formLayoutInfo =
                StringUtils.isBlank(formLayoutConfParam)
                ? new UserFormLayoutInfo()
                : FormLayoutInfoUtils.fromJsonString(formLayoutConfParam);

        wizardBuilder = (AjaxWizardBuilder<AnyWrapper<UserTO>>) FormLayoutInfoUtils.instantiate(
                userTO,
                SyncopeEnduserSession.get().getService(SyncopeService.class).platform().getUserClasses(),
                formLayoutInfo,
                this.getPageReference());
        wizardBuilder.setItem(new UserWrapper(userTO));
        return wizardBuilder.build(WIZARD_ID, mode);
    }

    private static UserTO buildNewUserTO(final PageParameters parameters) {
        final UserTO userTO = new UserTO();

        if (parameters != null) {
            if (!parameters.get("saml2SPUserAttrs").isNull()) {
                SyncopeWebApplication.extractAttrsFromExt(parameters.get("saml2SPUserAttrs").toString(), userTO);
            } else if (!parameters.get("oidcClientUserAttrs").isNull()) {
                SyncopeWebApplication.extractAttrsFromExt(parameters.get("oidcClientUserAttrs").toString(), userTO);
            }
        }

        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        return userTO;
    }

}
