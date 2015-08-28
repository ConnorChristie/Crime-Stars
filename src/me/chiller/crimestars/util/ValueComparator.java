package me.chiller.crimestars.util;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator<UUID> implements Comparator<UUID>
{
	private Map<UUID, Integer> base;
	
	public ValueComparator(Map<UUID, Integer> base)
	{
		this.base = base;
	}
	
	public int compare(UUID a, UUID b)
	{
		if (base.get(a) >= base.get(b))
		{
			return -1;
		} else
		{
			return 1;
		}
	}
}