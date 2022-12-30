package com.chinamobile.cmos.sms;

public class SmppSmsDcs extends AbstractSmsDcs {
	private static final long serialVersionUID = 1L;
	
	private SmsAlphabet defaultSmsAlphabet = SmsAlphabet.GSM;

	public SmppSmsDcs(byte dcs) {
		super(dcs);
	}

	public SmppSmsDcs(byte dcs,SmsAlphabet defa) {
		super(dcs);
		defaultSmsAlphabet = defa;
	}
	/**
	 * Builds a general-data-coding dcs.
	 *
	 * @param alphabet
	 *            The alphabet.
	 * @param messageClass
	 *            The message class.
	 * 
	 * @return A valid general data coding DCS.
	 */
	public static SmppSmsDcs getGeneralDataCodingDcs(SmsAlphabet alphabet, SmsMsgClass messageClass) {
		byte dcs = 0x00;

		// Bits 3 and 2 indicate the alphabet being used, as follows :
		// Bit3 Bit2 Alphabet:
		// 0 0 Default alphabet
		// 0 1 8 bit data
		// 1 0 UCS2 (16bit) [10]
		// 1 1 Reserved
		switch (alphabet) {
		case ASCII:
			dcs |= 0x01;
			break;
		case GSM:
			dcs |= 0x00;
			break;
		case LATIN1:
			dcs |= 0x04;
			break;
		case UCS2:
			dcs |= 0x08;
			break;
		case RESERVED:
			dcs |= 0x0C;
			break;
		}

		dcs = setMessageClass(dcs,messageClass);

		return new SmppSmsDcs(dcs);
	}

	/**
	 * Decodes the given dcs and returns the alphabet.
	 *
	 * @return Returns the alphabet or NULL if it was not possible to find an
	 *         alphabet for this dcs.
	 */
	public SmsAlphabet getAlphabet() {
		switch (getGroup()) {
		case GENERAL_DATA_CODING:
		case MSG_MARK_AUTO_DELETE:
		case MESSAGE_WAITING_DISCARD:
			// General Data Coding Indication

			switch (dcs_) {
			case 0:
				return defaultSmsAlphabet;
			case 1:
				return SmsAlphabet.ASCII;
			case 3:
				return SmsAlphabet.LATIN1;
			case 4:
				return SmsAlphabet.LATIN1;
			case 8:
				return SmsAlphabet.UCS2;
			case 15:
				return SmsAlphabet.RESERVED;
			}

			switch (dcs_ & 0x0C) {
			case 0x00:
				return defaultSmsAlphabet;
			case 0x04:
				return SmsAlphabet.LATIN1;
			case 0x08:
				return SmsAlphabet.UCS2;
			case 0x0C:
				return SmsAlphabet.RESERVED;
			default:
				return SmsAlphabet.UCS2;
			}

		case MESSAGE_WAITING_STORE_GSM:
			return defaultSmsAlphabet;

		case MESSAGE_WAITING_STORE_UCS2:
			return SmsAlphabet.UCS2;

		case DATA_CODING_MESSAGE:
			switch (dcs_ & 0x04) {
			case 0x00:
				return defaultSmsAlphabet;
			case 0x04:
				return SmsAlphabet.LATIN1;
			default:
				return SmsAlphabet.UCS2;
			}

		default:
			return SmsAlphabet.UCS2;
		}
	}
	
	public int getMaxMsglength() {
		switch(getAlphabet()) {
			case GSM:
				return 159;  //HongKong smpp Maxlength 159
			default:
				return 140;
		}
	}
	
	@Override
	public SmppSmsDcs create(SmsAlphabet alphabet, SmsMsgClass messageClass) {
		SmppSmsDcs dcs =  SmppSmsDcs.getGeneralDataCodingDcs(alphabet, messageClass);
		return new SmppSmsDcs(dcs.getValue(),defaultSmsAlphabet);
	}

}
