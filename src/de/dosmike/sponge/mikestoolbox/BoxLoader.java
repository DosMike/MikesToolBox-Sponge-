package de.dosmike.sponge.mikestoolbox;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.dosmike.sponge.mikestoolbox.nbtinspect.BoxNbtInspector;
import de.dosmike.sponge.mikestoolbox.zone.BoxZones;
import de.dosmike.sponge.mikestoolbox.zone.ZoneItems;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import com.google.inject.Inject;

import de.dosmike.sponge.mikestoolbox.listener.BoxItemEventListener;
import de.dosmike.sponge.mikestoolbox.listener.SpongeEventListener;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.service.ZoneService;
import de.dosmike.sponge.mikestoolbox.zone.ZoneServiceProvider;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;
import org.spongepowered.api.scheduler.Task;

/** <b>This is not the Package you are looking for :)</b><br>
 * The ToolBox plugin will load modules and provide base services<br>
 * Most Tools should be accessed in a static way. */
@Plugin(id = "dosmike_toolbbox", name = "Mike's ToolBox", version = "1.2.1")
public class BoxLoader {
	/** everyone needs some random in his life */
	public static Random RNG = new Random(System.currentTimeMillis());
	
	private static BoxLoader instance;
	public static BoxLoader getBoxLoader() { return instance; }
	private static PluginContainer container;
	public static PluginContainer getBoxContainer() { return container; }
	//private static Cause boxCause;
	/** jaja, just blame everything on me ...  */
	//public static Cause blame() { return boxCause; }
	
	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
//	private static PluginLoader toolbox = new PluginLoader();
	
	public static void main(String[] args) {
		System.out.println("This jar can not be run as executable!");
	}
	
	static ZoneService zoneService; public static ZoneService getZoneService() { return zoneService; }
	@Listener
	public void init(GameInitializationEvent event) { 
		instance = this; 
		//Service Providers have to be registered earlier
		Sponge.getServiceManager().setProvider(this, ZoneService.class, zoneService = new ZoneServiceProvider());
		l("The ZoneService is now available!");
	}
	public void onServiceChange(ChangeServiceProviderEvent event) {
		if (event.getService().equals(ZoneService.class)) {
			zoneService = (ZoneService)event.getNewProvider();
		}
	}
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;
	public Path getPrivateConfigDir() {
		File d = privateConfigDir.toFile();
		if (!d.exists()) d.mkdirs(); //sponge sais the folders are created, but i have yet to see that happen. until then I'll manually check that
		return privateConfigDir;
	}

	private SpongeExecutorService syncExecutor;

	public SpongeExecutorService getSyncExecutor() {
		return syncExecutor;
	}

	@Listener
	public void onServerStart(GamePostInitializationEvent event) {
		
		container = Sponge.getPluginManager().fromInstance(this).get();
		//boxCause = Cause.builder().owner(me).build();
		
		loadModules();
		//l("Mikes ToolBox loaded "+toolbox.getPluginsBySuperclass(Object.class).size()+" tools");
		
		Sponge.getEventManager().registerListeners(BoxLoader.getBoxLoader(), new BoxItemEventListener());
		syncExecutor =
		Sponge.getScheduler().createSyncExecutor(this);
		syncExecutor.scheduleAtFixedRate(()->{
			BoxLiving.tickCustomEffect();
		}, 1000, 100, TimeUnit.MILLISECONDS);
		Task.builder().execute(()->{
			BoxLiving.tickGravity();
		}).intervalTicks(1).delayTicks(10)
				.name("mtb Gravity ticker")
				.submit(this);

		Sponge.getEventManager().registerListeners(this, new SpongeEventListener()); //general event listener. Modules should register their own stuff
	}
	@Listener
	public void onServerStop(GameStoppingEvent event) {
	}
	
	private void loadModules() {
		BoxNbtInspector.prepareToolbox();
		BoxZones.prepareToolbox();
		NOP(ZoneItems.WAND); //load box item
	}

	private void NOP(Object object){}
}
