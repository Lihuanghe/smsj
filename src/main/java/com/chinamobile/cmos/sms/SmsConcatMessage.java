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
package com.chinamobile.cmos.sms;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chinamobile.cmos.util.StringUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Baseclass for messages that needs to be concatenated.
 * <p>
 * - Only usable for messages that uses the same UDH fields for all message
 * parts. <br>
 * - This class could be better written. There are several parts that are copy-
 * pasted. <br>
 * - The septet coding could be a bit optimized. <br>
 * 
 * @author Markus Eriksson
 * @version $Id$
 */
public abstract class SmsConcatMessage implements SmsMessage {
	private static final Logger logger = LoggerFactory.getLogger(SmsConcatMessage.class);
	private static final AtomicInteger rnd_ = new AtomicInteger((new Random()).nextInt(0xffff));
	
	private static final Integer refNoCacheTimeOut = Integer.valueOf(System.getProperty("refNoCacheTimeOut", "60"));
	// 长短信拆分，要使用 rnd_ 生成统一的长短信ID,但在针对同一个号码，下发多条长短信，并且高并发情况下，随机生成的ID,有机率带来同一个号码
	// ID重复，冲突，造成短信无法展示
	private String seqNoKey;
	
	private static final LoadingCache<String, AtomicInteger> refNoCache = CacheBuilder.newBuilder()
			.expireAfterAccess(refNoCacheTimeOut, TimeUnit.SECONDS).build(new CacheLoader<String, AtomicInteger>() {
				@Override
				public AtomicInteger load(String telephone) throws Exception {
					return new AtomicInteger(rnd_.incrementAndGet());
				}
			});

	/**
	 * Creates an empty SmsConcatMessage.
	 */
	protected SmsConcatMessage() {
		// Empty
	}

	/**
	 * Returns the whole UD
	 * 
	 * @return the UD
	 */
	public abstract SmsUserData getUserData();

	/**
	 * Returns the udh elements
	 * <p>
	 * The returned UDH is the same as specified when the message was created. No
	 * concat headers are added.
	 * 
	 * @return the UDH as SmsUdhElements
	 */
	public abstract SmsUdhElement[] getUdhElements();

	// 考虑在短时间内，针对相同一个号码生成不同的ID
	private int nextRandom() {
		if (StringUtil.isEmpty(seqNoKey))
			return rnd_.incrementAndGet() & 0xffff;
		else {
			AtomicInteger tt = refNoCache.getUnchecked(seqNoKey);
			return tt.incrementAndGet() & 0xffff;
		}
	}

	private SmsPdu[] createPdus(SmsUdhElement[] udhElements, SmsUserData ud, int maxBytes) {
		SmsPdu[] smsPdus = null;
		int nMaxLength = maxBytes;
		boolean use8bit = ud.getDcs().isUse8bit();
		if (ud.getLength() <= nMaxLength ) {
			if(SmsAlphabet.RESERVED ==ud.getDcs().getAlphabet()) {
				//GBK编码，里实际放的是UCS编码的数据，这个再转成GBK
				String ucs2Str = new String(ud.getData(),StandardCharsets.UTF_16BE);
				smsPdus = new SmsPdu[] { new SmsPdu(udhElements, new SmsUserData(ucs2Str.getBytes(SmsPduUtil.GBK), ud.getDcs())) };
			}else {
				smsPdus = new SmsPdu[] { new SmsPdu(udhElements, ud) };
			}
		} else {
			if(SmsAlphabet.GSM == ud.getDcs().getAlphabet()) {
				//如果是GSM编码，要实现7bit编码,如果是长短信，最大字符数再减一
				nMaxLength--;
			}
			// 使用8bit拆分兼容性好 ，16bit可能有些厂商不支持
			List<byte[]> slice = sliceUd(ud, nMaxLength,use8bit);

			int maxSlicLength = slice.size();
			if (maxSlicLength > 255) {
				logger.error("error SmsConcatMessage pkTotal Number {} .should be less than 256", maxSlicLength);
				maxSlicLength = 255;
			}
			smsPdus = new SmsPdu[maxSlicLength];
			// Calculate number of UDHI
			SmsUdhElement[] pduUdhElements = null;
			if (udhElements == null) {
				pduUdhElements = new SmsUdhElement[1];
			} else {
				pduUdhElements = new SmsUdhElement[udhElements.length + 1];
				// Copy the UDH headers
				System.arraycopy(udhElements, 0, pduUdhElements, 1, udhElements.length);
			}

			int refno = nextRandom();
			for (int i = 0; i < smsPdus.length; i++) {
				byte[] t = slice.get(i);
				// Create concat header
				pduUdhElements[0] = use8bit ? SmsUdhUtil.get8BitConcatUdh(refno, smsPdus.length, i + 1): SmsUdhUtil.get16BitConcatUdh(refno, smsPdus.length, i + 1);
				smsPdus[i] = new SmsPdu(pduUdhElements, t, t.length, ud.getDcs());
			}
		}
		return smsPdus;
	}

	private List<byte[]> sliceUd(SmsUserData ud, int maxBytes, boolean use8bit) {
		byte[] udbyte = ud.getData();
		int udLength = udbyte.length;
		int nMaxUdLength = use8bit ? maxBytes - 6 : maxBytes - 7;
		SmsAlphabet udAlphabet = ud.getDcs().getAlphabet();

		int udOffset = 0;
		List<byte[]> slice = new ArrayList<byte[]>();
		while (udOffset < udLength) {
			int oneCopyLength = nMaxUdLength;
			if (udOffset + oneCopyLength >= udLength) {
				oneCopyLength = udLength - udOffset; // 这里是最后一个分片了，长度可能小于nMaxUdLength最大长度
			} else {
				// 检查本次分片的最后一个字节是否为双字节字符，避免一个汉字被拆在两半
				if ((oneCopyLength & 0x01) == 1 && (SmsAlphabet.UCS2 == udAlphabet || SmsAlphabet.RESERVED == udAlphabet)) {
					// 如果是UCS2 ，并且maxBytes是奇数
					oneCopyLength--;
				}else if(SmsAlphabet.GSM == udAlphabet) {
					//GSM编码要避免把转义字符 0x1b 跟后边的字符分开
					byte lastByte = udbyte[udOffset + oneCopyLength-1];
					if(0x1b == lastByte) {
						oneCopyLength--;
					}
				}
			}

			byte[] tmp = new byte[oneCopyLength];
			System.arraycopy(udbyte, udOffset, tmp, 0, oneCopyLength);
			udOffset += oneCopyLength;
			
			if(SmsAlphabet.RESERVED == udAlphabet) {
				//GBK编码，里实际放的是UCS编码的数据，这个再转成GBK
				String ucs2Str = new String(tmp,StandardCharsets.UTF_16BE);
				slice.add(ucs2Str.getBytes(SmsPduUtil.GBK));
			}else {
				slice.add(tmp);
			}
		}
		return slice;
	}

	/**
	 * Converts this message into SmsPdu:s
	 * <p>
	 * If the message is too long to fit in one SmsPdu the message is divided into
	 * many SmsPdu:s with a 8-bit concat pdu UDH element.
	 * 
	 * @return Returns the message as SmsPdu:s
	 */
	public SmsPdu[] getPdus() {
		SmsPdu[] smsPdus;
		SmsUserData ud = getUserData();
		AbstractSmsDcs dcs = ud.getDcs();
		SmsUdhElement[] udhElements = getUdhElements();
		int udhLength = SmsUdhUtil.getTotalSize(udhElements);

		int nBytesLeft = dcs.getMaxMsglength() - udhLength;
		//针对GBK编码，因为短信按字符长度不能超过67，因此特殊处理
		//先按UCS2编码拆分，再转成GBK
		if(dcs.getAlphabet() == SmsAlphabet.RESERVED) {
			byte[] gbkData = ud.getData();
			String originText = new String(gbkData,SmsPduUtil.GBK);
			byte[] ucs2Data = originText.getBytes(StandardCharsets.UTF_16BE);
			ud =  new SmsUserData(ucs2Data, dcs);
			smsPdus = createPdus(udhElements, ud, nBytesLeft);
		}else {
			smsPdus = createPdus(udhElements, ud, nBytesLeft);
		}
		
		return smsPdus;
	}

	public void setSeqNoKey(String seqNoKey) {
		this.seqNoKey = seqNoKey;
	}
}
