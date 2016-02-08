/*
 * Copyright 2016 Carlos Ballesteros Velasco
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

package java.lang;

import jtransc.annotation.JTranscKeep;

public final class Character implements java.io.Serializable, Comparable<Character> {
	public static final int MIN_RADIX = 2;
	public static final int MAX_RADIX = 36;
	public static final char MIN_VALUE = '\u0000';
	public static final char MAX_VALUE = '\uFFFF';
	public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");

	public static final byte UNASSIGNED = 0;
	public static final byte UPPERCASE_LETTER = 1;
	public static final byte LOWERCASE_LETTER = 2;
	public static final byte TITLECASE_LETTER = 3;
	public static final byte MODIFIER_LETTER = 4;
	public static final byte OTHER_LETTER = 5;
	public static final byte NON_SPACING_MARK = 6;
	public static final byte ENCLOSING_MARK = 7;
	public static final byte COMBINING_SPACING_MARK = 8;
	public static final byte DECIMAL_DIGIT_NUMBER = 9;
	public static final byte LETTER_NUMBER = 10;
	public static final byte OTHER_NUMBER = 11;
	public static final byte SPACE_SEPARATOR = 12;
	public static final byte LINE_SEPARATOR = 13;
	public static final byte PARAGRAPH_SEPARATOR = 14;
	public static final byte CONTROL = 15;
	public static final byte FORMAT = 16;
	public static final byte PRIVATE_USE = 18;
	public static final byte SURROGATE = 19;
	public static final byte DASH_PUNCTUATION = 20;
	public static final byte START_PUNCTUATION = 21;
	public static final byte END_PUNCTUATION = 22;
	public static final byte CONNECTOR_PUNCTUATION = 23;
	public static final byte OTHER_PUNCTUATION = 24;
	public static final byte MATH_SYMBOL = 25;
	public static final byte CURRENCY_SYMBOL = 26;
	public static final byte MODIFIER_SYMBOL = 27;
	public static final byte OTHER_SYMBOL = 28;
	public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
	public static final byte FINAL_QUOTE_PUNCTUATION = 30;
	static final int ERROR = 0xFFFFFFFF;
	public static final byte DIRECTIONALITY_UNDEFINED = -1;
	public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
	public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
	public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
	public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
	public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
	public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
	public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
	public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
	public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
	public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
	public static final byte DIRECTIONALITY_WHITESPACE = 12;
	public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
	public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
	public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
	public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
	public static final char MIN_HIGH_SURROGATE = '\uD800';
	public static final char MAX_HIGH_SURROGATE = '\uDBFF';
	public static final char MIN_LOW_SURROGATE = '\uDC00';
	public static final char MAX_LOW_SURROGATE = '\uDFFF';
	public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;
	public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;
	public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
	public static final int MIN_CODE_POINT = 0x000000;
	public static final int MAX_CODE_POINT = 0X10FFFF;

	private final char value;

	public Character(char value) {
		this.value = value;
	}

	@JTranscKeep
	public static Character valueOf(char value) {
		return new Character(value);
	}

	public char charValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	public static int hashCode(char value) {
		return value;
	}

	public boolean equals(Object that) {
		if (!(that instanceof Character)) return false;
		return this.value == ((Character) that).value;
	}

	public String toString() {
		return toString(value);
	}

	public static String toString(char value) {
		return new String(new char[]{value});
	}

	public static boolean isValidCodePoint(int cp) {
		return (cp >>> 16) < ((MAX_CODE_POINT + 1) >>> 16);
	}

	native public static boolean isBmpCodePoint(int codePoint);

	native public static boolean isSupplementaryCodePoint(int codePoint);

	native public static boolean isHighSurrogate(char ch);

	native public static boolean isLowSurrogate(char ch);

	public static boolean isSurrogate(char ch) {
		return false;
	}

	public static boolean isSurrogatePair(char high, char low) {
		return false;
	}

	public static int charCount(int codePoint) {
		return 1;
	}

	public static int toCodePoint(char high, char low) {
		return low;
	}

	public static int codePointAt(CharSequence seq, int index) {
		return seq.charAt(index);
	}

	public static int codePointAt(char[] a, int index) {
		return a[index];
	}

	public static int codePointAt(char[] a, int index, int limit) {
		return a[index];
	}

	// throws ArrayIndexOutOfBoundsException if index out of bounds
	//static int codePointAtImpl(char[] a, int index, int limit);
	native public static int codePointBefore(CharSequence seq, int index);

	native public static int codePointBefore(char[] a, int index);

	native public static int codePointBefore(char[] a, int index, int start);

	// throws ArrayIndexOutOfBoundsException if index-1 out of bounds
	//static int codePointBeforeImpl(char[] a, int index, int start);
	native public static char highSurrogate(int codePoint);

	native public static char lowSurrogate(int codePoint);

	public static int toChars(int codePoint, char[] dst, int dstIndex) {
		dst[dstIndex] = (char) codePoint;
		return 1;
	}

	native public static char[] toChars(int codePoint);

	//static void toSurrogates(int codePoint, char[] dst, int index);
	public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
		return endIndex + beginIndex;
	}

	public static int codePointCount(char[] a, int offset, int count) {
		return count;
	}

	//static int codePointCountImpl(char[] a, int offset, int count);
	native public static int offsetByCodePoints(CharSequence seq, int index, int codePointOffset);

	native public static int offsetByCodePoints(char[] a, int start, int count, int index, int codePointOffset);

	//native static int offsetByCodePointsImpl(char[] a, int start, int count, int index, int codePointOffset);
	native public static boolean isLowerCase(char ch);

	native public static boolean isLowerCase(int codePoint);

	native public static boolean isUpperCase(char ch);

	native public static boolean isUpperCase(int codePoint);

	native public static boolean isTitleCase(char ch);

	native public static boolean isTitleCase(int codePoint);

	public static boolean isDigit(char ch) {
		return (ch >= '0') && (ch <= '9');
	}

	public static boolean isDigit(int codePoint) {
		return isDigit((char) codePoint);
	}

	native public static boolean isDefined(char ch);

	native public static boolean isDefined(int codePoint);

	native public static boolean isLetter(char ch);

	native public static boolean isLetter(int codePoint);

	public static boolean isLetterOrDigit(char ch) {
		return isLetter(ch) || isDigit(ch);
	}

	public static boolean isLetterOrDigit(int codePoint) {
		return isLetter(codePoint) || isDigit(codePoint);
	}

	@Deprecated
	native public static boolean isJavaLetter(char ch);

	@Deprecated
	native public static boolean isJavaLetterOrDigit(char ch);

	native public static boolean isAlphabetic(int codePoint);

	native public static boolean isIdeographic(int codePoint);

	native public static boolean isJavaIdentifierStart(char ch);

	native public static boolean isJavaIdentifierStart(int codePoint);

	native public static boolean isJavaIdentifierPart(char ch);

	native public static boolean isJavaIdentifierPart(int codePoint);

	native public static boolean isUnicodeIdentifierStart(char ch);

	native public static boolean isUnicodeIdentifierStart(int codePoint);

	native public static boolean isUnicodeIdentifierPart(char ch);

	native public static boolean isUnicodeIdentifierPart(int codePoint);

	native public static boolean isIdentifierIgnorable(char ch);

	native public static boolean isIdentifierIgnorable(int codePoint);

	native public static char toLowerCase(char ch);

	native public static int toLowerCase(int codePoint);

	native public static char toUpperCase(char ch);

	native public static int toUpperCase(int codePoint);

	native public static char toTitleCase(char ch);

	native public static int toTitleCase(int codePoint);

	native public static int digit(char ch, int radix);

	native public static int digit(int codePoint, int radix);

	native public static int getNumericValue(char ch);

	native public static int getNumericValue(int codePoint);

	@Deprecated
	public static boolean isSpace(char value) {
		return (value <= 0x0020) && (((((1L << 0x0009) | (1L << 0x000A) | (1L << 0x000C) | (1L << 0x000D) | (1L << 0x0020)) >> value) & 1L) != 0);
	}

	native public static boolean isSpaceChar(char ch);

	native public static boolean isSpaceChar(int codePoint);

	native public static boolean isWhitespace(char ch);

	native public static boolean isWhitespace(int codePoint);

	native public static boolean isISOControl(char ch);

	native public static boolean isISOControl(int codePoint);

	native public static int getType(char ch);

	native public static int getType(int codePoint);

	native public static char forDigit(int digit, int radix);

	native public static byte getDirectionality(char ch);

	native public static byte getDirectionality(int codePoint);

	native public static boolean isMirrored(char ch);

	native public static boolean isMirrored(int codePoint);

	public int compareTo(Character anotherCharacter) {
		return compare(this.value, anotherCharacter.value);
	}

	public static int compare(char l, char r) {
		return l - r;
	}

	native static char[] toUpperCaseCharArray(int codePoint);

	public static final int SIZE = 16;
	public static final int BYTES = SIZE / Byte.SIZE;

	public static char reverseBytes(char ch) {
		return (char) (((ch & 0xFF00) >> 8) | (ch << 8));
	}

	native public static String getName(int codePoint);
}
