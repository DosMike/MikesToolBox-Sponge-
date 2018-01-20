package mikestoolbox;

//@WookiePluginAnnotation(Name="MTB Gravity Demo Box", Description="Testing gravity modifiers", Author="DosMike", Build=1, Version="1.0")
public class GravityDemoBox {
	/*
	@WookiePluginFunction(Type.onPrepare)
	public void prepareToolbox() {
		BoxCommand.registerCommand("gravity <Entity> [Gravity{Double}]", "toolbox.gravity.cmd.gravity.base", (src, args)->{
			if (!args.hasAny("Entity")) throw new CommandException(Text.of("Invalid target"));
			if (args.hasAny("Gravity")) {
				double gravity = (Double)args.getOne("Gravity").orElse(1.0);
				args.getAll("Entity").forEach(entity-> BoxLiving.setGravity((Entity)entity, gravity));
			} else {
				Entity e = (Entity)args.getOne("Entity").get();
				double gravity = BoxLiving.getGravity(e);
				src.sendMessage(Text.of(e.toString(), " has a Gravity of ", gravity));
			}
			return CommandResult.success();
		});
	}
	@WookiePluginFunction(Type.onUnload) 
	public void destroyToolbox() {
		
	}*/
}
