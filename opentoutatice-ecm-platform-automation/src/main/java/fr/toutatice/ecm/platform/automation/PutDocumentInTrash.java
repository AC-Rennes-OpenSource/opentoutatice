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
package fr.toutatice.ecm.platform.automation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.runtime.api.Framework;


/**
 * @author David Chevrier
 */
@Operation(id = PutDocumentInTrash.ID, category = Constants.CAT_DOCUMENT, label = "PutDocumentInTrash", description = "Put a document in trash.")
public class PutDocumentInTrash {
    
    public static final String ID= "Document.PutDocumentInTrash";
    private static final String ABOUT_TO_REMOVE = "aboutToRemove";
    private static final String DELETE = "delete";
    
    @Context
    protected CoreSession session;
    
    @Param(name = "document", required = true)
    protected DocumentModel document;
    
    @OperationMethod
    public Object run() throws Exception {
        
        EventService eventService = Framework.getService(EventService.class);
        EventContext eventContext = new DocumentEventContext(session, session.getPrincipal(), document); 
        Event event = new EventImpl(ABOUT_TO_REMOVE, eventContext);
        eventService.fireEvent(event);
        
        session.followTransition(document.getRef(), DELETE);
        
        return document;
        
    }

}
