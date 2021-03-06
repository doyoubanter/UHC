package net.mcshockwave.UHC.Listeners;

import net.mcshockwave.UHC.NumberedTeamSystem.NumberTeam;
import net.mcshockwave.UHC.Option;
import net.mcshockwave.UHC.UltraHC;
import net.mcshockwave.UHC.Utils.ItemMetaUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MoleListener implements Listener, CommandExecutor {

	public static final String					molePre	= "�a[Mole] �7";

	public static HashMap<Integer, String>		moles	= new HashMap<>();
	public static HashMap<String, Inventory>	moleKit	= new HashMap<>();
	public static Inventory						ender	= null;

	public static boolean isMole(String s) {
		return moles.values().contains(s);
	}

	public static String getMoleName(Team t) {
		return moles.get(t);
	}

	public static Player getMole(Team t) {
		return Bukkit.getPlayer(getMoleName(t));
	}

	public static List<Player> getAllTrueMoles() {
		ArrayList<Player> ret = new ArrayList<>();

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (isMole(p.getName())) {
				ret.add(p);
			}
		}

		return ret;
	}

	public static List<Player> getAllMoles() {
		ArrayList<Player> ret = new ArrayList<>();

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (isMole(p.getName()) || (UltraHC.specs.contains(p.getName()) || !UltraHC.started) && p.isOp()) {
				ret.add(p);
			}
		}

		return ret;
	}

	public static void sendToMoles(String mes) {
		for (Player p : getAllMoles()) {
			p.sendMessage(mes);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(isMole(sender.getName()) || (UltraHC.specs.contains(sender.getName()) || !UltraHC.started)
				&& sender.isOp())) {
			sender.sendMessage("�cYou are not a Mole!");
			return false;
		}
		if (!(sender instanceof Player)) {
			return false;
		}
		Player p = (Player) sender;
		if (args.length == 0) {
			p.sendMessage("�8----- �a[Mole Commands] �8-----");
			p.sendMessage("�b/mc [Message] �a- Talk in mole chat");
			p.sendMessage("�b/mole location �a- Broadcast location to moles");
			p.sendMessage("�b/mole kit �a- Open inventory of all mole items");
			p.sendMessage("�b/mole ender �a- Open global chest all moles can access");
			p.sendMessage("�b/mole list �a- List all moles");
		}

		if (args.length == 1) {
			String cm = args[0];
			if (cm.equalsIgnoreCase("location")) {
				Location l = p.getLocation();
				String name = p.getName();
				name += name.endsWith("s") ? "'" : "'s";
				sendToMoles(molePre + name + " location is: x" + l.getBlockX() + " y" + l.getBlockY() + " z"
						+ l.getBlockZ());
			}

			if (cm.equalsIgnoreCase("kit")) {
				if (moleKit.containsKey(p.getName())) {
					Inventory i = moleKit.get(p.getName());

					p.openInventory(i);
				} else if (p.isOp() && !UltraHC.started) {
					p.openInventory(getInv());
				}
			}

			if (cm.equalsIgnoreCase("ender")) {
				p.openInventory(ender);
			}

			if (cm.equalsIgnoreCase("list")) {
				p.sendMessage("�8----- �a[Moles] �8-----");
				for (Player p2 : getAllTrueMoles()) {
					NumberTeam t = UltraHC.nts.getTeam(p2.getName());
					p.sendMessage("�b" + p2.getName() + " - "
							+ (t == null || UltraHC.specs.contains(p2.getName()) ? "�c[�lDEAD�c]" : "�e[" + t.id + "]"));
				}
			}
		}
		return true;
	}

	public static ItemStack[]	items	= { new ItemStack(Material.TNT, 4), new ItemStack(Material.STONE_PLATE, 2),
			new ItemStack(Material.POTION, 3, (short) 16460), new ItemStack(Material.MONSTER_EGG, 3, (short) 54),
			new ItemStack(Material.MONSTER_EGG, 3, (short) 52), new ItemStack(Material.MONSTER_EGG, 3, (short) 51),
			new ItemStack(Material.MONSTER_EGG, 3, (short) 50), new ItemStack(Material.GRAVEL, 8),
			new ItemStack(Material.STONE, 16) };

	public static Inventory getInv() {
		Inventory i = Bukkit.createInventory(null, ((items.length + 8) / 9) * 9, "Mole Kit - Use as storage too!");

		for (ItemStack it : items) {
			i.addItem(ItemMetaUtils.setLore(it, "�6Mole Item"));
		}

		return i;
	}

	public static void setAsMole(OfflinePlayer mole) {
		moleKit.remove(mole.getName());
		moleKit.put(mole.getName(), getInv());

		moles.put(UltraHC.nts.getTeam(mole.getName()).id, mole.getName());

		if (mole.isOnline()) {
			mole.getPlayer().sendMessage(molePre + "�lYou are a mole! Type /mole for a list of commands.");
		}
	}

	public static void onStart() {
		int erows = 4;
		ender = Bukkit.createInventory(null, erows * 9, "Mole Chest");

		Option.Friendly_Fire.set(true);
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity ee = event.getEntity();
		Entity de = event.getDamager();

		if (ee instanceof Player && de instanceof Player) {
			Player p = (Player) ee;
			Player d = (Player) de;

			if (isMole(p.getName()) && isMole(d.getName())) {
				event.setDamage(event.getDamage() / 3);
				d.sendMessage(molePre + "�c�lYou are killing a fellow mole!");
				d.playSound(d.getLocation(), Sound.ANVIL_LAND, 3, 0);
			}
		}
	}

}
