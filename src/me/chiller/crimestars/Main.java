package me.chiller.crimestars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import me.chiller.crimestars.util.ValueComparator;

public class Main extends JavaPlugin implements Listener
{
	private String scoreboardTitle = "&c&lWanted Levels";//ChatColor.RED + "" + ChatColor.BOLD + "Wanted Levels"
	private String scoreboardName = "&6{stars} &b{player}";
	
	private String tabName = "&6{stars} &5{player}";
	private String jailCommand = "/jail {player} jailOne";
	
	private List<Integer> starKills = new ArrayList<Integer>();
	
	private Map<UUID, Integer> playerKills;
	private Map<UUID, Integer> playerKillsSorted;
	
	private Scoreboard blankScoreboard;
	
	public void onEnable()
	{
		//getCommand("crimestars").setExecutor(this);
		getServer().getPluginManager().registerEvents(this, this);
		
		saveDefaultConfig();
		
		blankScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		
		playerKills = new HashMap<UUID, Integer>();
		playerKillsSorted = new TreeMap<UUID, Integer>(new ValueComparator<UUID>(playerKills));
		
		scoreboardTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("scoreboard-title", scoreboardTitle));
		scoreboardName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("scoreboard-name", scoreboardName));
		
		tabName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("tab-list-name", tabName));
		jailCommand = getConfig().getString("jail-command", jailCommand).replace("/", "");
		
		starKills = getConfig().getIntegerList("stars-per-kills");
		
		Set<String> killerUUIDs = getConfig().getConfigurationSection("kills").getKeys(false);
		
		for (String uuid : killerUUIDs)
		{
			playerKills.put(UUID.fromString(uuid), getConfig().getInt("kills." + uuid, 0));
		}
		
		playerKillsSorted.putAll(playerKills);
		
		setPlayersScoreboards();
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		player.setScoreboard(getStarsScoreboard());
		
		if (playerKills.containsKey(player.getUniqueId()))
		{
			setDisplayName(player);
		}
	}
	
	@EventHandler
	public void onPlayerKill(PlayerDeathEvent event)
	{
		LivingEntity killerEntity = event.getEntity().getKiller();
		LivingEntity entity = event.getEntity();
		
		if (killerEntity instanceof Player && entity instanceof Player)
		{
			Player killer = (Player) killerEntity;
			Player player = (Player) entity;
			
			if (killer.hasPermission("cs.jail"))
			{
				if (getKills(player.getUniqueId()) > 0)
				{
					playerKills.remove(player.getUniqueId());
					player.setPlayerListName(player.getName());
					
					setPlayersScoreboards();
					
					Bukkit.dispatchCommand(getServer().getConsoleSender(), jailCommand.replace("{player}", player.getName()));
				}
			} else
			{
				addKill(killer);
				setDisplayName(killer);
				
				setPlayersScoreboards();
			}
		}
	}
	
	private void setDisplayName(Player player)
	{
		player.setPlayerListName(tabName.replace("{stars}", getStarsString(getStars(getKills(player.getUniqueId())))).replace("{player}", player.getPlayerListName()));
	}
	
	private void addKill(Player player)
	{
		UUID uuid = player.getUniqueId();
		
		int kills = 0;
		
		if (playerKills.containsKey(uuid)) kills = playerKills.get(uuid);
		
		playerKills.put(uuid, kills + 1);
		playerKillsSorted.putAll(playerKills);
		
		for (UUID playerUUID : playerKills.keySet())
		{
			getConfig().set("kills." + playerUUID.toString(), playerKills.get(playerUUID));
		}
		
		saveConfig();
	}
	
	private void setPlayersScoreboards()
	{
		for (Player p : Bukkit.getOnlinePlayers())
		{
			p.setScoreboard(getStarsScoreboard());
		}
	}
	
	private Scoreboard getStarsScoreboard()
	{
		if (playerKills.size() == 0) return blankScoreboard;
		
		Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective obj = sb.registerNewObjective("stars", "dummy");
		
		obj.setDisplayName(scoreboardTitle);
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		for (UUID uuid : playerKills.keySet())
		{
			int stars = getStars(getKills(uuid));
			
			obj.getScore(getWantedLevels(Bukkit.getOfflinePlayer(uuid).getName(), stars)).setScore(stars);
		}
		
		return sb;
	}
	
	private String getWantedLevels(String player, int stars)
	{
		return scoreboardName.replace("{stars}", getStarsString(stars)).replace("{player}", player);
	}
	
	private String getStarsString(int stars)
	{
		return StringUtils.repeat(Character.toString('\u2736'), stars);
	}
	
	private int getKills(UUID uuid)
	{
		if (playerKills.containsKey(uuid))
		{
			return playerKills.get(uuid);
		}
		
		return 0;
	}
	
	private int getStars(int playerKills)
	{
		int stars = 0;
		
		for (int sk = 0; sk < starKills.size(); sk++)
		{
			int kills = starKills.get(sk);
			
			if (playerKills >= kills)
			{
				stars++;
			}
		}
		
		return stars;
	}
}