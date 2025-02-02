package net.sf.l2j.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.PrivilegeType;
import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.enums.actors.NpcTalkCond;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.type.ClanHallManagerNpcSiegeAI;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHallFunction;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ClanHallDecoration;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.WarehouseDepositList;
import net.sf.l2j.gameserver.network.serverpackets.WarehouseWithdrawList;
import net.sf.l2j.gameserver.skills.L2Skill;

public class ClanHallManagerNpcSiege extends Merchant
{
	private static final String HP_GRADE_0 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>]";
	private static final String HP_GRADE_1 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 400\">400%</a>]";
	
	private static final String MP_GRADE_0 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>]";
	private static final String MP_GRADE_1 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 50\">50%</a>]";
	
	private static final String EXP_GRADE_0 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>]";
	private static final String EXP_GRADE_1 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
	
	private static final String SUPPORT_GRADE_0 = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">5th Stage</a>]";
	private static final String SUPPORT_GRADE_1 = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">5th Stage</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 8\">8th Level</a>]";
	
	private static final String ITEM = "[<a action=\"bypass -h npc_%objectId%_manage other edit_item 1\">1st Stage</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 2\">2st Stage</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 3\">3st Stage</a>]";
	private static final String TELE = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">1st Stage</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">2st Stage</a>]";
	
	public ClanHallManagerNpcSiege(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public ClanHallManagerNpcSiegeAI getAI()
	{
		return (ClanHallManagerNpcSiegeAI) _ai;
	}
	
	@Override
	public void setAI()
	{
		_ai = new ClanHallManagerNpcSiegeAI(this);
	}
	
	@Override
	public boolean isWarehouse()
	{
		return true;
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final NpcTalkCond condition = getNpcTalkCond(player);
		if (condition != NpcTalkCond.OWNER)
			return;
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		String val = (st.hasMoreTokens()) ? st.nextToken() : "";
		
		if (actualCommand.equalsIgnoreCase("banish_foreigner"))
		{
			if (!validatePrivileges(player, PrivilegeType.CHP_RIGHT_TO_DISMISS))
				return;
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (val.equalsIgnoreCase("list"))
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/banish-list.htm");
			else if (val.equalsIgnoreCase("banish"))
			{
				getClanHall().banishForeigners();
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/banish.htm");
			}
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("manage_vault"))
		{
			if (!validatePrivileges(player, PrivilegeType.SP_WAREHOUSE_SEARCH))
				return;
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/clanHallManagerSiege/vault.htm");
			html.replace("%rent%", getClanHall().getLease());
			html.replace("%date%", new SimpleDateFormat("dd-MM-yyyy HH:mm").format(getClanHall().getPaidUntil()));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("door"))
		{
			if (!validatePrivileges(player, PrivilegeType.CHP_ENTRY_EXIT_RIGHTS))
				return;
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (val.equalsIgnoreCase("open"))
			{
				getClanHall().openDoors();
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/door-open.htm");
			}
			else if (val.equalsIgnoreCase("close"))
			{
				getClanHall().closeDoors();
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/door-close.htm");
			}
			else
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/door.htm");
			
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("functions"))
		{
			if (!validatePrivileges(player, PrivilegeType.CHP_USE_FUNCTIONS))
				return;
			
			if (val.equalsIgnoreCase("tele"))
			{
				final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_TELEPORT);
				if (chf == null)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/chamberlain-nac.htm");
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					return;
				}
				
				showTeleportWindow(player, (chf.getLvl() == 2) ? TeleportType.CHF_LEVEL_2 : TeleportType.CHF_LEVEL_1);
			}
			else if (val.equalsIgnoreCase("item_creation"))
			{
				if (!st.hasMoreTokens())
					return;
				
				final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE);
				if (chf == null)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/chamberlain-nac.htm");
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
					return;
				}
				
				showBuyWindow(player, Integer.parseInt(st.nextToken()) + (chf.getLvl() * 100000));
			}
			else if (val.equalsIgnoreCase("support"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				
				final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_SUPPORT);
				if (chf == null)
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/chamberlain-nac.htm");
				else
				{
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/support" + chf.getLvl() + ".htm");
					html.replace("%mp%", (int) getStatus().getMp());
				}
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
			else if (val.equalsIgnoreCase("back"))
				showChatWindow(player);
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions.htm");
				
				final ClanHallFunction chfExp = getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP);
				if (chfExp != null)
					html.replace("%xp_regen%", chfExp.getLvl());
				else
					html.replace("%xp_regen%", "0");
				
				final ClanHallFunction chfHp = getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP);
				if (chfHp != null)
					html.replace("%hp_regen%", chfHp.getLvl());
				else
					html.replace("%hp_regen%", "0");
				
				final ClanHallFunction chfMp = getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP);
				if (chfMp != null)
					html.replace("%mp_regen%", chfMp.getLvl());
				else
					html.replace("%mp_regen%", "0");
				
				html.replace("%npcId%", getNpcId());
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
		}
		else if (actualCommand.equalsIgnoreCase("manage"))
		{
			if (!validatePrivileges(player, PrivilegeType.CHP_SET_FUNCTIONS))
				return;
			
			if (val.equalsIgnoreCase("recovery"))
			{
				if (st.hasMoreTokens())
				{
					if (getClanHall().getOwnerId() == 0)
						return;
					
					val = st.nextToken();
					
					if (val.equalsIgnoreCase("hp_cancel"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel.htm");
						html.replace("%apply%", "recovery hp 0");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("mp_cancel"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel.htm");
						html.replace("%apply%", "recovery mp 0");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("exp_cancel"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel.htm");
						html.replace("%apply%", "recovery exp 0");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("edit_hp"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply.htm");
						html.replace("%name%", "Fireplace (HP Recovery Device)");
						
						final int percent = Integer.parseInt(st.nextToken());
						
						int cost;
						switch (percent)
						{
							case 20:
								cost = Config.CH_HPREG1_FEE;
								break;
							
							case 40:
								cost = Config.CH_HPREG2_FEE;
								break;
							
							case 80:
								cost = Config.CH_HPREG3_FEE;
								break;
							
							case 100:
								cost = Config.CH_HPREG4_FEE;
								break;
							
							case 120:
								cost = Config.CH_HPREG5_FEE;
								break;
							
							case 140:
								cost = Config.CH_HPREG6_FEE;
								break;
							
							case 160:
								cost = Config.CH_HPREG7_FEE;
								break;
							
							case 180:
								cost = Config.CH_HPREG8_FEE;
								break;
							
							case 200:
								cost = Config.CH_HPREG9_FEE;
								break;
							
							case 220:
								cost = Config.CH_HPREG10_FEE;
								break;
							
							case 240:
								cost = Config.CH_HPREG11_FEE;
								break;
							
							case 260:
								cost = Config.CH_HPREG12_FEE;
								break;
							
							default:
								cost = Config.CH_HPREG13_FEE;
								break;
						}
						
						html.replace("%cost%", cost + "</font> adenas / " + (Config.CH_HPREG_FEE_RATIO / 86400000) + " day</font>)");
						html.replace("%use%", "Provides additional HP recovery for clan members in the clan hall.<font color=\"00FFFF\">" + percent + "%</font>");
						html.replace("%apply%", "recovery hp " + percent);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("edit_mp"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply.htm");
						html.replace("%name%", "Carpet (MP Recovery)");
						
						final int percent = Integer.parseInt(st.nextToken());
						
						int cost;
						switch (percent)
						{
							case 5:
								cost = Config.CH_MPREG1_FEE;
								break;
							
							case 10:
								cost = Config.CH_MPREG2_FEE;
								break;
							
							case 15:
								cost = Config.CH_MPREG3_FEE;
								break;
							
							case 30:
								cost = Config.CH_MPREG4_FEE;
								break;
							
							default:
								cost = Config.CH_MPREG5_FEE;
								break;
						}
						
						html.replace("%cost%", cost + "</font> adenas / " + (Config.CH_MPREG_FEE_RATIO / 86400000) + " day</font>)");
						html.replace("%use%", "Provides additional MP recovery for clan members in the clan hall.<font color=\"00FFFF\">" + percent + "%</font>");
						html.replace("%apply%", "recovery mp " + percent);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("edit_exp"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply.htm");
						html.replace("%name%", "Chandelier (EXP Recovery Device)");
						
						final int percent = Integer.parseInt(st.nextToken());
						
						int cost;
						switch (percent)
						{
							case 5:
								cost = Config.CH_EXPREG1_FEE;
								break;
							
							case 10:
								cost = Config.CH_EXPREG2_FEE;
								break;
							
							case 15:
								cost = Config.CH_EXPREG3_FEE;
								break;
							
							case 25:
								cost = Config.CH_EXPREG4_FEE;
								break;
							
							case 35:
								cost = Config.CH_EXPREG5_FEE;
								break;
							
							case 40:
								cost = Config.CH_EXPREG6_FEE;
								break;
							
							default:
								cost = Config.CH_EXPREG7_FEE;
								break;
						}
						
						html.replace("%cost%", cost + "</font> adenas / " + (Config.CH_EXPREG_FEE_RATIO / 86400000) + " day</font>)");
						html.replace("%use%", "Restores the Exp of any clan member who is resurrected in the clan hall.<font color=\"00FFFF\">" + percent + "%</font>");
						html.replace("%apply%", "recovery exp " + percent);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("hp"))
					{
						val = st.nextToken();
						final int percent = Integer.parseInt(val);
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						
						final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP);
						if (chf != null && chf.getLvl() == percent)
						{
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-used.htm");
							html.replace("%val%", val + "%");
							html.replace("%objectId%", getObjectId());
							player.sendPacket(html);
							return;
						}
						
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply_confirmed.htm");
						
						int fee;
						switch (percent)
						{
							case 0:
								fee = 0;
								html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel_confirmed.htm");
								break;
							
							case 20:
								fee = Config.CH_HPREG1_FEE;
								break;
							
							case 40:
								fee = Config.CH_HPREG2_FEE;
								break;
							
							case 80:
								fee = Config.CH_HPREG3_FEE;
								break;
							
							case 100:
								fee = Config.CH_HPREG4_FEE;
								break;
							
							case 120:
								fee = Config.CH_HPREG5_FEE;
								break;
							
							case 140:
								fee = Config.CH_HPREG6_FEE;
								break;
							
							case 160:
								fee = Config.CH_HPREG7_FEE;
								break;
							
							case 180:
								fee = Config.CH_HPREG8_FEE;
								break;
							
							case 200:
								fee = Config.CH_HPREG9_FEE;
								break;
							
							case 220:
								fee = Config.CH_HPREG10_FEE;
								break;
							
							case 240:
								fee = Config.CH_HPREG11_FEE;
								break;
							
							case 260:
								fee = Config.CH_HPREG12_FEE;
								break;
							
							default:
								fee = Config.CH_HPREG13_FEE;
								break;
						}
						
						if (!getClanHall().updateFunction(player, ClanHall.FUNC_RESTORE_HP, percent, fee, Config.CH_HPREG_FEE_RATIO))
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/low_adena.htm");
						else
							revalidateDeco(player);
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("mp"))
					{
						val = st.nextToken();
						final int percent = Integer.parseInt(val);
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						
						final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP);
						if (chf != null && chf.getLvl() == percent)
						{
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-used.htm");
							html.replace("%val%", val + "%");
							html.replace("%objectId%", getObjectId());
							player.sendPacket(html);
							return;
						}
						
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply_confirmed.htm");
						
						int fee;
						switch (percent)
						{
							case 0:
								fee = 0;
								html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel_confirmed.htm");
								break;
							
							case 5:
								fee = Config.CH_MPREG1_FEE;
								break;
							
							case 10:
								fee = Config.CH_MPREG2_FEE;
								break;
							
							case 15:
								fee = Config.CH_MPREG3_FEE;
								break;
							
							case 30:
								fee = Config.CH_MPREG4_FEE;
								break;
							
							default:
								fee = Config.CH_MPREG5_FEE;
								break;
						}
						
						if (!getClanHall().updateFunction(player, ClanHall.FUNC_RESTORE_MP, percent, fee, Config.CH_MPREG_FEE_RATIO))
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/low_adena.htm");
						else
							revalidateDeco(player);
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("exp"))
					{
						val = st.nextToken();
						final int percent = Integer.parseInt(val);
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						
						final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP);
						if (chf != null && chf.getLvl() == percent)
						{
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-used.htm");
							html.replace("%val%", val + "%");
							html.replace("%objectId%", getObjectId());
							player.sendPacket(html);
							return;
						}
						
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply_confirmed.htm");
						
						int fee;
						switch (percent)
						{
							case 0:
								fee = 0;
								html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel_confirmed.htm");
								break;
							
							case 5:
								fee = Config.CH_EXPREG1_FEE;
								break;
							
							case 10:
								fee = Config.CH_EXPREG2_FEE;
								break;
							
							case 15:
								fee = Config.CH_EXPREG3_FEE;
								break;
							
							case 25:
								fee = Config.CH_EXPREG4_FEE;
								break;
							
							case 35:
								fee = Config.CH_EXPREG5_FEE;
								break;
							
							case 40:
								fee = Config.CH_EXPREG6_FEE;
								break;
							
							default:
								fee = Config.CH_EXPREG7_FEE;
								break;
						}
						
						if (!getClanHall().updateFunction(player, ClanHall.FUNC_RESTORE_EXP, percent, fee, Config.CH_EXPREG_FEE_RATIO))
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/low_adena.htm");
						else
							revalidateDeco(player);
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/edit_recovery.htm");
					
					final int grade = getClanHall().getSize();
					
					// Restore HP function.
					final ClanHallFunction chfHp = getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP);
					if (chfHp != null)
					{
						html.replace("%hp_recovery%", chfHp.getLvl() + "%</font> (<font color=\"FFAABB\">" + chfHp.getLease() + "</font> adenas / " + (Config.CH_HPREG_FEE_RATIO / 86400000) + " day)");
						html.replace("%hp_period%", "Next fee at " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(chfHp.getEndTime()));
						
						switch (grade)
						{
							case 0:
								html.replace("%change_hp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Stops using</a>]" + HP_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_hp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Stops using</a>]" + HP_GRADE_1);
								break;
						}
					}
					else
					{
						html.replace("%hp_recovery%", "Suspended");
						html.replace("%hp_period%", "Suspended");
						
						switch (grade)
						{
							case 0:
								html.replace("%change_hp%", HP_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_hp%", HP_GRADE_1);
								break;
						}
					}
					
					// Restore exp function.
					final ClanHallFunction chfExp = getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP);
					if (chfExp != null)
					{
						html.replace("%exp_recovery%", chfExp.getLvl() + "%</font> (<font color=\"FFAABB\">" + chfExp.getLease() + "</font> adenas / " + (Config.CH_EXPREG_FEE_RATIO / 86400000) + " day)");
						html.replace("%exp_period%", "Next fee at " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(chfExp.getEndTime()));
						
						switch (grade)
						{
							case 0:
								html.replace("%change_exp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Stops using</a>]" + EXP_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_exp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Stops using</a>]" + EXP_GRADE_1);
								break;
						}
					}
					else
					{
						html.replace("%exp_recovery%", "Suspended");
						html.replace("%exp_period%", "Suspended");
						
						switch (grade)
						{
							case 0:
								html.replace("%change_exp%", EXP_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_exp%", EXP_GRADE_1);
								break;
						}
					}
					
					// Restore MP function.
					final ClanHallFunction chfMp = getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP);
					if (chfMp != null)
					{
						html.replace("%mp_recovery%", chfMp.getLvl() + "%</font> (<font color=\"FFAABB\">" + chfMp.getLease() + "</font> adenas / " + (Config.CH_MPREG_FEE_RATIO / 86400000) + " day)");
						html.replace("%mp_period%", "Next fee at " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(chfMp.getEndTime()));
						
						switch (grade)
						{
							case 0:
								html.replace("%change_mp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Stops using</a>]" + MP_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_mp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Stops using</a>]" + MP_GRADE_1);
								break;
						}
					}
					else
					{
						html.replace("%mp_recovery%", "Suspended");
						html.replace("%mp_period%", "Suspended");
						
						switch (grade)
						{
							case 0:
								html.replace("%change_mp%", MP_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_mp%", MP_GRADE_1);
								break;
						}
					}
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
				}
			}
			else if (val.equalsIgnoreCase("other"))
			{
				if (st.hasMoreTokens())
				{
					if (getClanHall().getOwnerId() == 0)
						return;
					
					val = st.nextToken();
					
					if (val.equalsIgnoreCase("item_cancel"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel.htm");
						html.replace("%apply%", "other item 0");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("tele_cancel"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel.htm");
						html.replace("%apply%", "other tele 0");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("support_cancel"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel.htm");
						html.replace("%apply%", "other support 0");
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("edit_item"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply.htm");
						html.replace("%name%", "Magic Equipment (Item Production Facilities)");
						
						final int stage = Integer.parseInt(st.nextToken());
						
						int cost;
						switch (stage)
						{
							case 1:
								cost = Config.CH_ITEM1_FEE;
								break;
							
							case 2:
								cost = Config.CH_ITEM2_FEE;
								break;
							
							default:
								cost = Config.CH_ITEM3_FEE;
								break;
						}
						
						html.replace("%cost%", cost + "</font> adenas / " + (Config.CH_ITEM_FEE_RATIO / 86400000) + " day</font>)");
						html.replace("%use%", "Allow the purchase of special items at fixed intervals.");
						html.replace("%apply%", "other item " + stage);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("edit_support"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply.htm");
						html.replace("%name%", "Insignia (Supplementary Magic)");
						
						final int stage = Integer.parseInt(st.nextToken());
						
						int cost;
						switch (stage)
						{
							case 1:
								cost = Config.CH_SUPPORT1_FEE;
								break;
							
							case 2:
								cost = Config.CH_SUPPORT2_FEE;
								break;
							
							case 3:
								cost = Config.CH_SUPPORT3_FEE;
								break;
							
							case 4:
								cost = Config.CH_SUPPORT4_FEE;
								break;
							
							case 5:
								cost = Config.CH_SUPPORT5_FEE;
								break;
							
							case 6:
								cost = Config.CH_SUPPORT6_FEE;
								break;
							
							case 7:
								cost = Config.CH_SUPPORT7_FEE;
								break;
							
							default:
								cost = Config.CH_SUPPORT8_FEE;
								break;
						}
						
						html.replace("%cost%", cost + "</font> adenas / " + (Config.CH_SUPPORT_FEE_RATIO / 86400000) + " day</font>)");
						html.replace("%use%", "Enables the use of supplementary magic.");
						html.replace("%apply%", "other support " + stage);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("edit_tele"))
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply.htm");
						html.replace("%name%", "Mirror (Teleportation Device)");
						
						final int stage = Integer.parseInt(st.nextToken());
						
						int cost;
						switch (stage)
						{
							case 1:
								cost = Config.CH_TELE1_FEE;
								break;
							
							default:
								cost = Config.CH_TELE2_FEE;
								break;
						}
						
						html.replace("%cost%", cost + "</font> adenas / " + (Config.CH_TELE_FEE_RATIO / 86400000) + " day</font>)");
						html.replace("%use%", "Teleports clan members in a clan hall to the target <font color=\"00FFFF\">Stage " + stage + "</font> staging area");
						html.replace("%apply%", "other tele " + stage);
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("item"))
					{
						if (getClanHall().getOwnerId() == 0)
							return;
						
						val = st.nextToken();
						final int lvl = Integer.parseInt(val);
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						
						final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE);
						if (chf != null && chf.getLvl() == lvl)
						{
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-used.htm");
							html.replace("%val%", "Stage " + val);
							html.replace("%objectId%", getObjectId());
							player.sendPacket(html);
							return;
						}
						
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply_confirmed.htm");
						
						int fee;
						switch (lvl)
						{
							case 0:
								fee = 0;
								html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel_confirmed.htm");
								break;
							
							case 1:
								fee = Config.CH_ITEM1_FEE;
								break;
							
							case 2:
								fee = Config.CH_ITEM2_FEE;
								break;
							
							default:
								fee = Config.CH_ITEM3_FEE;
								break;
						}
						
						if (!getClanHall().updateFunction(player, ClanHall.FUNC_ITEM_CREATE, lvl, fee, Config.CH_ITEM_FEE_RATIO))
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/low_adena.htm");
						else
							revalidateDeco(player);
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("tele"))
					{
						val = st.nextToken();
						final int lvl = Integer.parseInt(val);
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						
						final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_TELEPORT);
						if (chf != null && chf.getLvl() == lvl)
						{
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-used.htm");
							html.replace("%val%", "Stage " + val);
							html.replace("%objectId%", getObjectId());
							player.sendPacket(html);
							return;
						}
						
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply_confirmed.htm");
						
						int fee;
						switch (lvl)
						{
							case 0:
								fee = 0;
								html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel_confirmed.htm");
								break;
							
							case 1:
								fee = Config.CH_TELE1_FEE;
								break;
							
							default:
								fee = Config.CH_TELE2_FEE;
								break;
						}
						
						if (!getClanHall().updateFunction(player, ClanHall.FUNC_TELEPORT, lvl, fee, Config.CH_TELE_FEE_RATIO))
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/low_adena.htm");
						else
							revalidateDeco(player);
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
					else if (val.equalsIgnoreCase("support"))
					{
						val = st.nextToken();
						final int lvl = Integer.parseInt(val);
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						
						final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_SUPPORT);
						if (chf != null && chf.getLvl() == lvl)
						{
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-used.htm");
							html.replace("%val%", "Stage " + val);
							html.replace("%objectId%", getObjectId());
							player.sendPacket(html);
							return;
						}
						
						html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-apply_confirmed.htm");
						
						int fee;
						switch (lvl)
						{
							case 0:
								fee = 0;
								html.setFile(player.getLocale(), "html/clanHallManagerSiege/functions-cancel_confirmed.htm");
								break;
							
							case 1:
								fee = Config.CH_SUPPORT1_FEE;
								break;
							
							case 2:
								fee = Config.CH_SUPPORT2_FEE;
								break;
							
							case 3:
								fee = Config.CH_SUPPORT3_FEE;
								break;
							
							case 4:
								fee = Config.CH_SUPPORT4_FEE;
								break;
							
							case 5:
								fee = Config.CH_SUPPORT5_FEE;
								break;
							
							case 6:
								fee = Config.CH_SUPPORT6_FEE;
								break;
							
							case 7:
								fee = Config.CH_SUPPORT7_FEE;
								break;
							
							default:
								fee = Config.CH_SUPPORT8_FEE;
								break;
						}
						
						if (!getClanHall().updateFunction(player, ClanHall.FUNC_SUPPORT, lvl, fee, Config.CH_SUPPORT_FEE_RATIO))
							html.setFile(player.getLocale(), "html/clanHallManagerSiege/low_adena.htm");
						else
							revalidateDeco(player);
						
						html.replace("%objectId%", getObjectId());
						player.sendPacket(html);
					}
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/edit_other.htm");
					
					final ClanHallFunction chfTel = getClanHall().getFunction(ClanHall.FUNC_TELEPORT);
					if (chfTel != null)
					{
						html.replace("%tele%", "- Stage " + chfTel.getLvl() + "</font> (<font color=\"FFAABB\">" + chfTel.getLease() + "</font> adenas / " + (Config.CH_TELE_FEE_RATIO / 86400000) + " day)");
						html.replace("%tele_period%", "Next fee at " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(chfTel.getEndTime()));
						html.replace("%change_tele%", "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Stops using</a>]" + TELE);
					}
					else
					{
						html.replace("%tele%", "Suspended");
						html.replace("%tele_period%", "Suspended");
						html.replace("%change_tele%", TELE);
					}
					
					final int grade = getClanHall().getSize();
					
					final ClanHallFunction chfSup = getClanHall().getFunction(ClanHall.FUNC_SUPPORT);
					if (chfSup != null)
					{
						html.replace("%support%", "- Stage " + chfSup.getLvl() + "</font> (<font color=\"FFAABB\">" + chfSup.getLease() + "</font> adenas / " + (Config.CH_SUPPORT_FEE_RATIO / 86400000) + " day)");
						html.replace("%support_period%", "Next fee at " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(chfSup.getEndTime()));
						
						switch (grade)
						{
							case 0:
								html.replace("%change_support%", "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Stops using</a>]" + SUPPORT_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_support%", "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Stops using</a>]" + SUPPORT_GRADE_1);
								break;
						}
					}
					else
					{
						html.replace("%support%", "Suspended");
						html.replace("%support_period%", "Suspended");
						
						switch (grade)
						{
							case 0:
								html.replace("%change_support%", SUPPORT_GRADE_0);
								break;
							
							case 1:
								html.replace("%change_support%", SUPPORT_GRADE_1);
								break;
						}
					}
					
					final ClanHallFunction chfCreate = getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE);
					if (chfCreate != null)
					{
						html.replace("%item%", "- Stage " + chfCreate.getLvl() + "</font> (<font color=\"FFAABB\">" + chfCreate.getLease() + "</font> adenas / " + (Config.CH_ITEM_FEE_RATIO / 86400000) + " day)");
						html.replace("%item_period%", "Next fee at " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(chfCreate.getEndTime()));
						html.replace("%change_item%", "[<a action=\"bypass -h npc_%objectId%_manage other item_cancel\">Stops using</a>]" + ITEM);
					}
					else
					{
						html.replace("%item%", "Suspended");
						html.replace("%item_period%", "Suspended");
						html.replace("%change_item%", ITEM);
					}
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
				}
			}
			else if (val.equalsIgnoreCase("back"))
				showChatWindow(player);
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getLocale(), "html/clanHallManagerSiege/manage.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
		}
		else if (actualCommand.equalsIgnoreCase("support"))
		{
			if (!validatePrivileges(player, PrivilegeType.CHP_USE_FUNCTIONS))
				return;
			
			final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_SUPPORT);
			if (chf == null || chf.getLvl() == 0)
				return;
			
			if (player.isCursedWeaponEquipped())
			{
				// Custom system message
				player.sendMessage("The wielder of a cursed weapon cannot receive outside heals or buffs");
				return;
			}
			
			setTarget(player);
			
			try
			{
				final int id = Integer.parseInt(val);
				final int lvl = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 0;
				
				if (Config.CUSTOM_BUFFER_MANAGER_NPC)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
					if (skill != null)
						skill.getEffects(this, player);
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getLocale(), "html/clanHallManagerSiege/support" + chf.getLvl() + ".htm");
					html.replace("%mp%", (int) getStatus().getMp());
					html.replace("%objectId%", getObjectId());
					player.sendPacket(html);
				}
				else
					getAI().addCastDesire(player, id, lvl, 1000000);
			}
			catch (final Exception e)
			{
				player.sendMessage("Invalid skill, contact your server support.");
			}
		}
		else if (actualCommand.equalsIgnoreCase("list_back"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/clanHallManagerSiege/chamberlain.htm");
			html.replace("%npcname%", getName());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("support_back"))
		{
			if (!validatePrivileges(player, PrivilegeType.CHP_USE_FUNCTIONS))
				return;
			
			final ClanHallFunction chf = getClanHall().getFunction(ClanHall.FUNC_SUPPORT);
			if (chf == null || chf.getLvl() == 0)
				return;
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/clanHallManagerSiege/support" + getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getLvl() + ".htm");
			html.replace("%mp%", (int) getStatus().getMp());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("WithdrawC"))
		{
			if (!validatePrivileges(player, PrivilegeType.SP_WAREHOUSE_SEARCH))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
				return;
			}
			
			if (player.getClan().getLevel() == 0)
			{
				player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
				return;
			}
			
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new WarehouseWithdrawList(player, WarehouseWithdrawList.CLAN));
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (actualCommand.equalsIgnoreCase("DepositC"))
		{
			if (player.getClan() != null)
			{
				if (player.getClan().getLevel() == 0)
					player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
				else
				{
					player.setActiveWarehouse(player.getClan().getWarehouse());
					player.tempInventoryDisable();
					player.sendPacket(new WarehouseDepositList(player, WarehouseDepositList.CLAN));
				}
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getLocale(), "html/clanHallManagerSiege/chamberlain" + ((getNpcTalkCond(player) == NpcTalkCond.OWNER) ? ".htm" : "-no.htm"));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	protected boolean isTeleportAllowed(Player player)
	{
		return validatePrivileges(player, PrivilegeType.CHP_USE_FUNCTIONS);
	}
	
	@Override
	protected NpcTalkCond getNpcTalkCond(Player player)
	{
		if (getClanHall() != null && player.getClan() != null && getClanHall().getOwnerId() == player.getClanId())
			return NpcTalkCond.OWNER;
		
		return NpcTalkCond.NONE;
	}
	
	private void revalidateDeco(Player player)
	{
		getClanHall().getZone().broadcastPacket(new ClanHallDecoration(getClanHall()));
	}
	
	private boolean validatePrivileges(Player player, PrivilegeType privilege)
	{
		if (!player.hasClanPrivileges(privilege))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/clanHallManagerSiege/not_authorized.htm");
			player.sendPacket(html);
			return false;
		}
		return true;
	}
}