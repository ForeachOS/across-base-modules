/*
 * Copyright 2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foreach.across.modules.debugweb.support;

/**
 * @author Stijn Vanhoof
 */

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class Table
{
	private String title;
	private String subTitle;
	private TableHeader header;
	private Collection<TableRow> rows = new LinkedList<>();

	public Table( String title ) {
		this.title = title;
	}

	public void addRow( TableRow row ) {
		rows.add( row );
	}

	public void addRow( Object... fields ) {
		rows.add( new TableRow( fields ) );
	}

	/**
	 * Converts a map into a table where the first column is the key
	 * and the second column the value.
	 *
	 * @param title Title for the table.
	 * @param data  Map to be converted.
	 * @return Table instance.
	 */
	public static Table fromMap( String title, Map data ) {
		Set<Object> sortedKeys = new LinkedHashSet<Object>( data.keySet() );

		Table table = new Table( title );

		for ( Object key : sortedKeys ) {
			table.addRow( key, data.get( key ) );
		}

		return table;
	}

	public boolean isEmpty() {
		return rows.isEmpty();
	}

	public int size() {
		return rows.size();
	}
}
