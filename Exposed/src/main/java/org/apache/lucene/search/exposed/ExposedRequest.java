package org.apache.lucene.search.exposed;

import org.apache.lucene.search.exposed.compare.NamedComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A request defines how a TermProvider is to behave with regard to fields
 * and sorting.
 * </p><p>
 * A FacetGroup is the primary access point for sorting. A FacetGroup contains one or
 * more Fields from one or more segments.
 * </p><p>
 * Field is single field and single segment oriented. It is used internally by
 * FacetGroup but can be used externally if the index is fully optimized and sorting
 * is requested for a single field.
 */
public class ExposedRequest {
  /**
   * A group is a collections of fields that are to be treated as one.
   * An example could be a group named "title" with the fields "lti",
   * "sort_title" and "subtitle" as fields. The same comparator must be used
   * for all fields in order for term ordering to be consistent for the group.
   */
  public static class Group {
    private final String name;
    private final List<Field> fields;
    private final NamedComparator comparator;

    public Group(String name, List<Field> fields, NamedComparator comparator) {
      this.name = name;
      this.fields = fields;
      this.comparator = comparator;
    }

    /**
     * Checks whether the given FacetGroup is equal to. The order of the
     * contained fields is irrelevant, but the set of fields must be equal.
     * </p><p>
     * The name of the group is not used while comparing.
     * @param other the group to compare to.
     * @return true if the two groups are equivalent..
     */
    // TODO: Fix hashcode to fulfill the equals vs. hashcode contract
    public boolean equals(Group other) {
      if (other.getFields().size() != getFields().size() ||
          !getComparator().getID().equals(other.getComparator().getID())) {
        return false;
      }

      fieldLoop:
      for (Field otherField: other.getFields()) {
        for (Field thisField: getFields()) {
          if (thisField.equals(otherField)) {
            continue fieldLoop;
          }
        }
        return false;
      }
      return true;
    }

    /**
     * Similar to {@link #equals(Group)} but if the comparatorID for the other
     * group is FREE_ORDER, it matches any comparatorID this group has.
     * @param other the group to check with.
     * @return true if this group can deliver data for the other group.
     */
    // TODO: Re-introduce re-use of structures when count order is requested
    public boolean worksfor(Group other) {
      if (other.getFields().size() != getFields().size()) {
        return false;
      }
        if (!getComparator().getID().equals(other.getComparator().getID())) {
        return false;
      }

      fieldLoop:
      for (Field otherField: other.getFields()) {
        for (Field thisField: getFields()) {
          if (thisField.worksfor(otherField)) {
            continue fieldLoop;
          }
        }
        return false;
      }
      return true;
    }

    public String getName() {
      return name;
    }
    public List<Field> getFields() {
      return fields;
    }
    public NamedComparator getComparator() {
      return comparator;
    }

    public List<String> getFieldNames() {
      List<String> fieldNames = new ArrayList<String>(fields.size());
      for (Field field: fields) {
        fieldNames.add(field.getField());
      }
      return fieldNames;
    }

    /**
     * If the comparatorID is {@link #FREE_ORDER} we set it to
     * {@link #LUCENE_ORDER};
     */
/*    void normalizeComparatorIDs() {
      if (!FREE_ORDER.equals(getComparatorID())) {
        return;
      }
      comparatorID = FREE_ORDER;
      for (Field field: fields) {
        field.comparatorID = FREE_ORDER;
      }
    }  */
  }

  public static class Field {
    private String field;
    private NamedComparator comparator;

    public Field(String field, NamedComparator comparator) {
      this.field = field;
      this.comparator = comparator;
    }

    /**
     * Checks whether the given object is also a Field and if so whether the
     * field and comparatorIDs are equal.
     * @param obj candidate for equality.
     * @return true if the given object is equivalent to this.
     */
    public boolean equals(Object obj) {
      if (!(obj instanceof Field)) {
        return false;
      }
      Field other = (Field)obj;
      return field.equals(other.field)
          && comparator.getID().equals(other.comparator.getID());
    }

    /**
     * Similar to {@link #equals} but if the comparatorID for the other
     * field is FREE_ORDER, it matches any comparatorID this field has.
     * @param other the field to check with.
     * @return true if this group can deliver data for the other field.
     **/
    // TODO: All structures works when count order is requested
    public boolean worksfor(Field other) {
      return field.equals(other.field)
          && comparator.getID().equals(other.comparator.getID());
    }

    public String getField() {
      return field;
    }
    public NamedComparator getComparator() {
      return comparator;
    }

    public boolean equals(Field otherField) {
      return getField().equals(otherField.getField()) &&
          getComparator().getID().equals(otherField.getComparator().getID());
    }

  }

}
