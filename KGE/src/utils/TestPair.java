package utils;

public class TestPair implements Comparable<TestPair>
{
	public String entity;
	public float distance;
	public TestPair(String entity, float distance) {
		// TODO Auto-generated constructor stub
		this.entity = entity;
		this.distance = distance;
	}
	public int compareTo(TestPair o) {
		// TODO Auto-generated method stub
		if(this.distance < o.distance)
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}
	
}
