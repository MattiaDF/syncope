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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.AuditDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogic extends AbstractTransactionalLogic<AuditEntryTO> {

    @Autowired
    private AuditDataBinder binder;

    @Autowired
    private AuditDAO auditDAO;

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_SEARCH + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<AuditEntryTO>> search(
            final String entityKey,
            final int page,
            final int size,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final List<OrderByClause> orderByClauses) {

        Integer count = auditDAO.count(entityKey);
        List<AuditEntry> matching = auditDAO.findByEntityKey(
                entityKey, page, size, type, category, subcategory, events, result, orderByClauses);
        List<AuditEntryTO> searchResults = matching.stream().
                map(auditEntry -> binder.getAuditTO(entityKey, auditEntry)).
                collect(Collectors.toList());
        return Pair.of(count, searchResults);
    }

    @Override
    protected AuditEntryTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }
}
