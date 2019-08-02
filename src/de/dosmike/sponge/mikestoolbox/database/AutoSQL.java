package de.dosmike.sponge.mikestoolbox.database;

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.ClassUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.service.sql.SqlService;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;

public class AutoSQL<T> {
	private static SqlService sql=null;
//	private static final String DB_URL = "jdbc:h2:./config/modid/database";

	private SpongeExecutorService scheduler;
	private String DB_URL;

	Class<T> clazz;
	Map<Field, DBColumn> annotations = new HashMap<>();
	public AutoSQL(@NotNull String DatabaseURL, @NotNull SpongeExecutorService scheduler, @NotNull Class<T> clazz) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
		this.scheduler = scheduler;
		this.DB_URL = DatabaseURL;
		this.clazz = clazz;
		for (Field f : clazz.getFields()) {
    		if (!f.isAnnotationPresent(H2Column.class)) continue;
    		H2Column rpdb = f.getAnnotation(H2Column.class);
    		annotations.put(f, new DBColumn<T>(f, rpdb));
		}
	}
	private boolean tableChecked=false;
	
	private static class DBColumn<Y> {
		private String columnName;
		private int sqlType;
		private String typeString;
		private int columnSize;
		private String createParams;
		private ReconstructionMethod reconstructionMethod;
		private H2Serializer<Y> blobSerializer;
		public DBColumn(Field field, H2Column fieldAnnotation) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
			columnName = fieldAnnotation.columnName();
			String columnType = fieldAnnotation.value();
			columnSize = fieldAnnotation.size();
			createParams = fieldAnnotation.columnParameters();
			reconstructionMethod = fieldAnnotation.method();
			Class<? extends H2Serializer> sc = fieldAnnotation.serializer();
			blobSerializer = null;

			Class<?> z = field.getType();
			if (columnType.equals("AUTO")) {
    			if (z.isPrimitive()) z = ClassUtils.primitiveToWrapper(z);
    			
    			if (z.equals(Double.class)) { sqlType = Types.DOUBLE; typeString = "DOUBLE"; }
    			else if (z.equals(Float.class)) { sqlType = Types.REAL; typeString = "REAL"; }
    			else if (z.equals(Long.class)) { sqlType = Types.BIGINT; typeString = "BIGINT"; }
    			else if (z.equals(Integer.class)) { sqlType = Types.INTEGER; typeString = "INTEGER"; }
    			else if (z.equals(Short.class)) { sqlType = Types.SMALLINT; typeString = "SMALLINT"; }
    			else if (z.equals(Byte.class)) { sqlType = Types.TINYINT; typeString = "TINYINT"; }
    			else if (z.equals(Boolean.class)) { sqlType = Types.BOOLEAN; typeString = "BOOLEAN"; }
    			else if (z.equals(String.class)) { sqlType = Types.VARCHAR; typeString = "VARCHAR"; }
    			else if (z.equals(BigDecimal.class)) { sqlType = Types.DECIMAL; typeString = "DECIMAL"; }
    			else if (z.equals(Time.class)) { sqlType = Types.TIME; typeString = "TIME"; }
    			else if (z.equals(Date.class)) { sqlType = Types.DATE; typeString = "DATE"; }
    			else if (z.equals(Timestamp.class)||z.equals(java.util.Date.class)) { sqlType = Types.TIMESTAMP; typeString = "TIMESTAMP"; }
    			else { sqlType = Types.VARCHAR; typeString = "VARCHAR"; } //just string convert it, heck idc

				if (reconstructionMethod.equals(ReconstructionMethod.CONSTRUCTOR) && !AutoSQL.classSupportsFromConstructor(z))
					throw new RuntimeException(String.format("%s::%s in %s does not support ReconstructionMethod.CONSTRUCTOR", z.getCanonicalName(), field.getName(), field.getDeclaringClass().getCanonicalName()));
				else if (reconstructionMethod.equals(ReconstructionMethod.FROMSTRING) && !AutoSQL.classSupportsFromStringMethod(z))
					throw new RuntimeException(String.format("%s::%s in %s does not support ReconstructionMethod.FROMSTRING", z.getCanonicalName(), field.getName(), field.getDeclaringClass().getCanonicalName()));
				else if (reconstructionMethod.equals(ReconstructionMethod.SERIALIZER)) {
					sqlType = Types.BLOB;
					typeString = "BLOB";
					try {
						blobSerializer = (H2Serializer<Y>) sc.newInstance();
					} catch (ClassCastException e) {
						throw new RuntimeException(String.format("H2Serializer for %s::%s in %s is invalid!", z.getCanonicalName(), field.getName(), field.getDeclaringClass().getCanonicalName()));
					}
				}
    		} else {
    			sqlType = Types.class.getDeclaredField(columnType).getInt(null);
    		}
    		if (columnName.equals("AUTO")) columnName = field.getName();
		}
		
		public String column() { return columnName; }
		public int sqlType() { return sqlType; }
//		public String columnType() { return typeString; }
		public String getColumnDefinition() {
			return String.format("`%s` %s(%d) %s,\n", columnName, typeString, columnSize, createParams);
		}
		public ReconstructionMethod getMethod() { return reconstructionMethod; }
	}
	
	public DataSource getDataSource() throws SQLException {
	    if (sql == null) {
	        sql = Sponge.getServiceManager().provide(SqlService.class).get();
	    }
	    if (!tableChecked) {
	    	tableChecked=true;
			try (Connection con = sql.getDataSource(DB_URL).getConnection()) {
				con.setAutoCommit(true);
				String query = "CREATE TABLE IF NOT EXISTS `"+clazz.getSimpleName()+"` (\n"+
						"`ID` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,\n"; //automatically created column so you don't have to bother - propper indexing FTW
				for (Entry<Field, DBColumn> annot : annotations.entrySet()) {
					query += annot.getValue().getColumnDefinition();
				}
				query += ");";
				con.prepareStatement(query).execute();
			}
		}
	    return sql.getDataSource(DB_URL);
	}
	
	private Object valueFor(Connection sqlConnection, T object, Field field) throws IllegalArgumentException, IllegalAccessException, SQLException {
		assert annotations.containsKey(field) : "Field not registered";
		DBColumn an = annotations.get(field);
		Object fieldValue = field.get(object);
		if (fieldValue == null)
			return null;
		else if (an.sqlType() == Types.BLOB) {
			Blob b = sqlConnection.createBlob();
			an.blobSerializer.serialize(fieldValue, b.setBinaryStream(1));
			return b;
		} else if (annotations.get(field).sqlType() == Types.VARCHAR) {
			return fieldValue.toString();
		} else {
			return fieldValue;
		}
	}
	
	public void insertOrUpdate(T object, Field key) {
		assert annotations.containsKey(key) : "Field not registered";
		
		scheduler.execute(()->{
			String[] fields = new String[annotations.size()];
			String templates = ""; //magic string that will fill with values in the prepared statement
			Field[] ordered = new Field[annotations.size()]; //to ensure we have the same order here as in fields
			{
				int i=0;
				for (Entry<Field, DBColumn> s : annotations.entrySet()) {
					ordered[i] = s.getKey();
					fields[i] = "`"+s.getValue().column()+"`";
					templates += ",?";
					i++;
				}
			}
			String sql = "MERGE INTO `"+clazz.getSimpleName()+"`("+String.join(",", fields)+") KEY (`"+annotations.get(key).column()+"`) VALUES ("+templates.substring(1)+");"; 
			try (Connection con = getDataSource().getConnection();
				PreparedStatement statement = con.prepareStatement(sql)) {
				con.setAutoCommit(true);
				for (int i = 0; i < fields.length; i++) {
					// statement starts counting params at 1
					Object setValue = valueFor(con, object, ordered[i]);
					if (setValue==null)
						statement.setNull(i+1, annotations.get(ordered[i]).sqlType());
					else
						statement.setObject(i+1, setValue, annotations.get(ordered[i]).sqlType());
				}
				statement.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void restoreField(Object dataHolder, Field dataField, DBColumn column, ResultSet results) throws SQLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Object value = null;
		if (column.sqlType == Types.BLOB) {
			Blob val = results.getBlob(column.column());
			if (!results.wasNull())
				value = column.blobSerializer.deserialize(val.getBinaryStream());
			val.free();
		} else  if (column.sqlType == Types.VARCHAR && column.getMethod() != ReconstructionMethod.NONE) {
			if (column.getMethod().equals(ReconstructionMethod.CONSTRUCTOR)) {
				String val = results.getString(column.column());
				if (!results.wasNull())
					value = dataField.getType().getConstructor(String.class).newInstance(val);
			} else if (column.getMethod().equals(ReconstructionMethod.FROMSTRING)) {
				String val = results.getString(column.column());
				if (!results.wasNull())
					value = getClassFromStringMethod(dataField.getType()).invoke(null, val);
			} else {
				throw new RuntimeException("Unsupported Reconstruction Method SERIALIZER for non BLOB SQL-Type");
			}
		} else {
			value = results.getObject(column.column());
		}
		dataField.set(dataHolder, value);
	}

	/** does not support blob columns as key */
	public Future<Optional<T>> selectOne(Field key, Object keyValue) {
		assert annotations.containsKey(key) : "Field not registered";
		
		Future<Optional<T>> result = //new CompletableFuture<>();
			scheduler.submit(()->{
					String sql = "SELECT * FROM `"+clazz.getSimpleName()+"` WHERE `"+annotations.get(key).column()+"`=?;";
					T object = null;
					try (Connection conn = getDataSource().getConnection();
						PreparedStatement stmt = conn.prepareStatement(sql)) {

						int type = annotations.get(key).sqlType();
						stmt.setObject(1, type == Types.VARCHAR ? keyValue.toString() : keyValue, type);

						ResultSet results = stmt.executeQuery();
						if (results.next()) {
							object = clazz.newInstance();
							for (Entry<Field, DBColumn> e : annotations.entrySet()) {
								restoreField(object, e.getKey(), e.getValue(), results);
							}
						}
						results.close();
					} catch (SQLException e) {
						e.printStackTrace();
						return Optional.empty();
					}
					return Optional.ofNullable(object);
				});
		
		return result;
	}
	/** does not support blob columns as key */
	public Future<Collection<T>> selectAll(Field key, Object keyValue) {
		assert annotations.containsKey(key) : "Field not registered";

		Future<Collection<T>> result = //new CompletableFuture<>();
				scheduler.submit(() -> {
					List<T> entries = new LinkedList<>();

					String sql = "SELECT * FROM `"+clazz.getSimpleName()+"` WHERE `"+annotations.get(key).column()+"`=?;";
					T object = null;
					try (Connection conn = getDataSource().getConnection();
						 PreparedStatement stmt = conn.prepareStatement(sql)) {

						int type = annotations.get(key).sqlType();
						stmt.setObject(1, type == Types.VARCHAR ? keyValue.toString() : keyValue, type);

						ResultSet results = stmt.executeQuery();
						while (results.next()) {
							object = clazz.newInstance();
							for (Entry<Field, DBColumn> e : annotations.entrySet()) {
								restoreField(object, e.getKey(), e.getValue(), results);
							}
							entries.add(object);
						}
						results.close();
					} catch (SQLException e) {
						e.printStackTrace();
						return new LinkedList<T>();
					}
					return entries;
				});
		return result;
	}
	/** does not support blob columns as key */
	public Future<Collection<T>> selectEverything() {
		Future<Collection<T>> result = //new CompletableFuture<>();
				scheduler.submit(() -> {
					List<T> entries = new LinkedList<>();

					String sql = "SELECT * FROM `"+clazz.getSimpleName()+"`;";
					T object = null;
					try (Connection conn = getDataSource().getConnection();
						 PreparedStatement stmt = conn.prepareStatement(sql)) {

						ResultSet results = stmt.executeQuery();
						while (results.next()) {
							object = clazz.newInstance();
							for (Entry<Field, DBColumn> e : annotations.entrySet()) {
								restoreField(object, e.getKey(), e.getValue(), results);
							}
							entries.add(object);
						}
						results.close();
					} catch (SQLException e) {
						e.printStackTrace();
						return new LinkedList<T>();
					}
					return entries;
				});
		return result;
	}
	/** does not support blob columns as key */
	public Future<Void> delete(Field key, Object keyValue) {
		assert annotations.containsKey(key) : "Field not registered";

		Future<Void> result = //new CompletableFuture<>();
				scheduler.submit(()-> {
					String sql = "DELETE FROM `"+clazz.getSimpleName()+"` WHERE `"+annotations.get(key).column()+"`=?;";
					T object = null;
					try (Connection conn = getDataSource().getConnection();
						 PreparedStatement stmt = conn.prepareStatement(sql)) {

						int type = annotations.get(key).sqlType();
						stmt.setObject(1, type == Types.VARCHAR ? keyValue.toString() : keyValue, type);

						stmt.executeUpdate();

					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				});
		return result;
	}
	/** !! DELETES ALL ENTRIES WITHIN THIS TABLE !! */
	public Future<Void> truncate() {
		Future<Void> result = //new CompletableFuture<>();
				scheduler.submit(()-> {
					String sql = "TRUNCATE TABLE `"+clazz.getSimpleName()+"`;";
					T object = null;
					try (Connection conn = getDataSource().getConnection();
						 PreparedStatement stmt = conn.prepareStatement(sql)) {

						stmt.executeUpdate();

					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				});
		return result;
	}
	
	public enum ReconstructionMethod {
		/** reconstruct value as the primitive type it was stored in the database as */
		NONE,
		/** use a string constructor from the object class to re-instantiate the value */
		CONSTRUCTOR,
		/** use common static method names in the object class to try to re-instantiate the value.<br>
		 * methods used in order are valueOf, fromString, parseString until the first one hits.<br>
		 * this method is most expensive, but most dynamic */
		FROMSTRING,
		/** this uses the specified H2Serializer to read/write the object into blob entries */
		SERIALIZER
	}

	private static boolean classSupportsFromConstructor(Class<?> clz) {
		try {
			clz.getConstructor(String.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	private static boolean classSupportsFromStringMethod(Class<?> clz) {
		return clz.isEnum() ||
				classHasMethod(clz, "valueOf", String.class) ||
				classHasMethod(clz, "fromString", String.class) ||
				classHasMethod(clz, "parseString", String.class);
	}
	private static boolean classHasMethod(Class<?> clazz, String name, Class<?>... args) {
		try {
			Method m = clazz.getMethod(name, args);
			return (m.getModifiers() & Modifier.STATIC) == Modifier.STATIC &&
					(m.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT;
		} catch (Exception e) {
			return false;
		}
	}
	private static Method getClassFromStringMethod(Class<?> clz) {
		Method m = null;
		try {
			m = clz.getMethod("valueOf", String.class);
		} catch (NoSuchMethodException e) {
			/**/
		}
		if (m == null) try {
			m = clz.getMethod("fromString", String.class);
		} catch (NoSuchMethodException e) {
			/**/
		}
		if (m == null) try {
			m = clz.getMethod("parseString", String.class);
		} catch (NoSuchMethodException e) {
			/**/
		}
		if (m == null)
			throw new RuntimeException("Can't find method for object reconstruction in class "+clz.getCanonicalName());
		return m;
	}
}
