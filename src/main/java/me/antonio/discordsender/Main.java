package me.antonio.discordsender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;

import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import org.apache.logging.log4j.Logger;

@Mod(modid=Main.MODID,version=Main.VERSION,name="DisNotifier",acceptableRemoteVersions = "*")
public class Main {
    public static final String MODID = "DisNotifier";
    public static final String VERSION = Tags.VERSION;
    public static final Logger Logger = LogManager.getLogger(MODID);
	
    public static Configuration config;
	
    public static Boolean sendMessageOnBooting;
    public static Boolean sendMessageOnBoot;
    public static Boolean sendMessageOnShuttingDown;
    public static Boolean sendMessageOnShutDown;
    public static Boolean sendMessageOnJoin;
    public static Boolean sendMessageOnLeave;
	
    public static String loadingMessage;
    public static String loadedMessage;
    public static String shuttingMessage;
    public static String shutMessage;
    public static String joinMessage;
    public static String leaveMessage;
	
    public static String webhook;
    public static String username;
    public static String avatarUrl;
    public static Boolean editSended;
    public static long messageID;
	
    public Boolean sendLoadingPingMessage;
    public String loadingPingMessage;
    public int loadingPingMessageWait;
    public Boolean sendLoadedPingMessage;
    public String loadedPingMessage;
    public int loadedPingMessageWait;
    public Boolean sendShuttingPingMessage;
    public String shuttingPingMessage;
    public int shuttingPingMessageWait;
    public Boolean sendShutPingMessage;
    public String shutPingMessage;
    public int shutPingMessageWait;
	
    public static double onlineTime = 0;
    public static double offlineTime = 0;
    public static String playerName = "";
    public static String messageL = "";
	
    public static long send(String username, String avatar, String method, String link, String message) throws IOException {
	String userAgent = "Mozilla/5.0";
	try {
	    HttpURLConnection con = (HttpURLConnection) (new URL(link + "?wait=true")).openConnection();
	    con.setRequestMethod(method);
	    con.setRequestProperty("User-Agent", userAgent);
	    con.setRequestProperty("Content-Type", "application/json");
	    if (method == "POST" || method == "PATCH") {
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		messageL = ("{"
				 + (username.trim().length() > 0 ? "\"username\":\"" + username + "\"," : "")
				 + (avatar.trim().length() > 0 ? "\"avatar_url\":\"" + avatar + "\"," : "")
				 + message.trim().substring(1));
		os.write(messageL.getBytes(StandardCharsets.UTF_8));
		os.flush();
		os.close();
	    }
	    int rc = con.getResponseCode();
	    StringBuffer out = new StringBuffer();
	    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    String line;
	    while ((line = br.readLine()) != null) {
		out.append(line);
	    }
	    br.close();
			
	    if ((199 < rc) && (rc < 300)) {
		return method != "DELETE" ? Long.parseLong(out.toString().split("\"id\":\"")[1].split("\"")[0]) : 0; // This is my best method on how to get the ID without parsing the entire JSON string.
	    } else {
		Logger.error("Coulnd't send the message! Code: " + rc + ", error:\n", out);
	    }
			
	} catch (MalformedURLException e) {
	    Logger.error("Malformed URL: " + link);
	}
	return 0;
    }
	
    public static String formatMessage(String m) {
	return m.replaceAll("\\[time\\]", ((long) (new Date()).getTime() / 1000) + "")
	    .replaceAll("\\[onlineTime\\]", ((long) onlineTime) + "")
	    .replaceAll("\\[offlineTime\\]", ((long) offlineTime) + "")
	    .replaceAll("\\[playerName\\]", playerName);
    }
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent e) throws IOException {
	if (e.getSide() == Side.CLIENT) {
	    Logger.error("This Mod is a SERVER SIDE mod only! Disabling...");
	    return;
	}
	allowMethods("PATCH", "DELETE");
	FMLCommonHandler.instance().bus().register(new FMLEvents());
	
	this.config = new Configuration(e.getSuggestedConfigurationFile());
	this.config.load();
		
	// Loading the configuration file
	this.sendMessageOnBooting = this.config.getBoolean("enabled", "1_onStarting", false, "Send a message to Discord when the server is loading.");
	this.sendMessageOnBoot = this.config.getBoolean("enabled", "2_onStart", true, "Send a message to Discord when the server is fully loaded.");
	this.sendMessageOnShuttingDown = this.config.getBoolean("enabled", "3_onStopping", false, "Send a message to Discord when the server is stopping.");
	this.sendMessageOnShutDown = this.config.getBoolean("enabled", "4_onStopped", true, "Send a message to Discord when the server is fully stopped.");
	this.sendMessageOnJoin = this.config.getBoolean("enabled", "5_onJoin", true, "Send a message to Discord when a player joins.");
	this.sendMessageOnLeave = this.config.getBoolean("enabled", "6_onLeave", true, "Send a message to Discord when a player leaves.");
		
	this.loadingMessage = this.config.getString("messageJSON",
						    "1_onStarting",
						    "{\"content\":null,\"embeds\":[{\"title\":\"Server is Loading\",\"description\":\"The Minecraft server is strarting!\",\"color\":16771840}],\"attachments\":[]}",
						    "The message to be sent (thanks to https://discohook.org/).");
	this.loadedMessage = this.config.getString("messageJSON",
						   "2_onStart",
						   "{\"content\":null,\"embeds\":[{\"title\":\"Server is Online\",\"description\":\"The Minecraft server is online!\\n<t:[time]:R>\",\"color\":4558592}],\"attachments\":[]}",
						   "The message to be sent.");
	this.shuttingMessage = this.config.getString("messageJSON",
						     "3_onStopping",
						     "{\"content\":null,\"embeds\":[{\"title\":\"Server is Shutting down!\",\"description\":\"The Minecraft server is shutting down!\\nWas online for: <t:[onlineTime]:>\\n\\n<t:[time]:R>\",\"color\":8940045}],\"attachments\":[]}",
						     "The message to be sent.");
	this.shutMessage = this.config.getString("messageJSON",
						 "4_onStopped",
						 "{\"content\":null,\"embeds\":[{\"title\":\"Server is Offline!\",\"description\":\"The Minecraft server is offline!\\n\\nLast time online: <t:[onlineTime]:d> <t:[onlineTime]:T>\",\"color\":16719390}],\"attachments\":[]}",
						 "The message to be sent.");
	this.joinMessage = this.config.getString("messageJSON",
						 "5_onJoin",
						 "{\"content\":null,\"embeds\":[{\"title\":\"[playerName] joined\",\"color\":4558592}],\"attachments\":[]}",
						 "The message to be sent.");
	this.leaveMessage = this.config.getString("messageJSON",
						 "6_onLeave",
						 "{\"content\":null,\"embeds\":[{\"title\":\"[playerName] left\",\"color\":16719390}],\"attachments\":[]}",
						 "The message to be sent.");
		
	this.webhook = this.config.getString("webhook", "0_general", "YOURWEBHOOKHERE", "The link with the token of the webhook (don't give other people the link you'll paste here).");
	this.editSended = this.config.getBoolean("editSended", "0_general", true, "If this is true then when a message is sended, the ID will be stored in this config file and will be used to edit the sended messages.");
	this.messageID = Long.parseLong(this.config.getString("messageID", "0_general", "0", "If this is bigger than 0, the webhook will replace the message with this ID (only if this message is sent by this webhook)"));
	this.username = this.config.getString("username", "0_general", " ", "This will be the name of your webhook.");
	this.avatarUrl = this.config.getString("avatarUrl", "0_general", " ", "This will be the avatar of your webhook.");
		
	this.sendLoadingPingMessage = this.config.getBoolean("pingEnabled", "1_onStarting", false, "If enabled, this will send a message that will be deleted when the server is loading.");
	this.sendLoadedPingMessage = this.config.getBoolean("pingEnabled", "2_onStart", false, "If enabled, this will send a message that will be deleted when the server is fully loaded.");
	this.sendShuttingPingMessage = this.config.getBoolean("pingEnabled", "3_onStopping", false, "If enabled, this will send a message that will be deleted when the server is stopping.");
	this.sendShutPingMessage = this.config.getBoolean("pingEnabled", "4_onStopped", false, "If enabled, this will send a message that will be deleted when the server is fully stopped.");
		
	this.loadingPingMessage = this.config.getString("pingMessageJSON", "1_onStarting", "{\"content\":\"<&@YOURROLEHERE>\",\"embeds\":null,\"attachments\":[]}", "The message that will be sent and then deleted.");
	this.loadingPingMessageWait = this.config.getInt("pingMessageTime", "1_onStarting", 50, 1, Integer.MAX_VALUE, "The life time of the ping message until deletion.");
	this.loadedPingMessage = this.config.getString("pingMessageJSON", "2_onStart", "{\"content\":\"<&@YOURROLEHERE>\",\"embeds\":null,\"attachments\":[]}", "The message that will be sent and then deleted.");
	this.loadedPingMessageWait = this.config.getInt("pingMessageTime", "2_onStart", 50, 1, Integer.MAX_VALUE, "The life time of the ping message until deletion.");
	this.shuttingPingMessage = this.config.getString("pingMessageJSON", "3_onStopping", "{\"content\":\"<&@YOURROLEHERE>\",\"embeds\":null,\"attachments\":[]}", "The message that will be sent and then deleted.");
	this.shuttingPingMessageWait = this.config.getInt("pingMessageTime", "3_onStopping", 50, 1, Integer.MAX_VALUE, "The life time of the ping message until deletion.");
	this.shutPingMessage = this.config.getString("pingMessageJSON", "4_onStopped", "{\"content\":\"<&@YOURROLEHERE>\",\"embeds\":null,\"attachments\":[]}", "The message that will be sent and then deleted.");
	this.shutPingMessageWait = this.config.getInt("pingMessageTime", "4_onStopped", 50, 1, Integer.MAX_VALUE, "The life time of the ping message until deletion.");
		
	if (this.config.hasChanged()) this.config.save();
    }


    private static long trySend(String message) {
	long id = 0;
	try {
	    id = send(username, avatarUrl, "POST", webhook, formatMessage(message));
	} catch(IOException ex) {
	    Logger.error("Coulnd't send the message! Body: " + messageL + ", error:");
	    ex.printStackTrace();
	}
	return id;
    }

    private void sendOrEdit(String message) {
	if (this.messageID < 1 || !this.editSended) {
	    long id = this.trySend(message);
	    if (this.editSended) {
		this.messageID = id;
		this.config.getCategory("0_general").get("messageID").set(id + "");
		this.config.save();
	    }
	} else {
	    try {
		send(this.username, this.avatarUrl, "PATCH", this.webhook + "/messages/" + this.messageID, this.formatMessage(message));
	    } catch(Exception ex) {
		long id = this.trySend(message);
		if (this.editSended) {
		    // it should be easy to prove that this path will always be taken, but also i'm tired
		    this.messageID = id;
		    this.config.getCategory("0_general").get("messageID").set(id + "");
		    this.config.save();
		}
	    }
	}
    }
    
    private void doPingMessage(String pingMessage, int pingMessageWait) throws InterruptedException {
	if (pingMessage == "" || pingMessageWait <= 0) {
	    return;
	}
	long id = this.trySend(pingMessage);
	Thread.sleep(pingMessageWait);
	try {
	    send("", "", "DELETE", this.webhook + "/messages/" + id, "");
	} catch(IOException ex) {
	    Logger.error("Coulnd't delete the message! Body: " + this.messageL + ", error:");
	    ex.printStackTrace();
	}
    }
	
    @EventHandler
    public void serverStrarting(FMLServerStartingEvent e) throws InterruptedException {
	if (e.getSide() == Side.SERVER && this.sendMessageOnBooting) {
	    this.sendOrEdit(this.loadingMessage);
	    
	    if (this.sendLoadingPingMessage) {
		this.doPingMessage(this.loadingPingMessage, this.loadingPingMessageWait);
	    }
	}
    }
	
    @EventHandler
    public void serverStrarted(FMLServerStartedEvent e) throws InterruptedException {
	if (e.getSide() == Side.SERVER && this.sendMessageOnBoot) {
	    this.onlineTime = Math.floor(((new Date()).getTime() / 1000));
	    this.sendOrEdit(this.loadedMessage);
	    
	    if (this.sendLoadedPingMessage) {
		this.doPingMessage(this.loadedPingMessage, this.loadedPingMessageWait);
	    }
	}
    }
	
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent e) throws InterruptedException {
	if (e.getSide() == Side.SERVER && this.sendMessageOnShuttingDown) {
	    this.sendOrEdit(this.shuttingMessage);
	    
	    if (this.sendShuttingPingMessage) {
		this.doPingMessage(this.shuttingPingMessage, this.shuttingPingMessageWait);
	    }
	}
    }
	
    @EventHandler
    public void serverStopped(FMLServerStoppedEvent e) throws InterruptedException {
	if (e.getSide() == Side.SERVER && this.sendMessageOnShutDown) {
	    this.offlineTime = Math.floor(((new Date()).getTime() / 1000));
	    this.sendOrEdit(this.shutMessage);
	    
	    if (this.sendShutPingMessage) {
		this.doPingMessage(this.shutPingMessage, this.shutPingMessageWait);
	    }
	}
    }

    // mostly copied from nilcord
    public static final class FMLEvents {
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP player
		&& sendMessageOnJoin) {
		playerName = player.getGameProfile().getName();
                trySend(joinMessage);
            }
        }
        @SubscribeEvent
        public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.player instanceof EntityPlayerMP player
		&& sendMessageOnLeave) {
		playerName = player.getGameProfile().getName();
                trySend(leaveMessage);
            }
        }
    }
	
    private static void allowMethods(String... methods) {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
	
}
