package net.sf.l2j.gameserver.communitybbs.custom;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFManager;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMManager;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMManager;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTManager;

public class EventsBBSManager extends BaseBBSManager
{
	@Override
	public void parseCmd(String command, Player player)
	{
		if (command.equals("_friendlist_0_"))
			showPage("index", player);
		else if (command.startsWith("_friend;cbctf_register"))
		{
			showPage("index", player);
			
			if (!CTFEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				CTFEvent.getInstance().onBypass("ctf_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_049));
				return;
			}
		}
		else if (command.startsWith("_friend;cbdm_register"))
		{
			showPage("index", player);
			
			if (!DMEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				DMEvent.getInstance().onBypass("dm_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_052));
				return;
			}
		}
		else if (command.startsWith("_friend;cblm_register"))
		{
			showPage("index", player);
			
			if (!LMEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				LMEvent.getInstance().onBypass("lm_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_055));
				return;
			}
		}
		else if (command.startsWith("_friend;cbtvt_register"))
		{
			showPage("index", player);
			
			if (!TvTEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				TvTEvent.getInstance().onBypass("tvt_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_058));
				return;
			}
		}
	}
	
	private void showPage(String page, Player player)
	{
		String content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + page + ".htm");
		content = content.replace("%name%", player.getName());
		content = content.replace("%ctf%", Config.CTF_EVENT_ENABLED ? CTFManager.getInstance().getNextTime() : "-");
		content = content.replace("%dm%", Config.DM_EVENT_ENABLED ? DMManager.getInstance().getNextTime() : "-");
		content = content.replace("%lm%", Config.LM_EVENT_ENABLED ? LMManager.getInstance().getNextTime() : "-");
		content = content.replace("%tvt%", Config.TVT_EVENT_ENABLED ? TvTManager.getInstance().getNextTime() : "-");
		separateAndSend(content, player);
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/events/";
	}
	
	public static EventsBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventsBBSManager INSTANCE = new EventsBBSManager();
	}
}