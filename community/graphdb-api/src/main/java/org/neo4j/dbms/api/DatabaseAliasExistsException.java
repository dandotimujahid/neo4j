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
package org.neo4j.dbms.api;

import org.neo4j.annotations.api.PublicApi;
import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.kernel.api.exceptions.Status;

/**
 * A {@link DatabaseManagementService} tried to drop a database with an alias.
 */
@PublicApi
public class DatabaseAliasExistsException extends DatabaseManagementException {

    /**
     * @deprecated replaced by {@link #DatabaseAliasExistsException(ErrorGqlStatusObject)}
     */
    @Deprecated(forRemoval = true, since = "5.26")
    public DatabaseAliasExistsException() {
        super();
    }

    public DatabaseAliasExistsException(ErrorGqlStatusObject gqlStatusObject) {
        super(gqlStatusObject);
    }

    /**
     * @deprecated replaced by {@link #DatabaseAliasExistsException(ErrorGqlStatusObject, String)}
     */
    @Deprecated(forRemoval = true, since = "5.26")
    public DatabaseAliasExistsException(String message) {
        super(message);
    }

    public DatabaseAliasExistsException(ErrorGqlStatusObject gqlStatusObject, String message) {
        super(gqlStatusObject, message);
    }

    /**
     * @deprecated replaced by {@link #DatabaseAliasExistsException(ErrorGqlStatusObject, String, Throwable)}
     */
    @Deprecated(forRemoval = true, since = "5.26")
    public DatabaseAliasExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseAliasExistsException(ErrorGqlStatusObject gqlStatusObject, String message, Throwable cause) {
        super(gqlStatusObject, message, cause);
    }

    /**
     * @deprecated replaced by {@link #DatabaseAliasExistsException(ErrorGqlStatusObject, Throwable)}
     */
    @Deprecated(forRemoval = true, since = "5.26")
    public DatabaseAliasExistsException(Throwable cause) {
        super(cause);
    }

    public DatabaseAliasExistsException(ErrorGqlStatusObject gqlStatusObject, Throwable cause) {
        super(gqlStatusObject, cause);
    }

    @Override
    public Status status() {
        return Status.Database.ExistingAliasFound;
    }
}
