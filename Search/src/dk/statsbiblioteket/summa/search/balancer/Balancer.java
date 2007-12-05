/* $Id: Balancer.java,v 1.2 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:18 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search.balancer;

import dk.statsbiblioteket.summa.search.SearchEngine;

/**
 * The search engine load balancer distributes queries to a number of search engines.
 *
 * <p>This interface defines methods for adding and removing search engines to the balancer.</p>
 *
 * <p>This interface extends the dk.statsbiblioteket.summa.search.SearchEngine interface.</p>
 *
 * <p>The methods defined by the SearchEngine interface are requests or get methods
 * and can be performed by any one search engine. The balancer chooses one of the
 * registered search engines, forwards the request and returns the answer.</p>
 *
 * User: bam. Date: Aug 29, 2006.
 */
public interface Balancer extends SearchEngine {

    /**
     * Register the search engine at the given url with the load balancer.
     */
    public void registerSearchModule(String url);

    /**
     * Remove the search engine at the given url from the load balancer engine list.
     */
    public void removeSearchModule(String url);

    //TODO use map implementation from findex
}
