package mikestoolbox;

//@WookiePluginAnnotation(Name="MTB Trace Demo Box", Description="Testing the tracers", Author="DosMike", Build=1, Version="1.0")
public class TraceDemoBox {
	/*BoxTracer cyanTrace = new BoxTracer(Color.CYAN); 
	
	@WookiePluginFunction(Type.onPrepare)
	public void prepareToolbox() {
		BoxCommand.registerCommand("trace [Start{Location}] <Entity> [HexColor{String}]", "toolbox.trace.cmd.trace.base", (src, args)->{
			if (!(src instanceof Locatable)) {
				src.sendMessage(Text.of("Players only ;P"));
				return CommandResult.success();
			}
			Locatable p = (Locatable) src;
			Vector3d from = args.<Location<World>>getOne("Start").orElse(p.getLocation()).getPosition();
			args.<Entity>getOne("Entity").ifPresent(entity->{
				Optional<String> acolor = args.<String>getOne("HexColor");
				Color color = null;
				if (acolor.isPresent()) try { color = Color.ofRgb(Integer.decode(acolor.get())); } catch (Exception ignore) {
					src.sendMessage(Text.of(TextColors.RED, "Invalid color code, use a Hex Color (see google)"));
					return;
				} 
				if (color == null) 
					cyanTrace.drawTrace(p.getWorld(), from, entity.getLocation().getPosition());
				else
					BoxTracer.drawTrace(color, p.getWorld(), from, entity.getLocation().getPosition());
			});
			return CommandResult.success();
		});
	}
	@WookiePluginFunction(Type.onUnload) 
	public void destroyToolbox() {
		
	}*/
}
