package com.del.pst.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!@#$%&*()_+-=[]|,./?><";
    private boolean useLower;
    private boolean useUpper;
    private boolean useDigits;
    private boolean usePunctuation;
    private int length;

    public PasswordGenerator() {
    }

    public boolean isUseLower() {
        return useLower;
    }

    public void setUseLower(boolean useLower) {
        this.useLower = useLower;
    }

    public boolean isUseUpper() {
        return useUpper;
    }

    public void setUseUpper(boolean useUpper) {
        this.useUpper = useUpper;
    }

    public boolean isUseDigits() {
        return useDigits;
    }

    public void setUseDigits(boolean useDigits) {
        this.useDigits = useDigits;
    }

    public boolean isUsePunctuation() {
        return usePunctuation;
    }

    public void setUsePunctuation(boolean usePunctuation) {
        this.usePunctuation = usePunctuation;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * This method will generate a password depending the use* properties you
     * define. It will use the categories with a probability. It is not sure
     * that all of the defined categories will be used.
     *
     * @return a password that uses the categories you define when constructing
     * the object with a probability.
     */
    public String generate() {
        // Argument Validation.
        if (length <= 0) {
            return "";
        }

        // Variables.
        StringBuilder password = new StringBuilder(length);
        Random random = new Random(System.nanoTime());

        // Collect the categories to use.
        List<String> charCategories = new ArrayList<>(4);
        if (useLower) {
            charCategories.add(LOWER);
        }
        if (useUpper) {
            charCategories.add(UPPER);
        }
        if (useDigits) {
            charCategories.add(DIGITS);
        }
        if (usePunctuation) {
            charCategories.add(PUNCTUATION);
        }

        // Build the password.
        for (int i = 0; i < length; i++) {
            String charCategory = charCategories.get(random.nextInt(charCategories.size()));
            int position = random.nextInt(charCategory.length());
            password.append(charCategory.charAt(position));
        }
        return new String(password);
    }
}
