package de.stadtrallye.rallyesoft.model.comm;

import java.nio.charset.Charset;

import android.annotation.TargetApi;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;

public class NfcCallback implements CreateNdefMessageCallback {

	@Override
	@TargetApi(14)
	public NdefMessage createNdefMessage(NfcEvent event) {
		String text = "BeamTest";
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] {
                		new NdefRecord(
                				NdefRecord.TNF_MIME_MEDIA, "application/de.stadtrallye.rallyesoft".getBytes(Charset.forName("US-ASCII")),
                				new byte[0],
                				text.getBytes(Charset.forName("US_ASCII"))),
                		NdefRecord.createApplicationRecord("de.stadtrallye.rallyesoft")
        });
        return msg;
	}

}
