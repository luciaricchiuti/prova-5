package com.jsoniter;
/**
 * classe di supporto per risolvere il problema di affidabilit� "Avoid using
 * bitwise operators to make comparisons"
 * 
 * @author Francesco
 */
public class SupportBitwise {
	/**
	 * costruttore privato, richiesto da kiuwan
	 */
	private SupportBitwise() {
	}

	/**
	 * 
	 */
	private final static char UNO = '1';
	/**
	 * 
	 */
	private final static int DUE = 2;
	/**
	 * 
	 */
	private final static String ZEROSTRING = "00";
	/**
	 * 
	 */
	private final static String UNOSTRING = "11";

	/**
	 * 
	 * @param bin1
	 * @param bin2
	 * @return
	 */
	public static boolean bitwise(String bin1, String bin2) {
		boolean flag = false;
		int l1 = bin1.length();
		int l2 = bin2.length();
		if (l1 <= l2) {
			for (int i = l1 - 1; i >= 0; i--) {
				l2--;
				flag = cyclomaticComplexity1(bin1, bin2, i, l2);
				if (!(flag)) {
					break;
				}
			}
		} else {
			for (int i = l2 - 1; i >= 0; i--) {
				l1--;
				flag = cyclomaticComplexity1(bin1, bin2, l1, i);
				if (!(flag)) {
					break;
				}
			}
		}
		return flag;
	}

	/**
	 * 
	 * @param long1
	 * @param long2
	 * @param c
	 * @return
	 */
	public static long bitwise(Long long1, Long long2, char c) {
		String newLong = "";
		long l = 0;
		String bin1 = Long.toBinaryString(long1);
		String bin2 = Long.toBinaryString(long2);
		int l1 = bin1.length();
		int l2 = bin2.length();
		if (l1 < l2) {
			bin1 = equalsLength(bin1, bin2);
			l1 = bin1.length();
		} else if (l1 > l2) {
			bin2 = equalsLength(bin1, bin2);
			l2 = bin2.length();
		}
		for (int i = l1 - 1; i >= 0; i--) {
			newLong = riempiBinaryString(bin1, bin2, c, newLong, i, --l2);
		}
		for (int i = newLong.length() - 1; i >= 0; i--) {
			if (newLong.charAt(i) == UNO) {
				l = Long.valueOf(Double.valueOf(Math.pow(DUE, (newLong.length() - 1) - i)).longValue()) + l;
			}
		}
		return l;
	}

	/**
	 * 
	 * @param bin1
	 * @param bin2
	 * @param index1
	 * @param index2
	 * @return
	 */
	private static boolean cyclomaticComplexity1(String bin1, String bin2, int index1, int index2) {
		return ((bin1.charAt(index1) != bin2.charAt(index2))
				|| (charNumericValue(bin1, index1) == 0) && (charNumericValue(bin2, index2) == 0));
	}

	/**
	 * 
	 * @param bin1
	 * @param bin2
	 * @param index1
	 * @param index2
	 * @return
	 */
	private static boolean cyclomaticComplexity2(String bin1, String bin2, int index1, int index2) {
		return ((charNumericValue(bin1, index1) == 0 && charNumericValue(bin2, index2) == 1)
				|| (charNumericValue(bin1, index1) == 1 && charNumericValue(bin2, index2) == 0));
	}

	/**
	 * 
	 * @param bin1
	 * @param bin2
	 * @param index1
	 * @param index2
	 * @param value
	 * @return
	 */
	private static boolean cyclomaticComplexity3(String bin1, String bin2, int index1, int index2, int value) {
		return charNumericValue(bin1, index1) == value && charNumericValue(bin2, index2) == value;
	}

	/**
	 * 
	 * @param bin
	 * @param index
	 * @return
	 */
	private static int charNumericValue(String bin, int index) {
		return Character.getNumericValue(bin.charAt(index));
	}

	/**
	 * 
	 * @param bin1
	 * @param bin2
	 * @param l1
	 * @param l2
	 * @return
	 */
	private static String equalsLength(String bin1, String bin2) {
		String toReturn = "";
		String temp1 = "".concat(bin1);
		String temp2 = "".concat(bin2);
		int l1 = bin1.length();
		int l2 = bin2.length();
		for (int j = Math.max(l1, l2) - Math.min(l1, l2); j >= 0; j--) {
			if (l1 > l2) {
				toReturn = "bin2";
				temp2 = ZEROSTRING.substring(0, 1).concat(temp2);
				l2++;
			} else if (l1 < l2) {
				toReturn = "bin1";
				temp1 = ZEROSTRING.substring(0, 1).concat(temp1);
				l1++;
			}
		}
		if ("bin1".equals(toReturn)) {
			toReturn = temp1;
		} else if ("bin2".equals(toReturn)) {
			toReturn = temp2;
		}
		return toReturn;
	}

	/**
	 * 
	 * @param bin1
	 * @param bin2
	 * @param c
	 * @param newLong
	 * @param index1
	 * @param index2
	 * @return
	 */
	private static String riempiBinaryString(String bin1, String bin2, char c, String newLong, int index1, int index2) {
		String newString = newLong;
		if ((c == '&') && (cyclomaticComplexity2(bin1, bin2, index1, index2))) {
			newString = ZEROSTRING.substring(0, 1).concat(newString);
		} else if ((c == '|') && (cyclomaticComplexity2(bin1, bin2, index1, index2))) {
			newString = UNOSTRING.substring(0, 1).concat(newString);
		}

		if (cyclomaticComplexity3(bin1, bin2, index1, index2, 0)) {
			newString = ZEROSTRING.substring(0, 1).concat(newString);
		}
		if (cyclomaticComplexity3(bin1, bin2, index1, index2, 1)) {
			newString = UNOSTRING.substring(0, 1).concat(newString);
		}
		return newString;
	}

}