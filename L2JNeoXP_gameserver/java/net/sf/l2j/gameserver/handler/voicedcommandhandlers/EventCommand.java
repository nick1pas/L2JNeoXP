package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class EventCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"ctfinfo",
		"ctfjoin",
		"ctfleave",
		"dminfo",
		"dmjoin",
		"dmleave",
		"lminfo",
		"lmjoin",
		"lmleave",
		"tvtinfo",
		"tvtjoin",
		"tvtleave"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("ctfinfo"))
		{
			if (CTFEvent.getInstance().isStarting() || CTFEvent.getInstance().isStarted())
			{
				showCTFStatusPage(player);
				return true;
			}
			player.sendMessage(player.getSysString(10_048));
			return false;
		}
		else if (command.equals("ctfjoin"))
		{
			if (!CTFEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				CTFEvent.getInstance().onBypass("ctf_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_049));
				return false;
			}
		}
		else if (command.equals("ctfleave"))
		{
			if (CTFEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				CTFEvent.getInstance().onBypass("ctf_event_remove_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_050));
				return false;
			}
		}
		else if (command.equals("dminfo"))
		{
			if (DMEvent.getInstance().isStarting() || DMEvent.getInstance().isStarted())
			{
				showDMStatusPage(player);
				return true;
			}
			player.sendMessage(player.getSysString(10_051));
			return false;
		}
		else if (command.equalsIgnoreCase("dmjoin"))
		{
			if (!DMEvent.getInstance().isPlayerParticipant(player))
				DMEvent.getInstance().onBypass("dm_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_052));
				return false;
			}
		}
		else if (command.equalsIgnoreCase("dmleave"))
		{
			if (DMEvent.getInstance().isPlayerParticipant(player))
				DMEvent.getInstance().onBypass("dm_event_remove_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_053));
				return false;
			}
		}
		else if (command.equals("lminfo"))
		{
			if (LMEvent.getInstance().isStarting() || LMEvent.getInstance().isStarted())
			{
				showLMStatusPage(player);
				return true;
			}
			player.sendMessage(player.getSysString(10_054));
			return false;
		}
		else if (command.equalsIgnoreCase("lmjoin"))
		{
			if (!LMEvent.getInstance().isPlayerParticipant(player))
				LMEvent.getInstance().onBypass("lm_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_055));
				return false;
			}
		}
		else if (command.equalsIgnoreCase("lmleave"))
		{
			if (LMEvent.getInstance().isPlayerParticipant(player))
				LMEvent.getInstance().onBypass("lm_event_remove_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_056));
				return false;
			}
		}
		else if (command.equals("tvtinfo"))
		{
			if (TvTEvent.getInstance().isStarting() || TvTEvent.getInstance().isStarted())
			{
				showTvTStatusPage(player);
				return true;
			}
			player.sendMessage(player.getSysString(10_057));
			return false;
		}
		else if (command.equals("tvtjoin"))
		{
			if (!TvTEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				TvTEvent.getInstance().onBypass("tvt_event_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_058));
				return false;
			}
		}
		else if (command.equals("tvtleave"))
		{
			if (TvTEvent.getInstance().isPlayerParticipant(player.getObjectId()))
				TvTEvent.getInstance().onBypass("tvt_event_remove_participation", player);
			else
			{
				player.sendMessage(player.getSysString(10_059));
				return false;
			}
		}
		return true;
	}
	
	private void showCTFStatusPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.getLocale(), "html/mods/events/ctf/Status.htm");
		html.replace("%team1name%", Config.CTF_EVENT_TEAM_1_NAME);
		html.replace("%team1playercount%", String.valueOf(CTFEvent.getInstance().getTeamsPlayerCounts()[0]));
		html.replace("%team1points%", String.valueOf(CTFEvent.getInstance().getTeamsPoints()[0]));
		html.replace("%team2name%", Config.CTF_EVENT_TEAM_2_NAME);
		html.replace("%team2playercount%", String.valueOf(CTFEvent.getInstance().getTeamsPlayerCounts()[1]));
		html.replace("%team2points%", String.valueOf(CTFEvent.getInstance().getTeamsPoints()[1]));
		player.sendPacket(html);
	}
	
	private void showDMStatusPage(Player player)
	{
		String[] firstPositions = DMEvent.getInstance().getFirstPosition(Config.DM_REWARD_FIRST_PLAYERS);
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.getLocale(), "html/mods/events/dm/Status.htm");
		
		String htmltext = "";
		if (firstPositions != null)
		{
			for (int i = 0; i < firstPositions.length; i++)
			{
				String[] row = firstPositions[i].split("\\,");
				htmltext += "<tr><td>" + row[0] + "</td><td width=\"100\" align=\"center\">" + row[1] + "</td></tr>";
			}
		}
		
		html.replace("%positions%", htmltext);
		player.sendPacket(html);
	}
	
	private void showLMStatusPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.getLocale(), "html/mods/events/lm/Status.htm");
		String htmltext = String.valueOf(LMEvent.getInstance().getPlayerCounts());
		html.replace("%countplayer%", htmltext);
		player.sendPacket(html);
	}
	
	private void showTvTStatusPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player.getLocale(), "html/mods/events/tvt/Status.htm");
		html.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
		html.replace("%team1playercount%", String.valueOf(TvTEvent.getInstance().getTeamsPlayerCounts()[0]));
		html.replace("%team1points%", String.valueOf(TvTEvent.getInstance().getTeamsPoints()[0]));
		html.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
		html.replace("%team2playercount%", String.valueOf(TvTEvent.getInstance().getTeamsPlayerCounts()[1]));
		html.replace("%team2points%", String.valueOf(TvTEvent.getInstance().getTeamsPoints()[1]));
		player.sendPacket(html);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}