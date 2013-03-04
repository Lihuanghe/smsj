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
package org.marre.sms.brew;

import org.marre.sms.*;

/**
 * Brew directed SMS message (BDSMS).
 * 
 * I haven't tried this myself but someone might find it useful. I'm not even
 * sure if there are any BREW enabled GSM devices.
 * 
 * See these links for more information.
 * http://brewforums.qualcomm.com/showthread.php?t=6543&highlight=brew+directed
 * http://www.simplewire.com/downloads/download.html?DOWNLOAD_ID=1165 
 * 
 * @author Markus
 * @version $Id$
 */
public class SmsBrewDirectedMessage implements SmsMessage
{
    private final String text_;
    private final String classId_;
    
    /**
     * Creates a BREW directed SMS message.
     * 
     * @param classId ClassId for the receiving app. Example: "0x00000000"
     * @param text Text to send.
     */
    public SmsBrewDirectedMessage(String classId, String text)
    {
        classId_ = classId;
        text_ = text;
    }

    /**
     * Returns the text message. 
     */
    public String getText()
    {
        return text_;
    }
    
    /**
     * Returns the classId. 
     */
    public String getClassId()
    {
        return classId_;
    }
        
    public SmsPdu[] getPdus()
    {
        String bdsmsText = "//BREW:" + classId_ + ":" + text_;
        
        SmsUserData userData = 
            new SmsUserData(SmsPduUtil.getSeptets(bdsmsText), bdsmsText.length(),
                SmsDcs.getGeneralDataCodingDcs(SmsAlphabet.LATIN1, SmsMsgClass.CLASS_UNKNOWN));
        
        return new SmsPdu[] {
                new SmsPdu(null, userData)
        };
    }
}