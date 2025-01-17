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

package org.eclipse.dataspaceconnector.identityhub.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtServiceImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IdentityHubCredentialsVerifierTest {

    private static final Faker FAKER = new Faker();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HUB_BASE_URL = "https://" + FAKER.internet().url();
    private static final DidDocument DID_DOCUMENT = DidDocument.Builder.newInstance()
            .service(List.of(new Service("IdentityHub", "IdentityHub", HUB_BASE_URL))).build();
    private static final String ISSUER = FAKER.internet().url();
    private static final String SUBJECT = FAKER.internet().url();
    private Monitor monitorMock = mock(Monitor.class);
    private IdentityHubClient identityHubClientMock = mock(IdentityHubClient.class);
    private JwtCredentialsVerifier jwtCredentialsVerifierMock = mock(JwtCredentialsVerifier.class);
    private VerifiableCredentialsJwtServiceImpl verifiableCredentialsJwtService = new VerifiableCredentialsJwtServiceImpl(OBJECT_MAPPER);
    private CredentialsVerifier credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClientMock, monitorMock, jwtCredentialsVerifierMock, verifiableCredentialsJwtService);

    @Test
    public void getVerifiedClaims_getValidClaims() throws Exception {

        // Arrange
        var credential = generateVerifiableCredential();
        var jws = buildSignedJwt(credential, ISSUER, SUBJECT, generateEcKey());
        setUpMocks(jws, true, true);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        // Assert
        assertThat(credentials.succeeded()).isTrue();
        assertThat(credentials.getContent())
                .usingRecursiveComparison()
                .isEqualTo(toMap(credential, ISSUER, SUBJECT));
    }

    private void setUpMocks(SignedJWT jws, boolean isSigned, boolean claimsValid) {
        when(identityHubClientMock.getVerifiableCredentials(HUB_BASE_URL)).thenReturn(StatusResult.success(List.of(jws)));
        when(jwtCredentialsVerifierMock.isSignedByIssuer(jws)).thenReturn(isSigned);
        when(jwtCredentialsVerifierMock.verifyClaims(eq(jws), any())).thenReturn(claimsValid);
    }

    @Test
    public void getVerifiedClaims_filtersSignedByWrongIssuer() throws Exception {

        // Arrange
        var credential = generateVerifiableCredential();
        var jws = buildSignedJwt(credential, ISSUER, SUBJECT, generateEcKey());
        setUpMocks(jws, true, false);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        // Assert
        assertThat(credentials.succeeded()).isTrue();
        assertThat(credentials.getContent().size()).isEqualTo(0);
    }

    @Test
    public void getVerifiedClaims_hubUrlNotResolved() {
        // Arrange
        var didDocument = DidDocument.Builder.newInstance().build();

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(didDocument);

        // Assert
        assertThat(credentials.failed()).isTrue();
        assertThat(credentials.getFailureMessages()).containsExactly("Failed getting Identity Hub URL");
    }

    @Test
    public void getVerifiedClaims_idHubCallFails() {

        // Arrange
        when(identityHubClientMock.getVerifiableCredentials(HUB_BASE_URL)).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR));

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        // Assert
        assertThat(credentials.failed()).isTrue();
    }

    @Test
    public void getVerifiedClaims_verifiableCredentialsWithWrongFormat() {

        // Arrange
        var jws = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), new JWTClaimsSet.Builder().build());
        setUpMocks(jws, true, true);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        // Assert
        assertThat(credentials.succeeded()).isTrue();
        assertThat(credentials.getContent().isEmpty());
        verify(monitorMock, times(1)).warning(anyString());

    }

    @Test
    public void getVerifiedClaims_verifiableCredentialsWithMissingId() {

        // Arrange
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var jwtClaims = new JWTClaimsSet.Builder()
                .claim("vc", Map.of(FAKER.lorem().word(), FAKER.lorem().word()))
                .issuer(FAKER.lorem().word())
                .subject(SUBJECT)
                .build();
        var jws = new SignedJWT(jwsHeader, jwtClaims);
        setUpMocks(jws, true, true);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        // Assert
        assertThat(credentials.succeeded()).isTrue();
        assertThat(credentials.getContent().isEmpty());
        verify(monitorMock, times(1)).warning(anyString());
    }
}
