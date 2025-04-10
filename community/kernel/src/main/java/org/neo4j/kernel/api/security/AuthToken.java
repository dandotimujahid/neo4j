/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.security;

import static org.neo4j.internal.helpers.collection.MapUtil.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.kernel.api.security.exception.InvalidAuthTokenException;
import org.neo4j.string.UTF8;

public interface AuthToken {
    String SCHEME_KEY = "scheme";
    String PRINCIPAL = "principal";
    String CREDENTIALS = "credentials";
    String REALM_KEY = "realm";
    String PARAMETERS = "parameters";
    String BASIC_SCHEME = "basic";
    String NATIVE_REALM = "native";

    static String safeCast(String key, Map<String, Object> authToken) throws InvalidAuthTokenException {
        Object value = authToken.get(key);
        if (value == null) {
            throw invalidToken("missing key `" + key + "`");
        } else if (!(value instanceof String)) {
            throw invalidToken("the value associated with the key `" + key + "` must be a String but was: "
                    + value.getClass().getSimpleName());
        }
        return (String) value;
    }

    static byte[] safeCastCredentials(String key, Map<String, Object> authToken) throws InvalidAuthTokenException {
        Object value = authToken.get(key);
        if (value == null) {
            throw invalidToken("missing key `" + key + "`");
        } else if (!(value instanceof byte[])) {
            throw invalidToken(
                    "the value associated with the key `" + key + "` must be a UTF-8 encoded string but was: "
                            + value.getClass().getSimpleName());
        }
        return (byte[]) value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> safeCastMap(String key, Map<String, Object> authToken) throws InvalidAuthTokenException {
        Object value = authToken.get(key);
        if (value == null) {
            return Collections.emptyMap();
        } else if (value instanceof Map) {
            return (Map<String, Object>) value;
        } else {
            throw InvalidAuthTokenException.valueMustBeMap(key, value.getClass().getSimpleName());
        }
    }

    static boolean containsSensitiveInformation(String key) {
        return CREDENTIALS.equals(key);
    }

    static void clearCredentials(Map<String, Object> authToken) {
        Object credentials = authToken.get(CREDENTIALS);
        if (credentials instanceof byte[]) {
            Arrays.fill((byte[]) credentials, (byte) 0);
        }
    }

    static InvalidAuthTokenException invalidToken(String explanation) {
        if (StringUtils.isNotEmpty(explanation) && !explanation.matches("^[,.:;].*")) {
            explanation = ", " + explanation;
        }
        return InvalidAuthTokenException.unsupportedAuthenticationToken(explanation);
    }

    static Map<String, Object> newBasicAuthToken(String username, byte[] password) {
        return map(AuthToken.SCHEME_KEY, BASIC_SCHEME, AuthToken.PRINCIPAL, username, AuthToken.CREDENTIALS, password);
    }

    static Map<String, Object> newBasicAuthToken(String username, byte[] password, String realm) {
        return map(
                AuthToken.SCHEME_KEY,
                BASIC_SCHEME,
                AuthToken.PRINCIPAL,
                username,
                AuthToken.CREDENTIALS,
                password,
                AuthToken.REALM_KEY,
                realm);
    }

    static Map<String, Object> newCustomAuthToken(String principle, byte[] credentials, String realm, String scheme) {
        return map(
                AuthToken.SCHEME_KEY,
                scheme,
                AuthToken.PRINCIPAL,
                principle,
                AuthToken.CREDENTIALS,
                credentials,
                AuthToken.REALM_KEY,
                realm);
    }

    static Map<String, Object> newCustomAuthToken(
            String principle, byte[] credentials, String realm, String scheme, Map<String, Object> parameters) {
        return map(
                AuthToken.SCHEME_KEY,
                scheme,
                AuthToken.PRINCIPAL,
                principle,
                AuthToken.CREDENTIALS,
                credentials,
                AuthToken.REALM_KEY,
                realm,
                AuthToken.PARAMETERS,
                parameters);
    }

    // For testing purposes only
    static Map<String, Object> newBasicAuthToken(String username, String password) {
        return newBasicAuthToken(username, UTF8.encode(password));
    }

    // For testing purposes only
    static Map<String, Object> newBasicAuthToken(String username, String password, String realm) {
        return newBasicAuthToken(username, UTF8.encode(password), realm);
    }

    // For testing purposes only
    static Map<String, Object> newCustomAuthToken(String principle, String credentials, String realm, String scheme) {
        return newCustomAuthToken(principle, UTF8.encode(credentials), realm, scheme);
    }

    // For testing purposes only
    static Map<String, Object> newCustomAuthToken(
            String principle, String credentials, String realm, String scheme, Map<String, Object> parameters) {
        return newCustomAuthToken(principle, UTF8.encode(credentials), realm, scheme, parameters);
    }
}
