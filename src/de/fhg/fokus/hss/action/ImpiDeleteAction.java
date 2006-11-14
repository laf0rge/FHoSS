/*
 * $Id$
 *
 * Copyright (C) 2004-2006 FhG Fokus
 *
 * This file is part of Open IMS Core - an open source IMS CSCFs & HSS
 * implementation
 *
 * Open IMS Core is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * For a license to use the Open IMS Core software under conditions
 * other than those described here, or to purchase support for this
 * software, please contact Fraunhofer FOKUS by e-mail at the following
 * addresses:
 *     info@open-ims.org
 *
 * Open IMS Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * It has to be noted that this Open Source IMS Core System is not
 * intended to become or act as a product in a commercial context! Its
 * sole purpose is to provide an IMS core reference implementation for
 * IMS technology testing and IMS application prototyping for research
 * purposes, typically performed in IMS test-beds.
 *
 * Users of the Open Source IMS Core System have to be aware that IMS
 * technology may be subject of patents and licence terms, as being
 * specified within the various IMS-related IETF, ITU-T, ETSI, and 3GPP
 * standards. Thus all Open IMS Core users have to take notice of this
 * fact and have to agree to check out carefully before installing,
 * using and extending the Open Source IMS Core System, if related
 * patents and licenses may become applicable to the intended usage
 * context. 
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA  
 * 
 */
package de.fhg.fokus.hss.action;

import de.fhg.fokus.hss.form.ImpiForm;
import de.fhg.fokus.hss.model.Impi;
import de.fhg.fokus.hss.model.Impu;

import org.apache.log4j.Logger;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Delete a private user identity from database.
 * 
 * @author Andre Charton (dev -at- open-ims dot org)
 */
public class ImpiDeleteAction extends HssAction
{
	private static final Logger LOGGER = Logger
			.getLogger(ImpiDeleteAction.class);

	public ActionForward execute(ActionMapping mapping, ActionForm actionForm,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception
	{
		LOGGER.debug("entering");

		ActionForward forward = null;
		ImpiForm form = (ImpiForm) actionForm;
		LOGGER.debug(form);

		try
		{
			doDeleteBusiness(form);
			forward = mapping.findForward(FORWARD_SUCCESS);
		} catch (ConstraintViolationException e)
		{
			ActionMessages actionMessages = new ActionMessages();
			actionMessages.add(Globals.MESSAGE_KEY, new ActionMessage(
					"error.constraint"));
			saveMessages(request, actionMessages);
			forward = mapping.findForward(FORWARD_FAILURE);
		}

		LOGGER.debug("exiting");

		return forward;
	}

	/**
	 * Delete a private user identity from database and disconnect impus.
	 * 
	 * @param form
	 */
	private void doDeleteBusiness(ImpiForm form)
			throws ConstraintViolationException
	{
		try
		{
			beginnTx();

			Integer primaryKey = form.getPrimaryKey();
			Impi impi = null;

			impi = (Impi) getSession().load(Impi.class, primaryKey);

			// Disconnect impus, if necessary unregister impu.
			Iterator it = impi.getImpus().iterator();

			while (it.hasNext())
			{
				Impu impu = (Impu) it.next();
				impu.getImpis().remove(impi);

				if (impu.getImpis().isEmpty())
				{
					impu.setUserStatus(Impu.USER_STATUS_NOT_REGISTERED);
				}

				getSession().update(impu);
			}

			getSession().delete(impi);
			endTx();
		} catch (ConstraintViolationException e)
		{
			LOGGER.warn(this, e);
			throw e;
		} finally
		{
			closeSession();
		}
	}
}
