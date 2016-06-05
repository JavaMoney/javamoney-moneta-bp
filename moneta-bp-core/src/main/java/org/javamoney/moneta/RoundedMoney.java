/**
 * Copyright (c) 2012, 2014, Credit Suisse (Anatole Tresch), Werner Keil and others by the @author tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.javamoney.moneta;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryAmountFactory;
import javax.money.MonetaryContext;
import javax.money.MonetaryContextBuilder;
import javax.money.MonetaryException;
import javax.money.MonetaryOperator;
import javax.money.MonetaryQuery;
import javax.money.NumberValue;
import javax.money.RoundingQueryBuilder;

import org.javamoney.moneta.internal.RoundedMoneyAmountBuilder;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.javamoney.moneta.spi.MoneyUtils;

import javax.money.format.MonetaryAmountFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Platform RI: Default immutable implementation of {@link MonetaryAmount} based on
 * {@link BigDecimal} for the numeric representation.
 * <p>
 * As required by {@link MonetaryAmount} this class is final, thread-safe, immutable and
 * serializable.
 *
 * @author Anatole Tresch
 * @author Werner Keil
 * @version 0.6.1
 */
public final class RoundedMoney implements MonetaryAmount, Comparable<MonetaryAmount>, Serializable {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = -6716367273185192901L;
    /**
     * The default {@link MonetaryContext} applied.
     */
    public static final MonetaryContext DEFAULT_MONETARY_CONTEXT = MonetaryContextBuilder.of(RoundedMoney.class)
            .set("MonetaryRounding", Monetary.getDefaultRounding()).
                    build();

    /**
     * The currency of this amount.
     */
    private final CurrencyUnit currency;

    /**
     * the {@link MonetaryContext} used by this instance, e.g. on division.
     */
    private final MonetaryContext monetaryContext;

    /**
     * The numeric part of this amount.
     */
    private final BigDecimal number;

    /**
     * The rounding to be done.
     */
    private MonetaryOperator rounding;


    /**
     * Creates a new instance os {@link RoundedMoney}.
     *
     * @param currency the currency, not null.
     * @param number   the amount, not null.
     */
    public RoundedMoney(Number number, CurrencyUnit currency, MonetaryOperator rounding) {
        this(number, currency, null, rounding);
    }

    @Deprecated
    public RoundedMoney(Number number, CurrencyUnit currency, MathContext mathContext) {
        Objects.requireNonNull(currency, "Currency is required.");
        this.currency = currency;
        this.rounding = Monetary.getRounding(RoundingQueryBuilder.of().set(mathContext).build());
        this.monetaryContext =
                DEFAULT_MONETARY_CONTEXT.toBuilder().set("MonetaryRounding", rounding).set(mathContext)
                        .build();
        Objects.requireNonNull(number, "Number is required.");
        checkNumber(number);
        this.number = MoneyUtils.getBigDecimal(number, monetaryContext);
    }

    @Deprecated
    public RoundedMoney(Number number, CurrencyUnit currency, MonetaryContext context, MonetaryOperator rounding) {
        Objects.requireNonNull(currency, "Currency is required.");
        this.currency = currency;
        Objects.requireNonNull(number, "Number is required.");
        checkNumber(number);

        MonetaryContextBuilder b = DEFAULT_MONETARY_CONTEXT.toBuilder();
        if (rounding!=null) {
            this.rounding = rounding;
        } else {
            if (context != null) {
                MathContext mc = context.get(MathContext.class);
                if (mc == null) {
                    RoundingMode rm = context.get(RoundingMode.class);
                    if (rm != null) {
                        Integer scale = context.getInt("scale");
                        if(scale==null){
                            scale = 2;
                        }
                        b.set(rm);
                        b.set("scale", scale);
                        this.rounding = Monetary
                                .getRounding(RoundingQueryBuilder.of().setScale(scale).set(rm).build());
                    }
                } else {
                    b.set(mc.getRoundingMode());
                    b.set("scale", 2);
                    this.rounding =
                            Monetary.getRounding(RoundingQueryBuilder.of().set(mc).setScale(2).build());
                }
                if (this.rounding == null) {
                    this.rounding = Monetary.getDefaultRounding();
                }
            }
        }
        b.set("MonetaryRounding", this.rounding);
        if (context != null) {
            b.importContext(context);
        }
        this.monetaryContext = b.build();
        this.number = MoneyUtils.getBigDecimal(number, monetaryContext);
    }

    // Static Factory Methods

    /**
     * Translates a {@code BigDecimal} value and a {@code CurrencyUnit} currency into a
     * {@code Money}.
     *
     * @param number   numeric value of the {@code Money}.
     * @param currency currency unit of the {@code Money}.
     * @return a {@code Money} combining the numeric value and currency unit.
     */
    public static RoundedMoney of(BigDecimal number, CurrencyUnit currency) {
        return new RoundedMoney(number, currency, Monetary.getDefaultRounding());
    }

    /**
     * Translates a {@code BigDecimal} value and a {@code CurrencyUnit} currency into a
     * {@code Money}.
     *
     * @param number   numeric value of the {@code Money}.
     * @param currency currency unit of the {@code Money}.
     * @param rounding The rounding to be applied.
     * @return a {@code Money} combining the numeric value and currency unit.
     */
    public static RoundedMoney of(BigDecimal number, CurrencyUnit currency, MonetaryOperator rounding) {
        return new RoundedMoney(number, currency, rounding);
    }

    /**
     * Translates a {@code BigDecimal} value and a {@code CurrencyUnit} currency into a
     * {@code Money}.
     *
     * @param number      numeric value of the {@code Money}.
     * @param currency    currency unit of the {@code Money}.
     * @param mathContext the {@link MathContext} to be used.
     * @return a {@code Money} combining the numeric value and currency unit.
     */
    public static RoundedMoney of(BigDecimal number, CurrencyUnit currency, MathContext mathContext) {
        return new RoundedMoney(number, currency, mathContext);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currency The target currency, not null.
     * @param number   The numeric part, not null.
     * @return A new instance of {@link RoundedMoney}.
     */
    @Deprecated
    public static RoundedMoney of(Number number, CurrencyUnit currency) {
        return new RoundedMoney(number, currency, (MonetaryOperator) null);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currency The target currency, not null.
     * @param number   The numeric part, not null.
     * @param rounding The rounding to be applied.
     * @return A new instance of {@link RoundedMoney}.
     */
    public static RoundedMoney of(Number number, CurrencyUnit currency, MonetaryOperator rounding) {
        return new RoundedMoney(number, currency, rounding);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currency The target currency, not null.
     * @param number   The numeric part, not null.
     * @return A new instance of {@link RoundedMoney}.
     */
    public static RoundedMoney of(Number number, CurrencyUnit currency, MonetaryContext monetaryContext) {
        return new RoundedMoney(number, currency,
                DEFAULT_MONETARY_CONTEXT.toBuilder().importContext(monetaryContext).build(), null);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currency        The target currency, not null.
     * @param number          The numeric part, not null.
     * @param monetaryContext the {@link MonetaryContext} to be used.
     * @param rounding        The rounding to be applied.
     * @return A new instance of {@link RoundedMoney}.
     */
    @Deprecated
    public static RoundedMoney of(CurrencyUnit currency, Number number, MonetaryContext monetaryContext,
                                  MonetaryOperator rounding) {
        return new RoundedMoney(number, currency,
                DEFAULT_MONETARY_CONTEXT.toBuilder().importContext(monetaryContext).build(), rounding);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currencyCode The target currency as ISO currency code.
     * @param number       The numeric part, not null.
     * @return A new instance of {@link RoundedMoney}.
     */
    @Deprecated
    public static RoundedMoney of(Number number, String currencyCode) {
        return new RoundedMoney(number, Monetary.getCurrency(currencyCode),
                Monetary.getDefaultRounding());
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currencyCode The target currency as ISO currency code.
     * @param number       The numeric part, not null.
     * @param rounding     The rounding to be applied.
     * @return A new instance of {@link RoundedMoney}.
     */
    public static RoundedMoney of(Number number, String currencyCode, MonetaryOperator rounding) {
        return new RoundedMoney(number, Monetary.getCurrency(currencyCode), rounding);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currencyCode The target currency as ISO currency code.
     * @param number       The numeric part, not null.
     * @return A new instance of {@link RoundedMoney}.
     */
    @Deprecated
    public static RoundedMoney of(Number number, String currencyCode, MonetaryContext monetaryContext) {
        return new RoundedMoney(number, Monetary.getCurrency(currencyCode),
                DEFAULT_MONETARY_CONTEXT.toBuilder().importContext(monetaryContext).build(), null);
    }

    /**
     * Static factory method for creating a new instance of {@link RoundedMoney} .
     *
     * @param currencyCode The target currency as ISO currency code.
     * @param number       The numeric part, not null.
     * @param rounding     The rounding to be applied.
     * @return A new instance of {@link RoundedMoney}.
     */
    public static RoundedMoney of(String currencyCode, Number number, MonetaryContext monetaryContext,
                                  MonetaryOperator rounding) {
        return new RoundedMoney(number, Monetary.getCurrency(currencyCode),
                DEFAULT_MONETARY_CONTEXT.toBuilder().importContext(monetaryContext).build(), rounding);
    }

    /**
     * Obtains an instance of {@link RoundedMoney} representing zero.
     * @param currency
     * @return
     * @since 1.0.1
     */
    public static RoundedMoney zero(CurrencyUnit currency) {
        return of(BigDecimal.ZERO, currency);
    }


   /**
    * Obtains an instance of {@code FastMoney} from an amount in minor units.
    * For example, {@code ofMinor(USD, 1234)} creates the instance {@code USD 12.34}.
    * @param currency  the currency, not null
    * @param amountMinor  the amount of money in the minor division of the currency
    * @return the Money from minor units
    * @see {@link CurrencyUnit#getDefaultFractionDigits()}
    * @throws NullPointerException when the currency is null
    * @throws IllegalArgumentException when {@link CurrencyUnit#getDefaultFractionDigits()} is lesser than zero.
    * @since 1.0.1
    */
   public static RoundedMoney ofMinor(CurrencyUnit currency, long amountMinor) {
	   return ofMinor(currency, amountMinor, currency.getDefaultFractionDigits());
   }

   /**
    * Obtains an instance of {@code Money} from an amount in minor units.
    * For example, {@code ofMinor(USD, 1234, 2)} creates the instance {@code USD 12.34}.
    * @param currency  the currency, not null
    * @param amountMinor  the amount of money in the minor division of the currency
    * @param factionDigits number of digits
    * @return the monetary amount from minor units
    * @see {@link CurrencyUnit#getDefaultFractionDigits()}
    * @see {@link Money#ofMinor(CurrencyUnit, long, int)}
    * @throws NullPointerException when the currency is null
    * @throws IllegalArgumentException when the factionDigits is negative
    * @since 1.0.1
    */
   public static RoundedMoney ofMinor(CurrencyUnit currency, long amountMinor, int factionDigits) {
   	if(factionDigits < 0) {
   		throw new IllegalArgumentException("The factionDigits cannot be negative");
   	}
   	return of(BigDecimal.valueOf(amountMinor, factionDigits), currency);
   }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#getCurrency()
     */
    @Override
    public CurrencyUnit getCurrency() {
        return currency;
    }

    /**
     * Access the {@link MathContext} used by this instance.
     *
     * @return the {@link MathContext} used, never null.
     */
    @Override
    public MonetaryContext getContext() {
        return monetaryContext;
    }

    @Override
    public RoundedMoney abs() {
        if (isPositiveOrZero()) {
            return this;
        }
        return negate();
    }

    // Arithmetic Operations

    @Override
    public RoundedMoney add(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        if (amount.isZero()) {
            return this;
        }
        return new RoundedMoney(number.add(amount.getNumber().numberValue(BigDecimal.class)), currency,
                rounding).with(rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#divide(MonetaryAmount)
     */
    @Override
    public RoundedMoney divide(Number divisor) {
        BigDecimal bd = MoneyUtils.getBigDecimal(divisor);
        if (isOne(bd)) {
            return this;
        }
        RoundingMode rm = monetaryContext.get(RoundingMode.class);
        if(rm==null){
            rm = RoundingMode.HALF_EVEN;
        }
        BigDecimal dec = number.divide(bd, rm);
        return new RoundedMoney(dec, currency, rounding).with(rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#divideAndRemainder(MonetaryAmount)
     */
    @Override
    public RoundedMoney[] divideAndRemainder(Number divisor) {
        BigDecimal bd = MoneyUtils.getBigDecimal(divisor);
        if (isOne(bd)) {
            return new RoundedMoney[]{this, new RoundedMoney(0L, getCurrency(), rounding)};
        }
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        BigDecimal[] dec = number.divideAndRemainder(MoneyUtils.getBigDecimal(divisor), mc);
        return new RoundedMoney[]{new RoundedMoney(dec[0], currency, rounding),
                new RoundedMoney(dec[1], currency, rounding).with(rounding)};
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#divideToIntegralValue(Number) )D
     */
    @Override
    public RoundedMoney divideToIntegralValue(Number divisor) {
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        BigDecimal dec = number.divideToIntegralValue(MoneyUtils.getBigDecimal(divisor), mc);
        return new RoundedMoney(dec, currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#multiply(Number)
     */
    @Override
    public RoundedMoney multiply(Number multiplicand) {
        BigDecimal bd = MoneyUtils.getBigDecimal(multiplicand);
        if (isOne(bd)) {
            return this;
        }
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        BigDecimal dec = number.multiply(bd, mc);
        return new RoundedMoney(dec, currency, rounding).with(rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#negate()
     */
    @Override
    public RoundedMoney negate() {
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        return new RoundedMoney(number.negate(mc),
                currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#plus()
     */
    @Override
    public RoundedMoney plus() {
        return this;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#subtract(MonetaryAmount)
     */
    @Override
    public RoundedMoney subtract(MonetaryAmount subtrahend) {
        MoneyUtils.checkAmountParameter(subtrahend, currency);
        if (subtrahend.isZero()) {
            return this;
        }
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        return new RoundedMoney(number.subtract(subtrahend.getNumber().numberValue(BigDecimal.class), mc),
                currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#pow(int)
     */
    public RoundedMoney pow(int n) {
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        return new RoundedMoney(number.pow(n, mc),
                currency, rounding).with(rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#ulp()
     */
    public RoundedMoney ulp() {
        return new RoundedMoney(number.ulp(), currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#remainder(Number)
     */
    @Override
    public RoundedMoney remainder(Number divisor) {
        MathContext mc = monetaryContext.get(MathContext.class);
        if(mc==null){
            mc = MathContext.DECIMAL64;
        }
        return new RoundedMoney(number.remainder(MoneyUtils.getBigDecimal(divisor), mc),
                currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#scaleByPowerOfTen(int)
     */
    @Override
    public RoundedMoney scaleByPowerOfTen(int n) {
        return new RoundedMoney(number.scaleByPowerOfTen(n), currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isZero()
     */
    @Override
    public boolean isZero() {
        return number.signum() == 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isPositive()
     */
    @Override
    public boolean isPositive() {
        return signum() == 1;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isPositiveOrZero()
     */
    @Override
    public boolean isPositiveOrZero() {
        return signum() >= 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isNegative()
     */
    @Override
    public boolean isNegative() {
        return signum() == -1;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isNegativeOrZero()
     */
    @Override
    public boolean isNegativeOrZero() {
        return signum() <= 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#with(java.lang.Number)
     */
    public RoundedMoney with(Number amount) {
        checkNumber(amount);
        return new RoundedMoney(MoneyUtils.getBigDecimal(amount), currency, rounding);
    }

    /**
     * Creates a new Money instance, by just replacing the {@link CurrencyUnit}.
     *
     * @param currency the currency unit to be replaced, not {@code null}
     * @return the new amount with the same numeric value and {@link MathContext}, but the new
     * {@link CurrencyUnit}.
     */
    public RoundedMoney with(CurrencyUnit currency) {
        Objects.requireNonNull(currency, "currency required");
        return new RoundedMoney(asType(BigDecimal.class), currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#with(CurrencyUnit, java.lang.Number)
     */
    public RoundedMoney with(CurrencyUnit currency, Number amount) {
        checkNumber(amount);
        return new RoundedMoney(MoneyUtils.getBigDecimal(amount), currency, rounding);
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#getScale()
     */
    public int getScale() {
        return number.scale();
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#getPrecision()
     */
    public int getPrecision() {
        return number.precision();
    }

	/*
     * (non-Javadoc)
	 * @see MonetaryAmount#signum()
	 */

    @Override
    public int signum() {
        return number.signum();
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#lessThan(MonetaryAmount)
     */
    @Override
    public boolean isLessThan(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        return number.stripTrailingZeros()
                .compareTo(amount.getNumber().numberValue(BigDecimal.class).stripTrailingZeros()) < 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#lessThanOrEqualTo(MonetaryAmount)
     */
    @Override
    public boolean isLessThanOrEqualTo(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        return number.stripTrailingZeros()
                .compareTo(amount.getNumber().numberValue(BigDecimal.class).stripTrailingZeros()) <= 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#greaterThan(MonetaryAmount)
     */
    @Override
    public boolean isGreaterThan(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        return number.stripTrailingZeros()
                .compareTo(amount.getNumber().numberValue(BigDecimal.class).stripTrailingZeros()) > 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#greaterThanOrEqualTo(MonetaryAmount ) #see
     */
    @Override
    public boolean isGreaterThanOrEqualTo(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        return number.stripTrailingZeros()
                .compareTo(amount.getNumber().numberValue(BigDecimal.class).stripTrailingZeros()) >= 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isEqualTo(MonetaryAmount)
     */
    @Override
    public boolean isEqualTo(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        return number.stripTrailingZeros()
                .compareTo(amount.getNumber().numberValue(BigDecimal.class).stripTrailingZeros()) == 0;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#isNotEqualTo(MonetaryAmount)
     */
    public boolean isNotEqualTo(MonetaryAmount amount) {
        MoneyUtils.checkAmountParameter(amount, currency);
        return number.stripTrailingZeros()
                .compareTo(amount.getNumber().numberValue(BigDecimal.class).stripTrailingZeros()) != 0;
    }

    /*
     * }(non-Javadoc)
     * @see MonetaryAmount#adjust(org.javamoney.bp.AmountAdjuster)
     */
    @Override
    public RoundedMoney with(MonetaryOperator operator) {
        Objects.requireNonNull(operator);
        try {
            return RoundedMoney.from(operator.apply(this));
        } catch (MonetaryException | ArithmeticException e) {
            throw e;
        } catch (Exception e) {
            throw new MonetaryException("Query failed: " + operator, e);
        }
    }

    public static RoundedMoney from(MonetaryAmount amt) {
        if (amt.getClass() == RoundedMoney.class) {
            return (RoundedMoney) amt;
        }
        if (amt.getClass() == FastMoney.class) {
            return RoundedMoney.of(amt.getNumber().numberValue(BigDecimal.class), amt.getCurrency());
        } else if (amt.getClass() == Money.class) {
            return RoundedMoney.of(amt.getNumber().numberValue(BigDecimal.class), amt.getCurrency());
        }
        return RoundedMoney.of(amt.getNumber().numberValue(BigDecimal.class), amt.getCurrency());
    }

    /**
     * Obtains an instance of RoundedMoney from a text string such as 'EUR
     * 25.25'.
     *
     * @param text the input text, not null.
     * @return RoundedMoney instance
     * @throws NullPointerException
     * @throws NumberFormatException
     * @throws javax.money.UnknownCurrencyException
     */
    public static RoundedMoney parse(CharSequence text) {
        return parse(text, DEFAULT_FORMATTER);
    }

    /**
     * Obtains an instance of FastMoney from a text using specific formatter.
     *
     * @param text      the text to parse not null
     * @param formatter the formatter to use not null
     * @return RoundedMoney instance
     */
    public static RoundedMoney parse(CharSequence text, MonetaryAmountFormat formatter) {
        return from(formatter.parse(text));
    }

    private static final ToStringMonetaryAmountFormat DEFAULT_FORMATTER = ToStringMonetaryAmountFormat
            .of(ToStringMonetaryAmountFormat.ToStringMonetaryAmountFormatStyle.ROUNDED_MONEY);

    /*
     * }(non-Javadoc)
     * @see MonetaryAmount#adjust(org.javamoney.bp.AmountAdjuster)
     */
    @Override
    public <T> T query(MonetaryQuery<T> query) {
        Objects.requireNonNull(query);
        try {
            return query.queryFrom(this);
        } catch (MonetaryException | ArithmeticException e) {
            throw e;
        } catch (Exception e) {
            throw new MonetaryException("Query failed: " + query, e);
        }
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> T asType(Class<T> type) {
        if (BigDecimal.class.equals(type)) {
            return (T) this.number;
        }
        if (Number.class.equals(type)) {
            return (T) this.number;
        }
        if (Double.class.equals(type)) {
            return (T) Double.valueOf(this.number.doubleValue());
        }
        if (Float.class.equals(type)) {
            return (T) Float.valueOf(this.number.floatValue());
        }
        if (Long.class.equals(type)) {
            return (T) Long.valueOf(this.number.longValue());
        }
        if (Integer.class.equals(type)) {
            return (T) Integer.valueOf(this.number.intValue());
        }
        if (Short.class.equals(type)) {
            return (T) Short.valueOf(this.number.shortValue());
        }
        if (Byte.class.equals(type)) {
            return (T) Byte.valueOf(this.number.byteValue());
        }
        if (BigInteger.class.equals(type)) {
            return (T) this.number.toBigInteger();
        }
        throw new IllegalArgumentException("Unsupported representation type: " + type);
    }

    @Deprecated
    public <T> T asType(Class<T> type, MonetaryOperator adjuster) {
        RoundedMoney amount = (RoundedMoney) adjuster.apply(this);
        return amount.asType(type);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return currency.getCurrencyCode() + ' ' + number;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(currency, asNumberStripped());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RoundedMoney) {
            RoundedMoney other = (RoundedMoney) obj;
            return Objects.equals(currency, other.currency) &&
                    Objects.equals(asNumberStripped(), other.asNumberStripped());
        }
        return false;
    }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MonetaryAmount o) {
        Objects.requireNonNull(o);
        int compare;
        if (currency.equals(o.getCurrency())) {
            compare = asNumberStripped().compareTo(RoundedMoney.from(o).asNumberStripped());
        } else {
            compare = currency.getCurrencyCode().compareTo(o.getCurrency().getCurrencyCode());
        }
        return compare;
    }

    /*
     * (non-Javadoc)
     * @see MonetaryAmount#getNumber()
     */
    @Override
    public NumberValue getNumber() {
        return new DefaultNumberValue(number);
    }

    /**
     * Method that returns BigDecimal.ZERO, if {@link #isZero()}, and #number
     * {@link #stripTrailingZeros()} in all other cases.
     *
     * @return the stripped number value.
     */
    public BigDecimal asNumberStripped() {
        if (isZero()) {
            return BigDecimal.ZERO;
        }
        return number.stripTrailingZeros();
    }

    /**
     * Internal method to check for correct number parameter.
     *
     * @param number the number to check.
     * @throws IllegalArgumentException If the number is null
     */
    private void checkNumber(Number number) {
        Objects.requireNonNull(number, "Number is required.");
    }

    @Override
    public RoundedMoney multiply(long amount) {
        if (amount == 1L) {
            return this;
        }
        return multiply(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney multiply(double amount) {
        Money.checkNoInfinityOrNaN(amount);
        if (amount == 1.0d) {
            return this;
        }
        return multiply(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney divide(long amount) {
        if (amount == 1L) {
            return this;
        }
        return divide(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney divide(double amount) {
        if (Money.isInfinityAndNotNaN(amount)) {
            return new RoundedMoney(0L, getCurrency(), monetaryContext, rounding);
        }
        if (amount == 1.0d) {
            return this;
        }
        return divide(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney remainder(long amount) {
        return remainder(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney remainder(double amount) {
        if (Money.isInfinityAndNotNaN(amount)) {
            return new RoundedMoney(0L, getCurrency(), monetaryContext, rounding);
        }
        return remainder(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney[] divideAndRemainder(long amount) {
        return divideAndRemainder(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney[] divideAndRemainder(double amount) {
        if (Money.isInfinityAndNotNaN(amount)) {
            RoundedMoney zero = new RoundedMoney(0L, getCurrency(), monetaryContext, rounding);
            return new RoundedMoney[]{zero, zero};
        }
        return divideAndRemainder(MoneyUtils.getBigDecimal(amount));
    }

    @Override
    public RoundedMoney stripTrailingZeros() {
        if (isZero()) {
            return of(BigDecimal.ZERO, getCurrency());
        }
        return of(number.stripTrailingZeros(), getCurrency());
    }

    @Override
    public RoundedMoney divideToIntegralValue(long divisor) {
        return divideToIntegralValue(MoneyUtils.getBigDecimal(divisor));
    }

    @Override
    public RoundedMoney divideToIntegralValue(double divisor) {
        return divideToIntegralValue(MoneyUtils.getBigDecimal(divisor));
    }

    @Override
    public MonetaryAmountFactory<RoundedMoney> getFactory() {
        return new RoundedMoneyAmountBuilder().setAmount(this);
    }

    private boolean isOne(Number number) {
        BigDecimal bd = MoneyUtils.getBigDecimal(number);
        try {
            return bd.scale() == 0 && bd.longValueExact() == 1L;
        } catch (Exception e) {
            // The only way to end up here is that longValueExact throws an ArithmeticException,
            // so the amount is definitively not equal to 1.
            return false;
        }
    }
}
