package com.chinamobile.cmos.sms;

import com.chinamobile.cmos.util.StringUtil;

public class SmsUnkownTypeMessage implements SmsMessage {

	private byte[] ud;
	private byte dcs ;
	
	public SmsUnkownTypeMessage(byte dcs,byte[] ud) {
		this.ud = ud;
		this.dcs = dcs;
	}
	@Override
	public SmsPdu[] getPdus() {
		SmsUserData sud = new SmsUserData(ud,ud.length,new SmsDcs(dcs));
		return new SmsPdu[] {new SmsPdu(new SmsUdhElement[] {},sud)};
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SmsUnkownTypeMessage:0x").append(StringUtil.bytesToHexString(ud));
		return sb.toString();
	}
	
	
}
