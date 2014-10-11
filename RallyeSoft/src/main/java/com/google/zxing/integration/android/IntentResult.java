/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package com.google.zxing.integration.android;

/**
 * <p>Encapsulates the result of a barcode scan invoked through {@link IntentIntegrator}.</p>
 *
 * @author Sean Owen
 */
public final class IntentResult {

  private final String contents;
  private final String formatName;
  private final byte[] rawBytes;
  private final Integer orientation;
  private final String errorCorrectionLevel;

  IntentResult() {
    this(null, null, null, null, null);
  }

  IntentResult(String contents,
               String formatName,
               byte[] rawBytes,
               Integer orientation,
               String errorCorrectionLevel) {
    this.contents = contents;
    this.formatName = formatName;
    this.rawBytes = rawBytes;
    this.orientation = orientation;
    this.errorCorrectionLevel = errorCorrectionLevel;
  }

  /**
   * @return raw content of barcode
   */
  public String getContents() {
    return contents;
  }

  /**
   * @return name of format, like "QR_CODE", "UPC_A". See {@code BarcodeFormat} for more format names.
   */
  public String getFormatName() {
    return formatName;
  }

  /**
   * @return raw bytes of the barcode content, if applicable, or null otherwise
   */
  public byte[] getRawBytes() {
    return rawBytes;
  }

  /**
   * @return rotation of the image, in degrees, which resulted in a successful scan. May be null.
   */
  public Integer getOrientation() {
    return orientation;
  }

  /**
   * @return name of the error correction level used in the barcode, if applicable
   */
  public String getErrorCorrectionLevel() {
    return errorCorrectionLevel;
  }
  
  @Override
  public String toString() {
	  return "Format: "+ formatName +'\n'+
				"Contents: "+ contents +'\n'+
				"Raw bytes: ("+ ((rawBytes == null)? 0 : rawBytes.length) +" bytes)\n"+
				"Orientation: "+ orientation +'\n'+
				"EC level: "+ errorCorrectionLevel +'\n';
  }

}
