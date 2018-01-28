package de.dosmike.sponge.mikestoolbox;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
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

/** <b>This is not the Package you are looking for :)</b><br>
 * The ToolBox plugin will load modules and provide base services<br>
 * Most Tools should be accessed in a static way. */
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
			Class<?>[] modules = loadJar(); // getModules();
			for (Class<?> module : modules)
				try {
					loadModule((Class<BoxModule>)module);
				} catch (Exception ee) {
					throw new RuntimeException("Could not register module "+module.getSimpleName(), ee);
				}
		} catch (Exception e) {
			throw new RuntimeException("Error while loading modules", e);
		}
	}
//	private Class<?>[] getModules() throws IOException, ClassNotFoundException {
//		//generic class search in package
//		String pkg = this.getClass().getPackage().getName();
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		assert cl != null;
//		Enumeration<URL> e = cl.getResources(pkg.replace('.', '/'));
//		List<Class<?>> clz = new ArrayList<>();
//		while (e.hasMoreElements()) 
//			clz.addAll(fc(new File(e.nextElement().getFile()), pkg));
//		//filter modules here:
//		clz = clz.stream().filter(claz->BoxModule.class.isAssignableFrom(claz)).collect(Collectors.toList());
//		return clz.toArray(new Class<?>[clz.size()]);
//	}
//	private List<Class<?>>fc(File f, String p) throws ClassNotFoundException {
//		List<Class<?>> clz = new ArrayList<>();
//		if (f.exists())
//			for (File e : f.listFiles())
//				if (e.isDirectory()) {
//					assert !e.getName().contains(".");
//					clz.addAll(fc(e,p+"."+e.getName()));
//				} else if (e.getName().endsWith(".class")) {
//					clz.add(Class.forName(p+"."+e.getName().substring(0, e.getName().length() - 6)));
//				}
//		return clz;
//	}
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
	
	public Class<?>[] loadJar() {
		//add jar to classloader
		File jarFile = null;
		try { 
			String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
			if (path.contains(".jar!")) path = path.split("\\.jar!")[0]+".jar";
			path = URLDecoder.decode(path,"utf-8"); //parse special chars 
			if (path.startsWith("file:")) path = path.substring(5);
			if (path.charAt(2) == ':') path = path.substring(1);//windows
			jarFile = new File(path);
			l("Scanning %s", path);
			
			//shouldn't it already be loaded?
//		    Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//		    addURL.setAccessible(true);
//		    addURL.invoke(URLClassLoader.getSystemClassLoader(), new Object[] {jarFile.toURI().toURL()});
		} catch (Exception ignore) {
			ignore.printStackTrace();
			return new Class<?>[]{};
		}
		
	    List<Class<?>> modules = new LinkedList<>();
	    JarFile jar=null;
	    try {
	    	jar= new JarFile(jarFile);
		    Enumeration<JarEntry> entries = jar.entries();
		    while (entries.hasMoreElements()) {
		        JarEntry entry = entries.nextElement();
		        String name = entry.getName();
		        if (name.endsWith(".class")) {
		            name = name.substring(0, name.lastIndexOf('.')).replace('/', '.');
		            Class<?> cls = Class.forName(name);
		            
		            if (!BoxModule.class.isAssignableFrom(cls)) continue;
		            
					modules.add(cls);
		        }
		    }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
	    	try { jar.close(); } catch (Exception ignore) {}
	    }
	    return modules.toArray(new Class<?>[modules.size()]);
	}
}
