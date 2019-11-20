package cn.inxiny.live.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JsonUtils {
	private static ObjectMapper objectMapper = new ObjectMapper();

	public static final String DEFAULT_JSON = "{}";
	public static final String DEFAULT_JSON_ARRAY = "[{}]";

	/**
	 * 设置日期格式
	 * 
	 * @param dateFormat
	 * @return
	 */
	private static ObjectMapper formatObjectMapper(String dateFormat) {
		DateFormat format = new SimpleDateFormat(dateFormat);
		return objectMapper.setDateFormat(format);
	}

	/**
	 * 获取List的JavaType
	 * 
	 * @param collectionClass
	 * @param objClass
	 * @return
	 */
	private static JavaType getCollectionType(Class<? extends Collection> collectionClass, Class<?> objClass) {
		return objectMapper.getTypeFactory().constructCollectionType(collectionClass, objClass);
	}

	/**
	 * 获取Map的JavaType
	 * @return
	 */
	private static JavaType getMapType(Class<?> mapClass, Class<?> keyClass, Class<?> valueClass) {
		return objectMapper.getTypeFactory().constructMapLikeType(mapClass, keyClass, valueClass);
	}
    
	/**
	 * 对象转json字符串
	 * 
	 * @param object
	 * @return
	 * @throws IOException
	 */
	public static String obj2Json(Object object) {
		String res = null;
		try {
			res = objectMapper.writeValueAsString(object);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return res;
	}

	/**
	 * 对象转字符串,设置日期格式
	 * 
	 * @param object
	 * @param dateFormat
	 *            <yyyy-mm-dd HH:MM:DD>
	 * @return
	 * @throws IOException
	 */
	public static String objWithDate2Json(Object object, String dateFormat) throws IOException {
		objectMapper = formatObjectMapper(dateFormat);
		return objectMapper.writeValueAsString(object);
	}

	/**
	 * List转字符串,设置日期格式
	 *
	 * @param list
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static String list2Json(List<?> list) throws JsonProcessingException {
		try {
			return objectMapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw e;
		}
	}

	/**
	 * List转字符串,设置日期格式
	 * 
	 * @param list
	 * @param dateFormat
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static String listWithDate2Json(List<?> list, String dateFormat) throws JsonProcessingException {
		objectMapper = formatObjectMapper(dateFormat);
		try {
			return objectMapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw e;
		}
	}

	/**
	 * Map转转字符串
	 * 
	 * @param map
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String map2Json(Map<?, ?> map) {
		String result = null;
		try {
			result = objectMapper.writeValueAsString(map);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return result;
	}

	/**
	 * Map转字符串,设置日期格式
	 * 
	 * @param map
	 * @param dateFormat
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String mapWithDate2Json(Map<?, ?> map, String dateFormat) throws JsonProcessingException {
		objectMapper = formatObjectMapper(dateFormat);
		return objectMapper.writeValueAsString(map);
	}

	/**
	 * JSON字符串转换为对象
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public static <T> T readJson2Object(String json, Class<T> objClass)
			throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json, objClass);
	}

	/**
	 * JSON字符串转换为对象
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public static <T> T readJson2ObjectFormat(String json, Class<T> objClass, String dateFormat)
			throws JsonParseException, JsonMappingException, IOException {
		// 设置日期格式
		SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
		objectMapper.setDateFormat(fmt);
	    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return readJson2Object(json, objClass);
	}

	/**
	 * JSON字符串转换为List
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public static List<?> readJson2List(String json, Class<? extends Collection> collectionClass, Class<?> objClass)
			throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json, getCollectionType(collectionClass, objClass));
	}

	/**
	 * JSON字符串转换为List
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public static List<?> readJson2ListFormat(String json, Class<? extends Collection> collectionClass,
			Class<?> objClass, String dateFormat) throws JsonParseException, JsonMappingException, IOException {
		// 设置日期格式
		SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
		objectMapper.setDateFormat(fmt);
		return readJson2List(json, collectionClass, objClass);
	}

	/**
	 * JSON字符串转换为对象
	 * 
	 * @param <K>
	 * @param <V>
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws Exception
	 */
	public static <K, V> Map<K, V> readJson2Map(String json, @SuppressWarnings("rawtypes") Class<Map> mapClass,
			Class<K> keyClass, Class<V> valueClass)
			throws IllegalArgumentException, JsonParseException, JsonMappingException, IOException {
		if (mapClass.isAssignableFrom(Map.class)) {
			return objectMapper.readValue(json, getMapType(mapClass, keyClass, valueClass));
		} else {
			throw new IllegalArgumentException("参数类型异常");
		}
	}

	/**
	 * JSON字符串转换为对象
	 * 
	 * @param <K>
	 * @param <V>
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws Exception
	 */
	public static <K, V> Map<K, V> readJson2MapFormat(String json, @SuppressWarnings("rawtypes") Class<Map> mapClass,
			Class<K> keyClass, Class<V> valueClass, String dateFormat)
			throws JsonParseException, JsonMappingException, IllegalArgumentException, IOException {
		// 设置日期格式
		SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
		objectMapper.setDateFormat(fmt);
		return readJson2Map(json, mapClass, keyClass, valueClass);
	}

}
