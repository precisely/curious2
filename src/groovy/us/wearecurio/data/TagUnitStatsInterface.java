package us.wearecurio.data;

import us.wearecurio.data.UnitGroupMap.UnitGroup;

public interface TagUnitStatsInterface {
	Long getUserId();
	Long getTagId();
	UnitGroup getUnitGroup();
	String getUnit();
	Long getTimesUsed();
}
