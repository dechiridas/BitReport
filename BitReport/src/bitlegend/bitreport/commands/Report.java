package bitlegend.bitreport.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import bitlegend.bitreport.BitReport;

public class Report implements CommandExecutor {
	private BitReport instance;
	
	// Database info
	private String user = "";
	private String pass = "";
	private String url = "";
	private String reports = "";
	
	public Report(BitReport instance) {
		this.instance = instance;
		user = instance.config.readString("DB_User");
		pass = instance.config.readString("DB_Pass");
		url = "jdbc:mysql://" + instance.config.readString("DB_Host") + 
				"/" + instance.config.readString("DB_Name");
		reports = instance.config.readString("DB_Table_reports");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
		boolean r = false;
		
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (instance.pex.has(player, "bitreport.report")) {
				if (split.length > 0) {
					String data = "";
					for (int i = 0; i < split.length; i++)
						data += " " + split[i];
					data = data.trim();

					try {
						// Store player location data
						String location = player.getLocation().getX() + 
								"," + player.getLocation().getY() + 
								"," + player.getLocation().getZ();
						String world = player.getWorld().getName();
						
						// Create query and prepared statement
						Connection conn = DriverManager.getConnection(url, user, pass);
						String query = "INSERT INTO `" + reports
								+ "` (`username`, `status`, `data`, `world`, `location`) VALUES (?,?,?,?,?)";
						PreparedStatement insert = conn.prepareStatement(query);

						// Insert the data into the prepared statement
						insert.setString(1, player.getName());
						insert.setInt(2, 0);
						insert.setString(3, data);
						insert.setString(4, world);
						insert.setString(5, location);
						
						// Execute the query
						insert.executeUpdate();
						
						// Get id to send to staff
						Statement select = conn.createStatement();
						ResultSet result = select
								.executeQuery("SELECT * FROM `" + reports
										+ "` WHERE `data` = '" + data
										+ "'AND `username` = '"
										+ player.getName() + "'");
						int id = 0;
						while (result.next()) {
							id = result.getInt(1);
						}

						// Clean up
						insert.close();
						conn.close();

						// Report back to player
						player.sendMessage(ChatColor.GREEN
								+ "Your ticket has been submitted and should be handled soon.");
						
						for (Player p : instance.getServer().getOnlinePlayers()) {
							if (p.hasPermission("bitreport.claim"))
								p.sendMessage(ChatColor.RED + "A new ticket ("
										+ ChatColor.YELLOW + "#" + id
										+ ChatColor.RED
										+ ") has been opened by "
										+ player.getName());
						}

						r = true;
					} catch (SQLException se) {
						se.printStackTrace();
					}
				}
			}
			else {
				player.sendMessage(ChatColor.YELLOW + 
						"You do not have access to this feature");
				r = true;
			}
		}
		if (sender instanceof ConsoleCommandSender) {
			instance.logInfo("This is an in-game only command.");
			r = true;
		}
		
		return r;
	}

}
