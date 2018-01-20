package de.dosmike.sponge.mikestoolbox.command;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.CommandFlags;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import de.dosmike.sponge.mikestoolbox.BoxLoader;
import de.dosmike.sponge.mikestoolbox.exception.ArgumentBuilderException;

/** Command toolbox to more easily bild args from a description string */
public class BoxCommand {
	public static void registerCommand(String command, String permission, CommandExecutor executor) {
		if (command.indexOf(' ')<0) {
			Sponge.getCommandManager().register(BoxLoader.getBoxLoader(), CommandSpec.builder().permission(permission).executor(executor).build(), command);
		} else {
			String name = command.substring(0, command.indexOf(' '));
			CommandSpec.Builder spec = CommandSpec.builder();
			if (permission!= null) spec.permission(permission);
			spec.executor(executor).arguments(parseArgs(command.substring(command.indexOf(' ')+1)));
			Sponge.getCommandManager().register(BoxLoader.getBoxLoader(), spec.build(), name);
		}
	}
	
	private static Pattern argFinder = Pattern.compile("((?:<.+?>)|(?:\\[.+?\\])|(?:-(?:(?:-\\w+)|\\w)(?: [^ -]+)?))"); //each match is a flag or argument 
	private static Pattern flag = Pattern.compile("-(-?\\w+)(?: ([^ -]+))?"); //g1: name  (g2: value)
	private static Pattern argument = Pattern.compile("([\\[<])(.+)[\\]>]"); //g1: <[  g2: run argFinder, if nothing it's a value 
	private static Pattern value = Pattern.compile("(\\w+)(?:\\{(\\w+)(?::([\\w\\.]+))?\\})?"); //g1: name  g2: if present type, other wise use g1
	
	
	private static CommandElement[] parseArgs(String argString) { return parseArgs(argString, true); }
	private static CommandElement[] parseArgs(String argString, boolean allowFlags) {
		List<CommandElement> elements = new LinkedList<>();
		CommandFlags.Builder cflags = null;
		Matcher m = argFinder.matcher(argString);
		while (m.find()) {
			String part = m.group();
			Matcher ma = argument.matcher(part);
			if (ma.matches()) {
				boolean optional=ma.group(1).charAt(0) == '[';
				CommandElement[] result = parseArgs(ma.group(2), false);
				if (result.length==0) result = new CommandElement[]{argValue(ma.group(2)).getPermissionArgument()};
				elements.add(optional?GenericArguments.optional(wrap(result)):wrap(result));
			} else {
				Matcher mf = flag.matcher(part);
				if (mf.matches()) {
					if (cflags == null) cflags = GenericArguments.flags();
					parseFlag(cflags, mf.group(1), mf.group(2));
				} else {
					throw new ArgumentBuilderException("Argument type was not recognized for `"+part+"`");
				}
			}
		}
		if (cflags==null) return elements.toArray(new CommandElement[elements.size()]);
		else return new CommandElement[]{ cflags.buildWith(wrap(elements.toArray(new CommandElement[elements.size()]))) };
	}
	private static void parseFlag(CommandFlags.Builder flags, String name, String value) {
		if (value == null) {
			int at;
			if ((at=name.indexOf(':'))>=0) {
				String a=name.substring(0, at), b=name.substring(at+1);
				flags.permissionFlag(b, a);
			} else {
				flags.flag(name);
			}
		} else {
			ArgumentValue av = argValue(value);
			if (name.indexOf(':')>=0) {
				throw new ArgumentBuilderException("Flag values do not support permissions at flag `"+name+"`. Permit the value instead");
			}
			flags.valueFlag(av.getPermissionArgument(), name);
		}
	}
	private static class ArgumentValue {
		CommandElement element; String permission;
		public ArgumentValue(CommandElement element, String permission) { this.element = element; this.permission = permission; }
//		public CommandElement getGenericArgument() { return element; }
		public CommandElement getPermissionArgument() { return permission==null?element:GenericArguments.requiringPermission(element, permission); }
//		public Optional<String> getPermission() { return permission==null?Optional.empty():Optional.of(permission); }
	}
	private static ArgumentValue argValue(String valueString) {
		String type;
		String key;
		String permission;
		Matcher vm = value.matcher(valueString);
		if (!vm.matches()) throw new ArgumentBuilderException("Unknown argument specification `"+valueString+"`, use Type or Name{Type} or Name{Type:Permission}");
		key = vm.group(1);
		type = vm.group(2);
		permission = vm.group(3);
		if (type==null) type=key;
		
		switch(type.toLowerCase()) {
		case "bool":
			return new ArgumentValue(GenericArguments.bool(Text.of(key)), permission);
		case "double":
			return new ArgumentValue(GenericArguments.doubleNum(Text.of(key)), permission);
		case "entity":
			return new ArgumentValue(GenericArguments.entity(Text.of(key)), permission);
		case "entityorsrouce":
			return new ArgumentValue(GenericArguments.entityOrSource(Text.of(key)), permission);
		case "integer":
			return new ArgumentValue(GenericArguments.integer(Text.of(key)), permission);
		case "location":
			return new ArgumentValue(GenericArguments.location(Text.of(key)), permission);
		case "long":
			return new ArgumentValue(GenericArguments.longNum(Text.of(key)), permission);
		case "player":
			return new ArgumentValue(GenericArguments.player(Text.of(key)), permission);
		case "playerorsource":
			return new ArgumentValue(GenericArguments.playerOrSource(Text.of(key)), permission);
		case "plugin":
			return new ArgumentValue(GenericArguments.plugin(Text.of(key)), permission);
		case "remainingstring":
			return new ArgumentValue(GenericArguments.remainingJoinedStrings(Text.of(key)), permission);
		case "string":
			return new ArgumentValue(GenericArguments.string(Text.of(key)), permission);
		case "user":
			return new ArgumentValue(GenericArguments.user(Text.of(key)), permission);
		case "userorsource":
			return new ArgumentValue(GenericArguments.userOrSource(Text.of(key)), permission);
		case "vector":
			return new ArgumentValue(GenericArguments.vector3d(Text.of(key)), permission);
		case "world":
			return new ArgumentValue(GenericArguments.world(Text.of(key)), permission);
		default:
			try {
				@SuppressWarnings("unchecked")
				Class<? extends CatalogType> clazz = (Class<? extends CatalogType>) Class.forName(type);
				return new ArgumentValue(GenericArguments.catalogedElement(Text.of(key), clazz), permission);
			} catch (Exception e) {
				throw new ArgumentBuilderException("No argument of type `"+type+"` can be read");
			}
		}
	}
	private static CommandElement wrap(CommandElement... elements) {
		if (elements.length==0) return GenericArguments.none();
		return elements.length==1?elements[0]:GenericArguments.seq(elements);
	}
}
