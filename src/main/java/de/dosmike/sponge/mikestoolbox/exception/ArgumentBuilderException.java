package de.dosmike.sponge.mikestoolbox.exception;

public class ArgumentBuilderException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public ArgumentBuilderException() {
		super("Could not build GenericArgument from String");
	}
	public ArgumentBuilderException(String message) {
		super("Could not build GenericArgument from String: "+message);
	}
	public ArgumentBuilderException(Throwable trown) {
		super("Could not build GenericArgument from String", trown);
	}
	public ArgumentBuilderException(String message, Throwable thrown) {
		super("Could not build GenericArgument from String: "+message, thrown);
	}
}
