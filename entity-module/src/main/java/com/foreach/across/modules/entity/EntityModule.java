package com.foreach.across.modules.entity;

import com.foreach.across.core.AcrossModule;
import com.foreach.across.core.annotations.AcrossDepends;

@AcrossDepends(
		required = { "AcrossHibernateModule" },
		optional = { "AdminWebModule" }
)
public class EntityModule extends AcrossModule
{
	public static final String NAME = "EntityModule";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Provide entity management functionality.";
	}
}