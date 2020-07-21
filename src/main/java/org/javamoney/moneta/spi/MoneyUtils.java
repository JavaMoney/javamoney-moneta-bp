/*
  Copyright (c) 2012, 2020, Werner Keil, Otavio Santana and others by the @author tag.

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy of
  the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.
 */
package org.javamoney.moneta.spi;

import org.javamoney.moneta.internal.JDKObjects;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.MonetaryContext;
import javax.money.MonetaryException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.logging.Logger;

import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.FINEST;

/**
 * Platform RI: This utility class simplifies implementing {@link MonetaryAmount},
 * by providing the common functionality. The different explicitly typed methods
 * are all reduced to methods using {@link BigDecimal} as input, hereby
 * performing any conversion to {@link BigDecimal} as needed. Obviously this
 * takes some time, so implementors that want to avoid this overhead should
 * implement {@link MonetaryAmount} directly.
 *
 * @author Anatole Tresch
 * @author Werner Keil
 */
public final class MoneyUtils {

    private static final Logger LOG = Logger.getLogger(MoneyUtils.class.getName());

    public static final String NBSP_STRING = "\u00A0";
    public static final String NNBSP_STRING = "\u202F";
    public static final char NBSP = NBSP_STRING.charAt(0);
    public static final char NNBSP = NNBSP_STRING.charAt(0);

    private MoneyUtils() {
    }


    // Supporting methods

    /**
     * Creates a {@link BigDecimal} from the given {@link Number} doing the
     * valid conversion depending the type given.
     *
     * @param num the number type
     * @return the corresponding {@link BigDecimal}
     */
    public static BigDecimal getBigDecimal(long num) {
        return BigDecimal.valueOf(num);
    }

    /**
     * Creates a {@link BigDecimal} from the given {@link Number} doing the
     * valid conversion depending the type given.
     *
     * @param num the number type
     * @return the corresponding {@link BigDecimal}
     */
    public static BigDecimal getBigDecimal(double num) {
        if (Double.isNaN(num)) {
            throw new ArithmeticException("Invalid input Double.NaN.");
        } else if(Double.isInfinite(num)) {
            throw new ArithmeticException("Invalid input Double.xxx_INFINITY.");
        }
        return new BigDecimal(String.valueOf(num));
    }

    /**
     * Creates a {@link BigDecimal} from the given {@link Number} doing the
     * valid conversion depending the type given.
     *
     * @param num the number type
     * @return the corresponding {@link BigDecimal}
     */
    public static BigDecimal getBigDecimal(Number num) {
        return ConvertBigDecimal.of(num);
    }

    /**
     * Creates a {@link BigDecimal} from the given {@link Number} doing the
     * valid conversion depending the type given, if a {@link MonetaryContext}
     * is given, it is applied to the number returned.
     *
     * @param num the number type
     * @return the corresponding {@link BigDecimal}
     */
    public static BigDecimal getBigDecimal(Number num, MonetaryContext moneyContext) {
        BigDecimal bd = getBigDecimal(num);
        if (JDKObjects.nonNull(moneyContext)) {
            MathContext mc = getMathContext(moneyContext, HALF_EVEN);
            bd = new BigDecimal(bd.toString(), mc);
            int maxScale = moneyContext.getMaxScale();
            if (maxScale > 0) {
                if (bd.scale() > maxScale) {
                    if (LOG.isLoggable(FINEST)) {
                        LOG.log(FINEST, "The number scale is " + bd.scale() + " but Max Scale is " + maxScale);
                    }
                    bd = bd.setScale(maxScale, mc.getRoundingMode());
                }
            }
        }
        return bd;
    }

    /**
     * Evaluates the {@link MathContext} from the given {@link MonetaryContext}.
     *
     * @param monetaryContext the {@link MonetaryContext}
     * @param defaultMode     the default {@link RoundingMode}, to be used if no one is set
     *                        in {@link MonetaryContext}.
     * @return the corresponding {@link MathContext}
     */
    public static MathContext getMathContext(MonetaryContext monetaryContext, RoundingMode defaultMode) {
        MathContext ctx = monetaryContext.get(MathContext.class);
        if (JDKObjects.nonNull(ctx)) {
            return ctx;
        }
        RoundingMode roundingMode = monetaryContext.get(RoundingMode.class);
        if (roundingMode == null) {
            roundingMode = HALF_EVEN;
        }
        return new MathContext(monetaryContext.getPrecision(), roundingMode);
    }

    /**
     * Method to check if a currency is compatible with this amount instance.
     *
     * @param amount       The monetary amount to be compared to, never null.
     * @param currencyUnit the currency unit to compare, never null.
     * @throws MonetaryException If the amount is null, or the amount's {@link CurrencyUnit} is not
     *                           compatible, meaning has a different value of
     *                           {@link CurrencyUnit#getCurrencyCode()}).
     */
    public static void checkAmountParameter(MonetaryAmount amount, CurrencyUnit currencyUnit) {
        requireNonNull(amount, "Amount must not be null.");
        final CurrencyUnit amountCurrency = amount.getCurrency();
        if (!currencyUnit.getCurrencyCode().equals(amountCurrency.getCurrencyCode())) {
            throw new MonetaryException("Currency mismatch: " + currencyUnit + '/' + amountCurrency);
        }
    }

    /**
     * Internal method to check for correct number parameter.
     *
     * @param number the number to be checked.
     * @throws IllegalArgumentException If the number is null
     */
    public static void checkNumberParameter(Number number) {
        requireNonNull(number, "Number is required.");
    }

    /**
     * Replaces the non-breaking space character U+00A0 and Narrow non-breaking space U+202F from the string with usual space.
     * https://en.wikipedia.org/wiki/Non-breaking_space}
     */
    public static String replaceNbspWithSpace(String s) {
        return s.replace(NBSP, ' ').replace(NNBSP, ' ');
    }
}
