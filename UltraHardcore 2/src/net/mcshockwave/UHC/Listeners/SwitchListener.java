package net.mcshockwave.UHC.Listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class SwitchListener implements Listener {

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity ee = event.getEntity();
		Entity de = event.getDamager();

		if (de instanceof Projectile && ee instanceof Player) {
			ProjectileSource le = ((Projectile) de).getShooter();

			if (le instanceof Player) {
				Location l1 = ee.getLocation();
				Location l2 = de.getLocation();

				ee.teleport(l2);
				de.teleport(l1);
			}
		}
	}
}