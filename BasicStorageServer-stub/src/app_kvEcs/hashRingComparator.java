package app_kvEcs;

import java.util.Comparator;

public class hashRingComparator implements Comparator<ECSNode>{

	@Override
	public int compare(ECSNode c1, ECSNode c2) {
		if(c1.getHashedValue().compareTo(c2.getHashedValue()) < 0 ){
			return -1;
		}
		else if (c1.getHashedValue().compareTo(c2.getHashedValue()) > 1){
			return 1;
		}
		return 0;
	}
	
}
