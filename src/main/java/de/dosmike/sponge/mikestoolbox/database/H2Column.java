package de.dosmike.sponge.mikestoolbox.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fields annotated with this declare a row for SQL tables 
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface H2Column {
	/** datatype */
	String value() default "AUTO";
	/** size represents the PrecisionInt, see <a href="http://www.h2database.com/html/datatypes.html">http://www.h2database.com/html/datatypes.html</a> */
	int size() default 11;
	/** describes the columnDefinition part of the CREATE TABLE query, see <a href="http://www.h2database.com/html/grammar.html#column_definition">http://www.h2database.com/html/grammar.html#column_definition</a>
	 * default is NOT NULL */
	String columnParameters() default "NOT NULL";
	/** specify a custom column name. Default is AUTO, which will use the field name as column name */
	String columnName() default "AUTO";
	/** when storing something that's not a {@link Number} or {@link String}
	 * it will be saved as varchar in the database after calling Object.toString().<br>
	 * This will determ, how the element is restored. CONSTRUCTOR will try to call a
	 * constructor that accepts one string argument, fromString will try to get one of
	 * the string reconstruction methods that accepts one string agument.<br>
	 * (Only used on Objects not Number or String and {@link @H2Column} value is AUTO) */
	AutoSQL.ReconstructionMethod method() default AutoSQL.ReconstructionMethod.NONE;
	/** only used if method is SERIALIZER. Specifies the class to use to serialize this field into blob objects */
	Class<? extends H2Serializer> serializer() default H2Serializer.class;
}
