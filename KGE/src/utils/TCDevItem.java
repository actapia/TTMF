package utils;

import java.util.*;

public class TCDevItem {
	public float threshold = 0.0f;
	
	public List<DistanceFlagPair> distanceFlagList;
	
	public TCDevItem()
	{
		distanceFlagList = new ArrayList<DistanceFlagPair>();
	}
	
	public void getThreshold()
	{
		Collections.sort(distanceFlagList);
		threshold = distanceFlagList.get(0).distance - 0.01f;
		int maxValue = 0;
		int currentValue = 0;
		
		for(int i = 1; i < distanceFlagList.size(); i++)
		{
			if(distanceFlagList.get(i - 1).flag == 1)
			{
				currentValue ++;
			}
			else {
				currentValue --;
			}
			if(currentValue > maxValue)
			{
				threshold = (distanceFlagList.get(i).distance + distanceFlagList.get(i - 1).distance) / 2.0f;
				maxValue = currentValue;
			}
		}
		
	}
}


