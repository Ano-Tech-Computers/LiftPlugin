package no.atc.floyd.bukkit.lift;


//import java.io.*;

//import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.World;
//import org.bukkit.Server;
//import org.bukkit.event.Event.Priority;
//import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;

//import com.nijikokun.bukkit.Permissions.Permissions;

/**
* LiftPlugin plugin for Bukkit
*
* @author FloydATC
*/
public class LiftPlugin extends JavaPlugin implements Listener {
    //public static Permissions Permissions = null;
    
	public static final Logger logger = Logger.getLogger("Minecraft.LiftPlugin");
	
    
//    public LiftPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents((Listener) this, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	
    }


    @EventHandler
    public void onInteract( PlayerInteractEvent event ) {
        if (event.hasBlock()) {
            //Player player = event.getPlayer();
            //String pname = player.getName();
	    	Block block = event.getClickedBlock();
	    	World world = block.getWorld();

	    	// Is this a wall sign or a button?
	    	if (block.getTypeId() == 68 || block.getTypeId() == 77) {
	    		
	    		// This may be an elevator. Scan vertically for floors.
	    		ArrayList<Integer> floors = new ArrayList<Integer>();
	    		Integer x = block.getX();
	    		Integer z = block.getZ();
	    	    for (Integer y = 0; y <= block.getY(); y++) {
	    	    	if (safeFloorAt(world, x, y, z)) {
						floors.add(y);
	    	    	}
	    	    }
	    		Integer floor = floors.size();
	    	    for (Integer y = block.getY(); y <= world.getMaxHeight(); y++) {
	    	    	if (safeFloorAt(world, x, y, z)) {
						floors.add(y);
	    	    	}
	    	    }
	    	    if (floors.size() > 1) {
					//logger.info("[Lift] " + pname + " is at floor " + floor + " of " + floors.size() + " floors in what appears to be a lift");
		    		
			    	// Is it a wall sign?
			    	if (block.getState() instanceof Sign) {
			    		Sign sign = (Sign) block.getState();
						//logger.info("[Lift] " + pname + " interacted with a sign");
						if (sign.getLine(0).equals("Floor:") || (sign.getLine(0).equals("") && sign.getLine(1).equals("") && sign.getLine(2).equals("") && sign.getLine(3).equals(""))) {
							//logger.info("[Lift] " + pname + " interacted with a lift sign (action="+event.getAction().toString()+")");
							
							// Get the currently selected floor number
							Integer destination = 0;
							try {
								destination = Integer.valueOf(sign.getLine(3));
							}
							catch (Exception e) {
								destination = 0;
							}
							//logger.info("[Lift] " + pname + " destination was " + destination);

							// Increment or decrement destination
							if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
								destination--;
								while (destination < 1 || destination == floor) {
									if (destination < 1) { destination = floors.size(); }
									if (destination == floor) { destination--; }
								}
							}
							if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
								destination++;
								while (destination > floors.size() || destination == floor) {
									if (destination > floors.size()) { destination = 1; }
									if (destination == floor) { destination++; }
								}
							}

							// Update sign
							sign.setLine(0, "Floor:");
							sign.setLine(1, floor + " of " + floors.size());
							sign.setLine(2, "Destination:");
							sign.setLine(3, destination.toString());
							sign.update();
							return;
						}
						
			    	}
			    	// Is it a button?
			    	if (block.getTypeId() == 77) {
						//logger.info("[Lift] " + pname + " interacted with a button");
						// Look for an adjacent lift sign
						Sign sign = adjacentLiftSign(world, block.getX(), block.getY(), block.getZ());
						if (sign != null) {
							// Get the currently selected floor number
							Integer destination = 0;
							try {
								destination = Integer.valueOf(sign.getLine(3));
							}
							catch (Exception e) {
								destination = 0;
							}
							//logger.info("[Lift] " + pname + " destination is " + destination);
							
							// Build an array of all players on this floor
							ArrayList<Player> passengers = new ArrayList<Player>();
							List<Player> players = world.getPlayers();
							Integer[] offset = { 0, -1, +1, -2, +2, -3, +3, -4, +4 };
							Integer floory = floors.get(floor-1);
							boolean maxx = false;
							boolean minx = false;
							for (Integer offsetx : offset) {
								//logger.info("[Lift] Axis X = " + offsetx);
								if (offsetx == 0 || (offsetx > 0 && maxx == false) || (offsetx < 0 && minx == false)) {
									boolean maxz = false;
									boolean minz = false;
									for (Integer offsetz : offset) {
										//logger.info("[Lift] Axis Z = " + offsetz);
										if (offsetz == 0 || (offsetz > 0 && maxz == false) || (offsetz < 0 && minz == false)) {
											if (safeFloorAt(world, x + offsetx, floory, z + offsetz)) {
												for (Player candidate : players) {
													Location loc = candidate.getLocation();
													if (loc.getBlockX() == x + offsetx && loc.getBlockY() == floory+1 && loc.getBlockZ() == z + offsetz) {
														passengers.add(candidate);
														//logger.info("[Lift] " + candidate.getName() + " is in the lift activated by " + pname);
													}
												}
											} else {
												if (offsetz == 0 && offsetx > 0) { maxx = true; }
												if (offsetz == 0 && offsetx < 0) { minx = true; }
												if (offsetz > 0) { maxz = true; }
												if (offsetz < 0) { minz = true; }
											}
										} else {
											//logger.info("[Lift] Ignored");
										}
									}
								} else {
									//logger.info("[Lift] Ignored");
								}
							}
							
							// Get y coordinate of the selected floor and send the players there
							if (destination > 0 && floors.size() >= destination) {
								for (Player p : passengers) {
									Location loc = p.getLocation();
									loc.setY(floors.get(destination-1) + 1);
									p.teleport(loc);
									p.playNote(loc, Instrument.PIANO, Note.natural(1, Tone.C));
								}
								return;
							}
						}
						
			    	}
	    	    }
	    	}
        }
    	return;
    }    
   
    private boolean safeFloorAt(World world, Integer x, Integer y, Integer z) {
    	Integer type = null;
    	type = world.getBlockAt(x, y, z).getTypeId();
    	if (type != 42) { 
    		return false; 
    	}
    	type = world.getBlockAt(x, y+1, z).getTypeId();
    	if (type != 0 && type != 68 && type != 77 && type != 50) { 
    		return false;
    	}
    	type = world.getBlockAt(x, y+2, z).getTypeId();
    	if (type != 0 && type != 68 && type != 77 && type != 50) { 
    		return false;
    	}
    	return true;
    }

    private Sign adjacentLiftSign(World world, Integer x, Integer y, Integer z) {
    	Sign sign = null;
    	Integer[][] neighbour = {
    			{  0,  0, -1 },
    			{  0,  0, +1 },
    			{  0, -1,  0 },
    			{  0, +1,  0 },
    			{ -1,  0,  0 },
    			{ +1,  0,  0 },
    	};
    	for (Integer i = 0; i < 6; i++) {
    		Integer nx = x + neighbour[i][0];
    		Integer ny = y + neighbour[i][1];
    		Integer nz = z + neighbour[i][2];
    		Block block = world.getBlockAt(nx, ny, nz);
    		if (block.getState() instanceof Sign) {
    			Sign candidate = (Sign) block.getState();
    			if (candidate.getLine(0).equals("Floor:")) {
    				sign = candidate;
    				break;
    			}
    		}
    	}
    	
   		return sign;
    }
    
}

