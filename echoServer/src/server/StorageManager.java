package ece419StorageManager;

public class StorageManager {
	public static void main(String[] args) {
        System.out.print("testing this shit kappa ");
        String string = "testing this string";
        int size = string.length();
        System.out.println(size);
        Cache d = new Cache(0,"testing","testing");
        CacheManager cacheManager = new CacheManager(1, 1000);
	}
}
