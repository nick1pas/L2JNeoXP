package net.sf.l2j.commons.logging.formatter;

import java.util.logging.LogRecord;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.MasterFormatter;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class ItemLogFormatter extends MasterFormatter
{
	@Override
	public String format(LogRecord record)
	{
		final StringBuilder sb = new StringBuilder();
		
		StringUtil.append(sb, "[", getFormatedDate(record.getMillis()), "] ", SPACE, record.getMessage());
		
		for (Object p : record.getParameters())
		{
			if (p == null)
				continue;
			
			if (p instanceof ItemInstance item)
				StringUtil.append(sb, SPACE, item.getLocation(), SPACE, item.getCount(), ((item.getEnchantLevel() > 0) ? " +" + item.getEnchantLevel() + " " : " "), p.toString());
			else
				StringUtil.append(sb, SPACE, p.toString());
		}
		sb.append(CRLF);
		
		return sb.toString();
	}
}