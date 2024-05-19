package net.sf.l2j.gameserver.model.records;

public record ManufactureItem(int recipeId, int cost, boolean isDwarven)
{
	public ManufactureItem(int recipeId, int cost)
	{
		this(recipeId, cost, false);
	}
}