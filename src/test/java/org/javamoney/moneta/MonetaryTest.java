/**
 * Copyright (c) 2012, 2017, Anatole Tresch, Werner Keil and others by the @author tag.
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import javax.money.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.testng.Assert.assertNotNull;

public class MonetaryTest {

	@Test
	public void shouldCreateMonetaryFactory() {
		MonetaryAmount monetaryAmount = Monetary
				.getAmountFactory(
						MonetaryAmountFactoryQueryBuilder.of().set(RoundingMode.DOWN).setPrecision(256).build())
				.setCurrency("CHF").setNumber(1234.5678).create();
		assertEquals(256, monetaryAmount.getContext().getPrecision());
		assertEquals(RoundingMode.DOWN, monetaryAmount.getContext().get(RoundingMode.class));
	}

	@Test
	public void shouldCreateMonetaryFactoryWithRoundindModeNull() {
		MonetaryAmount monetaryAmount = Monetary
				.getAmountFactory(MonetaryAmountFactoryQueryBuilder.of().setPrecision(256).build()).setCurrency("CHF")
				.setNumber(1234.5678).create();
		assertEquals(256, monetaryAmount.getContext().getPrecision());
		assertEquals(monetaryAmount.getContext().get(RoundingMode.class),RoundingMode.HALF_EVEN /*default*/);
	}

	@Test
	public void shouldCreateMonetaryFactoryWithPrecisionNull() {
		MonetaryAmount monetaryAmount = Monetary.getAmountFactory(
				MonetaryAmountFactoryQueryBuilder.of().set(RoundingMode.HALF_DOWN).setTargetType(Money.class).build())
				.setCurrency("CHF").setNumber(1234.5678).create();
		assertEquals(0, monetaryAmount.getContext().getPrecision());
		assertEquals(RoundingMode.HALF_DOWN, monetaryAmount.getContext().get(RoundingMode.class));
	}

	@Test
	public void shouldCreateMonetaryFactoryWithBothNull() {
		MonetaryAmount monetaryAmount = Monetary.getAmountFactory(
				MonetaryAmountFactoryQueryBuilder.of().set(RoundingMode.HALF_DOWN).setTargetType(Money.class).build())
				.setCurrency("CHF").setNumber(1234.5678).create();
		assertEquals(0, monetaryAmount.getContext().getPrecision());
		assertEquals(RoundingMode.HALF_DOWN, monetaryAmount.getContext().get(RoundingMode.class));
	}

	@Test
	public void shouldUseContextInFactory() {
		MonetaryContext mc = MonetaryContextBuilder.of().setMaxScale(2).setPrecision(64).set(RoundingMode.FLOOR).build();
		MonetaryAmount am = Monetary.getDefaultAmountFactory().setContext(mc).setNumber(999.999).setCurrency("EUR").create();
		assertEquals(am.getNumber().numberValue(BigDecimal.class), BigDecimal.valueOf(999.99));
	}

	@Test
	public void accessMultipleCurrenciesWithCode() {
		Collection<CurrencyUnit> currencies = Monetary.getCurrencies(CurrencyQueryBuilder.of().setNumericCodes(324).build());
		assertNotNull(currencies);
		assertTrue(currencies.size()==1);
		assertEquals(currencies.iterator().next().getCurrencyCode(), "GNF");
	}

	@Test
	public void accessMultipleCurrenciesWithCodeAll() {
		Collection<CurrencyUnit> currencies = Monetary.getCurrencies(CurrencyQueryBuilder.of().setNumericCodes(0).build());
		assertNotNull(currencies);
		assertTrue(currencies.size()>1);
	}

	@Test
	public void accessSingleCurrencyWithCode() {
		CurrencyUnit currency = Monetary.getCurrency(CurrencyQueryBuilder.of().setNumericCodes(324).build());
		assertNotNull(currency);
		assertEquals(currency.getCurrencyCode(), "GNF");
	}
}
