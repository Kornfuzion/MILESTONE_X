package datastore;

import java.util.Comparator;

public class LfuComparator implements Comparator<Cache>{

	@Override
	public int compare(Cache c1, Cache c2) {
		if(c1.getTimesUsed() < c2.getTimesUsed()){
			return -1;
		}
		else if (c1.getTimesUsed() > c2.getTimesUsed()){
			return 1;
		}
		return 0;
	}
	
}
