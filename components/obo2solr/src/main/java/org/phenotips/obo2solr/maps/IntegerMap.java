/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.obo2solr.maps;

public class IntegerMap<K> extends AbstractNumericValueMap<K, Integer>
{
    private static final long serialVersionUID = 1L;

    public IntegerMap()
    {
        super();
    }

    public IntegerMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    @Override
    protected final Integer getZero()
    {
        return 0;
    }

    @Override
    public Integer addTo(K key, Integer value)
    {
        Integer crtValue = this.get(key);
        if (value == null) {
            return this.put(key, value);
        } else {
            return this.put(key, crtValue + value);
        }
    }
}
