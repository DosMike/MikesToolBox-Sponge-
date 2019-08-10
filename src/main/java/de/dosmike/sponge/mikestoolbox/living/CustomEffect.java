package de.dosmike.sponge.mikestoolbox.living;

import org.spongepowered.api.entity.living.Living;

/** why is this a thing and why do it "duplicate" vanilla effects with it?<br>
 * As far as I know vanilla effects apply for time unit seconds. I want effects to be able to last less.
 * Also with this I can easily revert changes dont to a player as he e.g. leaves the world */
public interface CustomEffect {
	/** the name of this wcEffect if necessary */
	String getName();
	/** how long this effect will stay active in seconds. a duration &lt;= 0 will be permanent.<br><br>
	 * default implementation returns 0. */
	default double getDuration() {
		return 0;
	}
	/** if this returns true the effect will not be added to the queue saving the hassle of removing it the next tick.<br>
	 * As a result it will only call the onApply and ignore onTick and onRemove, so those can remain empty.<br><br>
	 * default implementation returns false */
	default boolean isInstant() {
		return false;
	}
	/** allows you to terminate the effect early, it will be removed, once the effect expired, or this method returns false */
	default boolean isRunning() {
		return true;
	}

	/** do something with the entity when the effect get's added */
	default void onApply(Living entity) {}
	/** should be called around every 100 ms, dt will give you the exact ms since the last tick. */
	default void onTick(Living entity, int dt) {}
	/** remove for example applied potion effects from the entity.<br>
	 * This may never be called on a effect instance if it's extended by a effect of the same class */
	default void onRemove(Living entity) {}

}
