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
package org.apache.syncope.client.console.layout;

import org.apache.syncope.client.ui.commons.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.ui.commons.wizards.any.AnyForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;

/**
 * Utility methods for dealing with form layout information.
 */
public final class FormLayoutInfoUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>> fetch(
            final Collection<String> anyTypes) {

        List<String> ownedRoles = SyncopeConsoleSession.get().getSelfTO().getRoles();
        try {
            JsonNode tree = null;
            for (int i = 0; i < ownedRoles.size() && tree == null; i++) {
                String consoleLayoutInfo = RoleRestClient.readConsoleLayoutInfo(ownedRoles.get(i));
                if (StringUtils.isNotBlank(consoleLayoutInfo)) {
                    tree = MAPPER.readTree(consoleLayoutInfo);
                }
            }
            if (tree == null) {
                tree = MAPPER.createObjectNode();
            }

            UserFormLayoutInfo userFormLayoutInfo = tree.has(AnyTypeKind.USER.name())
                    ? MAPPER.treeToValue(tree.get(AnyTypeKind.USER.name()), UserFormLayoutInfo.class)
                    : new UserFormLayoutInfo();

            GroupFormLayoutInfo groupFormLayoutInfo = tree.has(AnyTypeKind.USER.name())
                    ? MAPPER.treeToValue(tree.get(AnyTypeKind.GROUP.name()), GroupFormLayoutInfo.class)
                    : new GroupFormLayoutInfo();

            Map<String, AnyObjectFormLayoutInfo> anyObjectFormLayoutInfos = new HashMap<>();
            for (String anyType : anyTypes) {
                if (!anyType.equals(AnyTypeKind.USER.name()) && !anyType.equals(AnyTypeKind.GROUP.name())) {

                    anyObjectFormLayoutInfos.put(
                            anyType,
                            tree.has(anyType)
                            ? MAPPER.treeToValue(tree.get(anyType), AnyObjectFormLayoutInfo.class)
                            : new AnyObjectFormLayoutInfo());
                }
            }

            return Triple.of(userFormLayoutInfo, groupFormLayoutInfo, anyObjectFormLayoutInfos);
        } catch (IOException e) {
            throw new IllegalArgumentException("While parsing console layout info for "
                    + SyncopeConsoleSession.get().getSelfTO().getUsername(), e);
        }
    }

    public static String defaultConsoleLayoutInfoIfEmpty(final String content, final List<String> anyTypes) {
        String result;

        if (StringUtils.isBlank(content)) {
            try {
                ObjectNode tree = MAPPER.createObjectNode();

                tree.set(AnyTypeKind.USER.name(), MAPPER.valueToTree(new UserFormLayoutInfo()));
                tree.set(AnyTypeKind.GROUP.name(), MAPPER.valueToTree(new GroupFormLayoutInfo()));
                for (String anyType : anyTypes) {
                    if (!anyType.equals(AnyTypeKind.USER.name()) && !anyType.equals(AnyTypeKind.GROUP.name())) {
                        tree.set(anyType, MAPPER.valueToTree(new AnyObjectFormLayoutInfo()));
                    }
                }

                result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
            } catch (IOException e) {
                throw new IllegalArgumentException("While generating default console layout info for "
                        + SyncopeConsoleSession.get().getSelfTO().getUsername(), e);
            }
        } else {
            try {
                result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(content));
            } catch (IOException e) {
                result = content;
            }
        }

        return result;
    }

    public static <A extends AnyTO, F extends AnyForm<A>, FL extends AbstractAnyFormLayout<A, F>> F instantiate(
            final A anyTO,
            final List<String> anyTypeClasses,
            final FL anyFormLayout,
            final PageReference pageRef) {

        try {
            if (anyTO instanceof UserTO) {
                return anyFormLayout.getFormClass().getConstructor(
                        anyTO.getClass(), // previous
                        anyTO.getClass(), // actual
                        List.class,
                        anyFormLayout.getClass(),
                        pageRef.getClass()).
                        newInstance(null, anyTO, anyTypeClasses, anyFormLayout, pageRef);
            } else {
                return anyFormLayout.getFormClass().getConstructor(
                        anyTO.getClass(), // actual
                        List.class,
                        anyFormLayout.getClass(),
                        pageRef.getClass()).
                        newInstance(anyTO, anyTypeClasses, anyFormLayout, pageRef);
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate " + anyFormLayout.getFormClass().getName(), e);
        }
    }

    private FormLayoutInfoUtils() {
        // private constructor for static utility class
    }
}
