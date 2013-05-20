/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

import android.content.Context;

/**
 * Export the Network Monitor data to an HTML file. The HTML file includes CSS
 * specified in the strings XML file.
 */
public class HTMLExport extends FileExport {
	private static final String HTML_FILE = "networkmonitor.html";
	private PrintWriter mPrintWriter;

	public HTMLExport(Context context) throws FileNotFoundException {
		super(context, new File(context.getExternalFilesDir(null), HTML_FILE));
		mPrintWriter = new PrintWriter(mFile);
	}

	@Override
	void writeHeader(String[] columnNames) {
		mPrintWriter.println("<html>");
		mPrintWriter.println("  <head>");
		mPrintWriter.println(mContext.getString(R.string.css));
		mPrintWriter.println("  </head><body>");
		mPrintWriter.println("<table><thead>");

		mPrintWriter.println("  <tr>");
		for (String columnName : columnNames) {
			columnName = columnName.replaceAll("_", " ");
			mPrintWriter.println("    <th>" + columnName + "</th>");
		}
		mPrintWriter.println("  </tr></thead><tbody>");
	}

	@Override
	void writeRow(int rowNumber, String[] cellValues) {
		// Alternating styles for odd and even rows.
		String tdClass = "odd";
		if (rowNumber % 2 == 0)
			tdClass = "even";
		mPrintWriter.println("  <tr class=\"" + tdClass + "\">");
		
		for (String cellValue : cellValues) {
			// Highlight PASS in green and FAIL in red.
			if (Constants.CONNECTION_TEST_FAIL.equals(cellValue))
				mPrintWriter.println("    <td class=\"fail\">" + cellValue
						+ "</td>");
			else if (Constants.CONNECTION_TEST_PASS.equals(cellValue))
				mPrintWriter.println("    <td class=\"pass\">" + cellValue
						+ "</td>");
			else
				mPrintWriter.println("    <td>" + cellValue + "</td>");
		}
		mPrintWriter.println("  </tr>");
		mPrintWriter.flush();
	}

	@Override
	void writeFooter() {
		mPrintWriter.println("</tbody></table>");
		mPrintWriter.println("</body></html>");
		mPrintWriter.flush();
		mPrintWriter.close();
	}
}