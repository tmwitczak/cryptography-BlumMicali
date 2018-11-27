

package megaRollo;

//import sample.MutMatma;

//import MathContext;
//import sample.Matma;
//import sample.MutMatma;
//import RoundingMode;

import java.util.Arrays;

import static megaRollo.Matma.LONG_MASK;

public class DMatma extends Number implements Comparable<DMatma> {
    /**
     * The unscaled value of this DMatma, as returned by {@link
     * #unscaledValue}.
     *
     * @serial
     * @see #unscaledValue
     */
    private final Matma intVal;

    /**
     * The scale of this DMatma, as returned by {@link #scale}.
     *
     * @serial
     * @see #scale
     */
    private final int scale;  // Note: this may have any value, so
    // calculations must be done in longs

    /**
     * The number of decimal digits in this DMatma, or 0 if the
     * number of digits are not known (lookaside information).  If
     * nonzero, the value is guaranteed correct.  Use the precision()
     * method to obtain and set the value if it might be 0.  This
     * field is mutable until set nonzero.
     *
     * @since  1.5
     */
    private transient int precision;

    /**
     * Used to store the canonical string representation, if computed.
     */
    private transient String stringCache;

    /**
     * Sentinel value for {@link #intCompact} indicating the
     * significand information is only available from {@code intVal}.
     */
    static final long INFLATED = Long.MIN_VALUE;

    private static final Matma INFLATED_BIGINT = Matma.valueOf(INFLATED);

    /**
     * If the absolute value of the significand of this DMatma is
     * less than or equal to {@code Long.MAX_VALUE}, the value can be
     * compactly stored in this field and used in computations.
     */
    private final transient long intCompact;

    // All 18-digit base ten strings fit into a long; not all 19-digit
    // strings will
    private static final int MAX_COMPACT_DIGITS = 18;

    /* Appease the serialization gods */
    private static final long serialVersionUID = 6108874887143696463L;

    private static final ThreadLocal<StringBuilderHelper>
            threadLocalStringBuilderHelper = new ThreadLocal<StringBuilderHelper>() {
        @Override
        protected StringBuilderHelper initialValue() {
            return new StringBuilderHelper();
        }
    };

    // Cache of common small DMatma values.
    private static final DMatma zeroThroughTen[] = {
            new DMatma(Matma.ZERO,       0,  0, 1),
            new DMatma(Matma.ONE,        1,  0, 1),
            new DMatma(Matma.valueOf(2), 2,  0, 1),
            new DMatma(Matma.valueOf(3), 3,  0, 1),
            new DMatma(Matma.valueOf(4), 4,  0, 1),
            new DMatma(Matma.valueOf(5), 5,  0, 1),
            new DMatma(Matma.valueOf(6), 6,  0, 1),
            new DMatma(Matma.valueOf(7), 7,  0, 1),
            new DMatma(Matma.valueOf(8), 8,  0, 1),
            new DMatma(Matma.valueOf(9), 9,  0, 1),
            new DMatma(Matma.TEN,        10, 0, 2),
    };

    // Cache of zero scaled by 0 - 15
    private static final DMatma[] ZERO_SCALED_BY = {
            zeroThroughTen[0],
            new DMatma(Matma.ZERO, 0, 1, 1),
            new DMatma(Matma.ZERO, 0, 2, 1),
            new DMatma(Matma.ZERO, 0, 3, 1),
            new DMatma(Matma.ZERO, 0, 4, 1),
            new DMatma(Matma.ZERO, 0, 5, 1),
            new DMatma(Matma.ZERO, 0, 6, 1),
            new DMatma(Matma.ZERO, 0, 7, 1),
            new DMatma(Matma.ZERO, 0, 8, 1),
            new DMatma(Matma.ZERO, 0, 9, 1),
            new DMatma(Matma.ZERO, 0, 10, 1),
            new DMatma(Matma.ZERO, 0, 11, 1),
            new DMatma(Matma.ZERO, 0, 12, 1),
            new DMatma(Matma.ZERO, 0, 13, 1),
            new DMatma(Matma.ZERO, 0, 14, 1),
            new DMatma(Matma.ZERO, 0, 15, 1),
    };

    // Half of Long.MIN_VALUE & Long.MAX_VALUE.
    private static final long HALF_LONG_MAX_VALUE = Long.MAX_VALUE / 2;
    private static final long HALF_LONG_MIN_VALUE = Long.MIN_VALUE / 2;

    // Constants
    /**
     * The value 0, with a scale of 0.
     *
     * @since  1.5
     */
    public static final DMatma ZERO =
            zeroThroughTen[0];

    /**
     * The value 1, with a scale of 0.
     *
     * @since  1.5
     */
    public static final DMatma ONE =
            zeroThroughTen[1];

    /**
     * The value 10, with a scale of 0.
     *
     * @since  1.5
     */
    public static final DMatma TEN =
            zeroThroughTen[10];

    // Constructors

    /**
     * Trusted package private constructor.
     * Trusted simply means if val is INFLATED, intVal could not be null and
     * if intVal is null, val could not be INFLATED.
     */
    DMatma(Matma intVal, long val, int scale, int prec) {
        this.scale = scale;
        this.precision = prec;
        this.intCompact = val;
        this.intVal = intVal;
    }

    /**
     * Translates a character array representation of a
     * {@code DMatma} into a {@code DMatma}, accepting the
     * same sequence of characters as the {@link #DMatma(String)}
     * constructor, while allowing a sub-array to be specified.
     *
     * <p>Note that if the sequence of characters is already available
     * within a character array, using this constructor is faster than
     * converting the {@code char} array to string and using the
     * {@code DMatma(String)} constructor .
     *
     * @param  in {@code char} array that is the source of characters.
     * @param  offset first character in the array to inspect.
     * @param  len number of characters to consider.
     * @throws NumberFormatException if {@code in} is not a valid
     *         representation of a {@code DMatma} or the defined subarray
     *         is not wholly within {@code in}.
     * @since  1.5
     */
    public DMatma(char[] in, int offset, int len) {
        this(in,offset,len, MathContext.UNLIMITED);
    }

    /**
     * Translates a character array representation of a
     * {@code DMatma} into a {@code DMatma}, accepting the
     * same sequence of characters as the {@link #DMatma(String)}
     * constructor, while allowing a sub-array to be specified and
     * with rounding according to the context settings.
     *
     * <p>Note that if the sequence of characters is already available
     * within a character array, using this constructor is faster than
     * converting the {@code char} array to string and using the
     * {@code DMatma(String)} constructor .
     *
     * @param  in {@code char} array that is the source of characters.
     * @param  offset first character in the array to inspect.
     * @param  len number of characters to consider..
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @throws NumberFormatException if {@code in} is not a valid
     *         representation of a {@code DMatma} or the defined subarray
     *         is not wholly within {@code in}.
     * @since  1.5
     */
    public DMatma(char[] in, int offset, int len, MathContext mc) {
        // protect against huge length.
        if (offset + len > in.length || offset < 0)
            throw new NumberFormatException("Bad offset or len arguments for char[] input.");
        // This is the primary string to DMatma constructor; all
        // incoming strings end up here; it uses explicit (inline)
        // parsing for speed and generates at most one intermediate
        // (temporary) object (a char[] array) for non-compact case.

        // Use locals for all fields values until completion
        int prec = 0;                 // record precision value
        int scl = 0;                  // record scale value
        long rs = 0;                  // the compact value in long
        Matma rb = null;         // the inflated value in Matma
        // use array bounds checking to handle too-long, len == 0,
        // bad offset, etc.
        try {
            // handle the sign
            boolean isneg = false;          // assume positive
            if (in[offset] == '-') {
                isneg = true;               // leading minus means negative
                offset++;
                len--;
            } else if (in[offset] == '+') { // leading + allowed
                offset++;
                len--;
            }

            // should now be at numeric part of the significand
            boolean dot = false;             // true when there is a '.'
            long exp = 0;                    // exponent
            char c;                          // current character
            boolean isCompact = (len <= MAX_COMPACT_DIGITS);
            // integer significand array & idx is the index to it. The array
            // is ONLY used when we can't use a compact representation.
            int idx = 0;
            if (isCompact) {
                // First compact case, we need not to preserve the character
                // and we can just compute the value in place.
                for (; len > 0; offset++, len--) {
                    c = in[offset];
                    if ((c == '0')) { // have zero
                        if (prec == 0)
                            prec = 1;
                        else if (rs != 0) {
                            rs *= 10;
                            ++prec;
                        } // else digit is a redundant leading zero
                        if (dot)
                            ++scl;
                    } else if ((c >= '1' && c <= '9')) { // have digit
                        int digit = c - '0';
                        if (prec != 1 || rs != 0)
                            ++prec; // prec unchanged if preceded by 0s
                        rs = rs * 10 + digit;
                        if (dot)
                            ++scl;
                    } else if (c == '.') {   // have dot
                        // have dot
                        if (dot) // two dots
                            throw new NumberFormatException();
                        dot = true;
                    } else if (Character.isDigit(c)) { // slow path
                        int digit = Character.digit(c, 10);
                        if (digit == 0) {
                            if (prec == 0)
                                prec = 1;
                            else if (rs != 0) {
                                rs *= 10;
                                ++prec;
                            } // else digit is a redundant leading zero
                        } else {
                            if (prec != 1 || rs != 0)
                                ++prec; // prec unchanged if preceded by 0s
                            rs = rs * 10 + digit;
                        }
                        if (dot)
                            ++scl;
                    } else if ((c == 'e') || (c == 'E')) {
                        exp = parseExp(in, offset, len);
                        // Next test is required for backwards compatibility
                        if ((int) exp != exp) // overflow
                            throw new NumberFormatException();
                        break; // [saves a test]
                    } else {
                        throw new NumberFormatException();
                    }
                }
                if (prec == 0) // no digits found
                    throw new NumberFormatException();
                // Adjust scale if exp is not zero.
                if (exp != 0) { // had significant exponent
                    scl = adjustScale(scl, exp);
                }
                rs = isneg ? -rs : rs;
                int mcp = mc.precision;
                int drop = prec - mcp; // prec has range [1, MAX_INT], mcp has range [0, MAX_INT];
                // therefore, this subtract cannot overflow
                if (mcp > 0 && drop > 0) {  // do rounding
                    while (drop > 0) {
                        scl = checkScaleNonZero((long) scl - drop);
                        rs = divideAndRound(rs, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                        prec = longDigitLength(rs);
                        drop = prec - mcp;
                    }
                }
            } else {
                char coeff[] = new char[len];
                for (; len > 0; offset++, len--) {
                    c = in[offset];
                    // have digit
                    if ((c >= '0' && c <= '9') || Character.isDigit(c)) {
                        // First compact case, we need not to preserve the character
                        // and we can just compute the value in place.
                        if (c == '0' || Character.digit(c, 10) == 0) {
                            if (prec == 0) {
                                coeff[idx] = c;
                                prec = 1;
                            } else if (idx != 0) {
                                coeff[idx++] = c;
                                ++prec;
                            } // else c must be a redundant leading zero
                        } else {
                            if (prec != 1 || idx != 0)
                                ++prec; // prec unchanged if preceded by 0s
                            coeff[idx++] = c;
                        }
                        if (dot)
                            ++scl;
                        continue;
                    }
                    // have dot
                    if (c == '.') {
                        // have dot
                        if (dot) // two dots
                            throw new NumberFormatException();
                        dot = true;
                        continue;
                    }
                    // exponent expected
                    if ((c != 'e') && (c != 'E'))
                        throw new NumberFormatException();
                    exp = parseExp(in, offset, len);
                    // Next test is required for backwards compatibility
                    if ((int) exp != exp) // overflow
                        throw new NumberFormatException();
                    break; // [saves a test]
                }
                // here when no characters left
                if (prec == 0) // no digits found
                    throw new NumberFormatException();
                // Adjust scale if exp is not zero.
                if (exp != 0) { // had significant exponent
                    scl = adjustScale(scl, exp);
                }
                // Remove leading zeros from precision (digits count)
                rb = new Matma(coeff, isneg ? -1 : 1, prec);
                rs = compactValFor(rb);
                int mcp = mc.precision;
                if (mcp > 0 && (prec > mcp)) {
                    if (rs == INFLATED) {
                        int drop = prec - mcp;
                        while (drop > 0) {
                            scl = checkScaleNonZero((long) scl - drop);
                            rb = divideAndRoundByTenPow(rb, drop, mc.roundingMode.oldMode);
                            rs = compactValFor(rb);
                            if (rs != INFLATED) {
                                prec = longDigitLength(rs);
                                break;
                            }
                            prec = bigDigitLength(rb);
                            drop = prec - mcp;
                        }
                    }
                    if (rs != INFLATED) {
                        int drop = prec - mcp;
                        while (drop > 0) {
                            scl = checkScaleNonZero((long) scl - drop);
                            rs = divideAndRound(rs, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                            prec = longDigitLength(rs);
                            drop = prec - mcp;
                        }
                        rb = null;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NumberFormatException();
        } catch (NegativeArraySizeException e) {
            throw new NumberFormatException();
        }
        this.scale = scl;
        this.precision = prec;
        this.intCompact = rs;
        this.intVal = rb;
    }

    private int adjustScale(int scl, long exp) {
        long adjustedScale = scl - exp;
        if (adjustedScale > Integer.MAX_VALUE || adjustedScale < Integer.MIN_VALUE)
            throw new NumberFormatException("Scale out of range.");
        scl = (int) adjustedScale;
        return scl;
    }

    /*
     * parse exponent
     */
    private static long parseExp(char[] in, int offset, int len){
        long exp = 0;
        offset++;
        char c = in[offset];
        len--;
        boolean negexp = (c == '-');
        // optional sign
        if (negexp || c == '+') {
            offset++;
            c = in[offset];
            len--;
        }
        if (len <= 0) // no exponent digits
            throw new NumberFormatException();
        // skip leading zeros in the exponent
        while (len > 10 && (c=='0' || (Character.digit(c, 10) == 0))) {
            offset++;
            c = in[offset];
            len--;
        }
        if (len > 10) // too many nonzero exponent digits
            throw new NumberFormatException();
        // c now holds first digit of exponent
        for (;; len--) {
            int v;
            if (c >= '0' && c <= '9') {
                v = c - '0';
            } else {
                v = Character.digit(c, 10);
                if (v < 0) // not a digit
                    throw new NumberFormatException();
            }
            exp = exp * 10 + v;
            if (len == 1)
                break; // that was final character
            offset++;
            c = in[offset];
        }
        if (negexp) // apply sign
            exp = -exp;
        return exp;
    }

    /**
     * Translates a character array representation of a
     * {@code DMatma} into a {@code DMatma}, accepting the
     * same sequence of characters as the {@link #DMatma(String)}
     * constructor.
     *
     * <p>Note that if the sequence of characters is already available
     * as a character array, using this constructor is faster than
     * converting the {@code char} array to string and using the
     * {@code DMatma(String)} constructor .
     *
     * @param in {@code char} array that is the source of characters.
     * @throws NumberFormatException if {@code in} is not a valid
     *         representation of a {@code DMatma}.
     * @since  1.5
     */
    public DMatma(char[] in) {
        this(in, 0, in.length);
    }

    /**
     * Translates a character array representation of a
     * {@code DMatma} into a {@code DMatma}, accepting the
     * same sequence of characters as the {@link #DMatma(String)}
     * constructor and with rounding according to the context
     * settings.
     *
     * <p>Note that if the sequence of characters is already available
     * as a character array, using this constructor is faster than
     * converting the {@code char} array to string and using the
     * {@code DMatma(String)} constructor .
     *
     * @param  in {@code char} array that is the source of characters.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @throws NumberFormatException if {@code in} is not a valid
     *         representation of a {@code DMatma}.
     * @since  1.5
     */
    public DMatma(char[] in, MathContext mc) {
        this(in, 0, in.length, mc);
    }

    /**
     * Translates the string representation of a {@code DMatma}
     * into a {@code DMatma}.  The string representation consists
     * of an optional sign, {@code '+'} (<tt> '&#92;u002B'</tt>) or
     * {@code '-'} (<tt>'&#92;u002D'</tt>), followed by a sequence of
     * zero or more decimal digits ("the integer"), optionally
     * followed by a fraction, optionally followed by an exponent.
     *
     * <p>The fraction consists of a decimal point followed by zero
     * or more decimal digits.  The string must contain at least one
     * digit in either the integer or the fraction.  The number formed
     * by the sign, the integer and the fraction is referred to as the
     * <i>significand</i>.
     *
     * <p>The exponent consists of the character {@code 'e'}
     * (<tt>'&#92;u0065'</tt>) or {@code 'E'} (<tt>'&#92;u0045'</tt>)
     * followed by one or more decimal digits.  The value of the
     * exponent must lie between -{@link Integer#MAX_VALUE} ({@link
     * Integer#MIN_VALUE}+1) and {@link Integer#MAX_VALUE}, inclusive.
     *
     * <p>More formally, the strings this constructor accepts are
     * described by the following grammar:
     * <blockquote>
     * <dl>
     * <dt><i>BigDecimalString:</i>
     * <dd><i>Sign<sub>opt</sub> Significand Exponent<sub>opt</sub></i>
     * <dt><i>Sign:</i>
     * <dd>{@code +}
     * <dd>{@code -}
     * <dt><i>Significand:</i>
     * <dd><i>IntegerPart</i> {@code .} <i>FractionPart<sub>opt</sub></i>
     * <dd>{@code .} <i>FractionPart</i>
     * <dd><i>IntegerPart</i>
     * <dt><i>IntegerPart:</i>
     * <dd><i>Digits</i>
     * <dt><i>FractionPart:</i>
     * <dd><i>Digits</i>
     * <dt><i>Exponent:</i>
     * <dd><i>ExponentIndicator SignedInteger</i>
     * <dt><i>ExponentIndicator:</i>
     * <dd>{@code e}
     * <dd>{@code E}
     * <dt><i>SignedInteger:</i>
     * <dd><i>Sign<sub>opt</sub> Digits</i>
     * <dt><i>Digits:</i>
     * <dd><i>Digit</i>
     * <dd><i>Digits Digit</i>
     * <dt><i>Digit:</i>
     * <dd>any character for which {@link Character#isDigit}
     * returns {@code true}, including 0, 1, 2 ...
     * </dl>
     * </blockquote>
     *
     * <p>The scale of the returned {@code DMatma} will be the
     * number of digits in the fraction, or zero if the string
     * contains no decimal point, subject to adjustment for any
     * exponent; if the string contains an exponent, the exponent is
     * subtracted from the scale.  The value of the resulting scale
     * must lie between {@code Integer.MIN_VALUE} and
     * {@code Integer.MAX_VALUE}, inclusive.
     *
     * <p>The character-to-digit mapping is provided by {@link
     * Character#digit} set to convert to radix 10.  The
     * String may not contain any extraneous characters (whitespace,
     * for example).
     *
     * <p><b>Examples:</b><br>
     * The value of the returned {@code DMatma} is equal to
     * <i>significand</i> &times; 10<sup>&nbsp;<i>exponent</i></sup>.
     * For each string on the left, the resulting representation
     * [{@code Matma}, {@code scale}] is shown on the right.
     * <pre>
     * "0"            [0,0]
     * "0.00"         [0,2]
     * "123"          [123,0]
     * "-123"         [-123,0]
     * "1.23E3"       [123,-1]
     * "1.23E+3"      [123,-1]
     * "12.3E+7"      [123,-6]
     * "12.0"         [120,1]
     * "12.3"         [123,1]
     * "0.00123"      [123,5]
     * "-1.23E-12"    [-123,14]
     * "1234.5E-4"    [12345,5]
     * "0E+7"         [0,-7]
     * "-0"           [0,0]
     * </pre>
     *
     * <p>Note: For values other than {@code float} and
     * {@code double} NaN and &plusmn;Infinity, this constructor is
     * compatible with the values returned by {@link Float#toString}
     * and {@link Double#toString}.  This is generally the preferred
     * way to convert a {@code float} or {@code double} into a
     * DMatma, as it doesn't suffer from the unpredictability of
     * the {@link #DMatma(double)} constructor.
     *
     * @param val String representation of {@code DMatma}.
     *
     * @throws NumberFormatException if {@code val} is not a valid
     *         representation of a {@code DMatma}.
     */
    public DMatma(String val) {
        this(val.toCharArray(), 0, val.length());
    }

    /**
     * Translates the string representation of a {@code DMatma}
     * into a {@code DMatma}, accepting the same strings as the
     * {@link #DMatma(String)} constructor, with rounding
     * according to the context settings.
     *
     * @param  val string representation of a {@code DMatma}.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @throws NumberFormatException if {@code val} is not a valid
     *         representation of a DMatma.
     * @since  1.5
     */
    public DMatma(String val, MathContext mc) {
        this(val.toCharArray(), 0, val.length(), mc);
    }

    /**
     * Translates a {@code double} into a {@code DMatma} which
     * is the exact decimal representation of the {@code double}'s
     * binary floating-point value.  The scale of the returned
     * {@code DMatma} is the smallest value such that
     * <tt>(10<sup>scale</sup> &times; val)</tt> is an integer.
     * <p>
     * <b>Notes:</b>
     * <ol>
     * <li>
     * The results of this constructor can be somewhat unpredictable.
     * One might assume that writing {@code new DMatma(0.1)} in
     * Java creates a {@code DMatma} which is exactly equal to
     * 0.1 (an unscaled value of 1, with a scale of 1), but it is
     * actually equal to
     * 0.1000000000000000055511151231257827021181583404541015625.
     * This is because 0.1 cannot be represented exactly as a
     * {@code double} (or, for that matter, as a binary fraction of
     * any finite length).  Thus, the value that is being passed
     * <i>in</i> to the constructor is not exactly equal to 0.1,
     * appearances notwithstanding.
     *
     * <li>
     * The {@code String} constructor, on the other hand, is
     * perfectly predictable: writing {@code new DMatma("0.1")}
     * creates a {@code DMatma} which is <i>exactly</i> equal to
     * 0.1, as one would expect.  Therefore, it is generally
     * recommended that the {@linkplain #DMatma(String)
     * <tt>String</tt> constructor} be used in preference to this one.
     *
     * <li>
     * When a {@code double} must be used as a source for a
     * {@code DMatma}, note that this constructor provides an
     * exact conversion; it does not give the same result as
     * converting the {@code double} to a {@code String} using the
     * {@link Double#toString(double)} method and then using the
     * {@link #DMatma(String)} constructor.  To get that result,
     * use the {@code static} {@link #valueOf(double)} method.
     * </ol>
     *
     * @param val {@code double} value to be converted to
     *        {@code DMatma}.
     * @throws NumberFormatException if {@code val} is infinite or NaN.
     */
    public DMatma(double val) {
        this(val,MathContext.UNLIMITED);
    }

    /**
     * Translates a {@code double} into a {@code DMatma}, with
     * rounding according to the context settings.  The scale of the
     * {@code DMatma} is the smallest value such that
     * <tt>(10<sup>scale</sup> &times; val)</tt> is an integer.
     *
     * <p>The results of this constructor can be somewhat unpredictable
     * and its use is generally not recommended; see the notes under
     * the {@link #DMatma(double)} constructor.
     *
     * @param  val {@code double} value to be converted to
     *         {@code DMatma}.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         RoundingMode is UNNECESSARY.
     * @throws NumberFormatException if {@code val} is infinite or NaN.
     * @since  1.5
     */
    public DMatma(double val, MathContext mc) {
        if (Double.isInfinite(val) || Double.isNaN(val))
            throw new NumberFormatException("Infinite or NaN");
        // Translate the double into sign, exponent and significand, according
        // to the formulae in JLS, Section 20.10.22.
        long valBits = Double.doubleToLongBits(val);
        int sign = ((valBits >> 63) == 0 ? 1 : -1);
        int exponent = (int) ((valBits >> 52) & 0x7ffL);
        long significand = (exponent == 0
                ? (valBits & ((1L << 52) - 1)) << 1
                : (valBits & ((1L << 52) - 1)) | (1L << 52));
        exponent -= 1075;
        // At this point, val == sign * significand * 2**exponent.

        /*
         * Special case zero to supress nonterminating normalization and bogus
         * scale calculation.
         */
        if (significand == 0) {
            this.intVal = Matma.ZERO;
            this.scale = 0;
            this.intCompact = 0;
            this.precision = 1;
            return;
        }
        // Normalize
        while ((significand & 1) == 0) { // i.e., significand is even
            significand >>= 1;
            exponent++;
        }
        int scale = 0;
        // Calculate intVal and scale
        Matma intVal;
        long compactVal = sign * significand;
        if (exponent == 0) {
            intVal = (compactVal == INFLATED) ? INFLATED_BIGINT : null;
        } else {
            if (exponent < 0) {
                intVal = Matma.valueOf(5).pow(-exponent).multiply(compactVal);
                scale = -exponent;
            } else { //  (exponent > 0)
                intVal = Matma.valueOf(2).pow(exponent).multiply(compactVal);
            }
            compactVal = compactValFor(intVal);
        }
        int prec = 0;
        int mcp = mc.precision;
        if (mcp > 0) { // do rounding
            int mode = mc.roundingMode.oldMode;
            int drop;
            if (compactVal == INFLATED) {
                prec = bigDigitLength(intVal);
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    intVal = divideAndRoundByTenPow(intVal, drop, mode);
                    compactVal = compactValFor(intVal);
                    if (compactVal != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(intVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                }
                intVal = null;
            }
        }
        this.intVal = intVal;
        this.intCompact = compactVal;
        this.scale = scale;
        this.precision = prec;
    }

    /**
     * Translates a {@code Matma} into a {@code DMatma}.
     * The scale of the {@code DMatma} is zero.
     *
     * @param val {@code Matma} value to be converted to
     *            {@code DMatma}.
     */
    public DMatma(Matma val) {
        scale = 0;
        intVal = val;
        intCompact = compactValFor(val);
    }

    /**
     * Translates a {@code Matma} into a {@code DMatma}
     * rounding according to the context settings.  The scale of the
     * {@code DMatma} is zero.
     *
     * @param val {@code Matma} value to be converted to
     *            {@code DMatma}.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma(Matma val, MathContext mc) {
        this(val,0,mc);
    }

    /**
     * Translates a {@code Matma} unscaled value and an
     * {@code int} scale into a {@code DMatma}.  The value of
     * the {@code DMatma} is
     * <tt>(unscaledVal &times; 10<sup>-scale</sup>)</tt>.
     *
     * @param unscaledVal unscaled value of the {@code DMatma}.
     * @param scale scale of the {@code DMatma}.
     */
    public DMatma(Matma unscaledVal, int scale) {
        // Negative scales are now allowed
        this.intVal = unscaledVal;
        this.intCompact = compactValFor(unscaledVal);
        this.scale = scale;
    }

    /**
     * Translates a {@code Matma} unscaled value and an
     * {@code int} scale into a {@code DMatma}, with rounding
     * according to the context settings.  The value of the
     * {@code DMatma} is <tt>(unscaledVal &times;
     * 10<sup>-scale</sup>)</tt>, rounded according to the
     * {@code precision} and rounding mode settings.
     *
     * @param  unscaledVal unscaled value of the {@code DMatma}.
     * @param  scale scale of the {@code DMatma}.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma(Matma unscaledVal, int scale, MathContext mc) {
        long compactVal = compactValFor(unscaledVal);
        int mcp = mc.precision;
        int prec = 0;
        if (mcp > 0) { // do rounding
            int mode = mc.roundingMode.oldMode;
            if (compactVal == INFLATED) {
                prec = bigDigitLength(unscaledVal);
                int drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    unscaledVal = divideAndRoundByTenPow(unscaledVal, drop, mode);
                    compactVal = compactValFor(unscaledVal);
                    if (compactVal != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(unscaledVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                prec = longDigitLength(compactVal);
                int drop = prec - mcp;     // drop can't be more than 18
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mode);
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                }
                unscaledVal = null;
            }
        }
        this.intVal = unscaledVal;
        this.intCompact = compactVal;
        this.scale = scale;
        this.precision = prec;
    }

    /**
     * Translates an {@code int} into a {@code DMatma}.  The
     * scale of the {@code DMatma} is zero.
     *
     * @param val {@code int} value to be converted to
     *            {@code DMatma}.
     * @since  1.5
     */
    public DMatma(int val) {
        this.intCompact = val;
        this.scale = 0;
        this.intVal = null;
    }

    /**
     * Translates an {@code int} into a {@code DMatma}, with
     * rounding according to the context settings.  The scale of the
     * {@code DMatma}, before any rounding, is zero.
     *
     * @param  val {@code int} value to be converted to {@code DMatma}.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma(int val, MathContext mc) {
        int mcp = mc.precision;
        long compactVal = val;
        int scale = 0;
        int prec = 0;
        if (mcp > 0) { // do rounding
            prec = longDigitLength(compactVal);
            int drop = prec - mcp; // drop can't be more than 18
            while (drop > 0) {
                scale = checkScaleNonZero((long) scale - drop);
                compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
            }
        }
        this.intVal = null;
        this.intCompact = compactVal;
        this.scale = scale;
        this.precision = prec;
    }

    /**
     * Translates a {@code long} into a {@code DMatma}.  The
     * scale of the {@code DMatma} is zero.
     *
     * @param val {@code long} value to be converted to {@code DMatma}.
     * @since  1.5
     */
    public DMatma(long val) {
        this.intCompact = val;
        this.intVal = (val == INFLATED) ? INFLATED_BIGINT : null;
        this.scale = 0;
    }

    /**
     * Translates a {@code long} into a {@code DMatma}, with
     * rounding according to the context settings.  The scale of the
     * {@code DMatma}, before any rounding, is zero.
     *
     * @param  val {@code long} value to be converted to {@code DMatma}.
     * @param  mc the context to use.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma(long val, MathContext mc) {
        int mcp = mc.precision;
        int mode = mc.roundingMode.oldMode;
        int prec = 0;
        int scale = 0;
        Matma intVal = (val == INFLATED) ? INFLATED_BIGINT : null;
        if (mcp > 0) { // do rounding
            if (val == INFLATED) {
                prec = 19;
                int drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    intVal = divideAndRoundByTenPow(intVal, drop, mode);
                    val = compactValFor(intVal);
                    if (val != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(intVal);
                    drop = prec - mcp;
                }
            }
            if (val != INFLATED) {
                prec = longDigitLength(val);
                int drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    val = divideAndRound(val, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                    prec = longDigitLength(val);
                    drop = prec - mcp;
                }
                intVal = null;
            }
        }
        this.intVal = intVal;
        this.intCompact = val;
        this.scale = scale;
        this.precision = prec;
    }

    // Static Factory Methods

    /**
     * Translates a {@code long} unscaled value and an
     * {@code int} scale into a {@code DMatma}.  This
     * {@literal "static factory method"} is provided in preference to
     * a ({@code long}, {@code int}) constructor because it
     * allows for reuse of frequently used {@code DMatma} values..
     *
     * @param unscaledVal unscaled value of the {@code DMatma}.
     * @param scale scale of the {@code DMatma}.
     * @return a {@code DMatma} whose value is
     *         <tt>(unscaledVal &times; 10<sup>-scale</sup>)</tt>.
     */
    public static DMatma valueOf(long unscaledVal, int scale) {
        if (scale == 0)
            return valueOf(unscaledVal);
        else if (unscaledVal == 0) {
            return zeroValueOf(scale);
        }
        return new DMatma(unscaledVal == INFLATED ?
                INFLATED_BIGINT : null,
                unscaledVal, scale, 0);
    }

    /**
     * Translates a {@code long} value into a {@code DMatma}
     * with a scale of zero.  This {@literal "static factory method"}
     * is provided in preference to a ({@code long}) constructor
     * because it allows for reuse of frequently used
     * {@code DMatma} values.
     *
     * @param val value of the {@code DMatma}.
     * @return a {@code DMatma} whose value is {@code val}.
     */
    public static DMatma valueOf(long val) {
        if (val >= 0 && val < zeroThroughTen.length)
            return zeroThroughTen[(int)val];
        else if (val != INFLATED)
            return new DMatma(null, val, 0, 0);
        return new DMatma(INFLATED_BIGINT, val, 0, 0);
    }

    static DMatma valueOf(long unscaledVal, int scale, int prec) {
        if (scale == 0 && unscaledVal >= 0 && unscaledVal < zeroThroughTen.length) {
            return zeroThroughTen[(int) unscaledVal];
        } else if (unscaledVal == 0) {
            return zeroValueOf(scale);
        }
        return new DMatma(unscaledVal == INFLATED ? INFLATED_BIGINT : null,
                unscaledVal, scale, prec);
    }

    static DMatma valueOf(Matma intVal, int scale, int prec) {
        long val = compactValFor(intVal);
        if (val == 0) {
            return zeroValueOf(scale);
        } else if (scale == 0 && val >= 0 && val < zeroThroughTen.length) {
            return zeroThroughTen[(int) val];
        }
        return new DMatma(intVal, val, scale, prec);
    }

    static DMatma zeroValueOf(int scale) {
        if (scale >= 0 && scale < ZERO_SCALED_BY.length)
            return ZERO_SCALED_BY[scale];
        else
            return new DMatma(Matma.ZERO, 0, scale, 1);
    }

    /**
     * Translates a {@code double} into a {@code DMatma}, using
     * the {@code double}'s canonical string representation provided
     * by the {@link Double#toString(double)} method.
     *
     * <p><b>Note:</b> This is generally the preferred way to convert
     * a {@code double} (or {@code float}) into a
     * {@code DMatma}, as the value returned is equal to that
     * resulting from constructing a {@code DMatma} from the
     * result of using {@link Double#toString(double)}.
     *
     * @param  val {@code double} to convert to a {@code DMatma}.
     * @return a {@code DMatma} whose value is equal to or approximately
     *         equal to the value of {@code val}.
     * @throws NumberFormatException if {@code val} is infinite or NaN.
     * @since  1.5
     */
    public static DMatma valueOf(double val) {
        // Reminder: a zero double returns '0.0', so we cannot fastpath
        // to use the constant ZERO.  This might be important enough to
        // justify a factory approach, a cache, or a few private
        // constants, later.
        return new DMatma(Double.toString(val));
    }

    // Arithmetic Operations
    /**
     * Returns a {@code DMatma} whose value is {@code (this +
     * augend)}, and whose scale is {@code max(this.scale(),
     * augend.scale())}.
     *
     * @param  augend value to be added to this {@code DMatma}.
     * @return {@code this + augend}
     */
    public DMatma add(DMatma augend) {
        if (this.intCompact != INFLATED) {
            if ((augend.intCompact != INFLATED)) {
                return add(this.intCompact, this.scale, augend.intCompact, augend.scale);
            } else {
                return add(this.intCompact, this.scale, augend.intVal, augend.scale);
            }
        } else {
            if ((augend.intCompact != INFLATED)) {
                return add(augend.intCompact, augend.scale, this.intVal, this.scale);
            } else {
                return add(this.intVal, this.scale, augend.intVal, augend.scale);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this + augend)},
     * with rounding according to the context settings.
     *
     * If either number is zero and the precision setting is nonzero then
     * the other number, rounded if necessary, is used as the result.
     *
     * @param  augend value to be added to this {@code DMatma}.
     * @param  mc the context to use.
     * @return {@code this + augend}, rounded as necessary.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma add(DMatma augend, MathContext mc) {
        if (mc.precision == 0)
            return add(augend);
        DMatma lhs = this;

        // If either number is zero then the other number, rounded and
        // scaled if necessary, is used as the result.
        {
            boolean lhsIsZero = lhs.signum() == 0;
            boolean augendIsZero = augend.signum() == 0;

            if (lhsIsZero || augendIsZero) {
                int preferredScale = Math.max(lhs.scale(), augend.scale());
                DMatma result;

                if (lhsIsZero && augendIsZero)
                    return zeroValueOf(preferredScale);
                result = lhsIsZero ? doRound(augend, mc) : doRound(lhs, mc);

                if (result.scale() == preferredScale)
                    return result;
                else if (result.scale() > preferredScale) {
                    return stripZerosToMatchScale(result.intVal, result.intCompact, result.scale, preferredScale);
                } else { // result.scale < preferredScale
                    int precisionDiff = mc.precision - result.precision();
                    int scaleDiff     = preferredScale - result.scale();

                    if (precisionDiff >= scaleDiff)
                        return result.setScale(preferredScale); // can achieve target scale
                    else
                        return result.setScale(result.scale() + precisionDiff);
                }
            }
        }

        long padding = (long) lhs.scale - augend.scale;
        if (padding != 0) { // scales differ; alignment needed
            DMatma arg[] = preAlign(lhs, augend, padding, mc);
            matchScale(arg);
            lhs = arg[0];
            augend = arg[1];
        }
        return doRound(lhs.inflated().add(augend.inflated()), lhs.scale, mc);
    }

    /**
     * Returns an array of length two, the sum of whose entries is
     * equal to the rounded sum of the {@code DMatma} arguments.
     *
     * <p>If the digit positions of the arguments have a sufficient
     * gap between them, the value smaller in magnitude can be
     * condensed into a {@literal "sticky bit"} and the end result will
     * round the same way <em>if</em> the precision of the final
     * result does not include the high order digit of the small
     * magnitude operand.
     *
     * <p>Note that while strictly speaking this is an optimization,
     * it makes a much wider range of additions practical.
     *
     * <p>This corresponds to a pre-shift operation in a fixed
     * precision floating-point adder; this method is complicated by
     * variable precision of the result as determined by the
     * MathContext.  A more nuanced operation could implement a
     * {@literal "right shift"} on the smaller magnitude operand so
     * that the number of digits of the smaller operand could be
     * reduced even though the significands partially overlapped.
     */
    private DMatma[] preAlign(DMatma lhs, DMatma augend, long padding, MathContext mc) {
        assert padding != 0;
        DMatma big;
        DMatma small;

        if (padding < 0) { // lhs is big; augend is small
            big = lhs;
            small = augend;
        } else { // lhs is small; augend is big
            big = augend;
            small = lhs;
        }

        /*
         * This is the estimated scale of an ulp of the result; it assumes that
         * the result doesn't have a carry-out on a true add (e.g. 999 + 1 =>
         * 1000) or any subtractive cancellation on borrowing (e.g. 100 - 1.2 =>
         * 98.8)
         */
        long estResultUlpScale = (long) big.scale - big.precision() + mc.precision;

        /*
         * The low-order digit position of big is big.scale().  This
         * is true regardless of whether big has a positive or
         * negative scale.  The high-order digit position of small is
         * small.scale - (small.precision() - 1).  To do the full
         * condensation, the digit positions of big and small must be
         * disjoint *and* the digit positions of small should not be
         * directly visible in the result.
         */
        long smallHighDigitPos = (long) small.scale - small.precision() + 1;
        if (smallHighDigitPos > big.scale + 2 && // big and small disjoint
                smallHighDigitPos > estResultUlpScale + 2) { // small digits not visible
            small = DMatma.valueOf(small.signum(), this.checkScale(Math.max(big.scale, estResultUlpScale) + 3));
        }

        // Since addition is symmetric, preserving input order in
        // returned operands doesn't matter
        DMatma[] result = {big, small};
        return result;
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this -
     * subtrahend)}, and whose scale is {@code max(this.scale(),
     * subtrahend.scale())}.
     *
     * @param  subtrahend value to be subtracted from this {@code DMatma}.
     * @return {@code this - subtrahend}
     */
    public DMatma subtract(DMatma subtrahend) {
        if (this.intCompact != INFLATED) {
            if ((subtrahend.intCompact != INFLATED)) {
                return add(this.intCompact, this.scale, -subtrahend.intCompact, subtrahend.scale);
            } else {
                return add(this.intCompact, this.scale, subtrahend.intVal.negate(), subtrahend.scale);
            }
        } else {
            if ((subtrahend.intCompact != INFLATED)) {
                // Pair of subtrahend values given before pair of
                // values from this DMatma to avoid need for
                // method overloading on the specialized add method
                return add(-subtrahend.intCompact, subtrahend.scale, this.intVal, this.scale);
            } else {
                return add(this.intVal, this.scale, subtrahend.intVal.negate(), subtrahend.scale);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this - subtrahend)},
     * with rounding according to the context settings.
     *
     * If {@code subtrahend} is zero then this, rounded if necessary, is used as the
     * result.  If this is zero then the result is {@code subtrahend.negate(mc)}.
     *
     * @param  subtrahend value to be subtracted from this {@code DMatma}.
     * @param  mc the context to use.
     * @return {@code this - subtrahend}, rounded as necessary.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma subtract(DMatma subtrahend, MathContext mc) {
        if (mc.precision == 0)
            return subtract(subtrahend);
        // share the special rounding code in add()
        return add(subtrahend.negate(), mc);
    }

    /**
     * Returns a {@code DMatma} whose value is <tt>(this &times;
     * multiplicand)</tt>, and whose scale is {@code (this.scale() +
     * multiplicand.scale())}.
     *
     * @param  multiplicand value to be multiplied by this {@code DMatma}.
     * @return {@code this * multiplicand}
     */
    public DMatma multiply(DMatma multiplicand) {
        int productScale = checkScale((long) scale + multiplicand.scale);
        if (this.intCompact != INFLATED) {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiply(this.intCompact, multiplicand.intCompact, productScale);
            } else {
                return multiply(this.intCompact, multiplicand.intVal, productScale);
            }
        } else {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiply(multiplicand.intCompact, this.intVal, productScale);
            } else {
                return multiply(this.intVal, multiplicand.intVal, productScale);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose value is <tt>(this &times;
     * multiplicand)</tt>, with rounding according to the context settings.
     *
     * @param  multiplicand value to be multiplied by this {@code DMatma}.
     * @param  mc the context to use.
     * @return {@code this * multiplicand}, rounded as necessary.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma multiply(DMatma multiplicand, MathContext mc) {
        if (mc.precision == 0)
            return multiply(multiplicand);
        int productScale = checkScale((long) scale + multiplicand.scale);
        if (this.intCompact != INFLATED) {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiplyAndRound(this.intCompact, multiplicand.intCompact, productScale, mc);
            } else {
                return multiplyAndRound(this.intCompact, multiplicand.intVal, productScale, mc);
            }
        } else {
            if ((multiplicand.intCompact != INFLATED)) {
                return multiplyAndRound(multiplicand.intCompact, this.intVal, productScale, mc);
            } else {
                return multiplyAndRound(this.intVal, multiplicand.intVal, productScale, mc);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this /
     * divisor)}, and whose scale is as specified.  If rounding must
     * be performed to generate a result with the specified scale, the
     * specified rounding mode is applied.
     *
     * <p>The new {@link #divide(DMatma, int, RoundingMode)} method
     * should be used in preference to this legacy method.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @param  scale scale of the {@code DMatma} quotient to be returned.
     * @param  roundingMode rounding mode to apply.
     * @return {@code this / divisor}
     * @throws ArithmeticException if {@code divisor} is zero,
     *         {@code roundingMode==ROUND_UNNECESSARY} and
     *         the specified scale is insufficient to represent the result
     *         of the division exactly.
     * @throws IllegalArgumentException if {@code roundingMode} does not
     *         represent a valid rounding mode.
     * @see    #ROUND_UP
     * @see    #ROUND_DOWN
     * @see    #ROUND_CEILING
     * @see    #ROUND_FLOOR
     * @see    #ROUND_HALF_UP
     * @see    #ROUND_HALF_DOWN
     * @see    #ROUND_HALF_EVEN
     * @see    #ROUND_UNNECESSARY
     */
    public DMatma divide(DMatma divisor, int scale, int roundingMode) {
        if (roundingMode < ROUND_UP || roundingMode > ROUND_UNNECESSARY)
            throw new IllegalArgumentException("Invalid rounding mode");
        if (this.intCompact != INFLATED) {
            if ((divisor.intCompact != INFLATED)) {
                return divide(this.intCompact, this.scale, divisor.intCompact, divisor.scale, scale, roundingMode);
            } else {
                return divide(this.intCompact, this.scale, divisor.intVal, divisor.scale, scale, roundingMode);
            }
        } else {
            if ((divisor.intCompact != INFLATED)) {
                return divide(this.intVal, this.scale, divisor.intCompact, divisor.scale, scale, roundingMode);
            } else {
                return divide(this.intVal, this.scale, divisor.intVal, divisor.scale, scale, roundingMode);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this /
     * divisor)}, and whose scale is as specified.  If rounding must
     * be performed to generate a result with the specified scale, the
     * specified rounding mode is applied.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @param  scale scale of the {@code DMatma} quotient to be returned.
     * @param  roundingMode rounding mode to apply.
     * @return {@code this / divisor}
     * @throws ArithmeticException if {@code divisor} is zero,
     *         {@code roundingMode==RoundingMode.UNNECESSARY} and
     *         the specified scale is insufficient to represent the result
     *         of the division exactly.
     * @since 1.5
     */
    public DMatma divide(DMatma divisor, int scale, RoundingMode roundingMode) {
        return divide(divisor, scale, roundingMode.oldMode);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this /
     * divisor)}, and whose scale is {@code this.scale()}.  If
     * rounding must be performed to generate a result with the given
     * scale, the specified rounding mode is applied.
     *
     * <p>The new {@link #divide(DMatma, RoundingMode)} method
     * should be used in preference to this legacy method.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @param  roundingMode rounding mode to apply.
     * @return {@code this / divisor}
     * @throws ArithmeticException if {@code divisor==0}, or
     *         {@code roundingMode==ROUND_UNNECESSARY} and
     *         {@code this.scale()} is insufficient to represent the result
     *         of the division exactly.
     * @throws IllegalArgumentException if {@code roundingMode} does not
     *         represent a valid rounding mode.
     * @see    #ROUND_UP
     * @see    #ROUND_DOWN
     * @see    #ROUND_CEILING
     * @see    #ROUND_FLOOR
     * @see    #ROUND_HALF_UP
     * @see    #ROUND_HALF_DOWN
     * @see    #ROUND_HALF_EVEN
     * @see    #ROUND_UNNECESSARY
     */
    public DMatma divide(DMatma divisor, int roundingMode) {
        return this.divide(divisor, scale, roundingMode);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this /
     * divisor)}, and whose scale is {@code this.scale()}.  If
     * rounding must be performed to generate a result with the given
     * scale, the specified rounding mode is applied.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @param  roundingMode rounding mode to apply.
     * @return {@code this / divisor}
     * @throws ArithmeticException if {@code divisor==0}, or
     *         {@code roundingMode==RoundingMode.UNNECESSARY} and
     *         {@code this.scale()} is insufficient to represent the result
     *         of the division exactly.
     * @since 1.5
     */
    public DMatma divide(DMatma divisor, RoundingMode roundingMode) {
        return this.divide(divisor, scale, roundingMode.oldMode);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this /
     * divisor)}, and whose preferred scale is {@code (this.scale() -
     * divisor.scale())}; if the exact quotient cannot be
     * represented (because it has a non-terminating decimal
     * expansion) an {@code ArithmeticException} is thrown.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @throws ArithmeticException if the exact quotient does not have a
     *         terminating decimal expansion
     * @return {@code this / divisor}
     * @since 1.5
     * @author Joseph D. Darcy
     */
    public DMatma divide(DMatma divisor) {
        /*
         * Handle zero cases first.
         */
        if (divisor.signum() == 0) {   // x/0
            if (this.signum() == 0)    // 0/0
                throw new ArithmeticException("Division undefined");  // NaN
            throw new ArithmeticException("Division by zero");
        }

        // Calculate preferred scale
        int preferredScale = saturateLong((long) this.scale - divisor.scale);

        if (this.signum() == 0) // 0/y
            return zeroValueOf(preferredScale);
        else {
            /*
             * If the quotient this/divisor has a terminating decimal
             * expansion, the expansion can have no more than
             * (a.precision() + ceil(10*b.precision)/3) digits.
             * Therefore, create a MathContext object with this
             * precision and do a divide with the UNNECESSARY rounding
             * mode.
             */
            MathContext mc = new MathContext( (int)Math.min(this.precision() +
                            (long)Math.ceil(10.0*divisor.precision()/3.0),
                    Integer.MAX_VALUE),
                    RoundingMode.UNNECESSARY);
            DMatma quotient;
            try {
                quotient = this.divide(divisor, mc);
            } catch (ArithmeticException e) {
                throw new ArithmeticException("Non-terminating decimal expansion; " +
                        "no exact representable decimal result.");
            }

            int quotientScale = quotient.scale();

            // divide(DMatma, mc) tries to adjust the quotient to
            // the desired one by removing trailing zeros; since the
            // exact divide method does not have an explicit digit
            // limit, we can add zeros too.
            if (preferredScale > quotientScale)
                return quotient.setScale(preferredScale, ROUND_UNNECESSARY);

            return quotient;
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this /
     * divisor)}, with rounding according to the context settings.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @param  mc the context to use.
     * @return {@code this / divisor}, rounded as necessary.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY} or
     *         {@code mc.precision == 0} and the quotient has a
     *         non-terminating decimal expansion.
     * @since  1.5
     */
    public DMatma divide(DMatma divisor, MathContext mc) {
        int mcp = mc.precision;
        if (mcp == 0)
            return divide(divisor);

        DMatma dividend = this;
        long preferredScale = (long)dividend.scale - divisor.scale;
        // Now calculate the answer.  We use the existing
        // divide-and-round method, but as this rounds to scale we have
        // to normalize the values here to achieve the desired result.
        // For x/y we first handle y=0 and x=0, and then normalize x and
        // y to give x' and y' with the following constraints:
        //   (a) 0.1 <= x' < 1
        //   (b)  x' <= y' < 10*x'
        // Dividing x'/y' with the required scale set to mc.precision then
        // will give a result in the range 0.1 to 1 rounded to exactly
        // the right number of digits (except in the case of a result of
        // 1.000... which can arise when x=y, or when rounding overflows
        // The 1.000... case will reduce properly to 1.
        if (divisor.signum() == 0) {      // x/0
            if (dividend.signum() == 0)    // 0/0
                throw new ArithmeticException("Division undefined");  // NaN
            throw new ArithmeticException("Division by zero");
        }
        if (dividend.signum() == 0) // 0/y
            return zeroValueOf(saturateLong(preferredScale));
        int xscale = dividend.precision();
        int yscale = divisor.precision();
        if(dividend.intCompact!=INFLATED) {
            if(divisor.intCompact!=INFLATED) {
                return divide(dividend.intCompact, xscale, divisor.intCompact, yscale, preferredScale, mc);
            } else {
                return divide(dividend.intCompact, xscale, divisor.intVal, yscale, preferredScale, mc);
            }
        } else {
            if(divisor.intCompact!=INFLATED) {
                return divide(dividend.intVal, xscale, divisor.intCompact, yscale, preferredScale, mc);
            } else {
                return divide(dividend.intVal, xscale, divisor.intVal, yscale, preferredScale, mc);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose value is the integer part
     * of the quotient {@code (this / divisor)} rounded down.  The
     * preferred scale of the result is {@code (this.scale() -
     * divisor.scale())}.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @return The integer part of {@code this / divisor}.
     * @throws ArithmeticException if {@code divisor==0}
     * @since  1.5
     */
    public DMatma divideToIntegralValue(DMatma divisor) {
        // Calculate preferred scale
        int preferredScale = saturateLong((long) this.scale - divisor.scale);
        if (this.compareMagnitude(divisor) < 0) {
            // much faster when this << divisor
            return zeroValueOf(preferredScale);
        }

        if (this.signum() == 0 && divisor.signum() != 0)
            return this.setScale(preferredScale, ROUND_UNNECESSARY);

        // Perform a divide with enough digits to round to a correct
        // integer value; then remove any fractional digits

        int maxDigits = (int)Math.min(this.precision() +
                        (long)Math.ceil(10.0*divisor.precision()/3.0) +
                        Math.abs((long)this.scale() - divisor.scale()) + 2,
                Integer.MAX_VALUE);
        DMatma quotient = this.divide(divisor, new MathContext(maxDigits,
                RoundingMode.DOWN));
        if (quotient.scale > 0) {
            quotient = quotient.setScale(0, RoundingMode.DOWN);
            quotient = stripZerosToMatchScale(quotient.intVal, quotient.intCompact, quotient.scale, preferredScale);
        }

        if (quotient.scale < preferredScale) {
            // pad with zeros if necessary
            quotient = quotient.setScale(preferredScale, ROUND_UNNECESSARY);
        }

        return quotient;
    }

    /**
     * Returns a {@code DMatma} whose value is the integer part
     * of {@code (this / divisor)}.  Since the integer part of the
     * exact quotient does not depend on the rounding mode, the
     * rounding mode does not affect the values returned by this
     * method.  The preferred scale of the result is
     * {@code (this.scale() - divisor.scale())}.  An
     * {@code ArithmeticException} is thrown if the integer part of
     * the exact quotient needs more than {@code mc.precision}
     * digits.
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @param  mc the context to use.
     * @return The integer part of {@code this / divisor}.
     * @throws ArithmeticException if {@code divisor==0}
     * @throws ArithmeticException if {@code mc.precision} {@literal >} 0 and the result
     *         requires a precision of more than {@code mc.precision} digits.
     * @since  1.5
     * @author Joseph D. Darcy
     */
    public DMatma divideToIntegralValue(DMatma divisor, MathContext mc) {
        if (mc.precision == 0 || // exact result
                (this.compareMagnitude(divisor) < 0)) // zero result
            return divideToIntegralValue(divisor);

        // Calculate preferred scale
        int preferredScale = saturateLong((long)this.scale - divisor.scale);

        /*
         * Perform a normal divide to mc.precision digits.  If the
         * remainder has absolute value less than the divisor, the
         * integer portion of the quotient fits into mc.precision
         * digits.  Next, remove any fractional digits from the
         * quotient and adjust the scale to the preferred value.
         */
        DMatma result = this.divide(divisor, new MathContext(mc.precision, RoundingMode.DOWN));

        if (result.scale() < 0) {
            /*
             * Result is an integer. See if quotient represents the
             * full integer portion of the exact quotient; if it does,
             * the computed remainder will be less than the divisor.
             */
            DMatma product = result.multiply(divisor);
            // If the quotient is the full integer value,
            // |dividend-product| < |divisor|.
            if (this.subtract(product).compareMagnitude(divisor) >= 0) {
                throw new ArithmeticException("Division impossible");
            }
        } else if (result.scale() > 0) {
            /*
             * Integer portion of quotient will fit into precision
             * digits; recompute quotient to scale 0 to avoid double
             * rounding and then try to adjust, if necessary.
             */
            result = result.setScale(0, RoundingMode.DOWN);
        }
        // else result.scale() == 0;

        int precisionDiff;
        if ((preferredScale > result.scale()) &&
                (precisionDiff = mc.precision - result.precision()) > 0) {
            return result.setScale(result.scale() +
                    Math.min(precisionDiff, preferredScale - result.scale) );
        } else {
            return stripZerosToMatchScale(result.intVal,result.intCompact,result.scale,preferredScale);
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (this % divisor)}.
     *
     * <p>The remainder is given by
     * {@code this.subtract(this.divideToIntegralValue(divisor).multiply(divisor))}.
     * Note that this is not the modulo operation (the result can be
     * negative).
     *
     * @param  divisor value by which this {@code DMatma} is to be divided.
     * @return {@code this % divisor}.
     * @throws ArithmeticException if {@code divisor==0}
     * @since  1.5
     */
    public DMatma remainder(DMatma divisor) {
        DMatma divrem[] = this.divideAndRemainder(divisor);
        return divrem[1];
    }

    public DMatma remainder(DMatma divisor, MathContext mc) {
        DMatma divrem[] = this.divideAndRemainder(divisor, mc);
        return divrem[1];
    }

    public DMatma[] divideAndRemainder(DMatma divisor) {
        // we use the identity  x = i * y + r to determine r
        DMatma[] result = new DMatma[2];

        result[0] = this.divideToIntegralValue(divisor);
        result[1] = this.subtract(result[0].multiply(divisor));
        return result;
    }


    public DMatma[] divideAndRemainder(DMatma divisor, MathContext mc) {
        if (mc.precision == 0)
            return divideAndRemainder(divisor);

        DMatma[] result = new DMatma[2];
        DMatma lhs = this;

        result[0] = lhs.divideToIntegralValue(divisor, mc);
        result[1] = lhs.subtract(result[0].multiply(divisor));
        return result;
    }

    /**
     * Returns a {@code DMatma} whose value is
     * <tt>(this<sup>n</sup>)</tt>, The power is computed exactly, to
     * unlimited precision.
     *
     * <p>The parameter {@code n} must be in the range 0 through
     * 999999999, inclusive.  {@code ZERO.pow(0)} returns {@link
     * #ONE}.
     *
     * Note that future releases may expand the allowable exponent
     * range of this method.
     *
     * @param  n power to raise this {@code DMatma} to.
     * @return <tt>this<sup>n</sup></tt>
     * @throws ArithmeticException if {@code n} is out of range.
     * @since  1.5
     */
    public DMatma pow(int n) {
        if (n < 0 || n > 999999999)
            throw new ArithmeticException("Invalid operation");
        // No need to calculate pow(n) if result will over/underflow.
        // Don't attempt to support "supernormal" numbers.
        int newScale = checkScale((long)scale * n);
        return new DMatma(this.inflated().pow(n), newScale);
    }


    /**
     * Returns a {@code DMatma} whose value is
     * <tt>(this<sup>n</sup>)</tt>.  The current implementation uses
     * the core algorithm defined in ANSI standard X3.274-1996 with
     * rounding according to the context settings.  In general, the
     * returned numerical value is within two ulps of the exact
     * numerical value for the chosen precision.  Note that future
     * releases may use a different algorithm with a decreased
     * allowable error bound and increased allowable exponent range.
     *
     * <p>The X3.274-1996 algorithm is:
     *
     * <ul>
     * <li> An {@code ArithmeticException} exception is thrown if
     *  <ul>
     *    <li>{@code abs(n) > 999999999}
     *    <li>{@code mc.precision == 0} and {@code n < 0}
     *    <li>{@code mc.precision > 0} and {@code n} has more than
     *    {@code mc.precision} decimal digits
     *  </ul>
     *
     * <li> if {@code n} is zero, {@link #ONE} is returned even if
     * {@code this} is zero, otherwise
     * <ul>
     *   <li> if {@code n} is positive, the result is calculated via
     *   the repeated squaring technique into a single accumulator.
     *   The individual multiplications with the accumulator use the
     *   same math context settings as in {@code mc} except for a
     *   precision increased to {@code mc.precision + elength + 1}
     *   where {@code elength} is the number of decimal digits in
     *   {@code n}.
     *
     *   <li> if {@code n} is negative, the result is calculated as if
     *   {@code n} were positive; this value is then divided into one
     *   using the working precision specified above.
     *
     *   <li> The final value from either the positive or negative case
     *   is then rounded to the destination precision.
     *   </ul>
     * </ul>
     *
     * @param  n power to raise this {@code DMatma} to.
     * @param  mc the context to use.
     * @return <tt>this<sup>n</sup></tt> using the ANSI standard X3.274-1996
     *         algorithm
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}, or {@code n} is out
     *         of range.
     * @since  1.5
     */
    public DMatma pow(int n, MathContext mc) {
        if (mc.precision == 0)
            return pow(n);
        if (n < -999999999 || n > 999999999)
            throw new ArithmeticException("Invalid operation");
        if (n == 0)
            return ONE;                      // x**0 == 1 in X3.274
        DMatma lhs = this;
        MathContext workmc = mc;           // working settings
        int mag = Math.abs(n);               // magnitude of n
        if (mc.precision > 0) {
            int elength = longDigitLength(mag); // length of n in digits
            if (elength > mc.precision)        // X3.274 rule
                throw new ArithmeticException("Invalid operation");
            workmc = new MathContext(mc.precision + elength + 1,
                    mc.roundingMode);
        }
        // ready to carry out power calculation...
        DMatma acc = ONE;           // accumulator
        boolean seenbit = false;        // set once we've seen a 1-bit
        for (int i=1;;i++) {            // for each bit [top bit ignored]
            mag += mag;                 // shift left 1 bit
            if (mag < 0) {              // top bit is set
                seenbit = true;         // OK, we're off
                acc = acc.multiply(lhs, workmc); // acc=acc*x
            }
            if (i == 31)
                break;                  // that was the last bit
            if (seenbit)
                acc=acc.multiply(acc, workmc);   // acc=acc*acc [square]
            // else (!seenbit) no point in squaring ONE
        }
        // if negative n, calculate the reciprocal using working precision
        if (n < 0) // [hence mc.precision>0]
            acc=ONE.divide(acc, workmc);
        // round to final precision and strip zeros
        return doRound(acc, mc);
    }

    /**
     * Returns a {@code DMatma} whose value is the absolute value
     * of this {@code DMatma}, and whose scale is
     * {@code this.scale()}.
     *
     * @return {@code abs(this)}
     */
    public DMatma abs() {
        return (signum() < 0 ? negate() : this);
    }

    /**
     * Returns a {@code DMatma} whose value is the absolute value
     * of this {@code DMatma}, with rounding according to the
     * context settings.
     *
     * @param mc the context to use.
     * @return {@code abs(this)}, rounded as necessary.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since 1.5
     */
    public DMatma abs(MathContext mc) {
        return (signum() < 0 ? negate(mc) : plus(mc));
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (-this)},
     * and whose scale is {@code this.scale()}.
     *
     * @return {@code -this}.
     */
    public DMatma negate() {
        if (intCompact == INFLATED) {
            return new DMatma(intVal.negate(), INFLATED, scale, precision);
        } else {
            return valueOf(-intCompact, scale, precision);
        }
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (-this)},
     * with rounding according to the context settings.
     *
     * @param mc the context to use.
     * @return {@code -this}, rounded as necessary.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @since  1.5
     */
    public DMatma negate(MathContext mc) {
        return negate().plus(mc);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (+this)}, and whose
     * scale is {@code this.scale()}.
     *
     * <p>This method, which simply returns this {@code DMatma}
     * is included for symmetry with the unary minus method {@link
     * #negate()}.
     *
     * @return {@code this}.
     * @see #negate()
     * @since  1.5
     */
    public DMatma plus() {
        return this;
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (+this)},
     * with rounding according to the context settings.
     *
     * <p>The effect of this method is identical to that of the {@link
     * #round(MathContext)} method.
     *
     * @param mc the context to use.
     * @return {@code this}, rounded as necessary.  A zero result will
     *         have a scale of 0.
     * @throws ArithmeticException if the result is inexact but the
     *         rounding mode is {@code UNNECESSARY}.
     * @see    #round(MathContext)
     * @since  1.5
     */
    public DMatma plus(MathContext mc) {
        if (mc.precision == 0)                 // no rounding please
            return this;
        return doRound(this, mc);
    }

    /**
     * Returns the signum function of this {@code DMatma}.
     *
     * @return -1, 0, or 1 as the value of this {@code DMatma}
     *         is negative, zero, or positive.
     */
    public int signum() {
        return (intCompact != INFLATED)?
                Long.signum(intCompact):
                intVal.signum();
    }

    /**
     * Returns the <i>scale</i> of this {@code DMatma}.  If zero
     * or positive, the scale is the number of digits to the right of
     * the decimal point.  If negative, the unscaled value of the
     * number is multiplied by ten to the power of the negation of the
     * scale.  For example, a scale of {@code -3} means the unscaled
     * value is multiplied by 1000.
     *
     * @return the scale of this {@code DMatma}.
     */
    public int scale() {
        return scale;
    }

    /**
     * Returns the <i>precision</i> of this {@code DMatma}.  (The
     * precision is the number of digits in the unscaled value.)
     *
     * <p>The precision of a zero value is 1.
     *
     * @return the precision of this {@code DMatma}.
     * @since  1.5
     */
    public int precision() {
        int result = precision;
        if (result == 0) {
            long s = intCompact;
            if (s != INFLATED)
                result = longDigitLength(s);
            else
                result = bigDigitLength(intVal);
            precision = result;
        }
        return result;
    }


    /**
     * Returns a {@code Matma} whose value is the <i>unscaled
     * value</i> of this {@code DMatma}.  (Computes <tt>(this *
     * 10<sup>this.scale()</sup>)</tt>.)
     *
     * @return the unscaled value of this {@code DMatma}.
     * @since  1.2
     */
    public Matma unscaledValue() {
        return this.inflated();
    }

    // Rounding Modes

    /**
     * Rounding mode to round away from zero.  Always increments the
     * digit prior to a nonzero discarded fraction.  Note that this rounding
     * mode never decreases the magnitude of the calculated value.
     */
    public final static int ROUND_UP =           0;

    /**
     * Rounding mode to round towards zero.  Never increments the digit
     * prior to a discarded fraction (i.e., truncates).  Note that this
     * rounding mode never increases the magnitude of the calculated value.
     */
    public final static int ROUND_DOWN =         1;

    /**
     * Rounding mode to round towards positive infinity.  If the
     * {@code DMatma} is positive, behaves as for
     * {@code ROUND_UP}; if negative, behaves as for
     * {@code ROUND_DOWN}.  Note that this rounding mode never
     * decreases the calculated value.
     */
    public final static int ROUND_CEILING =      2;

    /**
     * Rounding mode to round towards negative infinity.  If the
     * {@code DMatma} is positive, behave as for
     * {@code ROUND_DOWN}; if negative, behave as for
     * {@code ROUND_UP}.  Note that this rounding mode never
     * increases the calculated value.
     */
    public final static int ROUND_FLOOR =        3;

    /**
     * Rounding mode to round towards {@literal "nearest neighbor"}
     * unless both neighbors are equidistant, in which case round up.
     * Behaves as for {@code ROUND_UP} if the discarded fraction is
     * &ge; 0.5; otherwise, behaves as for {@code ROUND_DOWN}.  Note
     * that this is the rounding mode that most of us were taught in
     * grade school.
     */
    public final static int ROUND_HALF_UP =      4;

    /**
     * Rounding mode to round towards {@literal "nearest neighbor"}
     * unless both neighbors are equidistant, in which case round
     * down.  Behaves as for {@code ROUND_UP} if the discarded
     * fraction is {@literal >} 0.5; otherwise, behaves as for
     * {@code ROUND_DOWN}.
     */
    public final static int ROUND_HALF_DOWN =    5;

    /**
     * Rounding mode to round towards the {@literal "nearest neighbor"}
     * unless both neighbors are equidistant, in which case, round
     * towards the even neighbor.  Behaves as for
     * {@code ROUND_HALF_UP} if the digit to the left of the
     * discarded fraction is odd; behaves as for
     * {@code ROUND_HALF_DOWN} if it's even.  Note that this is the
     * rounding mode that minimizes cumulative error when applied
     * repeatedly over a sequence of calculations.
     */
    public final static int ROUND_HALF_EVEN =    6;

    /**
     * Rounding mode to assert that the requested operation has an exact
     * result, hence no rounding is necessary.  If this rounding mode is
     * specified on an operation that yields an inexact result, an
     * {@code ArithmeticException} is thrown.
     */
    public final static int ROUND_UNNECESSARY =  7;


    // Scaling/Rounding Operations

    /**
     * Returns a {@code DMatma} rounded according to the
     * {@code MathContext} settings.  If the precision setting is 0 then
     * no rounding takes place.
     *
     * <p>The effect of this method is identical to that of the
     * {@link #plus(MathContext)} method.
     *
     * @param mc the context to use.
     * @return a {@code DMatma} rounded according to the
     *         {@code MathContext} settings.
     * @throws ArithmeticException if the rounding mode is
     *         {@code UNNECESSARY} and the
     *         {@code DMatma}  operation would require rounding.
     * @see    #plus(MathContext)
     * @since  1.5
     */
    public DMatma round(MathContext mc) {
        return plus(mc);
    }

    /**
     * Returns a {@code DMatma} whose scale is the specified
     * value, and whose unscaled value is determined by multiplying or
     * dividing this {@code DMatma}'s unscaled value by the
     * appropriate power of ten to maintain its overall value.  If the
     * scale is reduced by the operation, the unscaled value must be
     * divided (rather than multiplied), and the value may be changed;
     * in this case, the specified rounding mode is applied to the
     * division.
     *
     * <p>Note that since DMatma objects are immutable, calls of
     * this method do <i>not</i> result in the original object being
     * modified, contrary to the usual convention of having methods
     * named <tt>set<i>X</i></tt> mutate field <i>{@code X}</i>.
     * Instead, {@code setScale} returns an object with the proper
     * scale; the returned object may or may not be newly allocated.
     *
     * @param  newScale scale of the {@code DMatma} value to be returned.
     * @param  roundingMode The rounding mode to apply.
     * @return a {@code DMatma} whose scale is the specified value,
     *         and whose unscaled value is determined by multiplying or
     *         dividing this {@code DMatma}'s unscaled value by the
     *         appropriate power of ten to maintain its overall value.
     * @throws ArithmeticException if {@code roundingMode==UNNECESSARY}
     *         and the specified scaling operation would require
     *         rounding.
     * @see    RoundingMode
     * @since  1.5
     */
    public DMatma setScale(int newScale, RoundingMode roundingMode) {
        return setScale(newScale, roundingMode.oldMode);
    }

    /**
     * Returns a {@code DMatma} whose scale is the specified
     * value, and whose unscaled value is determined by multiplying or
     * dividing this {@code DMatma}'s unscaled value by the
     * appropriate power of ten to maintain its overall value.  If the
     * scale is reduced by the operation, the unscaled value must be
     * divided (rather than multiplied), and the value may be changed;
     * in this case, the specified rounding mode is applied to the
     * division.
     *
     * <p>Note that since DMatma objects are immutable, calls of
     * this method do <i>not</i> result in the original object being
     * modified, contrary to the usual convention of having methods
     * named <tt>set<i>X</i></tt> mutate field <i>{@code X}</i>.
     * Instead, {@code setScale} returns an object with the proper
     * scale; the returned object may or may not be newly allocated.
     *
     * <p>The new {@link #setScale(int, RoundingMode)} method should
     * be used in preference to this legacy method.
     *
     * @param  newScale scale of the {@code DMatma} value to be returned.
     * @param  roundingMode The rounding mode to apply.
     * @return a {@code DMatma} whose scale is the specified value,
     *         and whose unscaled value is determined by multiplying or
     *         dividing this {@code DMatma}'s unscaled value by the
     *         appropriate power of ten to maintain its overall value.
     * @throws ArithmeticException if {@code roundingMode==ROUND_UNNECESSARY}
     *         and the specified scaling operation would require
     *         rounding.
     * @throws IllegalArgumentException if {@code roundingMode} does not
     *         represent a valid rounding mode.
     * @see    #ROUND_UP
     * @see    #ROUND_DOWN
     * @see    #ROUND_CEILING
     * @see    #ROUND_FLOOR
     * @see    #ROUND_HALF_UP
     * @see    #ROUND_HALF_DOWN
     * @see    #ROUND_HALF_EVEN
     * @see    #ROUND_UNNECESSARY
     */
    public DMatma setScale(int newScale, int roundingMode) {
        if (roundingMode < ROUND_UP || roundingMode > ROUND_UNNECESSARY)
            throw new IllegalArgumentException("Invalid rounding mode");

        int oldScale = this.scale;
        if (newScale == oldScale)        // easy case
            return this;
        if (this.signum() == 0)            // zero can have any scale
            return zeroValueOf(newScale);
        if(this.intCompact!=INFLATED) {
            long rs = this.intCompact;
            if (newScale > oldScale) {
                int raise = checkScale((long) newScale - oldScale);
                if ((rs = longMultiplyPowerTen(rs, raise)) != INFLATED) {
                    return valueOf(rs,newScale);
                }
                Matma rb = bigMultiplyPowerTen(raise);
                return new DMatma(rb, INFLATED, newScale, (precision > 0) ? precision + raise : 0);
            } else {
                // newScale < oldScale -- drop some digits
                // Can't predict the precision due to the effect of rounding.
                int drop = checkScale((long) oldScale - newScale);
                if (drop < LONG_TEN_POWERS_TABLE.length) {
                    return divideAndRound(rs, LONG_TEN_POWERS_TABLE[drop], newScale, roundingMode, newScale);
                } else {
                    return divideAndRound(this.inflated(), bigTenToThe(drop), newScale, roundingMode, newScale);
                }
            }
        } else {
            if (newScale > oldScale) {
                int raise = checkScale((long) newScale - oldScale);
                Matma rb = bigMultiplyPowerTen(this.intVal,raise);
                return new DMatma(rb, INFLATED, newScale, (precision > 0) ? precision + raise : 0);
            } else {
                // newScale < oldScale -- drop some digits
                // Can't predict the precision due to the effect of rounding.
                int drop = checkScale((long) oldScale - newScale);
                if (drop < LONG_TEN_POWERS_TABLE.length)
                    return divideAndRound(this.intVal, LONG_TEN_POWERS_TABLE[drop], newScale, roundingMode,
                            newScale);
                else
                    return divideAndRound(this.intVal,  bigTenToThe(drop), newScale, roundingMode, newScale);
            }
        }
    }

    /**
     * Returns a {@code DMatma} whose scale is the specified
     * value, and whose value is numerically equal to this
     * {@code DMatma}'s.  Throws an {@code ArithmeticException}
     * if this is not possible.
     *
     * <p>This call is typically used to increase the scale, in which
     * case it is guaranteed that there exists a {@code DMatma}
     * of the specified scale and the correct value.  The call can
     * also be used to reduce the scale if the caller knows that the
     * {@code DMatma} has sufficiently many zeros at the end of
     * its fractional part (i.e., factors of ten in its integer value)
     * to allow for the rescaling without changing its value.
     *
     * <p>This method returns the same result as the two-argument
     * versions of {@code setScale}, but saves the caller the trouble
     * of specifying a rounding mode in cases where it is irrelevant.
     *
     * <p>Note that since {@code DMatma} objects are immutable,
     * calls of this method do <i>not</i> result in the original
     * object being modified, contrary to the usual convention of
     * having methods named <tt>set<i>X</i></tt> mutate field
     * <i>{@code X}</i>.  Instead, {@code setScale} returns an
     * object with the proper scale; the returned object may or may
     * not be newly allocated.
     *
     * @param  newScale scale of the {@code DMatma} value to be returned.
     * @return a {@code DMatma} whose scale is the specified value, and
     *         whose unscaled value is determined by multiplying or dividing
     *         this {@code DMatma}'s unscaled value by the appropriate
     *         power of ten to maintain its overall value.
     * @throws ArithmeticException if the specified scaling operation would
     *         require rounding.
     * @see    #setScale(int, int)
     * @see    #setScale(int, RoundingMode)
     */
    public DMatma setScale(int newScale) {
        return setScale(newScale, ROUND_UNNECESSARY);
    }

    // Decimal Point Motion Operations

    /**
     * Returns a {@code DMatma} which is equivalent to this one
     * with the decimal point moved {@code n} places to the left.  If
     * {@code n} is non-negative, the call merely adds {@code n} to
     * the scale.  If {@code n} is negative, the call is equivalent
     * to {@code movePointRight(-n)}.  The {@code DMatma}
     * returned by this call has value <tt>(this &times;
     * 10<sup>-n</sup>)</tt> and scale {@code max(this.scale()+n,
     * 0)}.
     *
     * @param  n number of places to move the decimal point to the left.
     * @return a {@code DMatma} which is equivalent to this one with the
     *         decimal point moved {@code n} places to the left.
     * @throws ArithmeticException if scale overflows.
     */
    public DMatma movePointLeft(int n) {
        // Cannot use movePointRight(-n) in case of n==Integer.MIN_VALUE
        int newScale = checkScale((long)scale + n);
        DMatma num = new DMatma(intVal, intCompact, newScale, 0);
        return num.scale < 0 ? num.setScale(0, ROUND_UNNECESSARY) : num;
    }

    /**
     * Returns a {@code DMatma} which is equivalent to this one
     * with the decimal point moved {@code n} places to the right.
     * If {@code n} is non-negative, the call merely subtracts
     * {@code n} from the scale.  If {@code n} is negative, the call
     * is equivalent to {@code movePointLeft(-n)}.  The
     * {@code DMatma} returned by this call has value <tt>(this
     * &times; 10<sup>n</sup>)</tt> and scale {@code max(this.scale()-n,
     * 0)}.
     *
     * @param  n number of places to move the decimal point to the right.
     * @return a {@code DMatma} which is equivalent to this one
     *         with the decimal point moved {@code n} places to the right.
     * @throws ArithmeticException if scale overflows.
     */
    public DMatma movePointRight(int n) {
        // Cannot use movePointLeft(-n) in case of n==Integer.MIN_VALUE
        int newScale = checkScale((long)scale - n);
        DMatma num = new DMatma(intVal, intCompact, newScale, 0);
        return num.scale < 0 ? num.setScale(0, ROUND_UNNECESSARY) : num;
    }

    /**
     * Returns a DMatma whose numerical value is equal to
     * ({@code this} * 10<sup>n</sup>).  The scale of
     * the result is {@code (this.scale() - n)}.
     *
     * @param n the exponent power of ten to scale by
     * @return a DMatma whose numerical value is equal to
     * ({@code this} * 10<sup>n</sup>)
     * @throws ArithmeticException if the scale would be
     *         outside the range of a 32-bit integer.
     *
     * @since 1.5
     */
    public DMatma scaleByPowerOfTen(int n) {
        return new DMatma(intVal, intCompact,
                checkScale((long)scale - n), precision);
    }

    /**
     * Returns a {@code DMatma} which is numerically equal to
     * this one but with any trailing zeros removed from the
     * representation.  For example, stripping the trailing zeros from
     * the {@code DMatma} value {@code 600.0}, which has
     * [{@code Matma}, {@code scale}] components equals to
     * [6000, 1], yields {@code 6E2} with [{@code Matma},
     * {@code scale}] components equals to [6, -2].  If
     * this DMatma is numerically equal to zero, then
     * {@code DMatma.ZERO} is returned.
     *
     * @return a numerically equal {@code DMatma} with any
     * trailing zeros removed.
     * @since 1.5
     */
    public DMatma stripTrailingZeros() {
        if (intCompact == 0 || (intVal != null && intVal.signum() == 0)) {
            return DMatma.ZERO;
        } else if (intCompact != INFLATED) {
            return createAndStripZerosToMatchScale(intCompact, scale, Long.MIN_VALUE);
        } else {
            return createAndStripZerosToMatchScale(intVal, scale, Long.MIN_VALUE);
        }
    }

    // Comparison Operations

    /**
     * Compares this {@code DMatma} with the specified
     * {@code DMatma}.  Two {@code DMatma} objects that are
     * equal in value but have a different scale (like 2.0 and 2.00)
     * are considered equal by this method.  This method is provided
     * in preference to individual methods for each of the six boolean
     * comparison operators ({@literal <}, ==,
     * {@literal >}, {@literal >=}, !=, {@literal <=}).  The
     * suggested idiom for performing these comparisons is:
     * {@code (x.compareTo(y)} &lt;<i>op</i>&gt; {@code 0)}, where
     * &lt;<i>op</i>&gt; is one of the six comparison operators.
     *
     * @param  val {@code DMatma} to which this {@code DMatma} is
     *         to be compared.
     * @return -1, 0, or 1 as this {@code DMatma} is numerically
     *          less than, equal to, or greater than {@code val}.
     */
    public int compareTo(DMatma val) {
        // Quick path for equal scale and non-inflated case.
        if (scale == val.scale) {
            long xs = intCompact;
            long ys = val.intCompact;
            if (xs != INFLATED && ys != INFLATED)
                return xs != ys ? ((xs > ys) ? 1 : -1) : 0;
        }
        int xsign = this.signum();
        int ysign = val.signum();
        if (xsign != ysign)
            return (xsign > ysign) ? 1 : -1;
        if (xsign == 0)
            return 0;
        int cmp = compareMagnitude(val);
        return (xsign > 0) ? cmp : -cmp;
    }

    /**
     * Version of compareTo that ignores sign.
     */
    private int compareMagnitude(DMatma val) {
        // Match scales, avoid unnecessary inflation
        long ys = val.intCompact;
        long xs = this.intCompact;
        if (xs == 0)
            return (ys == 0) ? 0 : -1;
        if (ys == 0)
            return 1;

        long sdiff = (long)this.scale - val.scale;
        if (sdiff != 0) {
            // Avoid matching scales if the (adjusted) exponents differ
            long xae = (long)this.precision() - this.scale;   // [-1]
            long yae = (long)val.precision() - val.scale;     // [-1]
            if (xae < yae)
                return -1;
            if (xae > yae)
                return 1;
            Matma rb = null;
            if (sdiff < 0) {
                // The cases sdiff <= Integer.MIN_VALUE intentionally fall through.
                if ( sdiff > Integer.MIN_VALUE &&
                        (xs == INFLATED ||
                                (xs = longMultiplyPowerTen(xs, (int)-sdiff)) == INFLATED) &&
                        ys == INFLATED) {
                    rb = bigMultiplyPowerTen((int)-sdiff);
                    return rb.compareMagnitude(val.intVal);
                }
            } else { // sdiff > 0
                // The cases sdiff > Integer.MAX_VALUE intentionally fall through.
                if ( sdiff <= Integer.MAX_VALUE &&
                        (ys == INFLATED ||
                                (ys = longMultiplyPowerTen(ys, (int)sdiff)) == INFLATED) &&
                        xs == INFLATED) {
                    rb = val.bigMultiplyPowerTen((int)sdiff);
                    return this.intVal.compareMagnitude(rb);
                }
            }
        }
        if (xs != INFLATED)
            return (ys != INFLATED) ? longCompareMagnitude(xs, ys) : -1;
        else if (ys != INFLATED)
            return 1;
        else
            return this.intVal.compareMagnitude(val.intVal);
    }


    @Override
    public boolean equals(Object x) {
        if (!(x instanceof DMatma))
            return false;
        DMatma xDec = (DMatma) x;
        if (x == this)
            return true;
        if (scale != xDec.scale)
            return false;
        long s = this.intCompact;
        long xs = xDec.intCompact;
        if (s != INFLATED) {
            if (xs == INFLATED)
                xs = compactValFor(xDec.intVal);
            return xs == s;
        } else if (xs != INFLATED)
            return xs == compactValFor(this.intVal);

        return this.inflated().equals(xDec.inflated());
    }


    public DMatma min(DMatma val) {
        return (compareTo(val) <= 0 ? this : val);
    }


    public DMatma max(DMatma val) {
        return (compareTo(val) >= 0 ? this : val);
    }

    // Hash Function

    /**
     * Returns the hash code for this {@code DMatma}.  Note that
     * two {@code DMatma} objects that are numerically equal but
     * differ in scale (like 2.0 and 2.00) will generally <i>not</i>
     * have the same hash code.
     *
     * @return hash code for this {@code DMatma}.
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        if (intCompact != INFLATED) {
            long val2 = (intCompact < 0)? -intCompact : intCompact;
            int temp = (int)( ((int)(val2 >>> 32)) * 31  +
                    (val2 & LONG_MASK));
            return 31*((intCompact < 0) ?-temp:temp) + scale;
        } else
            return 31*intVal.hashCode() + scale;
    }

    // Format Converters

    /**
     * Returns the string representation of this {@code DMatma},
     * using scientific notation if an exponent is needed.
     *
     * <p>A standard canonical string form of the {@code DMatma}
     * is created as though by the following steps: first, the
     * absolute value of the unscaled value of the {@code DMatma}
     * is converted to a string in base ten using the characters
     * {@code '0'} through {@code '9'} with no leading zeros (except
     * if its value is zero, in which case a single {@code '0'}
     * character is used).
     *
     * <p>Next, an <i>adjusted exponent</i> is calculated; this is the
     * negated scale, plus the number of characters in the converted
     * unscaled value, less one.  That is,
     * {@code -scale+(ulength-1)}, where {@code ulength} is the
     * length of the absolute value of the unscaled value in decimal
     * digits (its <i>precision</i>).
     *
     * <p>If the scale is greater than or equal to zero and the
     * adjusted exponent is greater than or equal to {@code -6}, the
     * number will be converted to a character form without using
     * exponential notation.  In this case, if the scale is zero then
     * no decimal point is added and if the scale is positive a
     * decimal point will be inserted with the scale specifying the
     * number of characters to the right of the decimal point.
     * {@code '0'} characters are added to the left of the converted
     * unscaled value as necessary.  If no character precedes the
     * decimal point after this insertion then a conventional
     * {@code '0'} character is prefixed.
     *
     * <p>Otherwise (that is, if the scale is negative, or the
     * adjusted exponent is less than {@code -6}), the number will be
     * converted to a character form using exponential notation.  In
     * this case, if the converted {@code Matma} has more than
     * one digit a decimal point is inserted after the first digit.
     * An exponent in character form is then suffixed to the converted
     * unscaled value (perhaps with inserted decimal point); this
     * comprises the letter {@code 'E'} followed immediately by the
     * adjusted exponent converted to a character form.  The latter is
     * in base ten, using the characters {@code '0'} through
     * {@code '9'} with no leading zeros, and is always prefixed by a
     * sign character {@code '-'} (<tt>'&#92;u002D'</tt>) if the
     * adjusted exponent is negative, {@code '+'}
     * (<tt>'&#92;u002B'</tt>) otherwise).
     *
     * <p>Finally, the entire string is prefixed by a minus sign
     * character {@code '-'} (<tt>'&#92;u002D'</tt>) if the unscaled
     * value is less than zero.  No sign character is prefixed if the
     * unscaled value is zero or positive.
     *
     * <p><b>Examples:</b>
     * <p>For each representation [<i>unscaled value</i>, <i>scale</i>]
     * on the left, the resulting string is shown on the right.
     * <pre>
     * [123,0]      "123"
     * [-123,0]     "-123"
     * [123,-1]     "1.23E+3"
     * [123,-3]     "1.23E+5"
     * [123,1]      "12.3"
     * [123,5]      "0.00123"
     * [123,10]     "1.23E-8"
     * [-123,12]    "-1.23E-10"
     * </pre>
     *
     * <b>Notes:</b>
     * <ol>
     *
     * <li>There is a one-to-one mapping between the distinguishable
     * {@code DMatma} values and the result of this conversion.
     * That is, every distinguishable {@code DMatma} value
     * (unscaled value and scale) has a unique string representation
     * as a result of using {@code toString}.  If that string
     * representation is converted back to a {@code DMatma} using
     * the {@link #DMatma(String)} constructor, then the original
     * value will be recovered.
     *
     * <li>The string produced for a given number is always the same;
     * it is not affected by locale.  This means that it can be used
     * as a canonical string representation for exchanging decimal
     * data, or as a key for a Hashtable, etc.  Locale-sensitive
     * number formatting and parsing is handled by the {@link
     * java.text.NumberFormat} class and its subclasses.
     *
     * <li>The {@link #toEngineeringString} method may be used for
     * presenting numbers with exponents in engineering notation, and the
     * {@link #setScale(int,RoundingMode) setScale} method may be used for
     * rounding a {@code DMatma} so it has a known number of digits after
     * the decimal point.
     *
     * <li>The digit-to-character mapping provided by
     * {@code Character.forDigit} is used.
     *
     * </ol>
     *
     * @return string representation of this {@code DMatma}.
     * @see    Character#forDigit
     * @see    #DMatma(String)
     */
    @Override
    public String toString() {
        String sc = stringCache;
        if (sc == null)
            stringCache = sc = layoutChars(true);
        return sc;
    }

    /**
     * Returns a string representation of this {@code DMatma},
     * using engineering notation if an exponent is needed.
     *
     * <p>Returns a string that represents the {@code DMatma} as
     * described in the {@link #toString()} method, except that if
     * exponential notation is used, the power of ten is adjusted to
     * be a multiple of three (engineering notation) such that the
     * integer part of nonzero values will be in the range 1 through
     * 999.  If exponential notation is used for zero values, a
     * decimal point and one or two fractional zero digits are used so
     * that the scale of the zero value is preserved.  Note that
     * unlike the output of {@link #toString()}, the output of this
     * method is <em>not</em> guaranteed to recover the same [integer,
     * scale] pair of this {@code DMatma} if the output string is
     * converting back to a {@code DMatma} using the {@linkplain
     * #DMatma(String) string constructor}.  The result of this method meets
     * the weaker constraint of always producing a numerically equal
     * result from applying the string constructor to the method's output.
     *
     * @return string representation of this {@code DMatma}, using
     *         engineering notation if an exponent is needed.
     * @since  1.5
     */
    public String toEngineeringString() {
        return layoutChars(false);
    }

    /**
     * Returns a string representation of this {@code DMatma}
     * without an exponent field.  For values with a positive scale,
     * the number of digits to the right of the decimal point is used
     * to indicate scale.  For values with a zero or negative scale,
     * the resulting string is generated as if the value were
     * converted to a numerically equal value with zero scale and as
     * if all the trailing zeros of the zero scale value were present
     * in the result.
     *
     * The entire string is prefixed by a minus sign character '-'
     * (<tt>'&#92;u002D'</tt>) if the unscaled value is less than
     * zero. No sign character is prefixed if the unscaled value is
     * zero or positive.
     *
     * Note that if the result of this method is passed to the
     * {@linkplain #DMatma(String) string constructor}, only the
     * numerical value of this {@code DMatma} will necessarily be
     * recovered; the representation of the new {@code DMatma}
     * may have a different scale.  In particular, if this
     * {@code DMatma} has a negative scale, the string resulting
     * from this method will have a scale of zero when processed by
     * the string constructor.
     *
     * (This method behaves analogously to the {@code toString}
     * method in 1.4 and earlier releases.)
     *
     * @return a string representation of this {@code DMatma}
     * without an exponent field.
     * @since 1.5
     * @see #toString()
     * @see #toEngineeringString()
     */
    public String toPlainString() {
        if(scale==0) {
            if(intCompact!=INFLATED) {
                return Long.toString(intCompact);
            } else {
                return intVal.toString();
            }
        }
        if(this.scale<0) { // No decimal point
            if(signum()==0) {
                return "0";
            }
            int tailingZeros = checkScaleNonZero((-(long)scale));
            StringBuilder buf;
            if(intCompact!=INFLATED) {
                buf = new StringBuilder(20+tailingZeros);
                buf.append(intCompact);
            } else {
                String str = intVal.toString();
                buf = new StringBuilder(str.length()+tailingZeros);
                buf.append(str);
            }
            for (int i = 0; i < tailingZeros; i++)
                buf.append('0');
            return buf.toString();
        }
        String str ;
        if(intCompact!=INFLATED) {
            str = Long.toString(Math.abs(intCompact));
        } else {
            str = intVal.abs().toString();
        }
        return getValueString(signum(), str, scale);
    }

    /* Returns a digit.digit string */
    private String getValueString(int signum, String intString, int scale) {
        /* Insert decimal point */
        StringBuilder buf;
        int insertionPoint = intString.length() - scale;
        if (insertionPoint == 0) {  /* Point goes right before intVal */
            return (signum<0 ? "-0." : "0.") + intString;
        } else if (insertionPoint > 0) { /* Point goes inside intVal */
            buf = new StringBuilder(intString);
            buf.insert(insertionPoint, '.');
            if (signum < 0)
                buf.insert(0, '-');
        } else { /* We must insert zeros between point and intVal */
            buf = new StringBuilder(3-insertionPoint + intString.length());
            buf.append(signum<0 ? "-0." : "0.");
            for (int i=0; i<-insertionPoint; i++)
                buf.append('0');
            buf.append(intString);
        }
        return buf.toString();
    }

    /**
     * Converts this {@code DMatma} to a {@code Matma}.
     * This conversion is analogous to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code long} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * any fractional part of this
     * {@code DMatma} will be discarded.  Note that this
     * conversion can lose information about the precision of the
     * {@code DMatma} value.
     * <p>
     * To have an exception thrown if the conversion is inexact (in
     * other words if a nonzero fractional part is discarded), use the
     * {@link #toBigIntegerExact()} method.
     *
     * @return this {@code DMatma} converted to a {@code Matma}.
     */
    public Matma toMatma() {
        // force to an integer, quietly
        return this.setScale(0, ROUND_DOWN).inflated();
    }

    /**
     * Converts this {@code DMatma} to a {@code Matma},
     * checking for lost information.  An exception is thrown if this
     * {@code DMatma} has a nonzero fractional part.
     *
     * @return this {@code DMatma} converted to a {@code Matma}.
     * @throws ArithmeticException if {@code this} has a nonzero
     *         fractional part.
     * @since  1.5
     */
    public Matma toBigIntegerExact() {
        // round to an integer, with Exception if decimal part non-0
        return this.setScale(0, ROUND_UNNECESSARY).inflated();
    }

    /**
     * Converts this {@code DMatma} to a {@code long}.
     * This conversion is analogous to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code short} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * any fractional part of this
     * {@code DMatma} will be discarded, and if the resulting
     * "{@code Matma}" is too big to fit in a
     * {@code long}, only the low-order 64 bits are returned.
     * Note that this conversion can lose information about the
     * overall magnitude and precision of this {@code DMatma} value as well
     * as return a result with the opposite sign.
     *
     * @return this {@code DMatma} converted to a {@code long}.
     */
    public long longValue(){
        return (intCompact != INFLATED && scale == 0) ?
                intCompact:
                toMatma().longValue();
    }

    /**
     * Converts this {@code DMatma} to a {@code long}, checking
     * for lost information.  If this {@code DMatma} has a
     * nonzero fractional part or is out of the possible range for a
     * {@code long} result then an {@code ArithmeticException} is
     * thrown.
     *
     * @return this {@code DMatma} converted to a {@code long}.
     * @throws ArithmeticException if {@code this} has a nonzero
     *         fractional part, or will not fit in a {@code long}.
     * @since  1.5
     */
    public long longValueExact() {
        if (intCompact != INFLATED && scale == 0)
            return intCompact;
        // If more than 19 digits in integer part it cannot possibly fit
        if ((precision() - scale) > 19) // [OK for negative scale too]
            throw new ArithmeticException("Overflow");
        // Fastpath zero and < 1.0 numbers (the latter can be very slow
        // to round if very small)
        if (this.signum() == 0)
            return 0;
        if ((this.precision() - this.scale) <= 0)
            throw new ArithmeticException("Rounding necessary");
        // round to an integer, with Exception if decimal part non-0
        DMatma num = this.setScale(0, ROUND_UNNECESSARY);
        if (num.precision() >= 19) // need to check carefully
            LongOverflow.check(num);
        return num.inflated().longValue();
    }

    private static class LongOverflow {
        /** Matma equal to Long.MIN_VALUE. */
        private static final Matma LONGMIN = Matma.valueOf(Long.MIN_VALUE);

        /** Matma equal to Long.MAX_VALUE. */
        private static final Matma LONGMAX = Matma.valueOf(Long.MAX_VALUE);

        public static void check(DMatma num) {
            Matma intVal = num.inflated();
            if (intVal.compareTo(LONGMIN) < 0 ||
                    intVal.compareTo(LONGMAX) > 0)
                throw new ArithmeticException("Overflow");
        }
    }

    /**
     * Converts this {@code DMatma} to an {@code int}.
     * This conversion is analogous to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code short} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * any fractional part of this
     * {@code DMatma} will be discarded, and if the resulting
     * "{@code Matma}" is too big to fit in an
     * {@code int}, only the low-order 32 bits are returned.
     * Note that this conversion can lose information about the
     * overall magnitude and precision of this {@code DMatma}
     * value as well as return a result with the opposite sign.
     *
     * @return this {@code DMatma} converted to an {@code int}.
     */
    public int intValue() {
        return  (intCompact != INFLATED && scale == 0) ?
                (int)intCompact :
                toMatma().intValue();
    }

    /**
     * Converts this {@code DMatma} to an {@code int}, checking
     * for lost information.  If this {@code DMatma} has a
     * nonzero fractional part or is out of the possible range for an
     * {@code int} result then an {@code ArithmeticException} is
     * thrown.
     *
     * @return this {@code DMatma} converted to an {@code int}.
     * @throws ArithmeticException if {@code this} has a nonzero
     *         fractional part, or will not fit in an {@code int}.
     * @since  1.5
     */
    public int intValueExact() {
        long num;
        num = this.longValueExact();     // will check decimal part
        if ((int)num != num)
            throw new ArithmeticException("Overflow");
        return (int)num;
    }

    /**
     * Converts this {@code DMatma} to a {@code short}, checking
     * for lost information.  If this {@code DMatma} has a
     * nonzero fractional part or is out of the possible range for a
     * {@code short} result then an {@code ArithmeticException} is
     * thrown.
     *
     * @return this {@code DMatma} converted to a {@code short}.
     * @throws ArithmeticException if {@code this} has a nonzero
     *         fractional part, or will not fit in a {@code short}.
     * @since  1.5
     */
    public short shortValueExact() {
        long num;
        num = this.longValueExact();     // will check decimal part
        if ((short)num != num)
            throw new ArithmeticException("Overflow");
        return (short)num;
    }

    /**
     * Converts this {@code DMatma} to a {@code byte}, checking
     * for lost information.  If this {@code DMatma} has a
     * nonzero fractional part or is out of the possible range for a
     * {@code byte} result then an {@code ArithmeticException} is
     * thrown.
     *
     * @return this {@code DMatma} converted to a {@code byte}.
     * @throws ArithmeticException if {@code this} has a nonzero
     *         fractional part, or will not fit in a {@code byte}.
     * @since  1.5
     */
    public byte byteValueExact() {
        long num;
        num = this.longValueExact();     // will check decimal part
        if ((byte)num != num)
            throw new ArithmeticException("Overflow");
        return (byte)num;
    }

    /**
     * Converts this {@code DMatma} to a {@code float}.
     * This conversion is similar to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code float} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * if this {@code DMatma} has too great a
     * magnitude to represent as a {@code float}, it will be
     * converted to {@link Float#NEGATIVE_INFINITY} or {@link
     * Float#POSITIVE_INFINITY} as appropriate.  Note that even when
     * the return value is finite, this conversion can lose
     * information about the precision of the {@code DMatma}
     * value.
     *
     * @return this {@code DMatma} converted to a {@code float}.
     */
    public float floatValue(){
        if(intCompact != INFLATED) {
            if (scale == 0) {
                return (float)intCompact;
            } else {
                /*
                 * If both intCompact and the scale can be exactly
                 * represented as float values, perform a single float
                 * multiply or divide to compute the (properly
                 * rounded) result.
                 */
                if (Math.abs(intCompact) < 1L<<22 ) {
                    // Don't have too guard against
                    // Math.abs(MIN_VALUE) because of outer check
                    // against INFLATED.
                    if (scale > 0 && scale < float10pow.length) {
                        return (float)intCompact / float10pow[scale];
                    } else if (scale < 0 && scale > -float10pow.length) {
                        return (float)intCompact * float10pow[-scale];
                    }
                }
            }
        }
        // Somewhat inefficient, but guaranteed to work.
        return Float.parseFloat(this.toString());
    }

    /**
     * Converts this {@code DMatma} to a {@code double}.
     * This conversion is similar to the
     * <i>narrowing primitive conversion</i> from {@code double} to
     * {@code float} as defined in section 5.1.3 of
     * <cite>The Java&trade; Language Specification</cite>:
     * if this {@code DMatma} has too great a
     * magnitude represent as a {@code double}, it will be
     * converted to {@link Double#NEGATIVE_INFINITY} or {@link
     * Double#POSITIVE_INFINITY} as appropriate.  Note that even when
     * the return value is finite, this conversion can lose
     * information about the precision of the {@code DMatma}
     * value.
     *
     * @return this {@code DMatma} converted to a {@code double}.
     */
    public double doubleValue(){
        if(intCompact != INFLATED) {
            if (scale == 0) {
                return (double)intCompact;
            } else {
                /*
                 * If both intCompact and the scale can be exactly
                 * represented as double values, perform a single
                 * double multiply or divide to compute the (properly
                 * rounded) result.
                 */
                if (Math.abs(intCompact) < 1L<<52 ) {
                    // Don't have too guard against
                    // Math.abs(MIN_VALUE) because of outer check
                    // against INFLATED.
                    if (scale > 0 && scale < double10pow.length) {
                        return (double)intCompact / double10pow[scale];
                    } else if (scale < 0 && scale > -double10pow.length) {
                        return (double)intCompact * double10pow[-scale];
                    }
                }
            }
        }
        // Somewhat inefficient, but guaranteed to work.
        return Double.parseDouble(this.toString());
    }

    /**
     * Powers of 10 which can be represented exactly in {@code
     * double}.
     */
    private static final double double10pow[] = {
            1.0e0,  1.0e1,  1.0e2,  1.0e3,  1.0e4,  1.0e5,
            1.0e6,  1.0e7,  1.0e8,  1.0e9,  1.0e10, 1.0e11,
            1.0e12, 1.0e13, 1.0e14, 1.0e15, 1.0e16, 1.0e17,
            1.0e18, 1.0e19, 1.0e20, 1.0e21, 1.0e22
    };

    /**
     * Powers of 10 which can be represented exactly in {@code
     * float}.
     */
    private static final float float10pow[] = {
            1.0e0f, 1.0e1f, 1.0e2f, 1.0e3f, 1.0e4f, 1.0e5f,
            1.0e6f, 1.0e7f, 1.0e8f, 1.0e9f, 1.0e10f
    };

    /**
     * Returns the size of an ulp, a unit in the last place, of this
     * {@code DMatma}.  An ulp of a nonzero {@code DMatma}
     * value is the positive distance between this value and the
     * {@code DMatma} value next larger in magnitude with the
     * same number of digits.  An ulp of a zero value is numerically
     * equal to 1 with the scale of {@code this}.  The result is
     * stored with the same scale as {@code this} so the result
     * for zero and nonzero values is equal to {@code [1,
     * this.scale()]}.
     *
     * @return the size of an ulp of {@code this}
     * @since 1.5
     */
    public DMatma ulp() {
        return DMatma.valueOf(1, this.scale(), 1);
    }

    // Private class to build a string representation for DMatma object.
    // "StringBuilderHelper" is constructed as a thread local variable so it is
    // thread safe. The StringBuilder field acts as a buffer to hold the temporary
    // representation of DMatma. The cmpCharArray holds all the characters for
    // the compact representation of DMatma (except for '-' sign' if it is
    // negative) if its intCompact field is not INFLATED. It is shared by all
    // calls to toString() and its variants in that particular thread.
    static class StringBuilderHelper {
        final StringBuilder sb;    // Placeholder for DMatma string
        final char[] cmpCharArray; // character array to place the intCompact

        StringBuilderHelper() {
            sb = new StringBuilder();
            // All non negative longs can be made to fit into 19 character array.
            cmpCharArray = new char[19];
        }

        // Accessors.
        StringBuilder getStringBuilder() {
            sb.setLength(0);
            return sb;
        }

        char[] getCompactCharArray() {
            return cmpCharArray;
        }

        /**
         * Places characters representing the intCompact in {@code long} into
         * cmpCharArray and returns the offset to the array where the
         * representation starts.
         *
         * @param intCompact the number to put into the cmpCharArray.
         * @return offset to the array where the representation starts.
         * Note: intCompact must be greater or equal to zero.
         */
        int putIntCompact(long intCompact) {
            assert intCompact >= 0;

            long q;
            int r;
            // since we start from the least significant digit, charPos points to
            // the last character in cmpCharArray.
            int charPos = cmpCharArray.length;

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (intCompact > Integer.MAX_VALUE) {
                q = intCompact / 100;
                r = (int)(intCompact - q * 100);
                intCompact = q;
                cmpCharArray[--charPos] = DIGIT_ONES[r];
                cmpCharArray[--charPos] = DIGIT_TENS[r];
            }

            // Get 2 digits/iteration using ints when i2 >= 100
            int q2;
            int i2 = (int)intCompact;
            while (i2 >= 100) {
                q2 = i2 / 100;
                r  = i2 - q2 * 100;
                i2 = q2;
                cmpCharArray[--charPos] = DIGIT_ONES[r];
                cmpCharArray[--charPos] = DIGIT_TENS[r];
            }

            cmpCharArray[--charPos] = DIGIT_ONES[i2];
            if (i2 >= 10)
                cmpCharArray[--charPos] = DIGIT_TENS[i2];

            return charPos;
        }

        final static char[] DIGIT_TENS = {
                '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
                '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
                '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
                '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
                '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
                '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
                '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
                '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
                '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
                '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        };

        final static char[] DIGIT_ONES = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        };
    }

    /**
     * Lay out this {@code DMatma} into a {@code char[]} array.
     * The Java 1.2 equivalent to this was called {@code getValueString}.
     *
     * @param  sci {@code true} for Scientific exponential notation;
     *          {@code false} for Engineering
     * @return string with canonical string representation of this
     *         {@code DMatma}
     */
    private String layoutChars(boolean sci) {
        if (scale == 0)                      // zero scale is trivial
            return (intCompact != INFLATED) ?
                    Long.toString(intCompact):
                    intVal.toString();
        if (scale == 2  &&
                intCompact >= 0 && intCompact < Integer.MAX_VALUE) {
            // currency fast path
            int lowInt = (int)intCompact % 100;
            int highInt = (int)intCompact / 100;
            return (Integer.toString(highInt) + '.' +
                    StringBuilderHelper.DIGIT_TENS[lowInt] +
                    StringBuilderHelper.DIGIT_ONES[lowInt]) ;
        }

        StringBuilderHelper sbHelper = threadLocalStringBuilderHelper.get();
        char[] coeff;
        int offset;  // offset is the starting index for coeff array
        // Get the significand as an absolute value
        if (intCompact != INFLATED) {
            offset = sbHelper.putIntCompact(Math.abs(intCompact));
            coeff  = sbHelper.getCompactCharArray();
        } else {
            offset = 0;
            coeff  = intVal.abs().toString().toCharArray();
        }

        // Construct a buffer, with sufficient capacity for all cases.
        // If E-notation is needed, length will be: +1 if negative, +1
        // if '.' needed, +2 for "E+", + up to 10 for adjusted exponent.
        // Otherwise it could have +1 if negative, plus leading "0.00000"
        StringBuilder buf = sbHelper.getStringBuilder();
        if (signum() < 0)             // prefix '-' if negative
            buf.append('-');
        int coeffLen = coeff.length - offset;
        long adjusted = -(long)scale + (coeffLen -1);
        if ((scale >= 0) && (adjusted >= -6)) { // plain number
            int pad = scale - coeffLen;         // count of padding zeros
            if (pad >= 0) {                     // 0.xxx form
                buf.append('0');
                buf.append('.');
                for (; pad>0; pad--) {
                    buf.append('0');
                }
                buf.append(coeff, offset, coeffLen);
            } else {                         // xx.xx form
                buf.append(coeff, offset, -pad);
                buf.append('.');
                buf.append(coeff, -pad + offset, scale);
            }
        } else { // E-notation is needed
            if (sci) {                       // Scientific notation
                buf.append(coeff[offset]);   // first character
                if (coeffLen > 1) {          // more to come
                    buf.append('.');
                    buf.append(coeff, offset + 1, coeffLen - 1);
                }
            } else {                         // Engineering notation
                int sig = (int)(adjusted % 3);
                if (sig < 0)
                    sig += 3;                // [adjusted was negative]
                adjusted -= sig;             // now a multiple of 3
                sig++;
                if (signum() == 0) {
                    switch (sig) {
                        case 1:
                            buf.append('0'); // exponent is a multiple of three
                            break;
                        case 2:
                            buf.append("0.00");
                            adjusted += 3;
                            break;
                        case 3:
                            buf.append("0.0");
                            adjusted += 3;
                            break;
                        default:
                            throw new AssertionError("Unexpected sig value " + sig);
                    }
                } else if (sig >= coeffLen) {   // significand all in integer
                    buf.append(coeff, offset, coeffLen);
                    // may need some zeros, too
                    for (int i = sig - coeffLen; i > 0; i--)
                        buf.append('0');
                } else {                     // xx.xxE form
                    buf.append(coeff, offset, sig);
                    buf.append('.');
                    buf.append(coeff, offset + sig, coeffLen - sig);
                }
            }
            if (adjusted != 0) {             // [!sci could have made 0]
                buf.append('E');
                if (adjusted > 0)            // force sign for positive
                    buf.append('+');
                buf.append(adjusted);
            }
        }
        return buf.toString();
    }

    /**
     * Return 10 to the power n, as a {@code Matma}.
     *
     * @param  n the power of ten to be returned (>=0)
     * @return a {@code Matma} with the value (10<sup>n</sup>)
     */
    private static Matma bigTenToThe(int n) {
        if (n < 0)
            return Matma.ZERO;

        if (n < BIG_TEN_POWERS_TABLE_MAX) {
            Matma[] pows = BIG_TEN_POWERS_TABLE;
            if (n < pows.length)
                return pows[n];
            else
                return expandBigIntegerTenPowers(n);
        }

        return Matma.TEN.pow(n);
    }

    /**
     * Expand the BIG_TEN_POWERS_TABLE array to contain at least 10**n.
     *
     * @param n the power of ten to be returned (>=0)
     * @return a {@code DMatma} with the value (10<sup>n</sup>) and
     *         in the meantime, the BIG_TEN_POWERS_TABLE array gets
     *         expanded to the size greater than n.
     */
    private static Matma expandBigIntegerTenPowers(int n) {
        synchronized(DMatma.class) {
            Matma[] pows = BIG_TEN_POWERS_TABLE;
            int curLen = pows.length;
            // The following comparison and the above synchronized statement is
            // to prevent multiple threads from expanding the same array.
            if (curLen <= n) {
                int newLen = curLen << 1;
                while (newLen <= n)
                    newLen <<= 1;
                pows = Arrays.copyOf(pows, newLen);
                for (int i = curLen; i < newLen; i++)
                    pows[i] = pows[i - 1].multiply(Matma.TEN);
                // Based on the following facts:
                // 1. pows is a private local varible;
                // 2. the following store is a volatile store.
                // the newly created array elements can be safely published.
                BIG_TEN_POWERS_TABLE = pows;
            }
            return pows[n];
        }
    }

    private static final long[] LONG_TEN_POWERS_TABLE = {
            1,                     // 0 / 10^0
            10,                    // 1 / 10^1
            100,                   // 2 / 10^2
            1000,                  // 3 / 10^3
            10000,                 // 4 / 10^4
            100000,                // 5 / 10^5
            1000000,               // 6 / 10^6
            10000000,              // 7 / 10^7
            100000000,             // 8 / 10^8
            1000000000,            // 9 / 10^9
            10000000000L,          // 10 / 10^10
            100000000000L,         // 11 / 10^11
            1000000000000L,        // 12 / 10^12
            10000000000000L,       // 13 / 10^13
            100000000000000L,      // 14 / 10^14
            1000000000000000L,     // 15 / 10^15
            10000000000000000L,    // 16 / 10^16
            100000000000000000L,   // 17 / 10^17
            1000000000000000000L   // 18 / 10^18
    };

    private static volatile Matma BIG_TEN_POWERS_TABLE[] = {
            Matma.ONE,
            Matma.valueOf(10),
            Matma.valueOf(100),
            Matma.valueOf(1000),
            Matma.valueOf(10000),
            Matma.valueOf(100000),
            Matma.valueOf(1000000),
            Matma.valueOf(10000000),
            Matma.valueOf(100000000),
            Matma.valueOf(1000000000),
            Matma.valueOf(10000000000L),
            Matma.valueOf(100000000000L),
            Matma.valueOf(1000000000000L),
            Matma.valueOf(10000000000000L),
            Matma.valueOf(100000000000000L),
            Matma.valueOf(1000000000000000L),
            Matma.valueOf(10000000000000000L),
            Matma.valueOf(100000000000000000L),
            Matma.valueOf(1000000000000000000L)
    };

    private static final int BIG_TEN_POWERS_TABLE_INITLEN =
            BIG_TEN_POWERS_TABLE.length;
    private static final int BIG_TEN_POWERS_TABLE_MAX =
            16 * BIG_TEN_POWERS_TABLE_INITLEN;

    private static final long THRESHOLDS_TABLE[] = {
            Long.MAX_VALUE,                     // 0
            Long.MAX_VALUE/10L,                 // 1
            Long.MAX_VALUE/100L,                // 2
            Long.MAX_VALUE/1000L,               // 3
            Long.MAX_VALUE/10000L,              // 4
            Long.MAX_VALUE/100000L,             // 5
            Long.MAX_VALUE/1000000L,            // 6
            Long.MAX_VALUE/10000000L,           // 7
            Long.MAX_VALUE/100000000L,          // 8
            Long.MAX_VALUE/1000000000L,         // 9
            Long.MAX_VALUE/10000000000L,        // 10
            Long.MAX_VALUE/100000000000L,       // 11
            Long.MAX_VALUE/1000000000000L,      // 12
            Long.MAX_VALUE/10000000000000L,     // 13
            Long.MAX_VALUE/100000000000000L,    // 14
            Long.MAX_VALUE/1000000000000000L,   // 15
            Long.MAX_VALUE/10000000000000000L,  // 16
            Long.MAX_VALUE/100000000000000000L, // 17
            Long.MAX_VALUE/1000000000000000000L // 18
    };

    /**
     * Compute val * 10 ^ n; return this product if it is
     * representable as a long, INFLATED otherwise.
     */
    private static long longMultiplyPowerTen(long val, int n) {
        if (val == 0 || n <= 0)
            return val;
        long[] tab = LONG_TEN_POWERS_TABLE;
        long[] bounds = THRESHOLDS_TABLE;
        if (n < tab.length && n < bounds.length) {
            long tenpower = tab[n];
            if (val == 1)
                return tenpower;
            if (Math.abs(val) <= bounds[n])
                return val * tenpower;
        }
        return INFLATED;
    }

    /**
     * Compute this * 10 ^ n.
     * Needed mainly to allow special casing to trap zero value
     */
    private Matma bigMultiplyPowerTen(int n) {
        if (n <= 0)
            return this.inflated();

        if (intCompact != INFLATED)
            return bigTenToThe(n).multiply(intCompact);
        else
            return intVal.multiply(bigTenToThe(n));
    }

    /**
     * Returns appropriate Matma from intVal field if intVal is
     * null, i.e. the compact representation is in use.
     */
    private Matma inflated() {
        if (intVal == null) {
            return Matma.valueOf(intCompact);
        }
        return intVal;
    }

    /**
     * Match the scales of two {@code DMatma}s to align their
     * least significant digits.
     *
     * <p>If the scales of val[0] and val[1] differ, rescale
     * (non-destructively) the lower-scaled {@code DMatma} so
     * they match.  That is, the lower-scaled reference will be
     * replaced by a reference to a new object with the same scale as
     * the other {@code DMatma}.
     *
     * @param  val array of two elements referring to the two
     *         {@code DMatma}s to be aligned.
     */
    private static void matchScale(DMatma[] val) {
        if (val[0].scale == val[1].scale) {
            return;
        } else if (val[0].scale < val[1].scale) {
            val[0] = val[0].setScale(val[1].scale, ROUND_UNNECESSARY);
        } else if (val[1].scale < val[0].scale) {
            val[1] = val[1].setScale(val[0].scale, ROUND_UNNECESSARY);
        }
    }

    private static class UnsafeHolder {
        private static final sun.misc.Unsafe unsafe;
        private static final long intCompactOffset;
        private static final long intValOffset;
        static {
            try {
                unsafe = sun.misc.Unsafe.getUnsafe();
                intCompactOffset = unsafe.objectFieldOffset
                        (DMatma.class.getDeclaredField("intCompact"));
                intValOffset = unsafe.objectFieldOffset
                        (DMatma.class.getDeclaredField("intVal"));
            } catch (Exception ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        static void setIntCompactVolatile(DMatma bd, long val) {
            unsafe.putLongVolatile(bd, intCompactOffset, val);
        }

        static void setIntValVolatile(DMatma bd, Matma val) {
            unsafe.putObjectVolatile(bd, intValOffset, val);
        }
    }

    /**
     * Reconstitute the {@code DMatma} instance from a stream (that is,
     * deserialize it).
     *
     * @param s the stream being read.
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in all fields
        s.defaultReadObject();
        // validate possibly bad fields
        if (intVal == null) {
            String message = "DMatma: null intVal in stream";
            throw new java.io.StreamCorruptedException(message);
            // [all values of scale are now allowed]
        }
        UnsafeHolder.setIntCompactVolatile(this, compactValFor(intVal));
    }

    /**
     * Serialize this {@code DMatma} to the stream in question
     *
     * @param s the stream to serialize to.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Must inflate to maintain compatible serial form.
        if (this.intVal == null)
            UnsafeHolder.setIntValVolatile(this, Matma.valueOf(this.intCompact));
        // Could reset intVal back to null if it has to be set.
        s.defaultWriteObject();
    }

    /**
     * Returns the length of the absolute value of a {@code long}, in decimal
     * digits.
     *
     * @param x the {@code long}
     * @return the length of the unscaled value, in deciaml digits.
     */
    static int longDigitLength(long x) {
        /*
         * As described in "Bit Twiddling Hacks" by Sean Anderson,
         * (http://graphics.stanford.edu/~seander/bithacks.html)
         * integer log 10 of x is within 1 of (1233/4096)* (1 +
         * integer log 2 of x). The fraction 1233/4096 approximates
         * log10(2). So we first do a version of log2 (a variant of
         * Long class with pre-checks and opposite directionality) and
         * then scale and check against powers table. This is a little
         * simpler in present context than the version in Hacker's
         * Delight sec 11-4. Adding one to bit length allows comparing
         * downward from the LONG_TEN_POWERS_TABLE that we need
         * anyway.
         */
        assert x != DMatma.INFLATED;
        if (x < 0)
            x = -x;
        if (x < 10) // must screen for 0, might as well 10
            return 1;
        int r = ((64 - Long.numberOfLeadingZeros(x) + 1) * 1233) >>> 12;
        long[] tab = LONG_TEN_POWERS_TABLE;
        // if r >= length, must have max possible digits for long
        return (r >= tab.length || x < tab[r]) ? r : r + 1;
    }

    /**
     * Returns the length of the absolute value of a Matma, in
     * decimal digits.
     *
     * @param b the Matma
     * @return the length of the unscaled value, in decimal digits
     */
    private static int bigDigitLength(Matma b) {
        /*
         * Same idea as the long version, but we need a better
         * approximation of log10(2). Using 646456993/2^31
         * is accurate up to max possible reported bitLength.
         */
        if (b.signum == 0)
            return 1;
        int r = (int)((((long)b.bitLength() + 1) * 646456993) >>> 31);
        return b.compareMagnitude(bigTenToThe(r)) < 0? r : r+1;
    }

    /**
     * Check a scale for Underflow or Overflow.  If this DMatma is
     * nonzero, throw an exception if the scale is outof range. If this
     * is zero, saturate the scale to the extreme value of the right
     * sign if the scale is out of range.
     *
     * @param val The new scale.
     * @throws ArithmeticException (overflow or underflow) if the new
     *         scale is out of range.
     * @return validated scale as an int.
     */
    private int checkScale(long val) {
        int asInt = (int)val;
        if (asInt != val) {
            asInt = val>Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            Matma b;
            if (intCompact != 0 &&
                    ((b = intVal) == null || b.signum() != 0))
                throw new ArithmeticException(asInt>0 ? "Underflow":"Overflow");
        }
        return asInt;
    }

    /**
     * Returns the compact value for given {@code Matma}, or
     * INFLATED if too big. Relies on internal representation of
     * {@code Matma}.
     */
    private static long compactValFor(Matma b) {
        int[] m = b.mag;
        int len = m.length;
        if (len == 0)
            return 0;
        int d = m[0];
        if (len > 2 || (len == 2 && d < 0))
            return INFLATED;

        long u = (len == 2)?
                (((long) m[1] & LONG_MASK) + (((long)d) << 32)) :
                (((long)d)   & LONG_MASK);
        return (b.signum < 0)? -u : u;
    }

    private static int longCompareMagnitude(long x, long y) {
        if (x < 0)
            x = -x;
        if (y < 0)
            y = -y;
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    private static int saturateLong(long s) {
        int i = (int)s;
        return (s == i) ? i : (s < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE);
    }

    /*
     * Internal printing routine
     */
    private static void print(String name, DMatma bd) {
        System.err.format("%s:\tintCompact %d\tintVal %d\tscale %d\tprecision %d%n",
                name,
                bd.intCompact,
                bd.intVal,
                bd.scale,
                bd.precision);
    }

    /**
     * Check internal invariants of this DMatma.  These invariants
     * include:
     *
     * <ul>
     *
     * <li>The object must be initialized; either intCompact must not be
     * INFLATED or intVal is non-null.  Both of these conditions may
     * be true.
     *
     * <li>If both intCompact and intVal and set, their values must be
     * consistent.
     *
     * <li>If precision is nonzero, it must have the right value.
     * </ul>
     *
     * Note: Since this is an audit method, we are not supposed to change the
     * state of this DMatma object.
     */
    private DMatma audit() {
        if (intCompact == INFLATED) {
            if (intVal == null) {
                print("audit", this);
                throw new AssertionError("null intVal");
            }
            // Check precision
            if (precision > 0 && precision != bigDigitLength(intVal)) {
                print("audit", this);
                throw new AssertionError("precision mismatch");
            }
        } else {
            if (intVal != null) {
                long val = intVal.longValue();
                if (val != intCompact) {
                    print("audit", this);
                    throw new AssertionError("Inconsistent state, intCompact=" +
                            intCompact + "\t intVal=" + val);
                }
            }
            // Check precision
            if (precision > 0 && precision != longDigitLength(intCompact)) {
                print("audit", this);
                throw new AssertionError("precision mismatch");
            }
        }
        return this;
    }

    /* the same as checkScale where value!=0 */
    private static int checkScaleNonZero(long val) {
        int asInt = (int)val;
        if (asInt != val) {
            throw new ArithmeticException(asInt>0 ? "Underflow":"Overflow");
        }
        return asInt;
    }

    private static int checkScale(long intCompact, long val) {
        int asInt = (int)val;
        if (asInt != val) {
            asInt = val>Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            if (intCompact != 0)
                throw new ArithmeticException(asInt>0 ? "Underflow":"Overflow");
        }
        return asInt;
    }

    private static int checkScale(Matma intVal, long val) {
        int asInt = (int)val;
        if (asInt != val) {
            asInt = val>Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            if (intVal.signum() != 0)
                throw new ArithmeticException(asInt>0 ? "Underflow":"Overflow");
        }
        return asInt;
    }

    /**
     * Returns a {@code DMatma} rounded according to the MathContext
     * settings;
     * If rounding is needed a new {@code DMatma} is created and returned.
     *
     * @param val the value to be rounded
     * @param mc the context to use.
     * @return a {@code DMatma} rounded according to the MathContext
     *         settings.  May return {@code value}, if no rounding needed.
     * @throws ArithmeticException if the rounding mode is
     *         {@code RoundingMode.UNNECESSARY} and the
     *         result is inexact.
     */
    private static DMatma doRound(DMatma val, MathContext mc) {
        int mcp = mc.precision;
        boolean wasDivided = false;
        if (mcp > 0) {
            Matma intVal = val.intVal;
            long compactVal = val.intCompact;
            int scale = val.scale;
            int prec = val.precision();
            int mode = mc.roundingMode.oldMode;
            int drop;
            if (compactVal == INFLATED) {
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    intVal = divideAndRoundByTenPow(intVal, drop, mode);
                    wasDivided = true;
                    compactVal = compactValFor(intVal);
                    if (compactVal != INFLATED) {
                        prec = longDigitLength(compactVal);
                        break;
                    }
                    prec = bigDigitLength(intVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                drop = prec - mcp;  // drop can't be more than 18
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                    wasDivided = true;
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                    intVal = null;
                }
            }
            return wasDivided ? new DMatma(intVal,compactVal,scale,prec) : val;
        }
        return val;
    }

    /*
     * Returns a {@code DMatma} created from {@code long} value with
     * given scale rounded according to the MathContext settings
     */
    private static DMatma doRound(long compactVal, int scale, MathContext mc) {
        int mcp = mc.precision;
        if (mcp > 0 && mcp < 19) {
            int prec = longDigitLength(compactVal);
            int drop = prec - mcp;  // drop can't be more than 18
            while (drop > 0) {
                scale = checkScaleNonZero((long) scale - drop);
                compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                prec = longDigitLength(compactVal);
                drop = prec - mcp;
            }
            return valueOf(compactVal, scale, prec);
        }
        return valueOf(compactVal, scale);
    }

    /*
     * Returns a {@code DMatma} created from {@code Matma} value with
     * given scale rounded according to the MathContext settings
     */
    private static DMatma doRound(Matma intVal, int scale, MathContext mc) {
        int mcp = mc.precision;
        int prec = 0;
        if (mcp > 0) {
            long compactVal = compactValFor(intVal);
            int mode = mc.roundingMode.oldMode;
            int drop;
            if (compactVal == INFLATED) {
                prec = bigDigitLength(intVal);
                drop = prec - mcp;
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    intVal = divideAndRoundByTenPow(intVal, drop, mode);
                    compactVal = compactValFor(intVal);
                    if (compactVal != INFLATED) {
                        break;
                    }
                    prec = bigDigitLength(intVal);
                    drop = prec - mcp;
                }
            }
            if (compactVal != INFLATED) {
                prec = longDigitLength(compactVal);
                drop = prec - mcp;     // drop can't be more than 18
                while (drop > 0) {
                    scale = checkScaleNonZero((long) scale - drop);
                    compactVal = divideAndRound(compactVal, LONG_TEN_POWERS_TABLE[drop], mc.roundingMode.oldMode);
                    prec = longDigitLength(compactVal);
                    drop = prec - mcp;
                }
                return valueOf(compactVal,scale,prec);
            }
        }
        return new DMatma(intVal,INFLATED,scale,prec);
    }

    /*
     * Divides {@code Matma} value by ten power.
     */
    private static Matma divideAndRoundByTenPow(Matma intVal, int tenPow, int roundingMode) {
        if (tenPow < LONG_TEN_POWERS_TABLE.length)
            intVal = divideAndRound(intVal, LONG_TEN_POWERS_TABLE[tenPow], roundingMode);
        else
            intVal = divideAndRound(intVal, bigTenToThe(tenPow), roundingMode);
        return intVal;
    }

    /**
     * Internally used for division operation for division {@code long} by
     * {@code long}.
     * The returned {@code DMatma} object is the quotient whose scale is set
     * to the passed in scale. If the remainder is not zero, it will be rounded
     * based on the passed in roundingMode. Also, if the remainder is zero and
     * the last parameter, i.e. preferredScale is NOT equal to scale, the
     * trailing zeros of the result is stripped to match the preferredScale.
     */
    private static DMatma divideAndRound(long ldividend, long ldivisor, int scale, int roundingMode,
                                         int preferredScale) {

        int qsign; // quotient sign
        long q = ldividend / ldivisor; // store quotient in long
        if (roundingMode == ROUND_DOWN && scale == preferredScale)
            return valueOf(q, scale);
        long r = ldividend % ldivisor; // store remainder in long
        qsign = ((ldividend < 0) == (ldivisor < 0)) ? 1 : -1;
        if (r != 0) {
            boolean increment = needIncrement(ldivisor, roundingMode, qsign, q, r);
            return valueOf((increment ? q + qsign : q), scale);
        } else {
            if (preferredScale != scale)
                return createAndStripZerosToMatchScale(q, scale, preferredScale);
            else
                return valueOf(q, scale);
        }
    }

    /**
     * Divides {@code long} by {@code long} and do rounding based on the
     * passed in roundingMode.
     */
    private static long divideAndRound(long ldividend, long ldivisor, int roundingMode) {
        int qsign; // quotient sign
        long q = ldividend / ldivisor; // store quotient in long
        if (roundingMode == ROUND_DOWN)
            return q;
        long r = ldividend % ldivisor; // store remainder in long
        qsign = ((ldividend < 0) == (ldivisor < 0)) ? 1 : -1;
        if (r != 0) {
            boolean increment = needIncrement(ldivisor, roundingMode, qsign, q,     r);
            return increment ? q + qsign : q;
        } else {
            return q;
        }
    }

    /**
     * Shared logic of need increment computation.
     */
    private static boolean commonNeedIncrement(int roundingMode, int qsign,
                                               int cmpFracHalf, boolean oddQuot) {
        switch(roundingMode) {
            case ROUND_UNNECESSARY:
                throw new ArithmeticException("Rounding necessary");

            case ROUND_UP: // Away from zero
                return true;

            case ROUND_DOWN: // Towards zero
                return false;

            case ROUND_CEILING: // Towards +infinity
                return qsign > 0;

            case ROUND_FLOOR: // Towards -infinity
                return qsign < 0;

            default: // Some kind of half-way rounding
                assert roundingMode >= ROUND_HALF_UP &&
                        roundingMode <= ROUND_HALF_EVEN: "Unexpected rounding mode" + RoundingMode.valueOf(roundingMode);

                if (cmpFracHalf < 0 ) // We're closer to higher digit
                    return false;
                else if (cmpFracHalf > 0 ) // We're closer to lower digit
                    return true;
                else { // half-way
                    assert cmpFracHalf == 0;

                    switch(roundingMode) {
                        case ROUND_HALF_DOWN:
                            return false;

                        case ROUND_HALF_UP:
                            return true;

                        case ROUND_HALF_EVEN:
                            return oddQuot;

                        default:
                            throw new AssertionError("Unexpected rounding mode" + roundingMode);
                    }
                }
        }
    }

    /**
     * Tests if quotient has to be incremented according the roundingMode
     */
    private static boolean needIncrement(long ldivisor, int roundingMode,
                                         int qsign, long q, long r) {
        assert r != 0L;

        int cmpFracHalf;
        if (r <= HALF_LONG_MIN_VALUE || r > HALF_LONG_MAX_VALUE) {
            cmpFracHalf = 1; // 2 * r can't fit into long
        } else {
            cmpFracHalf = longCompareMagnitude(2 * r, ldivisor);
        }

        return commonNeedIncrement(roundingMode, qsign, cmpFracHalf, (q & 1L) != 0L);
    }

    /**
     * Divides {@code Matma} value by {@code long} value and
     * do rounding based on the passed in roundingMode.
     */
    private static Matma divideAndRound(Matma bdividend, long ldivisor, int roundingMode) {
        boolean isRemainderZero; // record remainder is zero or not
        int qsign; // quotient sign
        long r = 0; // store quotient & remainder in long
        MutMatma mq = null; // store quotient
        // Descend into mutables for faster remainder checks
        MutMatma mdividend = new MutMatma(bdividend.mag);
        mq = new MutMatma();
        r = mdividend.divide(ldivisor, mq);
        isRemainderZero = (r == 0);
        qsign = (ldivisor < 0) ? -bdividend.signum : bdividend.signum;
        if (!isRemainderZero) {
            if(needIncrement(ldivisor, roundingMode, qsign, mq, r)) {
                mq.add(MutMatma.ONE);
            }
        }
        return mq.toMatma(qsign);
    }

    /**
     * Internally used for division operation for division {@code Matma}
     * by {@code long}.
     * The returned {@code DMatma} object is the quotient whose scale is set
     * to the passed in scale. If the remainder is not zero, it will be rounded
     * based on the passed in roundingMode. Also, if the remainder is zero and
     * the last parameter, i.e. preferredScale is NOT equal to scale, the
     * trailing zeros of the result is stripped to match the preferredScale.
     */
    private static DMatma divideAndRound(Matma bdividend,
                                         long ldivisor, int scale, int roundingMode, int preferredScale) {
        boolean isRemainderZero; // record remainder is zero or not
        int qsign; // quotient sign
        long r = 0; // store quotient & remainder in long
        MutMatma mq = null; // store quotient
        // Descend into mutables for faster remainder checks
        MutMatma mdividend = new MutMatma(bdividend.mag);
        mq = new MutMatma();
        r = mdividend.divide(ldivisor, mq);
        isRemainderZero = (r == 0);
        qsign = (ldivisor < 0) ? -bdividend.signum : bdividend.signum;
        if (!isRemainderZero) {
            if(needIncrement(ldivisor, roundingMode, qsign, mq, r)) {
                mq.add(MutMatma.ONE);
            }
            return mq.toBigDecimal(qsign, scale);
        } else {
            if (preferredScale != scale) {
                long compactVal = mq.toCompactValue(qsign);
                if(compactVal!=INFLATED) {
                    return createAndStripZerosToMatchScale(compactVal, scale, preferredScale);
                }
                Matma intVal =  mq.toMatma(qsign);
                return createAndStripZerosToMatchScale(intVal,scale, preferredScale);
            } else {
                return mq.toBigDecimal(qsign, scale);
            }
        }
    }

    /**
     * Tests if quotient has to be incremented according the roundingMode
     */
    private static boolean needIncrement(long ldivisor, int roundingMode,
                                         int qsign, MutMatma mq, long r) {
        assert r != 0L;

        int cmpFracHalf;
        if (r <= HALF_LONG_MIN_VALUE || r > HALF_LONG_MAX_VALUE) {
            cmpFracHalf = 1; // 2 * r can't fit into long
        } else {
            cmpFracHalf = longCompareMagnitude(2 * r, ldivisor);
        }

        return commonNeedIncrement(roundingMode, qsign, cmpFracHalf, mq.isOdd());
    }

    /**
     * Divides {@code Matma} value by {@code Matma} value and
     * do rounding based on the passed in roundingMode.
     */
    private static Matma divideAndRound(Matma bdividend, Matma bdivisor, int roundingMode) {
        boolean isRemainderZero; // record remainder is zero or not
        int qsign; // quotient sign
        // Descend into mutables for faster remainder checks
        MutMatma mdividend = new MutMatma(bdividend.mag);
        MutMatma mq = new MutMatma();
        MutMatma mdivisor = new MutMatma(bdivisor.mag);
        MutMatma mr = mdividend.divide(mdivisor, mq);
        isRemainderZero = mr.isZero();
        qsign = (bdividend.signum != bdivisor.signum) ? -1 : 1;
        if (!isRemainderZero) {
            if (needIncrement(mdivisor, roundingMode, qsign, mq, mr)) {
                mq.add(MutMatma.ONE);
            }
        }
        return mq.toMatma(qsign);
    }

    /**
     * Internally used for division operation for division {@code Matma}
     * by {@code Matma}.
     * The returned {@code DMatma} object is the quotient whose scale is set
     * to the passed in scale. If the remainder is not zero, it will be rounded
     * based on the passed in roundingMode. Also, if the remainder is zero and
     * the last parameter, i.e. preferredScale is NOT equal to scale, the
     * trailing zeros of the result is stripped to match the preferredScale.
     */
    private static DMatma divideAndRound(Matma bdividend, Matma bdivisor, int scale, int roundingMode,
                                         int preferredScale) {
        boolean isRemainderZero; // record remainder is zero or not
        int qsign; // quotient sign
        // Descend into mutables for faster remainder checks
        MutMatma mdividend = new MutMatma(bdividend.mag);
        MutMatma mq = new MutMatma();
        MutMatma mdivisor = new MutMatma(bdivisor.mag);
        MutMatma mr = mdividend.divide(mdivisor, mq);
        isRemainderZero = mr.isZero();
        qsign = (bdividend.signum != bdivisor.signum) ? -1 : 1;
        if (!isRemainderZero) {
            if (needIncrement(mdivisor, roundingMode, qsign, mq, mr)) {
                mq.add(MutMatma.ONE);
            }
            return mq.toBigDecimal(qsign, scale);
        } else {
            if (preferredScale != scale) {
                long compactVal = mq.toCompactValue(qsign);
                if (compactVal != INFLATED) {
                    return createAndStripZerosToMatchScale(compactVal, scale, preferredScale);
                }
                Matma intVal = mq.toMatma(qsign);
                return createAndStripZerosToMatchScale(intVal, scale, preferredScale);
            } else {
                return mq.toBigDecimal(qsign, scale);
            }
        }
    }

    /**
     * Tests if quotient has to be incremented according the roundingMode
     */
    private static boolean needIncrement(MutMatma mdivisor, int roundingMode,
                                         int qsign, MutMatma mq, MutMatma mr) {
        assert !mr.isZero();
        int cmpFracHalf = mr.compareHalf(mdivisor);
        return commonNeedIncrement(roundingMode, qsign, cmpFracHalf, mq.isOdd());
    }

    /**
     * Remove insignificant trailing zeros from this
     * {@code Matma} value until the preferred scale is reached or no
     * more zeros can be removed.  If the preferred scale is less than
     * Integer.MIN_VALUE, all the trailing zeros will be removed.
     *
     * @return new {@code DMatma} with a scale possibly reduced
     * to be closed to the preferred scale.
     */
    private static DMatma createAndStripZerosToMatchScale(Matma intVal, int scale, long preferredScale) {
        Matma qr[]; // quotient-remainder pair
        while (intVal.compareMagnitude(Matma.TEN) >= 0
                && scale > preferredScale) {
            if (intVal.testBit(0))
                break; // odd number cannot end in 0
            qr = intVal.divideAndRemainder(Matma.TEN);
            if (qr[1].signum() != 0)
                break; // non-0 remainder
            intVal = qr[0];
            scale = checkScale(intVal,(long) scale - 1); // could Overflow
        }
        return valueOf(intVal, scale, 0);
    }

    /**
     * Remove insignificant trailing zeros from this
     * {@code long} value until the preferred scale is reached or no
     * more zeros can be removed.  If the preferred scale is less than
     * Integer.MIN_VALUE, all the trailing zeros will be removed.
     *
     * @return new {@code DMatma} with a scale possibly reduced
     * to be closed to the preferred scale.
     */
    private static DMatma createAndStripZerosToMatchScale(long compactVal, int scale, long preferredScale) {
        while (Math.abs(compactVal) >= 10L && scale > preferredScale) {
            if ((compactVal & 1L) != 0L)
                break; // odd number cannot end in 0
            long r = compactVal % 10L;
            if (r != 0L)
                break; // non-0 remainder
            compactVal /= 10;
            scale = checkScale(compactVal, (long) scale - 1); // could Overflow
        }
        return valueOf(compactVal, scale);
    }

    private static DMatma stripZerosToMatchScale(Matma intVal, long intCompact, int scale, int preferredScale) {
        if(intCompact!=INFLATED) {
            return createAndStripZerosToMatchScale(intCompact, scale, preferredScale);
        } else {
            return createAndStripZerosToMatchScale(intVal==null ? INFLATED_BIGINT : intVal,
                    scale, preferredScale);
        }
    }

    /*
     * returns INFLATED if oveflow
     */
    private static long add(long xs, long ys){
        long sum = xs + ys;
        // See "Hacker's Delight" section 2-12 for explanation of
        // the overflow test.
        if ( (((sum ^ xs) & (sum ^ ys))) >= 0L) { // not overflowed
            return sum;
        }
        return INFLATED;
    }

    private static DMatma add(long xs, long ys, int scale){
        long sum = add(xs, ys);
        if (sum!=INFLATED)
            return DMatma.valueOf(sum, scale);
        return new DMatma(Matma.valueOf(xs).add(ys), scale);
    }

    private static DMatma add(final long xs, int scale1, final long ys, int scale2) {
        long sdiff = (long) scale1 - scale2;
        if (sdiff == 0) {
            return add(xs, ys, scale1);
        } else if (sdiff < 0) {
            int raise = checkScale(xs,-sdiff);
            long scaledX = longMultiplyPowerTen(xs, raise);
            if (scaledX != INFLATED) {
                return add(scaledX, ys, scale2);
            } else {
                Matma bigsum = bigMultiplyPowerTen(xs,raise).add(ys);
                return ((xs^ys)>=0) ? // same sign test
                        new DMatma(bigsum, INFLATED, scale2, 0)
                        : valueOf(bigsum, scale2, 0);
            }
        } else {
            int raise = checkScale(ys,sdiff);
            long scaledY = longMultiplyPowerTen(ys, raise);
            if (scaledY != INFLATED) {
                return add(xs, scaledY, scale1);
            } else {
                Matma bigsum = bigMultiplyPowerTen(ys,raise).add(xs);
                return ((xs^ys)>=0) ?
                        new DMatma(bigsum, INFLATED, scale1, 0)
                        : valueOf(bigsum, scale1, 0);
            }
        }
    }

    private static DMatma add(final long xs, int scale1, Matma snd, int scale2) {
        int rscale = scale1;
        long sdiff = (long)rscale - scale2;
        boolean sameSigns =  (Long.signum(xs) == snd.signum);
        Matma sum;
        if (sdiff < 0) {
            int raise = checkScale(xs,-sdiff);
            rscale = scale2;
            long scaledX = longMultiplyPowerTen(xs, raise);
            if (scaledX == INFLATED) {
                sum = snd.add(bigMultiplyPowerTen(xs,raise));
            } else {
                sum = snd.add(scaledX);
            }
        } else { //if (sdiff > 0) {
            int raise = checkScale(snd,sdiff);
            snd = bigMultiplyPowerTen(snd,raise);
            sum = snd.add(xs);
        }
        return (sameSigns) ?
                new DMatma(sum, INFLATED, rscale, 0) :
                valueOf(sum, rscale, 0);
    }

    private static DMatma add(Matma fst, int scale1, Matma snd, int scale2) {
        int rscale = scale1;
        long sdiff = (long)rscale - scale2;
        if (sdiff != 0) {
            if (sdiff < 0) {
                int raise = checkScale(fst,-sdiff);
                rscale = scale2;
                fst = bigMultiplyPowerTen(fst,raise);
            } else {
                int raise = checkScale(snd,sdiff);
                snd = bigMultiplyPowerTen(snd,raise);
            }
        }
        Matma sum = fst.add(snd);
        return (fst.signum == snd.signum) ?
                new DMatma(sum, INFLATED, rscale, 0) :
                valueOf(sum, rscale, 0);
    }

    private static Matma bigMultiplyPowerTen(long value, int n) {
        if (n <= 0)
            return Matma.valueOf(value);
        return bigTenToThe(n).multiply(value);
    }

    private static Matma bigMultiplyPowerTen(Matma value, int n) {
        if (n <= 0)
            return value;
        if(n<LONG_TEN_POWERS_TABLE.length) {
            return value.multiply(LONG_TEN_POWERS_TABLE[n]);
        }
        return value.multiply(bigTenToThe(n));
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (xs /
     * ys)}, with rounding according to the context settings.
     *
     * Fast path - used only when (xscale <= yscale && yscale < 18
     *  && mc.presision<18) {
     */
    private static DMatma divideSmallFastPath(final long xs, int xscale,
                                              final long ys, int yscale,
                                              long preferredScale, MathContext mc) {
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        assert (xscale <= yscale) && (yscale < 18) && (mcp < 18);
        int xraise = yscale - xscale; // xraise >=0
        long scaledX = (xraise==0) ? xs :
                longMultiplyPowerTen(xs, xraise); // can't overflow here!
        DMatma quotient;

        int cmp = longCompareMagnitude(scaledX, ys);
        if(cmp > 0) { // satisfy constraint (b)
            yscale -= 1; // [that is, divisor *= 10]
            int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
            if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
                // assert newScale >= xscale
                int raise = checkScaleNonZero((long) mcp + yscale - xscale);
                long scaledXs;
                if ((scaledXs = longMultiplyPowerTen(xs, raise)) == INFLATED) {
                    quotient = null;
                    if((mcp-1) >=0 && (mcp-1)<LONG_TEN_POWERS_TABLE.length) {
                        quotient = multiplyDivideAndRound(LONG_TEN_POWERS_TABLE[mcp-1], scaledX, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                    if(quotient==null) {
                        Matma rb = bigMultiplyPowerTen(scaledX,mcp-1);
                        quotient = divideAndRound(rb, ys,
                                scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                } else {
                    quotient = divideAndRound(scaledXs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                }
            } else {
                int newScale = checkScaleNonZero((long) xscale - mcp);
                // assert newScale >= yscale
                if (newScale == yscale) { // easy case
                    quotient = divideAndRound(xs, ys, scl, roundingMode,checkScaleNonZero(preferredScale));
                } else {
                    int raise = checkScaleNonZero((long) newScale - yscale);
                    long scaledYs;
                    if ((scaledYs = longMultiplyPowerTen(ys, raise)) == INFLATED) {
                        Matma rb = bigMultiplyPowerTen(ys,raise);
                        quotient = divideAndRound(Matma.valueOf(xs),
                                rb, scl, roundingMode,checkScaleNonZero(preferredScale));
                    } else {
                        quotient = divideAndRound(xs, scaledYs, scl, roundingMode,checkScaleNonZero(preferredScale));
                    }
                }
            }
        } else {
            // abs(scaledX) <= abs(ys)
            // result is "scaledX * 10^msp / ys"
            int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
            if(cmp==0) {
                // abs(scaleX)== abs(ys) => result will be scaled 10^mcp + correct sign
                quotient = roundedTenPower(((scaledX < 0) == (ys < 0)) ? 1 : -1, mcp, scl, checkScaleNonZero(preferredScale));
            } else {
                // abs(scaledX) < abs(ys)
                long scaledXs;
                if ((scaledXs = longMultiplyPowerTen(scaledX, mcp)) == INFLATED) {
                    quotient = null;
                    if(mcp<LONG_TEN_POWERS_TABLE.length) {
                        quotient = multiplyDivideAndRound(LONG_TEN_POWERS_TABLE[mcp], scaledX, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                    if(quotient==null) {
                        Matma rb = bigMultiplyPowerTen(scaledX,mcp);
                        quotient = divideAndRound(rb, ys,
                                scl, roundingMode, checkScaleNonZero(preferredScale));
                    }
                } else {
                    quotient = divideAndRound(scaledXs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
                }
            }
        }
        // doRound, here, only affects 1000000000 case.
        return doRound(quotient,mc);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (xs /
     * ys)}, with rounding according to the context settings.
     */
    private static DMatma divide(final long xs, int xscale, final long ys, int yscale, long preferredScale, MathContext mc) {
        int mcp = mc.precision;
        if(xscale <= yscale && yscale < 18 && mcp<18) {
            return divideSmallFastPath(xs, xscale, ys, yscale, preferredScale, mc);
        }
        if (compareMagnitudeNormalized(xs, xscale, ys, yscale) > 0) {// satisfy constraint (b)
            yscale -= 1; // [that is, divisor *= 10]
        }
        int roundingMode = mc.roundingMode.oldMode;
        // In order to find out whether the divide generates the exact result,
        // we avoid calling the above divide method. 'quotient' holds the
        // return DMatma object whose scale will be set to 'scl'.
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        DMatma quotient;
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            long scaledXs;
            if ((scaledXs = longMultiplyPowerTen(xs, raise)) == INFLATED) {
                Matma rb = bigMultiplyPowerTen(xs,raise);
                quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
            } else {
                quotient = divideAndRound(scaledXs, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
            }
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);
            // assert newScale >= yscale
            if (newScale == yscale) { // easy case
                quotient = divideAndRound(xs, ys, scl, roundingMode,checkScaleNonZero(preferredScale));
            } else {
                int raise = checkScaleNonZero((long) newScale - yscale);
                long scaledYs;
                if ((scaledYs = longMultiplyPowerTen(ys, raise)) == INFLATED) {
                    Matma rb = bigMultiplyPowerTen(ys,raise);
                    quotient = divideAndRound(Matma.valueOf(xs),
                            rb, scl, roundingMode,checkScaleNonZero(preferredScale));
                } else {
                    quotient = divideAndRound(xs, scaledYs, scl, roundingMode,checkScaleNonZero(preferredScale));
                }
            }
        }
        // doRound, here, only affects 1000000000 case.
        return doRound(quotient,mc);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (xs /
     * ys)}, with rounding according to the context settings.
     */
    private static DMatma divide(Matma xs, int xscale, long ys, int yscale, long preferredScale, MathContext mc) {
        // Normalize dividend & divisor so that both fall into [0.1, 0.999...]
        if ((-compareMagnitudeNormalized(ys, yscale, xs, xscale)) > 0) {// satisfy constraint (b)
            yscale -= 1; // [that is, divisor *= 10]
        }
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        // In order to find out whether the divide generates the exact result,
        // we avoid calling the above divide method. 'quotient' holds the
        // return DMatma object whose scale will be set to 'scl'.
        DMatma quotient;
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            Matma rb = bigMultiplyPowerTen(xs,raise);
            quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);
            // assert newScale >= yscale
            if (newScale == yscale) { // easy case
                quotient = divideAndRound(xs, ys, scl, roundingMode,checkScaleNonZero(preferredScale));
            } else {
                int raise = checkScaleNonZero((long) newScale - yscale);
                long scaledYs;
                if ((scaledYs = longMultiplyPowerTen(ys, raise)) == INFLATED) {
                    Matma rb = bigMultiplyPowerTen(ys,raise);
                    quotient = divideAndRound(xs, rb, scl, roundingMode,checkScaleNonZero(preferredScale));
                } else {
                    quotient = divideAndRound(xs, scaledYs, scl, roundingMode,checkScaleNonZero(preferredScale));
                }
            }
        }
        // doRound, here, only affects 1000000000 case.
        return doRound(quotient, mc);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (xs /
     * ys)}, with rounding according to the context settings.
     */
    private static DMatma divide(long xs, int xscale, Matma ys, int yscale, long preferredScale, MathContext mc) {
        // Normalize dividend & divisor so that both fall into [0.1, 0.999...]
        if (compareMagnitudeNormalized(xs, xscale, ys, yscale) > 0) {// satisfy constraint (b)
            yscale -= 1; // [that is, divisor *= 10]
        }
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        // In order to find out whether the divide generates the exact result,
        // we avoid calling the above divide method. 'quotient' holds the
        // return DMatma object whose scale will be set to 'scl'.
        DMatma quotient;
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            Matma rb = bigMultiplyPowerTen(xs,raise);
            quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);
            int raise = checkScaleNonZero((long) newScale - yscale);
            Matma rb = bigMultiplyPowerTen(ys,raise);
            quotient = divideAndRound(Matma.valueOf(xs), rb, scl, roundingMode,checkScaleNonZero(preferredScale));
        }
        // doRound, here, only affects 1000000000 case.
        return doRound(quotient, mc);
    }

    /**
     * Returns a {@code DMatma} whose value is {@code (xs /
     * ys)}, with rounding according to the context settings.
     */
    private static DMatma divide(Matma xs, int xscale, Matma ys, int yscale, long preferredScale, MathContext mc) {
        // Normalize dividend & divisor so that both fall into [0.1, 0.999...]
        if (compareMagnitudeNormalized(xs, xscale, ys, yscale) > 0) {// satisfy constraint (b)
            yscale -= 1; // [that is, divisor *= 10]
        }
        int mcp = mc.precision;
        int roundingMode = mc.roundingMode.oldMode;

        // In order to find out whether the divide generates the exact result,
        // we avoid calling the above divide method. 'quotient' holds the
        // return DMatma object whose scale will be set to 'scl'.
        DMatma quotient;
        int scl = checkScaleNonZero(preferredScale + yscale - xscale + mcp);
        if (checkScaleNonZero((long) mcp + yscale - xscale) > 0) {
            int raise = checkScaleNonZero((long) mcp + yscale - xscale);
            Matma rb = bigMultiplyPowerTen(xs,raise);
            quotient = divideAndRound(rb, ys, scl, roundingMode, checkScaleNonZero(preferredScale));
        } else {
            int newScale = checkScaleNonZero((long) xscale - mcp);
            int raise = checkScaleNonZero((long) newScale - yscale);
            Matma rb = bigMultiplyPowerTen(ys,raise);
            quotient = divideAndRound(xs, rb, scl, roundingMode,checkScaleNonZero(preferredScale));
        }
        // doRound, here, only affects 1000000000 case.
        return doRound(quotient, mc);
    }

    /*
     * performs divideAndRound for (dividend0*dividend1, divisor)
     * returns null if quotient can't fit into long value;
     */
    private static DMatma multiplyDivideAndRound(long dividend0, long dividend1, long divisor, int scale, int roundingMode,
                                                 int preferredScale) {
        int qsign = Long.signum(dividend0)*Long.signum(dividend1)*Long.signum(divisor);
        dividend0 = Math.abs(dividend0);
        dividend1 = Math.abs(dividend1);
        divisor = Math.abs(divisor);
        // multiply dividend0 * dividend1
        long d0_hi = dividend0 >>> 32;
        long d0_lo = dividend0 & LONG_MASK;
        long d1_hi = dividend1 >>> 32;
        long d1_lo = dividend1 & LONG_MASK;
        long product = d0_lo * d1_lo;
        long d0 = product & LONG_MASK;
        long d1 = product >>> 32;
        product = d0_hi * d1_lo + d1;
        d1 = product & LONG_MASK;
        long d2 = product >>> 32;
        product = d0_lo * d1_hi + d1;
        d1 = product & LONG_MASK;
        d2 += product >>> 32;
        long d3 = d2>>>32;
        d2 &= LONG_MASK;
        product = d0_hi*d1_hi + d2;
        d2 = product & LONG_MASK;
        d3 = ((product>>>32) + d3) & LONG_MASK;
        final long dividendHi = make64(d3,d2);
        final long dividendLo = make64(d1,d0);
        // divide
        return divideAndRound128(dividendHi, dividendLo, divisor, qsign, scale, roundingMode, preferredScale);
    }

    private static final long DIV_NUM_BASE = (1L<<32); // Number base (32 bits).

    /*
     * divideAndRound 128-bit value by long divisor.
     * returns null if quotient can't fit into long value;
     * Specialized version of Knuth's division
     */
    private static DMatma divideAndRound128(final long dividendHi, final long dividendLo, long divisor, int sign,
                                            int scale, int roundingMode, int preferredScale) {
        if (dividendHi >= divisor) {
            return null;
        }

        final int shift = Long.numberOfLeadingZeros(divisor);
        divisor <<= shift;

        final long v1 = divisor >>> 32;
        final long v0 = divisor & LONG_MASK;

        long tmp = dividendLo << shift;
        long u1 = tmp >>> 32;
        long u0 = tmp & LONG_MASK;

        tmp = (dividendHi << shift) | (dividendLo >>> 64 - shift);
        long u2 = tmp & LONG_MASK;
        long q1, r_tmp;
        if (v1 == 1) {
            q1 = tmp;
            r_tmp = 0;
        } else if (tmp >= 0) {
            q1 = tmp / v1;
            r_tmp = tmp - q1 * v1;
        } else {
            long[] rq = divRemNegativeLong(tmp, v1);
            q1 = rq[1];
            r_tmp = rq[0];
        }

        while(q1 >= DIV_NUM_BASE || unsignedLongCompare(q1*v0, make64(r_tmp, u1))) {
            q1--;
            r_tmp += v1;
            if (r_tmp >= DIV_NUM_BASE)
                break;
        }

        tmp = mulsub(u2,u1,v1,v0,q1);
        u1 = tmp & LONG_MASK;
        long q0;
        if (v1 == 1) {
            q0 = tmp;
            r_tmp = 0;
        } else if (tmp >= 0) {
            q0 = tmp / v1;
            r_tmp = tmp - q0 * v1;
        } else {
            long[] rq = divRemNegativeLong(tmp, v1);
            q0 = rq[1];
            r_tmp = rq[0];
        }

        while(q0 >= DIV_NUM_BASE || unsignedLongCompare(q0*v0,make64(r_tmp,u0))) {
            q0--;
            r_tmp += v1;
            if (r_tmp >= DIV_NUM_BASE)
                break;
        }

        if((int)q1 < 0) {
            // result (which is positive and unsigned here)
            // can't fit into long due to sign bit is used for value
            MutMatma mq = new MutMatma(new int[]{(int)q1, (int)q0});
            if (roundingMode == ROUND_DOWN && scale == preferredScale) {
                return mq.toBigDecimal(sign, scale);
            }
            long r = mulsub(u1, u0, v1, v0, q0) >>> shift;
            if (r != 0) {
                if(needIncrement(divisor >>> shift, roundingMode, sign, mq, r)){
                    mq.add(MutMatma.ONE);
                }
                return mq.toBigDecimal(sign, scale);
            } else {
                if (preferredScale != scale) {
                    Matma intVal =  mq.toMatma(sign);
                    return createAndStripZerosToMatchScale(intVal,scale, preferredScale);
                } else {
                    return mq.toBigDecimal(sign, scale);
                }
            }
        }

        long q = make64(q1,q0);
        q*=sign;

        if (roundingMode == ROUND_DOWN && scale == preferredScale)
            return valueOf(q, scale);

        long r = mulsub(u1, u0, v1, v0, q0) >>> shift;
        if (r != 0) {
            boolean increment = needIncrement(divisor >>> shift, roundingMode, sign, q, r);
            return valueOf((increment ? q + sign : q), scale);
        } else {
            if (preferredScale != scale) {
                return createAndStripZerosToMatchScale(q, scale, preferredScale);
            } else {
                return valueOf(q, scale);
            }
        }
    }

    /*
     * calculate divideAndRound for ldividend*10^raise / divisor
     * when abs(dividend)==abs(divisor);
     */
    private static DMatma roundedTenPower(int qsign, int raise, int scale, int preferredScale) {
        if (scale > preferredScale) {
            int diff = scale - preferredScale;
            if(diff < raise) {
                return scaledTenPow(raise - diff, qsign, preferredScale);
            } else {
                return valueOf(qsign,scale-raise);
            }
        } else {
            return scaledTenPow(raise, qsign, scale);
        }
    }

    static DMatma scaledTenPow(int n, int sign, int scale) {
        if (n < LONG_TEN_POWERS_TABLE.length)
            return valueOf(sign*LONG_TEN_POWERS_TABLE[n],scale);
        else {
            Matma unscaledVal = bigTenToThe(n);
            if(sign==-1) {
                unscaledVal = unscaledVal.negate();
            }
            return new DMatma(unscaledVal, INFLATED, scale, n+1);
        }
    }

    /**
     * Calculate the quotient and remainder of dividing a negative long by
     * another long.
     *
     * @param n the numerator; must be negative
     * @param d the denominator; must not be unity
     * @return a two-element {@long} array with the remainder and quotient in
     *         the initial and final elements, respectively
     */
    private static long[] divRemNegativeLong(long n, long d) {
        assert n < 0 : "Non-negative numerator " + n;
        assert d != 1 : "Unity denominator";

        // Approximate the quotient and remainder
        long q = (n >>> 1) / (d >>> 1);
        long r = n - q * d;

        // Correct the approximation
        while (r < 0) {
            r += d;
            q--;
        }
        while (r >= d) {
            r -= d;
            q++;
        }

        // n - q*d == r && 0 <= r < d, hence we're done.
        return new long[] {r, q};
    }

    private static long make64(long hi, long lo) {
        return hi<<32 | lo;
    }

    private static long mulsub(long u1, long u0, final long v1, final long v0, long q0) {
        long tmp = u0 - q0*v0;
        return make64(u1 + (tmp>>>32) - q0*v1,tmp & LONG_MASK);
    }

    private static boolean unsignedLongCompare(long one, long two) {
        return (one+Long.MIN_VALUE) > (two+Long.MIN_VALUE);
    }

    private static boolean unsignedLongCompareEq(long one, long two) {
        return (one+Long.MIN_VALUE) >= (two+Long.MIN_VALUE);
    }


    // Compare Normalize dividend & divisor so that both fall into [0.1, 0.999...]
    private static int compareMagnitudeNormalized(long xs, int xscale, long ys, int yscale) {
        // assert xs!=0 && ys!=0
        int sdiff = xscale - yscale;
        if (sdiff != 0) {
            if (sdiff < 0) {
                xs = longMultiplyPowerTen(xs, -sdiff);
            } else { // sdiff > 0
                ys = longMultiplyPowerTen(ys, sdiff);
            }
        }
        if (xs != INFLATED)
            return (ys != INFLATED) ? longCompareMagnitude(xs, ys) : -1;
        else
            return 1;
    }

    // Compare Normalize dividend & divisor so that both fall into [0.1, 0.999...]
    private static int compareMagnitudeNormalized(long xs, int xscale, Matma ys, int yscale) {
        // assert "ys can't be represented as long"
        if (xs == 0)
            return -1;
        int sdiff = xscale - yscale;
        if (sdiff < 0) {
            if (longMultiplyPowerTen(xs, -sdiff) == INFLATED ) {
                return bigMultiplyPowerTen(xs, -sdiff).compareMagnitude(ys);
            }
        }
        return -1;
    }

    // Compare Normalize dividend & divisor so that both fall into [0.1, 0.999...]
    private static int compareMagnitudeNormalized(Matma xs, int xscale, Matma ys, int yscale) {
        int sdiff = xscale - yscale;
        if (sdiff < 0) {
            return bigMultiplyPowerTen(xs, -sdiff).compareMagnitude(ys);
        } else { // sdiff >= 0
            return xs.compareMagnitude(bigMultiplyPowerTen(ys, sdiff));
        }
    }

    private static long multiply(long x, long y){
        long product = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 == 0) || (y == 0) || (product / y == x)){
            return product;
        }
        return INFLATED;
    }

    private static DMatma multiply(long x, long y, int scale) {
        long product = multiply(x, y);
        if(product!=INFLATED) {
            return valueOf(product,scale);
        }
        return new DMatma(Matma.valueOf(x).multiply(y),INFLATED,scale,0);
    }

    private static DMatma multiply(long x, Matma y, int scale) {
        if(x==0) {
            return zeroValueOf(scale);
        }
        return new DMatma(y.multiply(x),INFLATED,scale,0);
    }

    private static DMatma multiply(Matma x, Matma y, int scale) {
        return new DMatma(x.multiply(y),INFLATED,scale,0);
    }

    /**
     * Multiplies two long values and rounds according {@code MathContext}
     */
    private static DMatma multiplyAndRound(long x, long y, int scale, MathContext mc) {
        long product = multiply(x, y);
        if(product!=INFLATED) {
            return doRound(product, scale, mc);
        }
        // attempt to do it in 128 bits
        int rsign = 1;
        if(x < 0) {
            x = -x;
            rsign = -1;
        }
        if(y < 0) {
            y = -y;
            rsign *= -1;
        }
        // multiply dividend0 * dividend1
        long m0_hi = x >>> 32;
        long m0_lo = x & LONG_MASK;
        long m1_hi = y >>> 32;
        long m1_lo = y & LONG_MASK;
        product = m0_lo * m1_lo;
        long m0 = product & LONG_MASK;
        long m1 = product >>> 32;
        product = m0_hi * m1_lo + m1;
        m1 = product & LONG_MASK;
        long m2 = product >>> 32;
        product = m0_lo * m1_hi + m1;
        m1 = product & LONG_MASK;
        m2 += product >>> 32;
        long m3 = m2>>>32;
        m2 &= LONG_MASK;
        product = m0_hi*m1_hi + m2;
        m2 = product & LONG_MASK;
        m3 = ((product>>>32) + m3) & LONG_MASK;
        final long mHi = make64(m3,m2);
        final long mLo = make64(m1,m0);
        DMatma res = doRound128(mHi, mLo, rsign, scale, mc);
        if(res!=null) {
            return res;
        }
        res = new DMatma(Matma.valueOf(x).multiply(y*rsign), INFLATED, scale, 0);
        return doRound(res,mc);
    }

    private static DMatma multiplyAndRound(long x, Matma y, int scale, MathContext mc) {
        if(x==0) {
            return zeroValueOf(scale);
        }
        return doRound(y.multiply(x), scale, mc);
    }

    private static DMatma multiplyAndRound(Matma x, Matma y, int scale, MathContext mc) {
        return doRound(x.multiply(y), scale, mc);
    }

    /**
     * rounds 128-bit value according {@code MathContext}
     * returns null if result can't be repsented as compact DMatma.
     */
    private static DMatma doRound128(long hi, long lo, int sign, int scale, MathContext mc) {
        int mcp = mc.precision;
        int drop;
        DMatma res = null;
        if(((drop = precision(hi, lo) - mcp) > 0)&&(drop<LONG_TEN_POWERS_TABLE.length)) {
            scale = checkScaleNonZero((long)scale - drop);
            res = divideAndRound128(hi, lo, LONG_TEN_POWERS_TABLE[drop], sign, scale, mc.roundingMode.oldMode, scale);
        }
        if(res!=null) {
            return doRound(res,mc);
        }
        return null;
    }

    private static final long[][] LONGLONG_TEN_POWERS_TABLE = {
            {   0L, 0x8AC7230489E80000L },  //10^19
            {       0x5L, 0x6bc75e2d63100000L },  //10^20
            {       0x36L, 0x35c9adc5dea00000L },  //10^21
            {       0x21eL, 0x19e0c9bab2400000L  },  //10^22
            {       0x152dL, 0x02c7e14af6800000L  },  //10^23
            {       0xd3c2L, 0x1bcecceda1000000L  },  //10^24
            {       0x84595L, 0x161401484a000000L  },  //10^25
            {       0x52b7d2L, 0xdcc80cd2e4000000L  },  //10^26
            {       0x33b2e3cL, 0x9fd0803ce8000000L  },  //10^27
            {       0x204fce5eL, 0x3e25026110000000L  },  //10^28
            {       0x1431e0faeL, 0x6d7217caa0000000L  },  //10^29
            {       0xc9f2c9cd0L, 0x4674edea40000000L  },  //10^30
            {       0x7e37be2022L, 0xc0914b2680000000L  },  //10^31
            {       0x4ee2d6d415bL, 0x85acef8100000000L  },  //10^32
            {       0x314dc6448d93L, 0x38c15b0a00000000L  },  //10^33
            {       0x1ed09bead87c0L, 0x378d8e6400000000L  },  //10^34
            {       0x13426172c74d82L, 0x2b878fe800000000L  },  //10^35
            {       0xc097ce7bc90715L, 0xb34b9f1000000000L  },  //10^36
            {       0x785ee10d5da46d9L, 0x00f436a000000000L  },  //10^37
            {       0x4b3b4ca85a86c47aL, 0x098a224000000000L  },  //10^38
    };

    /*
     * returns precision of 128-bit value
     */
    private static int precision(long hi, long lo){
        if(hi==0) {
            if(lo>=0) {
                return longDigitLength(lo);
            }
            return (unsignedLongCompareEq(lo, LONGLONG_TEN_POWERS_TABLE[0][1])) ? 20 : 19;
            // 0x8AC7230489E80000L  = unsigned 2^19
        }
        int r = ((128 - Long.numberOfLeadingZeros(hi) + 1) * 1233) >>> 12;
        int idx = r-19;
        return (idx >= LONGLONG_TEN_POWERS_TABLE.length || longLongCompareMagnitude(hi, lo,
                LONGLONG_TEN_POWERS_TABLE[idx][0], LONGLONG_TEN_POWERS_TABLE[idx][1])) ? r : r + 1;
    }

    /*
     * returns true if 128 bit number <hi0,lo0> is less then <hi1,lo1>
     * hi0 & hi1 should be non-negative
     */
    private static boolean longLongCompareMagnitude(long hi0, long lo0, long hi1, long lo1) {
        if(hi0!=hi1) {
            return hi0<hi1;
        }
        return (lo0+Long.MIN_VALUE) <(lo1+Long.MIN_VALUE);
    }

    private static DMatma divide(long dividend, int dividendScale, long divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend,(long)scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            if(raise<LONG_TEN_POWERS_TABLE.length) {
                long xs = dividend;
                if ((xs = longMultiplyPowerTen(xs, raise)) != INFLATED) {
                    return divideAndRound(xs, divisor, scale, roundingMode, scale);
                }
                DMatma q = multiplyDivideAndRound(LONG_TEN_POWERS_TABLE[raise], dividend, divisor, scale, roundingMode, scale);
                if(q!=null) {
                    return q;
                }
            }
            Matma scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor,(long)dividendScale - scale);
            int raise = newScale - divisorScale;
            if(raise<LONG_TEN_POWERS_TABLE.length) {
                long ys = divisor;
                if ((ys = longMultiplyPowerTen(ys, raise)) != INFLATED) {
                    return divideAndRound(dividend, ys, scale, roundingMode, scale);
                }
            }
            Matma scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(Matma.valueOf(dividend), scaledDivisor, scale, roundingMode, scale);
        }
    }

    private static DMatma divide(Matma dividend, int dividendScale, long divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend,(long)scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            Matma scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor,(long)dividendScale - scale);
            int raise = newScale - divisorScale;
            if(raise<LONG_TEN_POWERS_TABLE.length) {
                long ys = divisor;
                if ((ys = longMultiplyPowerTen(ys, raise)) != INFLATED) {
                    return divideAndRound(dividend, ys, scale, roundingMode, scale);
                }
            }
            Matma scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(dividend, scaledDivisor, scale, roundingMode, scale);
        }
    }

    private static DMatma divide(long dividend, int dividendScale, Matma divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend,(long)scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            Matma scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor,(long)dividendScale - scale);
            int raise = newScale - divisorScale;
            Matma scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(Matma.valueOf(dividend), scaledDivisor, scale, roundingMode, scale);
        }
    }

    private static DMatma divide(Matma dividend, int dividendScale, Matma divisor, int divisorScale, int scale, int roundingMode) {
        if (checkScale(dividend,(long)scale + divisorScale) > dividendScale) {
            int newScale = scale + divisorScale;
            int raise = newScale - dividendScale;
            Matma scaledDividend = bigMultiplyPowerTen(dividend, raise);
            return divideAndRound(scaledDividend, divisor, scale, roundingMode, scale);
        } else {
            int newScale = checkScale(divisor,(long)dividendScale - scale);
            int raise = newScale - divisorScale;
            Matma scaledDivisor = bigMultiplyPowerTen(divisor, raise);
            return divideAndRound(dividend, scaledDivisor, scale, roundingMode, scale);
        }
    }

}
