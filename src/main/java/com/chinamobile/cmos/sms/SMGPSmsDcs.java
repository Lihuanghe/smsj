package com.chinamobile.cmos.sms;

public class SMGPSmsDcs extends SmsDcs {
	private static final long serialVersionUID = 1L;

	public SMGPSmsDcs(byte dcs) {
		super(dcs);
	}

	public static SMGPSmsDcs getGeneralDataCodingDcs(SmsAlphabet alphabet, SmsMsgClass messageClass) {
		SmsDcs smsDcs = SmsDcs.getGeneralDataCodingDcs(alphabet,messageClass);
		return new SMGPSmsDcs(smsDcs.getValue());
	}
	
	public int getMaxMsglength() {
		switch (getAlphabet()) {
		default:
			return 140;
		}
	}
	@Override
	public SMGPSmsDcs create(SmsAlphabet alphabet, SmsMsgClass messageClass) {
		return SMGPSmsDcs.getGeneralDataCodingDcs(alphabet, messageClass);
	}
}
