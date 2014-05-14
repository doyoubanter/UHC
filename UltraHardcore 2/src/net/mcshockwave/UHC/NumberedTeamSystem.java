package net.mcshockwave.UHC;

import net.mcshockwave.UHC.Menu.ItemMenu;
import net.mcshockwave.UHC.Menu.ItemMenu.Button;
import net.mcshockwave.UHC.Menu.ItemMenu.ButtonRunnable;
import net.mcshockwave.UHC.Utils.ItemMetaUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class NumberedTeamSystem {

	public static HashMap<Player, NumberTeam>	enteringPassword	= new HashMap<>();

	public boolean								friendlyfire		= false;

	public Scoreboard							s;

	public ArrayList<NumberTeam>				teams				= new ArrayList<>();

	public BukkitTask							updater				= null;

	// usable colors: 14, usable formats: 3, total # of colored teams: 42!
	public static String						usableColors		= "123456789abcde";
	public static String						usableFormats		= "xon";

	public NumberedTeamSystem(Scoreboard s) {
		this.s = s;

		updater = Bukkit.getScheduler().runTaskTimer(UltraHC.ins, new Runnable() {
			public void run() {
				updateScoreboard();
			}
		}, 0l, 1l);

		for (final Team t : s.getTeams()) {
			if (t.getName().startsWith("T") && t.getPlayers().size() > 0) {
				int id = Integer.parseInt(t.getName().replaceFirst("T", ""));
				String owner = t.getPlayers().toArray(new OfflinePlayer[0])[0].getName();

				final NumberTeam nt = new NumberTeam(id, null, owner);
				for (final OfflinePlayer op : t.getPlayers()) {
					Bukkit.getScheduler().runTaskLater(UltraHC.ins, new Runnable() {
						public void run() {
							nt.addPlayer(op.getName());
						}
					}, 5l);
				}
				teams.add(nt);
			}
		}

		Bukkit.getScheduler().runTaskLater(UltraHC.ins, new Runnable() {
			public void run() {
				updateScoreboard();
			}
		}, 10l);
	}

	public void updateScoreboard() {
		for (NumberTeam nt : teams) {
			for (Objective o : s.getObjectives()) {
				Objective nto = nt.sc.getObjective(o.getName());
				if (nto == null) {
					nto = nt.sc.registerNewObjective(o.getName(), o.getCriteria());
				}
				cloneObjective(nto, o);
			}

			cloneScores(nt.sc, s);

			for (NumberTeam nt2 : teams) {
				Team ntt = nt.sc.getTeam("T" + nt2.id);
				if (ntt == null) {
					ntt = nt.sc.registerNewTeam("T" + nt2.id);
					ntt.setCanSeeFriendlyInvisibles(true);
					ntt.setDisplayName("Team " + nt2.id);
					ntt.setPrefix(getPrefix(nt2.id, false, nt2 == nt));
					if (nt2 == nt) {
						nt.t = ntt;
					}
				}
				ntt.setAllowFriendlyFire(friendlyfire);
			}

			if (s.getTeam("T" + nt.id) == null) {
				Team t = s.registerNewTeam("T" + nt.id);
				t.setAllowFriendlyFire(true);
				t.setCanSeeFriendlyInvisibles(false);
				t.setPrefix(getPrefix(nt.id, true, false));
				t.setSuffix("�r");
			}

			updatePlayersForTeam(nt);
		}
	}

	public static String getPrefix(int id, boolean noteam, boolean sameteam) {
		if (UltraHC.nts.teams.size() > (usableColors.length() * usableFormats.length())) {
			String color = "�e";
			if (!noteam) {
				color = sameteam ? "�a" : "�c";
			}

			return color + id + "|�f";
		} else {
			return getColorFromId(id);
		}
	}

	public static String getColorFromId(int id) {
		id--;
		int formatid = id / usableColors.length();
		int colorid = id % usableColors.length();

		return "�" + usableColors.charAt(colorid) + (formatid == 0 ? "" : "�" + usableFormats.charAt(formatid));
	}

	private void cloneObjective(Objective n, Objective cl) {
		if (!n.getDisplayName().equals(cl.getDisplayName()))
			n.setDisplayName(cl.getDisplayName());
		if (n.getDisplaySlot() != cl.getDisplaySlot())
			n.setDisplaySlot(cl.getDisplaySlot());
	}

	private void updatePlayersForAllTeams() {
		for (NumberTeam nt : teams) {
			updatePlayersForTeam(nt);
		}
	}

	@SuppressWarnings("deprecation")
	private void updatePlayersForTeam(NumberTeam tou) {
		for (NumberTeam nt : teams) {
			Team t = tou.sc.getTeam("T" + nt.id);

			for (String s : nt.players) {
				OfflinePlayer op = Bukkit.getOfflinePlayer(s);
				if (!t.hasPlayer(op)) {
					t.addPlayer(op);
					if (op.isOnline() && !ChatColor.stripColor(op.getPlayer().getPlayerListName()).equals(op.getName())) {
						t.addPlayer(Bukkit.getOfflinePlayer(ChatColor.stripColor(op.getPlayer().getPlayerListName())));
					}
				}
			}
			for (String s : tou.players) {
				OfflinePlayer op = Bukkit.getOfflinePlayer(s);
				if (!nt.players.contains(s) && t.hasPlayer(op)) {
					t.removePlayer(op);
				}
			}
		}

		Team st = s.getTeam("T" + tou.id);
		for (OfflinePlayer op : st.getPlayers()) {
			if (!tou.getPlayers().contains(op.getName())) {
				st.removePlayer(op);
			}
		}
		for (String s : tou.players) {
			OfflinePlayer op = Bukkit.getOfflinePlayer(s);
			if (!st.hasPlayer(op)) {
				st.addPlayer(op);
				if (op.isOnline() && !ChatColor.stripColor(op.getPlayer().getPlayerListName()).equals(op.getName())) {
					st.addPlayer(Bukkit.getOfflinePlayer(ChatColor.stripColor(op.getPlayer().getPlayerListName())));
				}
			}
		}
	}

	private void cloneScores(Scoreboard to, Scoreboard from) {
		for (OfflinePlayer op : from.getPlayers()) {
			for (Score f : from.getScores(op)) {
				if (to.getObjective(f.getObjective().getName()) != null) {
					Objective t = to.getObjective(f.getObjective().getName());

					if (t.getScore(op).getScore() != f.getScore()) {
						t.getScore(op).setScore(f.getScore());
					}
				}
			}
		}
	}

	public int getHighestId() {
		int ret = 0;
		for (NumberTeam nt : teams) {
			if (nt.id > ret) {
				ret = nt.id;
			}
		}
		return ret;
	}

	public ItemMenu getMenu(Player p, boolean edit) {
		ItemMenu m = new ItemMenu("Teams" + (edit ? " - Editing" : ""), getHighestId());

		int teamlimit = Option.Team_Limit.getInt();

		for (final NumberTeam nt : teams) {
			int data = getTeam(p.getName()) == nt ? 4 : nt.players.size() >= teamlimit ? 14 : nt.password == null ? 5
					: 13;

			Button b = new Button(true, Material.WOOL, nt.id, data, "Team #" + nt.id,
					nt.password == null ? "�aClick to join" : "�cPassword Protected", "�eOwner: " + nt.owner,
					"�aOnline Players: " + nt.getOnlinePlayers().size(), "", "�bPlayers: (max "
							+ Option.Team_Limit.getInt() + ")");
			ArrayList<String> lore = (ArrayList<String>) ItemMetaUtils.getLoreList(b.button);
			for (String s : nt.players) {
				String pre = (Bukkit.getPlayer(s) == null ? "�c" : "�a") + (nt.owner.equals(s) ? "�o" : "");
				lore.add(pre + s);
			}
			ItemMetaUtils.setLore(b.button, lore.toArray(new String[0]));
			m.addButton(b, nt.id - 1);
			if (edit) {
				m.addSubMenu(nt.getSubMenu(), b, true);
			} else {
				b.setOnClick(new ButtonRunnable() {
					public void run(final Player p, InventoryClickEvent event) {
						if (UltraHC.started || !Option.Team_Commands.getBoolean()) {
							p.sendMessage("�cThat is not enabled right now!");
							return;
						}

						if (nt.players.contains(p.getName())) {
							return;
						}

						if (nt.players.size() + 1 > Option.Team_Limit.getInt()) {
							p.sendMessage("�cTeam is full!");
							return;
						}

						if (nt.password == null) {
							nt.addPlayer(p.getName());
						} else {
							enteringPassword.put(p, nt);
							p.sendMessage("�6Please enter the password for team " + nt.id);
							Bukkit.getScheduler().runTaskLater(UltraHC.ins, new Runnable() {
								public void run() {
									if (enteringPassword.containsKey(p) && enteringPassword.get(p) == nt) {
										p.sendMessage("�6You did not enter the password for team " + nt.id);
										enteringPassword.remove(p);
									}
								}
							}, 600l);
						}
					}
				});
			}
		}

		return m;
	}

	public NumberTeam getTeam(String s) {
		for (NumberTeam nt : teams) {
			if (nt.players.contains(s)) {
				return nt;
			}
		}
		return null;
	}

	public boolean isOnTeam(String name, int id) {
		return getTeam(name) == getFromId(id);
	}

	public NumberTeam createTeam(int id, String pass, String owner) {
		NumberTeam nt = createTeam(pass, owner);
		nt.id = id;
		return nt;
	}

	public NumberTeam createTeam(String pass, String owner) {
		NumberTeam nt = new NumberTeam(pass, owner);
		teams.add(nt);
		return nt;
	}

	public void removeTeam(NumberTeam nt) {
		for (Player p : nt.getOnlinePlayers()) {
			p.setScoreboard(s);
		}
		updatePlayersForTeam(nt);
		teams.remove(nt);
		nt.players.clear();
		s.getTeam("T" + nt.id).unregister();
	}

	public NumberTeam getFromId(int id) {
		for (NumberTeam nt : teams) {
			if (nt.id == id) {
				return nt;
			}
		}
		return null;
	}

	public class NumberTeam {
		public int			id;
		public String		password, owner;
		ArrayList<String>	players;
		public Scoreboard	sc;
		public Team			t;

		public NumberTeam(int id, String pass, String owner) {
			this.id = id;
			init(pass, owner);
		}

		NumberTeam(String pass, String owner) {
			id = getValidId();
			init(pass, owner);
		}

		public void init(String pass, String owner) {
			password = pass;
			this.owner = owner;
			players = new ArrayList<>();

			sc = Bukkit.getScoreboardManager().getNewScoreboard();
		}

		public void addPlayer(String name) {
			for (NumberTeam nt : teams.toArray(new NumberTeam[0])) {
				if (nt.getPlayers().contains(name)) {
					nt.removePlayer(name);
				}
			}

			players.add(name);

			if (Bukkit.getPlayer(name) != null) {
				Bukkit.getPlayer(name).setScoreboard(sc);
			}

			messageAll("�6" + name + " joined your team");

			if (owner == null) {
				owner = name;
			}
		}

		public void messageAll(String msg) {
			for (Player p : getOnlinePlayers()) {
				p.sendMessage(msg);
			}
		}

		public void removePlayer(String name) {
			players.remove(name);

			if (Bukkit.getPlayer(name) != null) {
				Player p = Bukkit.getPlayer(name);
				p.setScoreboard(s);
				t.removePlayer(p);
			}

			updatePlayersForAllTeams();

			messageAll("�6" + name + " left your team");

			if (players.size() < 1) {
				removeTeam(this);
				return;
			}

			if (name.equalsIgnoreCase(owner)) {
				String newOwner = players.get(0);
				messageAll("�6The owner of the team left, the new owner is " + newOwner);
				owner = newOwner;
			}
		}

		public ArrayList<Player> getOnlinePlayers() {
			ArrayList<Player> ret = new ArrayList<>();

			for (String s : players) {
				if (Bukkit.getPlayer(s) != null) {
					ret.add(Bukkit.getPlayer(s));
				}
			}

			return ret;
		}

		public ItemMenu getSubMenu() {
			ItemMenu m = new ItemMenu("Team " + id + " - Editing", 9);

			Button delete = new Button(false, Material.WOOL, 1, 14, "Delete Team", "", "�cWARNING: CAN NOT BE UNDONE");
			m.addButton(delete, 0);
			final NumberTeam team = this;
			delete.setOnClick(new ButtonRunnable() {
				public void run(final Player p, InventoryClickEvent event) {
					removeTeam(team);
					p.sendMessage("�cDeleted team #" + team.id);

					p.closeInventory();
					Bukkit.getScheduler().runTaskLater(UltraHC.ins, new Runnable() {
						public void run() {
							getMenu(p, true).open(p);
						}
					}, 1l);
				}
			});

			Button pass = new Button(false, Material.TRIPWIRE_HOOK, 1, 0, "Password", "Click to see password", "",
					"Pass: �kPASSWORD");
			m.addButton(pass, 7);
			pass.setOnClick(new ButtonRunnable() {
				public void run(Player p, InventoryClickEvent event) {
					event.setCurrentItem(ItemMetaUtils.setLore(event.getCurrentItem(), "Click to see password", "",
							"Pass: " + password));
				}
			});

			return m;
		}

		public ArrayList<String> getPlayers() {
			return players;
		}

		public String[] getPlayersArray() {
			return players.toArray(new String[0]);
		}

		public void setOwner(String name) {
			String old = owner;
			owner = name;
			players.set(0, owner);
			players.add(old);
		}

		public boolean isFull() {
			return players.size() + 1 > Option.Team_Limit.getInt();
		}
	}

	public int getValidId() {
		int check = 0;
		while (check <= 64) {
			check++;

			boolean found = true;
			for (NumberTeam nt : teams) {
				if (nt.id == check) {
					found = false;
				}
			}

			if (found) {
				break;
			}
		}

		return check;
	}

	public boolean isTeamGame() {
		return teams.size() > 0;
	}

	Random	rand	= new Random();

	public void randomize(int number, boolean isTeamSize, boolean remaining) {
		double num = number;

		if (remaining) {
			ArrayList<Player> noteam = new ArrayList<>();
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (getTeam(p.getName()) == null) {
					noteam.add(p);
				}
			}

			while (noteam.size() > 0) {
				boolean allfull = true;

				for (NumberTeam nt : teams) {
					if (!nt.isFull()) {
						Player p = noteam.get(rand.nextInt(noteam.size()));

						nt.addPlayer(p.getName());

						noteam.remove(p);

						allfull = false;
					}
				}

				if (allfull) {
					break;
				}
			}
		} else {
			double tcount = 0;
			double numPlayers = Bukkit.getOnlinePlayers().length;

			if (isTeamSize) {
				tcount = (int) Math.ceil(numPlayers / num);
				Option.Team_Limit.setInt((int) num);
			} else {
				tcount = num;
				Option.Team_Limit.setInt((int) Math.ceil(numPlayers / num));
			}
			Option.Max_Teams.setInt((int) tcount);

			// clear teams
			for (NumberTeam nt : teams.toArray(new NumberTeam[0])) {
				removeTeam(nt);
			}

			// create teams
			for (int i = 1; i <= tcount; i++) {
				createTeam(i, null, "None");
			}

			// put players on teams
			ArrayList<Player> undef = new ArrayList<>();
			for (Player p : Bukkit.getOnlinePlayers()) {
				undef.add(p);
			}
			int times = 0;

			int tid = 1;
			while (undef.size() > 0) {
				Player p = undef.get(rand.nextInt(undef.size()));

				NumberTeam addTo = getFromId(tid);
				if (addTo != null) {
					addTo.addPlayer(p.getName());
					undef.remove(p);
				} else {
					times++;
					if (times > 100) {
						Bukkit.broadcastMessage("�4Error: �cTeam with id " + tid + " does not exist");
						break;
					}
				}

				tid++;
				if (tid > teams.size()) {
					tid = 1;
				}
			}
		}
	}

	public ItemMenu getRandomMenu() {
		ItemMenu m = new ItemMenu("Randomize", 9);

		Button rmt = new Button(true, Material.DIAMOND, 1, 0, "Randomize:", "Spread players based",
				"on max number of teams", "", "Current max teams: " + Option.Max_Teams.getInt());
		m.addButton(rmt, 2);
		rmt.setOnClick(new ButtonRunnable() {
			public void run(Player p, InventoryClickEvent event) {
				randomize(Option.Max_Teams.getInt(), false, false);
			}
		});

		Button rtl = new Button(true, Material.EMERALD, 1, 0, "Randomize:", "Spread players based", "on team limit",
				"", "Current team limit: " + Option.Team_Limit.getInt());
		m.addButton(rtl, 4);
		rtl.setOnClick(new ButtonRunnable() {
			public void run(Player p, InventoryClickEvent event) {
				randomize(Option.Team_Limit.getInt(), true, false);
			}
		});

		Button rrm = new Button(true, Material.GOLD_INGOT, 1, 0, "Randomize:", "Spread remaining players",
				"onto created teams", "", "Created teams: " + teams.size());
		m.addButton(rrm, 6);
		rrm.setOnClick(new ButtonRunnable() {
			public void run(Player p, InventoryClickEvent event) {
				randomize(0, false, true);
			}
		});

		return m;
	}

}
