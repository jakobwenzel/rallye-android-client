package de.stadtrallye.rallyesoft.model.converters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helpers to enable saving and reading Set<String> to and from {@link android.content.SharedPreferences}, although the actual method is not available before API 11
 */
public class PreferenceConverters {

	public static Set<String> fromSingleString(String s) {
		HashSet<String> res = new HashSet<String>();

		if (s.length() > 0) {
			String[] str = s.split("\\|");
			Collections.addAll(res, str);
		}
		return res;
	}

	public static String toSingleString(Set<String> set) {
		StringBuilder sb = new StringBuilder();

		for (String s :set) {
			sb.append(s).append('|');
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}


}
