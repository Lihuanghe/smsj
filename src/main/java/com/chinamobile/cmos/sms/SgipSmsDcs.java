package com.chinamobile.cmos.sms;

public class SgipSmsDcs extends SmsDcs {
	private static final long serialVersionUID = 1L;

	public SgipSmsDcs(byte dcs) {
		super(dcs);
	}

	public static SgipSmsDcs getGeneralDataCodingDcs(SmsAlphabet alphabet, SmsMsgClass messageClass) {
		SmsDcs smsDcs = SmsDcs.getGeneralDataCodingDcs(alphabet,messageClass);
		return new SgipSmsDcs(smsDcs.getValue());
	}
	@Override
	
	public SgipSmsDcs create(SmsAlphabet alphabet, SmsMsgClass messageClass) {
		return SgipSmsDcs.getGeneralDataCodingDcs(alphabet, messageClass);
	}
	
	public int getMaxMsglength() {
		switch (getAlphabet()) {
		case ASCII:
			return 140;
		default:
			return 140;
		}
	}
}
