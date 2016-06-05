/*
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
package org.javamoney.moneta.convert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Collection;

import javax.money.Monetary;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;

import org.javamoney.moneta.internal.convert.ECBCurrentRateProvider;
import org.javamoney.moneta.spi.CompoundRateProvider;
import org.testng.annotations.Test;

public class MonetaryConversionTest {



	@Test
	public void testGetExchangeRateProvider() {
		ExchangeRateProvider prov = MonetaryConversions
				.getExchangeRateProvider("ECB");
        assertNotNull(prov);
		assertEquals(ECBCurrentRateProvider.class, prov.getClass());
	}

	@Test
	public void testGetExchangeRateProvider_Chained() throws InterruptedException {
		ExchangeRateProvider prov = MonetaryConversions
				.getExchangeRateProvider("ECB");
        assertNotNull(prov);
		assertEquals(ECBCurrentRateProvider.class, prov.getClass());
		// Test rate provided by IMF (derived)
		Thread.sleep(5000L); // wait for provider to load...
		ExchangeRate r = prov.getExchangeRate(
				Monetary.getCurrency("USD"),
				Monetary.getCurrency("INR"));
        assertNotNull(r);
		assertTrue(r.isDerived());
		// Test rate provided by ECB
		r = prov.getExchangeRate(Monetary.getCurrency("EUR"),
				Monetary.getCurrency("CHF"));
        assertNotNull(r);
		assertFalse(r.isDerived());
	}

	@Test
	public void testGetSupportedProviderContexts() {
		Collection<String> types = MonetaryConversions.getConversionProviderNames();
		assertNotNull(types);
		assertTrue(types.size() >= 1);
		assertTrue(types.contains("ECB"));
	}

}
