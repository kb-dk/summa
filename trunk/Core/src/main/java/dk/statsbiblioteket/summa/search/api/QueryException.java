/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.search.api;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;

/**
 * Search problems caused by the Query which does not affect general Searcher availability.
 * </p><p>
 * This Exception is intended as a signaling mechanism over RMI and should not normally contain a clause.
 * If a clause is added, it should be part of plain Java, not a third party Exception.
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryException extends RemoteException {
    private String component;
    private String query;
    private QueryException clause = null;

    /**
     * Base constructor for query-related Exceptions.
     * @param component the component responsible for the exception.
     * @param message   the cause of the Exception.
     * @param query     the query if available else null.
     */
    public QueryException(String component, String message, String query) {
        super(message);
        this.component = component;
        this.query = query;
    }

    /**
     * Chaining of QueryExceptions.
     * @param component the component responsible for the exception.
     * @param e         the inner query Exception.
     */
    public QueryException(String component, QueryException e) {
        super("");
        this.component = component;
        this.query = null;
        clause = e;
    }

    public QueryException getClause() {
        return clause;
    }

    /**
     * @return the query that caused the exception or null if not applicable.
     */
    public String getQuery() {
        return query;
    }
    public String getQuery(QueryException e) {
        if (query != null) {
            return query;
        }
        if (e.getCause() == null || !(e.getCause() instanceof QueryException)) {
            return null;
        }
        return getQuery((QueryException)e.getCause());
    }

    /**
     * @return the Search component responsible for the exception (normally the the throwing class).
     */
    public String getComponent() {
        return component;
    }
    private String getComponent(QueryException e) {
        return String.format("(%s%s)",
                             component,
                             e.getCause() != null && e.getCause() instanceof QueryException
                             ? getComponent((QueryException)e.getCause()) : "");
    }

    @Override
    public String toString() {
        return "QueryException(" + getComponent(this) + "): " + getMessage()
               + (getQuery(this) == null ? "" : " caused by " + getQuery(this));
    }
}
