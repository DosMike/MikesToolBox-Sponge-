package de.dosmike.sponge.mikestoolbox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import com.google.inject.Inject;

import de.dosmike.sponge.mikestoolbox.listener.BoxItemEventListener;
import de.dosmike.sponge.mikestoolbox.listener.SpongeEventListener;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.service.ZoneService;
import de.dosmike.sponge.mikestoolbox.zone.ZoneServiceProvider;

@Plugin(id = "dosmike_toolbbox", name = "Mike's ToolBox", version = "1.0")
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
	
	@Listener
	public void init(GameInitializationEvent event) { 
		instance = this; 
		//Service Providers have to be registered earlier
		Sponge.getServiceManager().setProvider(this, ZoneService.class, new ZoneServiceProvider());
		l("The ZoneService is now available!");
	}
	@Listener
	public void onServerStart(GamePostInitializationEvent event) {
		
		container = Sponge.getPluginManager().fromInstance(this).get();
		//boxCause = Cause.builder().owner(me).build();
		
		loadModules();
		//l("Mikes ToolBox loaded "+toolbox.getPluginsBySuperclass(Object.class).size()+" tools");
		
		Sponge.getEventManager().registerListeners(BoxLoader.getBoxLoader(), new BoxItemEventListener());
		Sponge.getScheduler().createSyncExecutor(this).scheduleAtFixedRate(()->{
			BoxLiving.tickCustomEffect();
		}, 1000, 100, TimeUnit.MILLISECONDS);
		Sponge.getScheduler().createSyncExecutor(this).scheduleAtFixedRate(()->{
			BoxLiving.tickGravity();
		}, 50, 24, TimeUnit.MILLISECONDS);
		
		Sponge.getEventManager().registerListeners(this, new SpongeEventListener()); //general event listener. Modules should register their own stuff
	}
	@Listener
	public void onServerStop(GameStoppingEvent event) {
	}
	
	@SuppressWarnings("unchecked")
	private void loadModules() {
		try {
			Class<?>[] modules = getModules();
			for (Class<?> module : modules)
				loadModule((Class<BoxModule>)module);
		} catch (Exception e) {
			throw new RuntimeException("Error while loading modules", e);
		}
	}
	private Class<?>[] getModules() throws IOException, ClassNotFoundException {
		//generic class search in package
		String pkg = this.getClass().getPackage().getName();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		assert cl != null;
		Enumeration<URL> e = cl.getResources(pkg.replace('.', '/'));
		List<Class<?>> clz = new ArrayList<>();
		while (e.hasMoreElements()) 
			clz.addAll(fc(new File(e.nextElement().getFile()), pkg));
		//filter modules here:
		clz = clz.stream().filter(claz->BoxModule.class.isAssignableFrom(claz)).collect(Collectors.toList());
		return clz.toArray(new Class<?>[clz.size()]);
	}
	private List<Class<?>>fc(File f, String p) throws ClassNotFoundException {
		List<Class<?>> clz = new ArrayList<>();
		if (f.exists())
			for (File e : f.listFiles())
				if (e.isDirectory()) {
					assert !e.getName().contains(".");
					clz.addAll(fc(e,p+"."+e.getName()));
				} else if (e.getName().endsWith(".class")) {
					clz.add(Class.forName(p+"."+e.getName().substring(0, e.getName().length() - 6)));
				}
		return clz;
	}
	private void loadModule(Class<BoxModule> module) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		assert module != null;
		Method regMeth = null;
		for (Method m : module.getDeclaredMethods()) {
			if (m.isAnnotationPresent(BoxModuleRegistration.class)) {
				assert regMeth == null;
				regMeth = m;
			}
		}
		if (regMeth == null)
			w("Module "+module.getSimpleName()+" does not register!");
		else {
			regMeth.setAccessible(true);
			regMeth.invoke(module.newInstance());
			l("Registered module "+module.getSimpleName());
		}
	}
}
