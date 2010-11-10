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
 * CVS:  $Id: TagHandlerFactory.java,v 1.9 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;


/**
 * A simple factory for TagHandlers. The purpose is to make it possible to
 * select TagHandlers based on properties.
 */
@QAInfo(state=QAInfo.State.IN_DEVELOPMENT)
public class TagHandlerFactory {
    private static Logger log = Logger.getLogger(TagHandlerFactory.class);

    /**
     * The class of the TagHandler to use.
     * <p><p>
     * Optional. Default is {@link TagHandlerImpl}.
     */
    public static final String CONF_TAG_HANDLER = "summa.facet.taghandler";
    public static final String DEFAULT_TAG_HANDLER =
            TagHandlerImpl.class.toString();

    /**
     * Construct a TagHandler. Note that the TagHandler does not open any
     * persistent state from storage as part of construction.
     * @param conf      setup for the TagHandler.
     * @param structure definition of Facets et al.
     * @param readOnly  if true, the TagHandler is opened as read only.
     * @return a TagHandler ready for use.
     * @throws RemoteException if the TagHandler could not be created.
     */
    public static TagHandler getTagHandler(Configuration conf,
                                           Structure structure,
                                           Boolean readOnly)
                                                        throws RemoteException {
        Class<? extends TagHandler> tagHandlerClass =
                Configuration.getClass(CONF_TAG_HANDLER, TagHandler.class,
                                       TagHandlerImpl.class, conf);
        log.debug(String.format(
                "Got TagHandler class %s. Creating instance", tagHandlerClass));

        Constructor<? extends TagHandler> con;
        try {
            con = tagHandlerClass.getConstructor(
                    Configuration.class, Structure.class, Boolean.class);
        } catch (NoSuchMethodException e) {
            throw new RemoteException(String.format(
                    "Constructor(Configuration, Structure, Boolean) not found "
                    + "in '%s'", tagHandlerClass), e);
        }
        Exception ex;
        try {
            return con.newInstance(conf, structure, readOnly);
        } catch (InstantiationException e) {
            ex = e;
        } catch (IllegalAccessException e) {
            ex = e;
        } catch (InvocationTargetException e) {
            ex = e;
        }
        //noinspection DuplicateStringLiteralInspection
        throw new RemoteException(String.format(
                "Could not instantiate '%s'", tagHandlerClass), ex);
    }

}




