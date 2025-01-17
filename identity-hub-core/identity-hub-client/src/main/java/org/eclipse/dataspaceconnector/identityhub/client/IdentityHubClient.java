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

package org.eclipse.dataspaceconnector.identityhub.client;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import java.io.IOException;
import java.util.Collection;

/**
 * IdentityHub Client
 * This client is used to call the IdentityHub endpoints in order query and write VerifiableCredentials.
 * Eventually, this may be expanded to handle other types of objects and operations.
 */
public interface IdentityHubClient {

    /**
     * Get VerifiableCredentials provided by an Identity Hub instance.
     *
     * @param hubBaseUrl Base URL of the IdentityHub instance.
     * @return status result containing VerifiableCredentials if request successful.
     * @throws IOException Signaling that an I/O exception has occurred. For example during JSON serialization or when
     *                     reaching out to the Identity Hub server.
     */
    StatusResult<Collection<SignedJWT>> getVerifiableCredentials(String hubBaseUrl);

    /**
     * Write a VerifiableCredential.
     *
     * @param hubBaseUrl           Base URL of the IdentityHub instance.
     * @param verifiableCredential A verifiable credential to be saved.
     * @return status result.
     * @throws IOException Signaling that an I/O exception has occurred. For example during JSON serialization or when
     *                     reaching out to the Identity Hub server.
     */
    StatusResult<Void> addVerifiableCredential(String hubBaseUrl, SignedJWT verifiableCredential);

}
