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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: CoreMapFactory.java,v 1.5 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapFactory {
    private static Logger log = Logger.getLogger(CoreMapFactory.class);

    /**
     * The CoreMap-implementating class that {@link #getCoreMap} returns.
     * </p><p>
     * Optional. Default is {@link CoreMapBitStuffed}.
     */
    public static final String CONF_COREMAP_CLASS =
            "summa.facet.coremap.class";
    public static final Class<? extends CoreMap> DEFAULT_CLASS =
            CoreMapBitStuffed.class;

    public static CoreMap getCoreMap(Configuration conf, Structure structure)
                                                        throws RemoteException {
        Class<? extends CoreMap> coreMapClass =
                Configuration.getClass(CONF_COREMAP_CLASS, CoreMap.class,
                                       DEFAULT_CLASS, conf);
        log.debug(String.format(
                "Got CoreMap class %s. Creating instance", coreMapClass));

        Constructor<? extends CoreMap> con;
        try {
            con = coreMapClass.getConstructor(
                    Configuration.class, Structure.class);
        } catch (NoSuchMethodException e) {
            throw new RemoteException(String.format(
                    "Constructor(Configuration, Structure) not found "
                    + "in '%s'", coreMapClass), e);
        }
        Exception ex;
        try {
            return con.newInstance(conf, structure);
        } catch (InstantiationException e) {
            ex = e;
        } catch (IllegalAccessException e) {
            ex = e;
        } catch (InvocationTargetException e) {
            ex = e;
        }
        //noinspection DuplicateStringLiteralInspection
        throw new RemoteException(String.format(
                "Could not instantiate '%s'", coreMapClass), ex);
    }


}




