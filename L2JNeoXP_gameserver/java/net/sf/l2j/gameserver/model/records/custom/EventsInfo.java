package net.sf.l2j.gameserver.model.records.custom;

public record EventsInfo(int[] itemId, int[] itemCount, int[] itemChance, String eventName, int[] minLvl)
{
}