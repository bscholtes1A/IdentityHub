/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.store;

import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory store of Hub Objects.
 */
public class InMemoryIdentityHubStore implements IdentityHubStore {

    // Using a Map because concurrent hashset does not exist
    private final Map<byte[], Boolean> hubObjects = new ConcurrentHashMap<>();

    @Override
    public Collection<byte[]> getAll() {
        return new HashSet<>(hubObjects.keySet());
    }

    @Override
    public void add(byte[] hubObject) {
        hubObjects.put(hubObject, true);
    }
}
