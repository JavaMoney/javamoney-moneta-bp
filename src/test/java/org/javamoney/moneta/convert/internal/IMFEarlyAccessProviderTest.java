package org.javamoney.moneta.convert.internal;


import org.javamoney.moneta.Money;
import org.testng.annotations.Test;

import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;

/**
 * Created by atsticks on 07.07.17.
 */
public class IMFEarlyAccessProviderTest {

    @Test
    public void testAccess_IMF() throws InterruptedException {
        final MonetaryAmount inEuro = Money.of(10, "EUR");
        for(int i=0; i<100;i++){
            try {
                final ExchangeRateProvider rateProvider = MonetaryConversions.getExchangeRateProvider("IMF");
                final CurrencyConversion dollarConversion = rateProvider.getCurrencyConversion("USD");
                final MonetaryAmount inDollar = inEuro.with(dollarConversion);
                System.out.println(String.format("RUN: %n - %s: %s ≙ %s", i, rateProvider, inEuro, inDollar));
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        for(int i=0; i<100;i++){
            final ExchangeRateProvider rateProvider = MonetaryConversions.getExchangeRateProvider("IMF");
            final CurrencyConversion dollarConversion = rateProvider.getCurrencyConversion("USD");
            Thread.sleep(100L);
            final MonetaryAmount inDollar = inEuro.with(dollarConversion);
            System.out.println(String.format("RUN: %n - %s: %s ≙ %s", i, rateProvider, inEuro, inDollar));
        }

    }

}
