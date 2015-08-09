package us.wearecurio.units;

import java.util.Collection;
import us.wearecurio.units.TagUnitStatsInterface;

public interface TagUnitStatsServiceInterface {
	public TagUnitStatsInterface mostUsedTagUnitStatsForTags(Long userId, Collection<Long> tagIds);
	public TagUnitStatsInterface mostUsedTagUnitStats(Long userId, Long tagId);
}
