package net.sf.l2j.gameserver.communitybbs.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.BufferManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.records.BuffSkill;
import net.sf.l2j.gameserver.skills.L2Skill;

public class BuffBBSManager extends BaseBBSManager
{
	private static final int PAGE_LIMIT = 6;
	
	@Override
	public void parseCmd(String command, Player player)
	{
		final Summon summon = player.getSummon();
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		switch (command)
		{
			case "_bbsloc;premium":
				if (player.getPremiumService() == 1)
				{
					String vip = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "premium.htm");
					vip.replace("%name%", player.getName());
					separateAndSend(vip, player);
				}
				else
					separateAndSend(HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "noPremium.htm"), player);
				
				break;
			case "_bbsloc;heal":
				player.getStatus().setMaxCpHpMp();
				if (summon != null)
					summon.getStatus().setMaxHpMp();
				
				showIndexPage(player);
				break;
			case "_bbsloc;cleanse":
				player.stopAllEffectsDebuff();
				if (summon != null)
					summon.stopAllEffectsDebuff();
				
				showIndexPage(player);
				break;
			case "_bbsloc;support":
				showGiveBuffsWindow(player);
				break;
			case "_bbsloc;cancel":
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				if (summon != null)
					summon.stopAllEffectsExceptThoseThatLastThroughDeath();
				
				showIndexPage(player);
				break;
		}
		
		if (command.equals("_bbsloc"))
			showIndexPage(player);
		else if (command.startsWith("_bbsloc;page"))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				showPage(args[1], player);
		}
		else if (command.startsWith("_bbsloc;getscheme"))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				BufferManager.getInstance().getSchemeSkills(BufferManager.BufferSchemeType.valueOf(args[1].toUpperCase())).forEach(buffId -> getEffect(player, buffId));
			else
				player.sendMessage("Wrong command!");
			
			showIndexPage(player);
		}
		else if (command.startsWith("_bbsloc;buff"))
		{
			try
			{
				String[] args = command.split(" ");
				if (args.length == 4)
				{
					final int skillId = Integer.parseInt(args[1]);
					final int skillLvl = Integer.parseInt(args[2]);
					final String nextPage = args[3];
					
					if (skillId != 0)
					{
						final List<L2Skill> list = new ArrayList<>();
						list.add(SkillTable.getInstance().getInfo(skillId, skillLvl));
						int cost = getFee(list);
						if (cost == 0 || player.reduceAdena(cost, true))
							list.forEach(buffId -> getEffect(player, buffId));
					}
					
					showPage(nextPage, player);
				}
			}
			catch (NumberFormatException e)
			{
				LOGGER.error("Error while processing buff command : " + e.getMessage());
			}
		}
		else if (command.startsWith("_bbsloc;skill"))
		{
			final String groupType = st.nextToken();
			final String schemeName = st.nextToken();
			
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLevel = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			final List<L2Skill> skills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
			premiumSkills(player, skills);
			if (command.startsWith("_bbsloc;skillselect") && !schemeName.equalsIgnoreCase("none"))
			{
				if (skills.size() < player.getMaxBuffCount())
					skills.add(skill);
				else
					player.sendMessage("This scheme has reached the maximum amount of buffs.");
			}
			else if (command.startsWith("_bbsloc;skillunselect"))
				skills.remove(skill);
			
			showEditSchemeWindow(player, groupType, schemeName, page);
		}
		else if (command.startsWith("_bbsloc;createscheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				if (schemeName.length() > 14)
				{
					player.sendMessage("Scheme's name must contain up to chars. Spaces are trimmed.");
					return;
				}
				
				final Map<String, ArrayList<L2Skill>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
				if (schemes != null)
				{
					if (schemes.size() == Config.BUFFER_MAX_SCHEMES)
					{
						player.sendMessage("Maximum schemes amount is already reached.");
						return;
					}
					
					if (schemes.containsKey(schemeName))
						player.sendMessage("The scheme name already exists.");
				}
				
				BufferManager.getInstance().setScheme(player.getObjectId(), schemeName.trim(), new ArrayList<>());
				showGiveBuffsWindow(player);
			}
			catch (Exception e)
			{
				player.sendMessage("Scheme's name must contain up to 14 chars. Spaces are trimmed.");
			}
		}
		else if (command.startsWith("_bbsloc;editschemes"))
			showEditSchemeWindow(player, st.nextToken(), st.nextToken(), Integer.parseInt(st.nextToken()));
		else if (command.startsWith("_bbsloc;deletescheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				final Map<String, ArrayList<L2Skill>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
				
				if (schemes != null && schemes.containsKey(schemeName))
					schemes.remove(schemeName);
			}
			catch (Exception e)
			{
				player.sendMessage("This scheme name is invalid.");
			}
			showGiveBuffsWindow(player);
		}
		else if (command.startsWith("_bbsloc;getbuff"))
		{
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLvl = Integer.parseInt(st.nextToken());
			final List<L2Skill> list = new ArrayList<>();
			list.add(SkillTable.getInstance().getInfo(skillId, skillLvl));
			int cost = getFee(list);
			if (cost == 0 || player.reduceAdena(cost, true))
				list.forEach(buffId -> getEffect(player, buffId));
		}
		else if (command.startsWith("_bbsloc;singlebuff"))
			showSingleBuffSelectionWindow(player, st.nextToken(), Integer.parseInt(st.nextToken()));
		else if (command.startsWith("_bbsloc;givebuffs"))
		{
			final String schemeName = st.nextToken();
			final int cost = Integer.parseInt(st.nextToken());
			
			showGiveBuffsWindow(player);
			
			Creature target = null;
			if (st.hasMoreTokens())
			{
				final String targetType = st.nextToken();
				if (targetType != null && targetType.equalsIgnoreCase("pet"))
					target = player.getSummon();
			}
			else
				target = player;
			
			if (target == null)
				player.sendMessage("You have not summon");
			else if (cost == 0 || player.reduceAdena(cost, true))
				BufferManager.getInstance().applySchemeEffects(null, target, player.getObjectId(), schemeName);
		}
	}
	
	private void showIndexPage(Player player)
	{
		String index = HTMLData.getInstance().getHtm(player.getLocale(), String.format(CB_PATH + getFolder() + "index.htm"));
		index = index.replace("%name%", player.getName());
		separateAndSend(index, player);
	}
	
	private void showPage(String page, Player player)
	{
		String content = HTMLData.getInstance().getHtm(player.getLocale(), String.format(CB_PATH + getFolder() + "%s.htm", page));
		if (content != null)
		{
			content.replace("%name%", player.getName());
			separateAndSend(content, player);
		}
		else
			LOGGER.error("Player %s tried access unknown page: %s", player.getName(), page);
	}
	
	private void getEffect(Player player, L2Skill buff)
	{
		buff.getEffects(player, player);
	}
	
	/**
	 * Send an html packet to the {@link Player} set a parameter with Give Buffs menu info for player and pet, depending on targetType parameter {player, pet}.
	 * @param player : The {@link Player} to make checks on.
	 */
	private void showGiveBuffsWindow(Player player)
	{
		final StringBuilder sb = new StringBuilder(200);
		
		final Map<String, ArrayList<L2Skill>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes == null || schemes.isEmpty())
			sb.append("<font color=\"LEVEL\">Вы не сделали ни одного профиля.</font>");
		else
		{
			for (Map.Entry<String, ArrayList<L2Skill>> scheme : schemes.entrySet())
			{
				final int cost = getFee(scheme.getValue());
				StringUtil.append(sb, "<table width=\"320\"><tr><td width=\"200\"><font color=\"LEVEL\">", scheme.getKey(), " [", scheme.getValue().size(), " / ", player.getMaxBuffCount(), "]", ((cost > 0) ? " - cost: " + StringUtil.formatNumber(cost) : ""), "</font></td></tr></table><br1>");
				StringUtil.append(sb, "<table><tr><td><button value=" + player.getSysString(10_099) + " action=\"bypass _bbsloc;givebuffs ", scheme.getKey(), " ", cost, "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
				StringUtil.append(sb, "<td><button value=" + player.getSysString(10_100) + " action=\"bypass _bbsloc;givebuffs ", scheme.getKey(), " ", cost, " pet\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
				StringUtil.append(sb, "<td><button value=" + player.getSysString(10_101) + " action=\"bypass _bbsloc;editschemes Buffs ", scheme.getKey(), " 1\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
				StringUtil.append(sb, "<td><button value=" + player.getSysString(10_102) + " action=\"bypass _bbsloc;deletescheme ", scheme.getKey(), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr></table><br>");
			}
		}
		
		String html = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "profile/index.htm");
		
		html = html.replace("%schemes%", sb.toString());
		html = html.replace("%name%", player.getName());
		html = html.replace("%max_schemes%", String.valueOf(Config.BUFFER_MAX_SCHEMES));
		separateAndSend(html, player);
	}
	
	/**
	 * Send an html packet to the {@link Player} set as parameter with Edit Scheme Menu info. This allows the {@link Player} to edit each created scheme (add/delete skills)
	 * @param player : The {@link Player} to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @param page : The current checked page.
	 */
	private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page)
	{
		String html = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "profile/edit.htm");
		final List<L2Skill> schemeSkills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
		premiumSkills(player, schemeSkills);
		html = html.replace("%name%", player.getName());
		html = html.replace("%schemename%", schemeName);
		html = html.replace("%count%", schemeSkills.size() + " / " + player.getMaxBuffCount());
		html = html.replace("%typesframe%", getTypesFrame(player, groupType, schemeName, false));
		html = html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName, false, page));
		separateAndSend(html, player);
	}
	
	private void showSingleBuffSelectionWindow(Player player, String groupType, int page)
	{
		String html = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "profile/single.htm");
		html = html.replace("%name%", player.getName());
		html = html.replace("%typesframe%", getTypesFrame(player, groupType, "", true));
		html = html.replace("%skilllistframe%", getGroupSkillList(player, groupType, "", true, page));
		
		separateAndSend(html, player);
	}
	
	/**
	 * @param player : The {@link Player} to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @param singleSelection : Indicate if it's single buff selection or not.
	 * @param page : The current checked page.
	 * @return A {@link String} representing skills available for selection for a given groupType.
	 */
	private String getGroupSkillList(Player player, String groupType, String schemeName, boolean singleSelection, int page)
	{
		// Retrieve the entire skills list based on group type.
		List<L2Skill> skills = BufferManager.getInstance().getSkillsIdsByType(groupType);
		
		premiumSkills(player, skills);
		
		if (skills.isEmpty())
			return "That group doesn't contain any skills.";
		
		// Calculate page number.
		final int max = MathUtil.countPagesNumber(skills.size(), PAGE_LIMIT);
		if (page > max)
			page = max;
		
		// Cut skills list up to page number.
		skills = skills.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skills.size()));
		
		final List<L2Skill> schemeSkills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
		final StringBuilder sb = new StringBuilder(skills.size() * 150);
		
		int row = 0;
		for (L2Skill skill : skills)
		{
			int skillId = skill.getId();
			int skillLevel = skill.getLevel();
			
			sb.append(((row % 2) == 0 ? "<table width=\"280\" bgcolor=\"000000\"><tr>" : "<table width=\"280\"><tr>"));
			
			if (singleSelection)
				StringUtil.append(sb, "<td height=40 width=40><button action=\"bypass _bbsloc;getbuff ", skillId, " ", skillLevel, " ", "\" width=32 height=32 back=\"", skill.getIcon(), "\" fore=\"", skill.getIcon(), "\" /></td><td width=190>", SkillTable.getInstance().getInfo(skillId, 1).getName(), "<br1><font color=\"B09878\">", BufferManager.getInstance().getAvailableBuff(skill).description(), "</font></td>");
			else
			{
				if (schemeSkills.contains(skill))
					StringUtil.append(sb, "<td height=40 width=40><img src=\"", skill.getIcon(), "\" width=32 height=32></td><td width=190>", SkillTable.getInstance().getInfo(skillId, 1).getName(), "<br1><font color=\"B09878\">", BufferManager.getInstance().getAvailableBuff(skill).description(), "</font></td><td><button action=\"bypass _bbsloc;skillunselect ", groupType, " ", schemeName, " ", skillId, " ", skillLevel, " ", page, "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
				else
					StringUtil.append(sb, "<td height=40 width=40><img src=\"", skill.getIcon(), "\" width=32 height=32></td><td width=190>", SkillTable.getInstance().getInfo(skillId, 1).getName(), "<br1><font color=\"B09878\">", BufferManager.getInstance().getAvailableBuff(skill).description(), "</font></td><td><button action=\"bypass _bbsloc;skillselect ", groupType, " ", schemeName, " ", skillId, " ", skillLevel, " ", page, "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			}
			
			sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
			StringUtil.append(sb, "<img height=41>");
		
		// Build page footer.
		sb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1><table width=\"100%\" bgcolor=000000><tr>");
		
		if (page > 1)
		{
			if (singleSelection)
				StringUtil.append(sb, "<td align=left width=70><a action=\"bypass _bbsloc;singlebuff ", groupType, " ", page - 1, "\">Предыдущая</a></td>");
			else
				StringUtil.append(sb, "<td align=left width=70><a action=\"bypass _bbsloc;editschemes ", groupType, " ", schemeName, " ", page - 1, "\">Предыдущая</a></td>");
		}
		else
			StringUtil.append(sb, "<td align=left width=70>Предыдущая</td>");
		
		StringUtil.append(sb, "<td align=center width=100>Страница ", page, "</td>");
		
		if (page < max)
		{
			if (singleSelection)
				StringUtil.append(sb, "<td align=right width=70><a action=\"bypass _bbsloc;singlebuff ", groupType, " ", page + 1, "\">Следующая</a></td>");
			else
				StringUtil.append(sb, "<td align=right width=70><a action=\"bypass _bbsloc;editschemes ", groupType, " ", schemeName, " ", page + 1, "\">Следующая</a></td>");
		}
		else
			StringUtil.append(sb, "<td align=right width=70>Следующая</td>");
		
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
		
		return sb.toString();
	}
	
	/**
	 * @param player
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @param singleSelection : Indicate if it's single buff selection or not.
	 * @return A {@link String} representing all groupTypes available. The group currently on selection isn't linkable.
	 */
	private static String getTypesFrame(Player player, String groupType, String schemeName, boolean singleSelection)
	{
		final StringBuilder sb = new StringBuilder(500);
		sb.append("<table>");
		
		int count = 0;
		for (String type : BufferManager.getInstance().getSkillTypes())
		{
			if (player.getPremiumService() == 0)
			{
				if (Config.PREMIUM_BUFFS_CATEGORY.isEmpty())
					continue;
			}
			
			if (count == 0)
				sb.append("<tr>");
			
			if (groupType.equalsIgnoreCase(type))
				StringUtil.append(sb, "<td width=65>", type, "</td>");
			else
			{
				if (singleSelection)
					StringUtil.append(sb, "<td width=65><a action=\"bypass _bbsloc;singlebuff ", type, " 1\">", type, "</a></td>");
				else
					StringUtil.append(sb, "<td width=65><a action=\"bypass _bbsloc;editschemes ", type, " ", schemeName, " 1\">", type, "</a></td>");
			}
			
			count++;
			if (count == 4)
			{
				sb.append("</tr>");
				count = 0;
			}
		}
		
		if (!sb.toString().endsWith("</tr>"))
			sb.append("</tr>");
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * @param list : A {@link List} of skill ids.
	 * @return a global fee for all skills contained in the {@link List}.
	 */
	private static int getFee(List<L2Skill> list)
	{
		if (Config.BUFFER_STATIC_BUFF_COST > 0)
			return list.size() * Config.BUFFER_STATIC_BUFF_COST;
		
		int fee = 0;
		for (L2Skill sk : list)
		{
			if (sk != null)
			{
				BuffSkill buffSkill = BufferManager.getInstance().getAvailableBuff(sk);
				if (buffSkill != null)
					fee += buffSkill.price();
			}
		}
		
		return fee;
	}
	
	private void premiumSkills(Player player, List<L2Skill> skills)
	{
		if (player.getPremiumService() == 0)
			skills.removeIf(skill -> Config.BUFFS_CATEGORY.contains(BufferManager.getInstance().getAvailableBuff(skill).type()));
	}
	
	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		super.parseWrite(ar1, ar2, ar3, ar4, ar5, player);
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/buff/";
	}
	
	public static BuffBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BuffBBSManager INSTANCE = new BuffBBSManager();
	}
}