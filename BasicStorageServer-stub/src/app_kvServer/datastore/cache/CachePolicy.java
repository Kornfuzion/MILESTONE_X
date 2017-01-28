package cache;

/*
 * Simple enum class for parsing the string form of the policy into the enum
 * Enumerates the types of cache replacement policies
 */
public enum CachePolicy {
    LRU,
    LFU,
    FIFO;
    
    public static CachePolicy parseString(String policy) {
        switch (policy) {
            case "LRU":
                return LRU;
		    case "LFU":
                return LFU;
		    case "FIFO":
                return FIFO;
            default:
                return LRU;
        }
    }
}
