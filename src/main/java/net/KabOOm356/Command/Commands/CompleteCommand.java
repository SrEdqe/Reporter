package net.KabOOm356.Command.Commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import net.KabOOm356.Command.ReporterCommand;
import net.KabOOm356.Command.ReporterCommandManager;
import net.KabOOm356.Database.ExtendedDatabaseHandler;
import net.KabOOm356.Database.SQLResultSet;
import net.KabOOm356.Locale.Entry.LocalePhrases.CompletePhrases;
import net.KabOOm356.Manager.Messager.Group;
import net.KabOOm356.Manager.SQLStatManagers.ModeratorStatManager.ModeratorStat;
import net.KabOOm356.Reporter.Reporter;
import net.KabOOm356.Util.BukkitUtil;
import net.KabOOm356.Util.Util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A {@link ReporterCommand} that will handle completing reports.
 */
public class CompleteCommand extends ReporterCommand
{
	private static final Logger log = LogManager.getLogger(CompleteCommand.class);
	
	private static final String name = "Complete";
	private static final int minimumNumberOfArguments = 1;
	private final static String permissionNode = "reporter.complete";
	
	private boolean sendMessage;
	public final static Group messageGroup = new Group("Completion");
	
	/**
	 * Constructor.
	 * 
	 * @param manager The {@link ReporterCommandManager} managing this Command.
	 */
	public CompleteCommand(ReporterCommandManager manager)
	{
		super(manager, name, permissionNode, minimumNumberOfArguments);
		
		super.getAliases().add("Finish");
		
		sendMessage = getManager().getConfig().getBoolean(
				"general.messaging.completedMessageOnLogin.completedMessageOnLogin", true);
		
		updateDocumentation();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(CommandSender sender, ArrayList<String> args)
	{
		try {
			if(!hasRequiredPermission(sender))
				return;
			
			int index = Util.parseInt(args.get(0));
			
			if(args.get(0).equalsIgnoreCase("last"))
			{
				if(!hasRequiredLastViewed(sender))
					return;
				
				index = getLastViewed(sender);
			}
			
			if(!getManager().isReportIndexValid(sender, index))
				return;
			
			if(!getManager().canAlterReport(sender, index))
				return;
			
			String summary = "";
			
			for(int LCV = 1; LCV < args.size(); LCV++)
				summary = summary + " " + args.get(LCV);
			
			summary = summary.trim();
			
			if(!isSummaryValid(sender, summary))
				return;
			
			completeReport(sender, index, summary);
			broadcastCompletedMessage(index);
		} catch (final Exception e) {
			log.log(Level.ERROR, "Failed to complete report!", e);
			sender.sendMessage(getErrorMessage());
		}
	}
	
	private void completeReport(CommandSender sender, int index, String summary) throws ClassNotFoundException, SQLException, InterruptedException
	{
		ArrayList<String> params = new ArrayList<String>(5);
		params.add(0, "1");
		params.add(BukkitUtil.getUUIDString(sender));
		params.add(2, sender.getName());
		params.add(3, Reporter.getDateformat().format(new Date()));
		params.add(4, summary);
		params.add(5, Integer.toString(index));
		
		String query = "UPDATE Reports " +
				"SET CompletionStatus=?, " +
				"CompletedByUUID=?, " +
				"CompletedBy=?, " +
				"CompletionDate=?, " +
				"CompletionSummary=? " +
				"WHERE id=?";
		
		final ExtendedDatabaseHandler database = getManager().getDatabaseHandler();
		final int connectionId = database.openPooledConnection();
		try {
			database.preparedUpdateQuery(connectionId, query, params);
		} catch (final SQLException e) {
			log.error(String.format("Failed to execute complete query on connection [%d]!", connectionId));
			throw e;
		} finally {
			database.closeConnection(connectionId);
		}
		
		sender.sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() +
				ChatColor.WHITE + BukkitUtil.colorCodeReplaceAll(
				getManager().getLocale().getString(CompletePhrases.playerComplete)));
		
		if(BukkitUtil.isOfflinePlayer(sender))
		{
			OfflinePlayer senderPlayer = (OfflinePlayer) sender;
		
			getManager().getModStatsManager().incrementStat(senderPlayer, ModeratorStat.COMPLETED);
		}
	}
	
	private boolean isSummaryValid(CommandSender sender, String summary)
	{
		if(!summary.equalsIgnoreCase("") || getManager().getConfig().getBoolean("general.canCompleteWithoutSummary", false))
			return true;
		
		sender.sendMessage(ChatColor.RED + 
				getManager().getLocale().getString(CompletePhrases.completeNoSummary));
		
		return false;
	}
	
	private void broadcastCompletedMessage(int index)
	{
		Player[] p = Bukkit.getOnlinePlayers();
		
		String reportCompleted = BukkitUtil.colorCodeReplaceAll(
				getManager().getLocale().getString(CompletePhrases.broadcastCompleted));	
		
		reportCompleted = reportCompleted.replaceAll("%i", ChatColor.GOLD + Integer.toString(index) + ChatColor.WHITE);
		
		OfflinePlayer sender = null;
		
		String playerName = null;
		String yourReportCompleted = null;
		
		if(getManager().getConfig().getBoolean("general.canViewSubmittedReports", true))
		{
			final ExtendedDatabaseHandler database = getManager().getDatabaseHandler();
			Integer connectionId = null;
			try
			{
				connectionId = database.openPooledConnection();
				
				String query = "SELECT SenderUUID, Sender FROM Reports WHERE ID=" + Integer.toString(index);
				
				SQLResultSet result = database.sqlQuery(connectionId, query);
				
				String uuidString = result.getString("SenderUUID");
				
				if(!uuidString.isEmpty())
				{
					UUID uuid = UUID.fromString(uuidString);
					sender = Bukkit.getOfflinePlayer(uuid);
					playerName = sender.getName();
				}
				else
				{
					playerName = result.getString("Sender");
				}
			} catch (final Exception e) {
				log.warn(String.format("Failed to broadcast report completion message on connection [%d]!", connectionId), e);
			} finally {
				if (connectionId != null) {
					database.closeConnection(connectionId);
				}
			}
			
			yourReportCompleted = BukkitUtil.colorCodeReplaceAll(
					getManager().getLocale().getString(CompletePhrases.broadcastYourReportCompleted));
			
			yourReportCompleted = yourReportCompleted.replaceAll("%i", ChatColor.GOLD + Integer.toString(index) + ChatColor.WHITE);
		}
		
		boolean isReporterOnline = false;
		
		for(int LCV = 0; LCV < p.length; LCV++)
		{
			if(hasPermission(p[LCV], "reporter.list"))
				p[LCV].sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() + ChatColor.WHITE + reportCompleted);
			else if(playerName != null && !playerName.equals("") && playerName.equals(p[LCV].getName()))
			{
				isReporterOnline = true;
				p[LCV].sendMessage(ChatColor.BLUE + Reporter.getLogPrefix() + ChatColor.WHITE + yourReportCompleted);
			}
		}
		
		if(sendMessage && !isReporterOnline)
		{
			String message = ChatColor.BLUE + Reporter.getLogPrefix() + ChatColor.WHITE + 
					getManager().getLocale().getString(CompletePhrases.yourReportsCompleted);
			
			if(sender != null)
			{
				getManager().getMessageManager().addMessage(sender.getUniqueId().toString(), messageGroup, message, index);
			}
			else if(playerName != null && !playerName.equals(""))
			{
				getManager().getMessageManager().addMessage(playerName, messageGroup, message, index);
			}
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
				getManager().getLocale().getString(CompletePhrases.completeHelp),
				getManager().getLocale().getString(CompletePhrases.completeHelpDetails));
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
