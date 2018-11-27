
package megaRollo;

;

public enum RoundingMode {

        /**
         * Rounding mode to round away from zero.  Always increments the
         * digit prior to a non-zero discarded fraction.  Note that this
         * rounding mode never decreases the magnitude of the calculated
         * value.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode UP Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code UP} rounding
         *<tr align=right><td>5.5</td>  <td>6</td>
         *<tr align=right><td>2.5</td>  <td>3</td>
         *<tr align=right><td>1.6</td>  <td>2</td>
         *<tr align=right><td>1.1</td>  <td>2</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-2</td>
         *<tr align=right><td>-1.6</td> <td>-2</td>
         *<tr align=right><td>-2.5</td> <td>-3</td>
         *<tr align=right><td>-5.5</td> <td>-6</td>
         *</table>
         */
    UP(DMatma.ROUND_UP),

        /**
         * Rounding mode to round towards zero.  Never increments the digit
         * prior to a discarded fraction (i.e., truncates).  Note that this
         * rounding mode never increases the magnitude of the calculated value.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode DOWN Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code DOWN} rounding
         *<tr align=right><td>5.5</td>  <td>5</td>
         *<tr align=right><td>2.5</td>  <td>2</td>
         *<tr align=right><td>1.6</td>  <td>1</td>
         *<tr align=right><td>1.1</td>  <td>1</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-1</td>
         *<tr align=right><td>-1.6</td> <td>-1</td>
         *<tr align=right><td>-2.5</td> <td>-2</td>
         *<tr align=right><td>-5.5</td> <td>-5</td>
         *</table>
         */
    DOWN(DMatma.ROUND_DOWN),

        /**
         * Rounding mode to round towards positive infinity.  If the
         * result is positive, behaves as for {@code RoundingMode.UP};
         * if negative, behaves as for {@code RoundingMode.DOWN}.  Note
         * that this rounding mode never decreases the calculated value.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode CEILING Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code CEILING} rounding
         *<tr align=right><td>5.5</td>  <td>6</td>
         *<tr align=right><td>2.5</td>  <td>3</td>
         *<tr align=right><td>1.6</td>  <td>2</td>
         *<tr align=right><td>1.1</td>  <td>2</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-1</td>
         *<tr align=right><td>-1.6</td> <td>-1</td>
         *<tr align=right><td>-2.5</td> <td>-2</td>
         *<tr align=right><td>-5.5</td> <td>-5</td>
         *</table>
         */
    CEILING(DMatma.ROUND_CEILING),

        /**
         * Rounding mode to round towards negative infinity.  If the
         * result is positive, behave as for {@code RoundingMode.DOWN};
         * if negative, behave as for {@code RoundingMode.UP}.  Note that
         * this rounding mode never increases the calculated value.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode FLOOR Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code FLOOR} rounding
         *<tr align=right><td>5.5</td>  <td>5</td>
         *<tr align=right><td>2.5</td>  <td>2</td>
         *<tr align=right><td>1.6</td>  <td>1</td>
         *<tr align=right><td>1.1</td>  <td>1</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-2</td>
         *<tr align=right><td>-1.6</td> <td>-2</td>
         *<tr align=right><td>-2.5</td> <td>-3</td>
         *<tr align=right><td>-5.5</td> <td>-6</td>
         *</table>
         */
    FLOOR(DMatma.ROUND_FLOOR),

        /**
         * Rounding mode to round towards {@literal "nearest neighbor"}
         * unless both neighbors are equidistant, in which case round up.
         * Behaves as for {@code RoundingMode.UP} if the discarded
         * fraction is &ge; 0.5; otherwise, behaves as for
         * {@code RoundingMode.DOWN}.  Note that this is the rounding
         * mode commonly taught at school.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode HALF_UP Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code HALF_UP} rounding
         *<tr align=right><td>5.5</td>  <td>6</td>
         *<tr align=right><td>2.5</td>  <td>3</td>
         *<tr align=right><td>1.6</td>  <td>2</td>
         *<tr align=right><td>1.1</td>  <td>1</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-1</td>
         *<tr align=right><td>-1.6</td> <td>-2</td>
         *<tr align=right><td>-2.5</td> <td>-3</td>
         *<tr align=right><td>-5.5</td> <td>-6</td>
         *</table>
         */
    HALF_UP(DMatma.ROUND_HALF_UP),

        /**
         * Rounding mode to round towards {@literal "nearest neighbor"}
         * unless both neighbors are equidistant, in which case round
         * down.  Behaves as for {@code RoundingMode.UP} if the discarded
         * fraction is &gt; 0.5; otherwise, behaves as for
         * {@code RoundingMode.DOWN}.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode HALF_DOWN Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code HALF_DOWN} rounding
         *<tr align=right><td>5.5</td>  <td>5</td>
         *<tr align=right><td>2.5</td>  <td>2</td>
         *<tr align=right><td>1.6</td>  <td>2</td>
         *<tr align=right><td>1.1</td>  <td>1</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-1</td>
         *<tr align=right><td>-1.6</td> <td>-2</td>
         *<tr align=right><td>-2.5</td> <td>-2</td>
         *<tr align=right><td>-5.5</td> <td>-5</td>
         *</table>
         */
    HALF_DOWN(DMatma.ROUND_HALF_DOWN),

        /**
         * Rounding mode to round towards the {@literal "nearest neighbor"}
         * unless both neighbors are equidistant, in which case, round
         * towards the even neighbor.  Behaves as for
         * {@code RoundingMode.HALF_UP} if the digit to the left of the
         * discarded fraction is odd; behaves as for
         * {@code RoundingMode.HALF_DOWN} if it's even.  Note that this
         * is the rounding mode that statistically minimizes cumulative
         * error when applied repeatedly over a sequence of calculations.
         * It is sometimes known as {@literal "Banker's rounding,"} and is
         * chiefly used in the USA.  This rounding mode is analogous to
         * the rounding policy used for {@code float} and {@code double}
         * arithmetic in Java.
         *
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode HALF_EVEN Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code HALF_EVEN} rounding
         *<tr align=right><td>5.5</td>  <td>6</td>
         *<tr align=right><td>2.5</td>  <td>2</td>
         *<tr align=right><td>1.6</td>  <td>2</td>
         *<tr align=right><td>1.1</td>  <td>1</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>-1</td>
         *<tr align=right><td>-1.6</td> <td>-2</td>
         *<tr align=right><td>-2.5</td> <td>-2</td>
         *<tr align=right><td>-5.5</td> <td>-6</td>
         *</table>
         */
    HALF_EVEN(DMatma.ROUND_HALF_EVEN),

        /**
         * Rounding mode to assert that the requested operation has an exact
         * result, hence no rounding is necessary.  If this rounding mode is
         * specified on an operation that yields an inexact result, an
         * {@code ArithmeticException} is thrown.
         *<p>Example:
         *<table border>
         * <caption><b>Rounding mode UNNECESSARY Examples</b></caption>
         *<tr valign=top><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code UNNECESSARY} rounding
         *<tr align=right><td>5.5</td>  <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>2.5</td>  <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>1.6</td>  <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>1.1</td>  <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>1.0</td>  <td>1</td>
         *<tr align=right><td>-1.0</td> <td>-1</td>
         *<tr align=right><td>-1.1</td> <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>-1.6</td> <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>-2.5</td> <td>throw {@code ArithmeticException}</td>
         *<tr align=right><td>-5.5</td> <td>throw {@code ArithmeticException}</td>
         *</table>
         */
    UNNECESSARY(DMatma.ROUND_UNNECESSARY);

    // Corresponding DMatma rounding constant
    final int oldMode;

    /**
     * Constructor
     *
     * @param oldMode The {@code DMatma} constant corresponding to
     *        this mode
     */
    private RoundingMode(int oldMode) {
        this.oldMode = oldMode;
    }

    /**
     * Returns the {@code RoundingMode} object corresponding to a
     * legacy integer rounding mode constant in {@link DMatma}.
     *
     * @param  rm legacy integer rounding mode to convert
     * @return {@code RoundingMode} corresponding to the given integer.
     * @throws IllegalArgumentException integer is out of range
     */
    public static RoundingMode valueOf(int rm) {
        switch(rm) {

        case DMatma.ROUND_UP:
            return UP;

        case DMatma.ROUND_DOWN:
            return DOWN;

        case DMatma.ROUND_CEILING:
            return CEILING;

        case DMatma.ROUND_FLOOR:
            return FLOOR;

        case DMatma.ROUND_HALF_UP:
            return HALF_UP;

        case DMatma.ROUND_HALF_DOWN:
            return HALF_DOWN;

        case DMatma.ROUND_HALF_EVEN:
            return HALF_EVEN;

        case DMatma.ROUND_UNNECESSARY:
            return UNNECESSARY;

        default:
            throw new IllegalArgumentException("argument out of range");
        }
    }
}
