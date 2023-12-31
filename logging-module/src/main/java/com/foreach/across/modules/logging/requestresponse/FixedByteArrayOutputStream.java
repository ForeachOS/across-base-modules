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
package com.foreach.across.modules.logging.requestresponse;

import java.io.ByteArrayOutputStream;

public class FixedByteArrayOutputStream extends ByteArrayOutputStream
{
	private int maxSize;
	private boolean maximumReached;
	private long byteCount;

	public FixedByteArrayOutputStream( int maxSize ) {
		super();
		this.maxSize = maxSize;
	}

	public FixedByteArrayOutputStream( int initialSize, int maxSize ) {
		super( initialSize );
		this.maxSize = maxSize;
	}

	@Override
	public synchronized void write( byte[] b, int off, int len ) {
		if ( maximumNotReached() ) {
			super.write( b, off, len );
		}
		else {
			maximumReached = true;
		}
		byteCount = byteCount + len;
	}

	@Override
	public synchronized void write( int b ) {
		if ( maximumNotReached() ) {
			super.write( b );
		}
		else {
			maximumReached = true;
		}
		byteCount++;
	}

	private boolean maximumNotReached() {
		return !maximumReached && byteCount < maxSize;
	}

	public long getRealSize() {
		return byteCount;
	}

	public boolean isMaximumReached() {
		return maximumReached;
	}
}
