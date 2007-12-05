/* $Id: Element.java,v 1.6 2007/12/04 09:28:20 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/12/04 09:28:20 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.facetbrowser.build.facet;

import java.util.LinkedHashSet;
import java.util.HashMap;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Superclass for Facet and Tag. Provides support for sorting.
 * @deprecated in favor of {@link SortedPool}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public abstract class Element implements Cloneable, Comparable {
    private Logger log = Logger.getLogger(Element.class);

    public enum SortOrder       {INSERT, ALPHA, REVERSEALPHA, SCORE,
                                 REVERSESCORE, CUSTOM, REVERSECUSTOM,
                                 SIZE, REVERSESIZE}
//    public enum TagSortOrder       {INSERT, POPULARITY, ALPHA, PROPERTIES}
    public static final Float NOSCORE = Float.NaN;

    private String customString = "";

    // TODO: Implement sorting
    // TODO: Should we add a hitcount, that sums the tags hitcount in facets?

    /**
     * The name of the Element, which is also its ID. The ID must not contain
     * ":". No other restrictions apply.
     */
    protected String name;

    /**
     * The number of added subelements. Note that this number increases
     * for each put, but that the get methods only returns a maximum of
     * maxSubElements elements.
     */
    protected int size = 0;

    /**
     * The maximum number of subElements, that this Element should keep.
     * Setting this to -1 (the default) means that there is no limit.
     */
    protected int maxSubElements = -1;

    /**
     * The sorting order for the children of this element (if any).
     * Setting this to CUSTOM means that the sort order will be defined
     * by the order of the strings in costomOrder.
     */
    protected LinkedHashSet<SortOrder> childrenSortOrders;

    /**
     * The sorting order for this element.
     */
    protected LinkedHashSet<SortOrder> sortOrders;


    /**
     * If TagSortOrder.CUSTOM is specified for childrenSortOrders, the order is
     * dictated by the Strings in childrenCustomOrder. All children objects with
     * names not in customOrder, will be added after those with matching names.
     */
    protected HashMap<String, Integer> childrenCustomOrder;

    /**
     * If TagSortOrder.CUSTOM is specified for sortOrder, the order is
     * dictated by the Strings in customOrder. All elements with names
     * not in customOrder, will be added after those with matching names.
     */
    protected HashMap<String, Integer> customOrder;

    /**
     * An implementation-specific score for the Element.
     */
    protected Float score = NOSCORE;

    /**
     * Constructor which used the default value (-1) for maxSubElements.
     * @param name the name of this Element
     */
    public Element(String name) {
        this.name = name;
    }

    /**
     * Constructor where the maxSubElements is specified.
     * @param name the name of this Element
     * @param maxSubElements the maximum number of subElements, that this
     * Element should keep
     */
    public Element(String name, int maxSubElements) {
        this.name = name;
        this.maxSubElements = maxSubElements;
    }

    /**
     * Constructor where the maxSubElements is specified.
     * @param name the name of this Element
     * @param maxSubElements the maximum number of subElements, that this
     * @param initialSize the initial size for this Element
     * Element should keep
     */
    public Element(String name, int maxSubElements, int initialSize) {
        this.name = name;
        this.maxSubElements = maxSubElements;
        size = initialSize;
    }

    /* Accessors */
    public String getName() {
        return name;
    }
    public int getSize() {
        return size;
    }
    public void setScore(Float score) {
        this.score = score;
    }
    public Float getScore() {
        return score;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    /**
     * Set the customString.
     * @param custom the string to put in customString
     */
    public void setCustomString(String custom) {
        customString = custom;
    }

    /**
     * customString is meant for extra Tag information. It is up to the user of
     * the Facet framework to decide its use. It will be part of the XML
     * generated by toXML in FacetModelImplOO.
     * @return the custom string
     */
    public String getCustomString() {
        return customString;
    }

    protected void setChildrenSorting(LinkedHashSet<SortOrder> sortOrders,
                                      HashMap<String, Integer> customOrder) {
        if (childrenSortOrders == null) {
            childrenSortOrders = new LinkedHashSet<SortOrder>(5);
        } else {
            childrenSortOrders.clear();
        }
        if (sortOrders != null) {
            childrenSortOrders.addAll(sortOrders);
        }
        childrenCustomOrder = customOrder;
        childrenSortOrderChanged();
    }
    protected void setSorting(LinkedHashSet<SortOrder> sortOrders,
                                 HashMap<String, Integer> customOrder) {
        if (this.sortOrders == null) {
            this.sortOrders = new LinkedHashSet<SortOrder>(5);
        } else {
            this.sortOrders.clear();
        }
        if (sortOrders != null) {
            this.sortOrders.addAll(sortOrders);
        }
        this.customOrder = customOrder;
    }



    /**
     * This method is called whenever the sorting order of the children elements
     * are changed.
     */
    protected abstract void childrenSortOrderChanged();

    public int compareTo(Object o) {
        //noinspection ObjectEquality
        if (o == this || !(o instanceof Element) || sortOrders == null) {
            return 0;
        }
        Element other = (Element) o;
        for (SortOrder sortOrder: sortOrders) {
            int result = compareTo(other, sortOrder);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
    protected int compareTo(Element other, SortOrder sortOrder) {
        switch (sortOrder) {
            case INSERT: {
                return 0;
            }
            case ALPHA: {
                return getName().compareTo(other.getName());
            }
            case REVERSEALPHA: {
                return getName().compareTo(other.getName()) * -1;
            }
            case SCORE: {
                return getScore().compareTo(other.getScore()) * -1;
            }
            case REVERSESCORE: {
                return getScore().compareTo(other.getScore());
            }
            case CUSTOM:
            case REVERSECUSTOM: {
                int modifier = sortOrder == SortOrder.REVERSECUSTOM ? -1 : 1;
                if (customOrder == null) {
                    log.error("CUSTOM sortOrder specified, but no customOrder" +
                              " strings are given!");
                    return 0;
                }
                Integer first =  customOrder.get(getName());
                Integer second = customOrder.get(other.getName());
                if (first == null && second == null) {
                    return 0;
                }
                if (first == null) {
                    //noinspection PointlessArithmeticExpression
                    return 1 * modifier;
                }
                if (second == null) {
                    return -1 * modifier;
                }
                return (first-second) * modifier;
            }
            case SIZE: {
                return getSize()-other.getSize();
            }
            case REVERSESIZE: {
                return (getSize()-other.getSize()) * -1;
            }
            default: return 0;
        }
    }

}
