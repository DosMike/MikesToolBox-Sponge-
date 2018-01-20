package mikestoolbox;

//@WookiePluginAnnotation(Name="MTB Fun Box", Description="A collection of fun commands", Author="DosMike", Build=1, Version="1.0")
public class CustomItemBox {
	/*static BoxItem cursedHelmet;
	static BoxItem luckyCharm;
	static CustomEffect hunger = new CustomEffect() {

		@Override
		public String getName() {
			return "Hunger of the Cursed Helmet";
		}

		@Override
		public void onApply(Living entity) {
			if (entity instanceof Player)
				((Player)entity).sendMessage(Text.of("You shall suffer from eternal hunger"));
		}
		int dtc = 0;
		@Override
		public void onTick(Living entity, int dt) {
			if (!(entity instanceof Player)) return;
			dtc += dt;
			while (dtc>1000) {
				((Player)entity).saturation().set(0d);
				int fl = entity.get(Keys.FOOD_LEVEL).orElse(20);
				if (fl>0)
					entity.offer(Keys.FOOD_LEVEL, fl-1);
				dtc-=1000;
			}
		}

		@Override
		public void onRemove(Living entity) {
			if (entity instanceof Player)
				((Player)entity).sendMessage(Text.of("The curse of eternal hunger was broken"));
		}
		
	};
	
	@WookiePluginFunction(Type.onPrepare)
	public void prepareToolbox() {
		cursedHelmet = BoxItem.builder(
				ItemStack.builder()
					.itemType(ItemTypes.IRON_HELMET)
					.quantity(64)
					.add(Keys.DISPLAY_NAME, Text.of("Cursed Helmet"))
					.build()
					, "boxItem:cursedHelmet"
				)
				.addPassives(Arrays.asList(new CustomEffect[]{
					hunger
				}))
				.build();
				
		BoxCommand.registerCommand("boxitem", "toolbox.item.cmd.give.base", (src, args)->{
			if (src instanceof Player) {
				Player player = (Player) src;
				src.sendMessage(Text.of("The command was executed!"));
				//player.equip(EquipmentTypes.HEADWEAR, cursedHelmet.item().createStack());
				Iterator<Inventory> s = player.getInventory().parent().slots().iterator();
				Inventory slot=null;
				for (int i=0; i<5; i++) slot = s.next(); //helmet slot
				if (slot!=null) slot.set(cursedHelmet.item().createStack());
			};
			return CommandResult.success();
		});
	}
	@WookiePluginFunction(Type.onUnload) 
	public void destroyToolbox() {
		
	}*/
}
