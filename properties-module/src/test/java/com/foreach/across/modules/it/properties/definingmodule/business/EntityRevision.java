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
package com.foreach.across.modules.it.properties.definingmodule.business;

import com.foreach.across.core.revision.*;

/**
 * @author Arne Vandamme
 */
public class EntityRevision implements com.foreach.across.core.revision.Revision<Long>
{
	private Entity entity;
	private int revisionId;
	private boolean draft, latest;

	public EntityRevision( Entity entity, int revisionId, boolean draft, boolean latest ) {
		this.entity = entity;
		this.revisionId = revisionId;
		this.draft = draft;
		this.latest = latest;
	}

	@Override
	public Long getRevisionOwner() {
		return entity.getId();
	}

	@Override
	public int getRevisionNumber() {
		return revisionId;
	}

	@Override
	public boolean isDraftRevision() {
		return draft;
	}

	@Override
	public boolean isLatestRevision() {
		return latest;
	}
}