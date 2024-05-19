package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.SpawnData;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Epic implements IVoicedCommandHandler
{
	protected static final CLogger LOGGER = new CLogger(Epic.class.getName());
	
	public static final int PAGE_LIMIT_15 = 15;
	
	private static final String[] VOICED_COMMANDS =
	{
		"epic"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		try
		{
			final int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;
			showEpicHtm(player, page);
		}
		catch (Exception e)
		{
			LOGGER.error(e);
			showEpicHtm(player, 1);
		}
		
		return true;
	}
	
	private void showEpicHtm(Player player, int page)
	{
		if (!Config.SHOW_EPIC_HTM)
			return;
		
		int row = 0;
		final List<Integer> id = Arrays.asList(29066, 29067, 29068, 29045, 29046);
		final Pagination<NpcTemplate> list = new Pagination<>(NpcData.getInstance().getTemplates(t -> t.isType("GrandBoss")).stream().filter(t -> !id.contains(t.getNpcId())).sorted(Comparator.comparing(NpcTemplate::getLevel)), page, PAGE_LIMIT_15);
		list.append("<html><title>Epic Boss Spawn Info</title><body>");
		list.append("<center><br>");
		for (NpcTemplate npc : list)
		{
			String npcName = npc.getName().length() > 23 ? npc.getName().substring(0, 23) + "..." : npc.getName();
			
			final var spawn = SpawnManager.getInstance().getSpawn(npc.getNpcId());
			if (spawn != null)
			{
				final SpawnData spawnData = spawn.getSpawnData();
				if (spawnData != null && spawnData.getRespawnTime() > 0 && spawn.getNpc() != null && !spawn.getNpc().isDecayed())
				{
					list.append((row % 2) == 0 ? "<table width=280 height=22 bgcolor=000000><tr>" : "<table width=280><tr>");
					list.append("<td width=\"146\" align=\"left\">", npcName, "[" + npc.getLevel() + "]", "</td>".toString());
					list.append("<td width=\"110\" align=\"right\"><font color=\"FB5858\">" + player.getSysString(10_073, npc.getCorpseTime() / 60) + "</font></td>");
					list.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
				}
				else if (spawnData != null && spawnData.getRespawnTime() > 0)
				{
					list.append((row % 2) == 0 ? "<table width=280 height=22 bgcolor=000000><tr>" : "<table width=280><tr>");
					list.append("<td width=\"146\" align=\"left\">", npcName, "[" + npc.getLevel() + "]", "</td>".toString());
					list.append("<td width=\"110\" align=\"right\"><font color=\"FB5858\">" + new SimpleDateFormat("dd.MM HH:mm").format(spawnData.getRespawnTime()).toString() + "</font></td>");
					list.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
				}
				else
				{
					list.append((row % 2) == 0 ? "<table width=280 height=22 bgcolor=000000><tr>" : "<table width=280><tr>");
					list.append("<td width=\"180\" align=\"left\">", npcName, "[" + npc.getLevel() + "]", "</td>".toString());
					list.append("<td width=\"110\" align=\"right\"><font color=\"9CC300\">" + player.getSysString(10_074) + "</font></td>");
					list.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
				}
			}
			else
			{
				list.append((row % 2) == 0 ? "<table width=280 height=22 bgcolor=000000><tr>" : "<table width=280><tr>");
				list.append("<td width=\"180\" align=\"left\">", npcName, "[" + npc.getLevel() + "]", "</td>".toString());
				list.append("<td width=\"110\" align=\"right\"><font color=\"9CC300\">" + player.getSysString(10_074) + "</font></td>");
				list.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
			}
			
			row++;
		}
		list.generateSpace(22);
		list.generatePages("bypass voiced_epic %page%");
		list.append("</body></html>");
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(list.getContent());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}