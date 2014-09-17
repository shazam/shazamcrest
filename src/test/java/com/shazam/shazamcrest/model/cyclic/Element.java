/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.model.cyclic;

import java.util.*;

@SuppressWarnings("unused")
public class Element {
    private static final Map<String, Element> ELEMENTS = new HashMap<String, Element>();
    public static final Element ONE = element("one");
    public static final Element TWO = element("two");
    private static final Element THREE = element("three");
    private static final Element FOUR = element("four");
    private static final Element FIVE = element("five");
    private static final Element SIX = element("six");
    private static final Element SEVEN = element("seven");
    private static final Element EIGHT = element("eight");
    private static final Element NINE = element("nine");

    private static Element element(String elementString) {
        Element element = new Element(elementString);
        ELEMENTS.put(elementString.toUpperCase(), element);
        return element;
    }

    private String element;

    public Element(String element) {
        this.element = element;
    }
}
