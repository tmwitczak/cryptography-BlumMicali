package megaRollo;


class SMutMatma extends MutMatma {

   /**
     * The sign of this MutMatma.
     */
    int sign = 1;

    // Constructors

    /**
     * The default constructor. An empty MutMatma is created with
     * a one word capacity.
     */
    SMutMatma() {
        super();
    }

    /**
     * Construct a new MutMatma with a magnitude specified by
     * the int val.
     */
    SMutMatma(int val) {
        super(val);
    }

    /**
     * Construct a new MutMatma with a magnitude equal to the
     * specified MutMatma.
     */
    SMutMatma(MutMatma val) {
        super(val);
    }

   // Arithmetic Operations

   /**
     * Signed addition built upon unsigned add and subtract.
     */
    void signedAdd(SMutMatma addend) {
        if (sign == addend.sign)
            add(addend);
        else
            sign = sign * subtract(addend);

    }

   /**
     * Signed addition built upon unsigned add and subtract.
     */
    void signedAdd(MutMatma addend) {
        if (sign == 1)
            add(addend);
        else
            sign = sign * subtract(addend);

    }

   /**
     * Signed subtraction built upon unsigned add and subtract.
     */
    void signedSubtract(SMutMatma addend) {
        if (sign == addend.sign)
            sign = sign * subtract(addend);
        else
            add(addend);

    }

   /**
     * Signed subtraction built upon unsigned add and subtract.
     */
    void signedSubtract(MutMatma addend) {
        if (sign == 1)
            sign = sign * subtract(addend);
        else
            add(addend);
        if (intLen == 0)
             sign = 1;
    }

    /**
     * Print out the first intLen ints of this MutMatma's value
     * array starting at offset.
     */
    public String toString() {
        return this.toMatma(sign).toString();
    }

}
