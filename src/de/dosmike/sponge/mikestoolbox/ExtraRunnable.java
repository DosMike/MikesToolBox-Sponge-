package de.dosmike.sponge.mikestoolbox;

public abstract class ExtraRunnable<T> implements Runnable {
	public T extra;
	public ExtraRunnable(T extra) { this.extra=extra; }
}
