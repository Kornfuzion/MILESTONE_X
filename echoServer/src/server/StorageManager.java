package ece419StorageManager; //TODO WE change this later LOIS!

public class StorageManager {
	public static void main(String[] args) {
        System.out.print("testing this shit kappa ");
        String string = "testing this string";
        int size = string.length();
        System.out.println(size);
        Cache d = new Cache(0,"testing","testing");
        CacheManager cacheManager = new CacheManager(1, 1000);
        CacheManager cacheManager2 = new CacheManager(2, 1000);
        CacheManager cacheManager3 = new CacheManager(3, 1000);
	}
}
