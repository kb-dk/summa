/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
/*
 * Copyright (c) 1998 - 2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.control.rmid;

import java.rmi.MarshalledObject;
import java.rmi.Naming;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationGroupDesc;
import java.rmi.activation.ActivationGroup;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.Activatable;
import java.util.Properties;
import java.io.File;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class SetupServices {
    public static final File TESTDIR
            = new File("Control/test/dk/statsbiblioteket/summa/control/rmid")
            .getAbsoluteFile();
    public static final File POLICY = new File(TESTDIR, "java.policy");
    public static final String LOCATION = TESTDIR.toString() + File.separator;

    public SetupServices() throws Exception {

//	    System.setSecurityManager(new RMISecurityManager());

        // Because of the 1.2 security model, a security POLICY should
    	// be specified for the ActivationGroup VM. The first argument
        // to the Properties put method, inherited from Hashtable, is
        // the key and the second is the value
        //
 	    Properties props = new Properties();
        props.put("java.security.policy", POLICY.toString());
        // TODO: Check if -Djava.security.manager is needed here


        ActivationGroupDesc.CommandEnvironment ace = null;
 	    ActivationGroupDesc exampleGroup = new ActivationGroupDesc(props, ace);

    	// Once the ActivationGroupDesc has been created, register it
    	// with the activation system to obtain its ID
    	//
        ActivationGroupID agi =
                   ActivationGroup.getSystem().registerGroup(exampleGroup);

        // The "LOCATION" String specifies a URL from where the class
        // definition will come when this object is requested (activated).
        // Don't forget the trailing slash at the end of the URL
        // or your classes won't be found.
        //
//    	String LOCATION = "file:/home/rmi_tutorial/activation/";

        // Create the rest of the parameters that will be passed to
        // the ActivationDesc constructor
        //
        MarshalledObject data = null;

        // The LOCATION argument to the ActivationDesc constructor will be used
        // to uniquely identify this class; it's LOCATION is relative to the
        // URL-formatted String, LOCATION.
    	//
        ActivationDesc desc = new ActivationDesc(agi,
                         "dk.statsbiblioteket.summa.control.rmid.RemoteHelloImpl",
                         "file:" + LOCATION, data);

        // Register with rmid
        //
        RemoteHello mri = (RemoteHello) Activatable.register(desc);
//        System.out.println("Got the stub for the ActivatableImplementation");

        // Bind the stub to a name in the registry running on 1099
        //
        Naming.rebind("RemoteHello", mri);
//        System.out.println("Exported ActivatableImplementation");
    }

}
