package utils;

public class DistanceFlagPair implements Comparable<DistanceFlagPair>
{
	public float distance;
	public int flag;
	
	public DistanceFlagPair(float distance, int flag)
	{
		this.distance = distance;
		this.flag = flag;
	}

	public int compareTo(DistanceFlagPair o) {
		// TODO Auto-generated method stub
		if(distance < o.distance)
		{
			return -1;
		}
		else if (distance > o.distance){
			return 1;
		}
		else {
			return 0;
		}
	}
}