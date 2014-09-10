/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *
 * Contributors:
 *   mberhaut1
 *    
 */
package fr.toutatice.ecm.platform.core.helper;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.platform.jbpm.core.helper.LifecycleTransitionActionHandler;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;

public class ToutaticeLifecycleTransitionActionHandler extends LifecycleTransitionActionHandler {

	private static final long serialVersionUID = 1L;
	
    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
        	String endLifecycle = getEndLifecycleTransition();
        	if (!ToutaticeNuxeoStudioConst.CST_OPERATION_PARAM_NO_TRANSITION.equals(endLifecycle)) {
        		super.execute(executionContext);
        	} else {
        		executionContext.getToken().signal();
        	}
        }
    }
    
}
