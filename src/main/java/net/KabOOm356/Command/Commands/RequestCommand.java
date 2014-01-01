package net.KabOOm356.Command.Commands;

import java.sql.SQLException;
import java.util.ArrayList;

import net.KabOOm356.Command.ReporterCommand;
import net.KabOOm356.Command.ReporterCommandManager;
import net.KabOOm356.Database.ResultRow;
import net.KabOOm356.Database.SQLResultSet;
import net.KabOOm356.Locale.Entry.LocalePhrases.GeneralPhrases;
import net.KabOOm356.Locale.Entry.LocalePhrases.RequestPhrases;
import net.KabOOm356.Reporter.Reporter;
import net.KabOOm356.Util.BukkitUtil;
import net.KabOOm356.Util.Util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * A {@link ReporterCommand} that will handle users requesting reported players from reports.
 */
public class RequestCommand extends ReporterCommand
{
	private static final String name = "Request";
	private static final int minimumNumberOfArguments = 1;
	private final static String permissionNode = "reporter.request";
	
	/**
	 * Constructor.
	 * 
	 * @param manager The {@link ReporterCommandManager} managing this Command.
	 */
	public RequestCommand(ReporterCommandManager manager)
	{
		super(manager, name, permissionNode, minimumNumberOfArguments);
		
		updateDocumentation();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(CommandSender sender, ArrayList<String> args)
	{
		if(hasRequiredPermission(sender))
		{
			if(args.get(0).equalsIgnoreCase("most"))
				requestMostReported(sender);
			else
				requestPlayer(sender, args.get(0));
		}
	}
	
	private void requestMostReported(CommandSender sender)
	{
		// Return the most reported players and the number of reports against them.
		String query = "SELECT COUNT(*) AS Count, Reported, ReportedRaw " +
				"FROM Reports " +
				"GROUP BY ReportedRaw HAVING COUNT(*) = " +
				"(" +
				"SELECT COUNT(*) " +
				"FROM Reports " +
				"GROUP BY ReportedRaw ORDER BY COUNT(*) DESC " +
				"LIMIT 1" +
				")";
		
		try
		{
			ArrayList<String> players = new ArrayList<String>();
			SQLResultSet result;
			int numberOfReports = -1;
			
			result = getManager().getDatabaseHandler().sqlQuery(query);
			
			for(ResultRow row : result)
			{
				numberOfReports = result.getInt("Count");
				
				String player = row.getString("Reported");
				String playerRaw = row.getString("ReportedRaw");
				
				players.add(BukkitUtil.formatPlayerName(player, playerRaw));
			}
			
			if(!players.isEmpty())
			{
				String out = getManager().getLocale().getString(RequestPhrases.numberOfReportsAgainst);
				
				out = out.replaceAll("%n", ChatColor.GOLD + Integer.toString(numberOfReports) + ChatColor.WHITE);
				out = out.replaceAll("%p", Util.indexesToString(players, ChatColor.GOLD, ChatColor.WHITE) + ChatColor.WHITE);
				
				sender.sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() +
						ChatColor.WHITE + out);
			}
			else
			{
				sender.sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() + 
						ChatColor.WHITE + getManager().getLocale().getString(GeneralPhrases.noReports));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			sender.sendMessage(getErrorMessage());
			return;
		}
		finally
		{
			try
			{
				getManager().getDatabaseHandler().closeConnection();
			}
			catch (SQLException e)
			{
			}
		}
	}
	
	private void requestPlayer(CommandSender sender, String playerName)
	{
		String indexes = "";
		
		OfflinePlayer player = null;
		
		if(playerName.equals("!") || playerName.equals("*"))
			playerName = "* (Anonymous)";
		else
			player = getManager().getPlayer(playerName);
		
		if(player == null && !playerName.equalsIgnoreCase("* (Anonymous)"))
		{
			sender.sendMessage(ChatColor.RED + BukkitUtil.colorCodeReplaceAll(
					getManager().getLocale().getString(GeneralPhrases.playerDoesNotExist)));
			return;
		}
		
		if(playerName.equalsIgnoreCase("* (Anonymous)"))
			player = Bukkit.getOfflinePlayer(playerName);
		
		try
		{
			String query = "SELECT ID FROM Reports WHERE ReportedRaw=?";
			ArrayList<String> params = new ArrayList<String>();
			
			params.add(player.getName());
			
			if(getManager().getDatabaseHandler().usingSQLite())
				query += " COLLATE NOCASE";
			
			SQLResultSet result = getManager().getDatabaseHandler().preparedSQLQuery(query, params);
			
			indexes = Util.indexesToString(result, "ID", ChatColor.GOLD, ChatColor.WHITE);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			sender.sendMessage(getErrorMessage());
			return;
		}
		finally
		{
			try
			{
				getManager().getDatabaseHandler().closeConnection();
			}
			catch(SQLException e)
			{
			}
		}
		
		String out = null;
		if(indexes.equals(""))
		{
			out = BukkitUtil.colorCodeReplaceAll(
					getManager().getLocale().getString(RequestPhrases.reqNF));
			
			out = out.replaceAll("%p", ChatColor.GOLD + player.getName() + ChatColor.RED);
		
			sender.sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() + ChatColor.RED + out);
		}
		else
		{
			out = BukkitUtil.colorCodeReplaceAll(
					getManager().getLocale().getString(RequestPhrases.reqFI));
			
			out = out.replaceAll("%p", ChatColor.GOLD + player.getName() + ChatColor.WHITE);
			
			out = out.replaceAll("%i", indexes);
			
			sender.sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() + ChatColor.WHITE + out);
		}
	}
	
	/**
	 * Updates the documentation for the command.
	 * <br/>
	 * This should be called after the locale has changed.
	 */
	public void updateDocumentation()
	{
		super.updateDocumentation(
				getManager().getLocale().getString(RequestPhrases.requestHelp),
				getManager().getLocale().getString(RequestPhrases.requestHelpDetails));
		
		removeAllAlternateUsages();
		removeAllAlternateDescriptions();
		
		super.addUsage("/report request most");
		super.addDescription(getManager().getLocale().getString(RequestPhrases.requestMostHelpDetails));
	}
	
	/**
	 * Returns the name of this command.
	 * 
	 * @return The name of this command.
	 */
	public static String getCommandName()
	{
		return name;
	}
	
	/**
	 * Returns the permission node of this command.
	 * 
	 * @return The permission node of this command.
	 */
	public static String getCommandPermissionNode()
	{
		return permissionNode;
	}
}
