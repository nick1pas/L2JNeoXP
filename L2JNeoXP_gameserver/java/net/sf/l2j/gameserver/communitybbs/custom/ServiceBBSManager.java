package net.sf.l2j.gameserver.communitybbs.custom;

import static net.sf.l2j.gameserver.model.actor.instance.Service.isValidNick;
import static net.sf.l2j.gameserver.model.actor.instance.Service.updateDatabasePremium;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.taskmanager.PremiumTaskManager;

public class ServiceBBSManager extends BaseBBSManager
{
	@Override
	public void parseCmd(String command, Player player)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		if (command.equals("_bbsgetfav_add"))
			showPage("index", player);
		else if (command.startsWith("_bbsgetfav_add;page"))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				showPage(args[1], player);
		}
		else if (command.startsWith("_bbsgetfav_add;noble"))
		{
			int nobleItemId = Integer.parseInt(st.nextToken());
			int nobleItemCount = Integer.parseInt(st.nextToken());
			
			showPage("character", player);
			
			if (player.getClassId().getLevel() < 3)
			{
				player.sendMessage(player.getSysString(10_020));
				return;
			}
			
			if (player.isNoble())
			{
				player.sendMessage(player.getSysString(10_021));
				return;
			}
			
			if (!player.destroyItemByItemId(nobleItemId, nobleItemCount, true))
				return;
			
			player.setNoble(true, true);
			player.broadcastUserInfo();
		}
		else if (command.startsWith("_bbsgetfav_add;hero"))
		{
			int heroDays = Integer.parseInt(st.nextToken());
			int heroItemId = Integer.parseInt(st.nextToken());
			int heroItemCount = Integer.parseInt(st.nextToken());
			
			showPage("character", player);
			
			if (player.isHero())
			{
				player.sendMessage(player.getSysString(10_022));
				return;
			}
			
			if (player.getInventory().getItemByItemId(heroItemId) == null || player.getInventory().getItemByItemId(heroItemId).getCount() < heroItemCount)
			{
				player.sendMessage(player.getSysString(10_023, heroItemCount));
				return;
			}
			
			player.destroyItemByItemId(heroItemId, heroItemCount, true);
			player.sendPacket(new ItemList(player, false));
			
			player.setHero(true);
			player.setHeroUntil(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(heroDays));
			player.store();
			player.sendMessage(player.getSysString(10_024, heroDays));
			player.broadcastUserInfo();
			ThreadPool.schedule(new Runnable()
			{
				@Override
				public void run()
				{
					if (player.isOnline() && player.isHero())
					{
						player.setHero(false);
						player.setHeroUntil(0);
						player.store();
						player.broadcastUserInfo();
						player.sendMessage(player.getSysString(10_025));
					}
				}
			}, player.getHeroUntil() - System.currentTimeMillis());
		}
		else if (command.startsWith("_bbsgetfav_add;multisell"))
		{
			String[] args = command.split(" ");
			if (args.length < 2)
				return;
			
			MultisellData.getInstance().separateAndSendCB(args[1], player, false);
		}
		else if (command.startsWith("_bbsgetfav_add;setnamecolor"))
		{
			int nameColorId = Integer.parseInt(st.nextToken());
			int nameColorCount = Integer.parseInt(st.nextToken());
			int colorName = Integer.decode("0x" + st.nextToken());
			
			showPage("colorname", player);
			
			if (!player.destroyItemByItemId(nameColorId, nameColorCount, true))
				return;
			
			player.getAppearance().setNameColor(colorName);
			player.setNameColor(colorName);
			player.broadcastUserInfo();
			player.store();
			player.sendMessage(player.getSysString(10_026));
		}
		else if (command.startsWith("_bbsgetfav_add;settitlecolor"))
		{
			int titleColorId = Integer.parseInt(st.nextToken());
			int titleColorCount = Integer.parseInt(st.nextToken());
			int colorTitle = Integer.decode("0x" + st.nextToken());
			
			showPage("colortitle", player);
			
			if (!player.destroyItemByItemId(titleColorId, titleColorCount, true))
				return;
			
			player.getAppearance().setTitleColor(colorTitle);
			player.setTitleColor(colorTitle);
			player.broadcastUserInfo();
			player.store();
			player.sendMessage(player.getSysString(10_027));
		}
		else if (command.startsWith("_bbsgetfav_add;setname"))
		{
			if (st.countTokens() < 1)
				return;
			
			String nick = st.nextToken();
			if (nick.length() < 1 || nick.length() > 16 || !isValidNick(nick))
			{
				player.sendMessage(player.getSysString(10_028));
				return;
			}
			
			if (Config.LIST_RESTRICTED_CHAR_NAMES.contains(nick.toLowerCase()))
			{
				player.sendMessage(player.getSysString(10_028));
				return;
			}
			
			if (PlayerInfoTable.getInstance().getPlayerObjectId(nick) > 0)
			{
				player.sendMessage(player.getSysString(10_029));
				return;
			}
			
			int nameItemId = Integer.parseInt(st.nextToken());
			int nameItemCount = Integer.parseInt(st.nextToken());
			
			if (!player.destroyItemByItemId(nameItemId, nameItemCount, true))
				return;
			
			player.setName(nick);
			PlayerInfoTable.getInstance().updatePlayerData(player, false);
			
			player.store();
			player.broadcastUserInfo();
			
			if (player.getClan() != null)
				player.getClan().broadcastClanStatus();
			
			player.sendMessage(player.getSysString(10_030));
		}
		else if (command.startsWith("_bbsgetfav_add;premium"))
		{
			int day = Integer.parseInt(st.nextToken());
			int itemPremiumId = Integer.parseInt(st.nextToken());
			int price = Integer.parseInt(st.nextToken());
			
			showPage("premium", player);
			
			long premiumTime = 0L;
			
			if (!Config.USE_PREMIUM_SERVICE)
			{
				player.sendMessage(player.getSysString(10_031));
				return;
			}
			
			if (player.getPremServiceData() > Calendar.getInstance().getTimeInMillis())
			{
				player.sendMessage(player.getSysString(10_032));
				return;
			}
			
			if (st.countTokens() >= 1)
			{
				try
				{
					day = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			
			try
			{
				Calendar now = Calendar.getInstance();
				now.add(Calendar.DATE, day);
				premiumTime = now.getTimeInMillis();
			}
			catch (NumberFormatException nfe)
			{
				return;
			}
			
			if (!player.destroyItemByItemId(itemPremiumId, price, true))
				return;
			
			player.setPremiumService(1);
			PremiumTaskManager.getInstance().add(player);
			updateDatabasePremium(premiumTime, player.getAccountName());
			player.sendMessage(player.getSysString(10_033, day));
			player.broadcastUserInfo();
		}
		else if (command.startsWith("_bbsgetfav_add;gender"))
		{
			int itemGenderId = Integer.parseInt(st.nextToken());
			int itemGenderCount = Integer.parseInt(st.nextToken());
			
			if (!player.destroyItemByItemId(itemGenderId, itemGenderCount, true))
				return;
			
			switch (player.getAppearance().getSex())
			{
				case MALE:
					player.getAppearance().setSex(Sex.FEMALE);
					break;
				
				case FEMALE:
					player.getAppearance().setSex(Sex.MALE);
					break;
			}
			
			player.store();
			player.broadcastUserInfo();
			player.sendMessage(player.getSysString(10_034));
			player.decayMe();
			player.spawnMe();
			player.logout(false);
		}
		else if (command.startsWith("_bbsgetfav_add;nullpk"))
		{
			int itemNullPkId = Integer.parseInt(st.nextToken());
			int itemNullPkCount = Integer.parseInt(st.nextToken());
			
			showPage("character", player);
			
			if (player.getPkKills() == 0 && player.getKarma() == 0)
			{
				player.sendMessage(player.getSysString(10_035));
				return;
			}
			
			if (player.getInventory().getItemByItemId(itemNullPkId) == null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			if (player.getInventory().getItemByItemId(itemNullPkId).getCount() < itemNullPkCount)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			player.destroyItemByItemId(itemNullPkId, itemNullPkCount, true);
			player.setPkKills(0);
			player.setKarma(0);
			player.broadcastUserInfo();
			player.sendMessage(player.getSysString(10_036));
		}
		else if (command.startsWith("_bbsgetfav_add;clanlvl"))
		{
			int clanItemId = Integer.parseInt(st.nextToken());
			int lvl = Integer.parseInt(st.nextToken());
			int lvlPrice = Integer.parseInt(st.nextToken());
			
			showPage("clan", player);
			
			if (st.countTokens() >= 1)
			{
				try
				{
					clanItemId = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			
			if (player.getClan().getLevel() == 8)
			{
				player.sendMessage(player.getSysString(10_037));
				return;
			}
			
			if (!player.isClanLeader())
			{
				player.sendMessage(player.getSysString(10_038));
				return;
			}
			
			if (player.getInventory().getItemByItemId(clanItemId) == null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			if (player.getInventory().getItemByItemId(clanItemId).getCount() < lvlPrice)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			if (!(lvl <= player.getClan().getLevel()))
			{
				player.destroyItemByItemId(clanItemId, lvlPrice, true);
				player.getClan().changeLevel(lvl);
				player.sendMessage(player.getSysString(10_039));
			}
		}
		else if (command.startsWith("_bbsgetfav_add;clanskill"))
		{
			int clanSkillItemId = Integer.parseInt(st.nextToken());
			int clanSkillItemCount = Integer.parseInt(st.nextToken());
			
			showPage("clan", player);
			
			if (!player.isClanLeader())
			{
				player.sendMessage(player.getSysString(10_040));
				return;
			}
			
			if (player.getClan() == null || player.getClan().getLevel() < 5)
			{
				player.sendMessage(player.getSysString(10_041));
				return;
			}
			
			if (player.getInventory().getItemByItemId(clanSkillItemId) == null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			if (player.getInventory().getItemByItemId(clanSkillItemId).getCount() < clanSkillItemCount)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			if (!player.getClan().addAllClanSkills())
			{
				player.sendMessage(player.getSysString(10_042));
				return;
			}
			
			player.destroyItemByItemId(clanSkillItemId, clanSkillItemCount, true);
			player.getClan().addAllClanSkills();
			player.getClan().broadcastToMembers(new PledgeSkillList(player.getClan()));
			player.sendMessage(player.getSysString(10_043));
		}
		else if (command.startsWith("_bbsgetfav_add;clanrep"))
		{
			int clanRepItemId = Integer.parseInt(st.nextToken());
			int clanRepItemCount = Integer.parseInt(st.nextToken());
			int clanRepCount = Integer.parseInt(st.nextToken());
			
			showPage("clan", player);
			
			if (!player.isClanLeader())
			{
				player.sendMessage(player.getSysString(10_038));
				return;
			}
			
			if (player.getClan() == null || player.getClan().getLevel() < 5)
			{
				player.sendMessage(player.getSysString(10_041));
				return;
			}
			
			if (player.getInventory().getItemByItemId(clanRepItemId) == null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			if (player.getInventory().getItemByItemId(clanRepItemId).getCount() < clanRepItemCount)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			player.destroyItemByItemId(clanRepItemId, clanRepItemCount, true);
			player.getClan().addReputationScore(clanRepCount);
			player.sendMessage(player.getSysString(10_044, player.getClan().getReputationScore()));
		}
	}
	
	private void showPage(String page, Player player)
	{
		String content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + page + ".htm");
		content = content.replace("%name%", player.getName());
		separateAndSend(content, player);
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/donate/";
	}
	
	public static ServiceBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ServiceBBSManager INSTANCE = new ServiceBBSManager();
	}
}