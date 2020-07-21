/*
  Copyright (c) 2020, Werner Keil and others by the @author tag.

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
package org.javamoney.moneta.internal;

/**
 * This is a drop-in-replacement for some convenience methods like <code>nonNull</code> added to the
 * <type>Objects</type> class in Java 8 or beyond.
 */
public class JDKObjects {
    /**
     * JDK Drop-in-replacement
     * @since 1.4.1
     */
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    /**
     * JDK Drop-in-replacement
     * @since 1.4.1
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
}
