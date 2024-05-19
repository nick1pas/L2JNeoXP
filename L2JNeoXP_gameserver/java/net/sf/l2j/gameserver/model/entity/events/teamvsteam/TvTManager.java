package net.sf.l2j.gameserver.model.entity.events.teamvsteam;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

public class TvTManager
{
	private static final CLogger LOGGER = new CLogger(TvTManager.class.getName());
	
	private TvTStartTask _task;
	
	public static final int JOIN_TVT_REQ_ID = 100_004;
	
	public Calendar _nextEvent;
	
	protected TvTManager()
	{
		if (Config.TVT_EVENT_ENABLED)
		{
			TvTEvent.getInstance().init();
			
			scheduleEventStart();
			LOGGER.info("Team vs Team Engine: is Started.");
		}
		else
			LOGGER.info("Team vs Team Engine: is disabled.");
	}
	
	public void scheduleEventStart()
	{
		try
		{
			final Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			
			for (String timeOfDay : Config.TVT_EVENT_INTERVAL)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, Integer.parseInt("00"));
				
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				
				if ((nextStartTime == null) || (testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis()))
					nextStartTime = testStartTime;
			}
			
			_nextEvent = nextStartTime;
			
			if (nextStartTime != null)
			{
				_task = new TvTStartTask(nextStartTime.getTimeInMillis());
				ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("TvTEventEngine: Error figuring out a start time. Check TvTEventInterval in config file.");
		}
	}
	
	public void startReg()
	{
		if (!TvTEvent.getInstance().startParticipation())
		{
			World.announceToOnlinePlayers("Team vs Team: Event was cancelled.");
			scheduleEventStart();
		}
		else
		{
			World.announceToOnlinePlayers("Team vs Team: Joinable in " + Config.TVT_NPC_LOC_NAME + "!");
			
			if (Config.EVENT_COMMANDS)
				World.announceToOnlinePlayers("Team vs Team: Command: .tvtjoin / .tvtleave / .tvtinfo");
			
			if (Config.ALLOW_TVT_DLG)
			{
				for (Player players : World.getInstance().getPlayers())
				{
					if (OlympiadManager.getInstance().isRegistered(players) || players.isAlikeDead() || players.isTeleporting() || players.isDead() || players.isInObserverMode() || players.isInStoreMode() || TvTEvent.getInstance().isStarted() && TvTEvent.getInstance().isPlayerParticipant(players.getObjectId()) || players.isInsideZone(ZoneId.CASTLE) || players.isInsideZone(ZoneId.SIEGE) || players.getClassId() == ClassId.BISHOP || players.getClassId() == ClassId.CARDINAL || players.getClassId() == ClassId.SHILLIEN_ELDER || players.getClassId() == ClassId.SHILLIEN_SAINT || players.getClassId() == ClassId.ELVEN_ELDER || players.getClassId() == ClassId.EVAS_SAINT || players.getClassId() == ClassId.PROPHET || players.getClassId() == ClassId.HIEROPHANT)
						continue;
					
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1);
					confirm.addString("Do you wish to Join TvT Event?");
					confirm.addTime(45000);
					confirm.addRequesterId(JOIN_TVT_REQ_ID);
					players.sendPacket(confirm);
				}
			}
			
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.TVT_EVENT_PARTICIPATION_TIME));
			ThreadPool.execute(_task);
		}
	}
	
	public void startEvent()
	{
		if (!TvTEvent.getInstance().startFight())
		{
			World.announceToOnlinePlayers("Team vs Team: Event cancelled due to lack of Participation.");
			scheduleEventStart();
		}
		else
		{
			TvTEvent.getInstance().sysMsgToAllParticipants("Teleporting participants in " + Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.TVT_EVENT_RUNNING_TIME));
			ThreadPool.execute(_task);
		}
	}
	
	public void endEvent()
	{
		World.announceToOnlinePlayers(TvTEvent.getInstance().calculateRewards());
		TvTEvent.getInstance().sysMsgToAllParticipants("Teleporting back to town in " + Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		TvTEvent.getInstance().stopFight();
		
		scheduleEventStart();
	}
	
	public void skipDelay()
	{
		if (_task == null)
			return;
		
		if (_task.nextRun.cancel(false))
		{
			_task.setStartTime(System.currentTimeMillis());
			ThreadPool.execute(_task);
		}
	}
	
	class TvTStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;
		
		public TvTStartTask(long startTime)
		{
			_startTime = startTime;
		}
		
		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}
		
		@Override
		public void run()
		{
			int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);
			
			if (delay > 0)
				announce(delay);
			
			int nextMsg = 0;
			if (delay > 3600)
				nextMsg = delay - 3600;
			else if (delay > 1800)
				nextMsg = delay - 1800;
			else if (delay > 900)
				nextMsg = delay - 900;
			else if (delay > 600)
				nextMsg = delay - 600;
			else if (delay > 300)
				nextMsg = delay - 300;
			else if (delay > 60)
				nextMsg = delay - 60;
			else if (delay > 5)
				nextMsg = delay - 5;
			else if (delay > 0)
				nextMsg = delay;
			else
			{
				if (TvTEvent.getInstance().isInactive())
					startReg();
				else if (TvTEvent.getInstance().isParticipating())
					startEvent();
				else
					endEvent();
			}
			
			if (delay > 0)
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
		}
		
		private void announce(long time)
		{
			if ((time >= 3600) && ((time % 3600) == 0))
			{
				if (TvTEvent.getInstance().isParticipating())
					World.announceToOnlinePlayers("Team vs Team: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				else if (TvTEvent.getInstance().isStarted())
					TvTEvent.getInstance().sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
			}
			else if (time >= 60)
			{
				if (TvTEvent.getInstance().isParticipating())
					World.announceToOnlinePlayers("Team vs Team: " + (time / 60) + " minute(s) until registration is closed!");
				else if (TvTEvent.getInstance().isStarted())
					TvTEvent.getInstance().sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
			}
			else
			{
				if (TvTEvent.getInstance().isParticipating())
					World.announceToOnlinePlayers("Team vs Team: " + time + " second(s) until registration is closed!");
				else if (TvTEvent.getInstance().isStarted())
					TvTEvent.getInstance().sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
			}
		}
	}
	
	public String getNextTime()
	{
		Calendar currentTime = Calendar.getInstance();
		for (String timeOfDay : Config.TVT_EVENT_INTERVAL)
		{
			String[] splitTimeOfDay = timeOfDay.split(":");
			int eventHour = Integer.parseInt(splitTimeOfDay[0]);
			int eventMinute = Integer.parseInt(splitTimeOfDay[1]);
			
			Calendar eventTime = Calendar.getInstance();
			eventTime.setLenient(true);
			eventTime.set(Calendar.HOUR_OF_DAY, eventHour);
			eventTime.set(Calendar.MINUTE, eventMinute);
			eventTime.set(Calendar.SECOND, 0);
			
			if (eventTime.getTimeInMillis() > currentTime.getTimeInMillis())
			{
				SimpleDateFormat format = new SimpleDateFormat("HH:mm");
				return format.format(eventTime.getTime());
			}
		}
		
		return "00:00";
	}
	
	public static final TvTManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TvTManager INSTANCE = new TvTManager();
	}
}