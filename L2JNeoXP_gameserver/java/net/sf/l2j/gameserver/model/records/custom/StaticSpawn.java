package net.sf.l2j.gameserver.model.records.custom;

import java.util.Calendar;

public record StaticSpawn(boolean isEnabled, int id, int days, int[] time, int randomTime, boolean earthQuake, boolean announce)
{
	public boolean canSpawnOnSameDay(int dayWeek)
	{
		return (days & (1 << (dayWeek - 1))) != 0;
	}
	
	public int getSpawnDayTime(int dayTime)
	{
		for (var item : time)
		{
			if (item > dayTime)
				return item;
		}
		return -1;
	}
	
	public int getFirstSpawnDayTime()
	{
		return time[0];
	}
	
	public int getSpawnDayWeek(int dayWeek)
	{
		int index = dayWeek % 7;
		for (int i = 0; i < 7; i++)
		{
			if ((days & (1 << index)) != 0)
				return index + 1;
			
			index = (index + 1) % 7;
		}
		return -1;
	}
	
	public long calcNextDate()
	{
		Calendar cal = Calendar.getInstance();
		final int cDayTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
		final int curDayWeek = cal.get(Calendar.DAY_OF_WEEK);
		int spawnDayTime = getSpawnDayTime(cDayTime);
		if (spawnDayTime != -1 && canSpawnOnSameDay(curDayWeek))
		{
			cal.set(Calendar.HOUR_OF_DAY, spawnDayTime / 60);
			cal.set(Calendar.MINUTE, spawnDayTime % 60);
			cal.set(Calendar.SECOND, 0);
		}
		else
		{
			final int spawnDayWeek = getSpawnDayWeek(curDayWeek);
			int deltaDay = 0;
			if (curDayWeek > spawnDayWeek)
				deltaDay = (7 - curDayWeek) + spawnDayWeek;
			else if (curDayWeek < spawnDayWeek)
				deltaDay = spawnDayWeek - curDayWeek;
			else
				deltaDay = 7;
			
			spawnDayTime = getFirstSpawnDayTime();
			cal.add(Calendar.DAY_OF_MONTH, deltaDay);
			cal.set(Calendar.HOUR_OF_DAY, spawnDayTime / 60);
			cal.set(Calendar.MINUTE, spawnDayTime % 60);
			cal.set(Calendar.SECOND, 0);
		}
		return cal.getTimeInMillis();
	}
}