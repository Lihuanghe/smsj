/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is "SMS Library for the Java platform".
 *
 * The Initial Developer of the Original Code is Markus Eriksson.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.marre.mms.transport.mm1;

import java.io.*;
import java.net.*;
import java.util.Properties;

import org.marre.mime.MimeBodyPart;
import org.marre.mime.MimeHeader;
import org.marre.mime.MimeMultipart;
import org.marre.mime.encoder.wap.WapMimeEncoder;
import org.marre.mms.MmsException;
import org.marre.mms.transport.MmsTransport;
import org.marre.wap.MmsHeaderEncoder;
import org.marre.wap.util.WspUtil;

/**
 * 
 *
 * @author Markus Eriksson
 * @version $Id$
 */
public class Mm1Transport implements MmsTransport
{
	public static final String CONTENT_TYPE_WAP_MMS_MESSAGE = "application/vnd.wap.mms-message";
	
    /**
     * URL for the proxy gateway
     */
    private String myMmsProxyGatewayAddress = null;

	/**
	 * Mime encoder
	 */
	private WapMimeEncoder myWapMimeEncoder = new WapMimeEncoder();

    /**
     * @see org.marre.mms.transport.MmsTransport#init(java.util.Properties)
     */
    public void init(Properties theProps) throws MmsException
    {
        myMmsProxyGatewayAddress = theProps.getProperty("smsj.mm1.proxygateway");

        if (myMmsProxyGatewayAddress == null)
        {
            throw new MmsException("sms.mm1.proxygateway not set");
        }
    }

    /**
     * @see org.marre.mms.transport.MmsTransport#connect()
     */
    public void connect() 
    	throws MmsException
    {
    }

	/**
	 * @see org.marre.mms.transport.MmsTransport#send(org.marre.mime.MimeBodyPart, org.marre.mime.MimeHeader[])
	 */
	public void send(MimeBodyPart theMessage, MimeHeader[] theHeaders) 
		throws MmsException 
	{		
		try
		{
			FileOutputStream fos = new FileOutputStream("c:\\mm1.mms");
			writeMessageToStream(fos, theMessage, theHeaders);
			fos.close();
/*			
			// POST
			URL url = new URL(myMmsProxyGatewayAddress);
			URLConnection urlConn = url.openConnection();
			urlConn.setDoOutput(true);
			urlConn.setDoInput(true);
			urlConn.setAllowUserInteraction(false);
			OutputStream out = urlConn.getOutputStream();
		
			writeMessageToStream(out, theMessage, theHeaders);
		
			out.close();

			// Read the response
			InputStream response = urlConn.getInputStream();
			while (response.read() != -1)
				;
			response.close();
*/			
		}
		catch (IOException ex)
		{
			throw new MmsException(ex.getMessage());
		}
	}

	private void writeMessageToStream(OutputStream out, MimeBodyPart theMessage, MimeHeader[] theHeaders)
		throws MmsException
	{
		try
		{
			// Add headers	
			for(int i=0; i < theHeaders.length; i++)
			{
				MmsHeaderEncoder.writeHeader(out, theHeaders[i]);
			}
			
			// Add content-type

            if (theMessage instanceof MimeMultipart)
            {
                // Convert multipart headers...            
                // TODO: Clone content type... We shouldn't change theMsg...
                String ct = theMessage.getContentType().getValue();
                String newCt = WspUtil.convertMultipartContentType(ct);
                theMessage.getContentType().setValue(newCt);
            }
                        
			MmsHeaderEncoder.writeHeaderContentType(out, theMessage.getContentType());
			
			// Add content
			myWapMimeEncoder.writeData(out, theMessage);
		}
		catch (IOException ex)
		{
			throw new MmsException(ex.getMessage());
		}
	}

    /**
     * @see org.marre.mms.transport.MmsTransport#disconnect()
     */
    public void disconnect() throws MmsException
    {
    }
}