package utils;

public class PRPair implements Comparable<PRPair>{
	public double distance;
	public int flag;
	public PRPair(double distance, int flag)
	{
		this.distance = distance;
		this.flag = flag;
	}
	public int compareTo(PRPair o) {
		// TODO Auto-generated method stub
		if(this.distance < o.distance)
			return -1;
		else
			return 1;
	}
}
