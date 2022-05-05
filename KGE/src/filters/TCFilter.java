package filters;

import java.util.HashSet;

import utils.IO;

public class TCFilter {
//
//	public static final String TRAIN_FILE_PATH = "datasets/FB13/train.txt";
//	public static final String DEV_FILE_PATH = "datasets/FB13/dev.txt";
//	public static final String TEST_FILE_PATH = "datasets/FB13/test.txt";
//	
//	public static final String FILTER_DEV_FILE_PATH = "datasets/FB13/dev.filter.txt";
//	public static final String FILTER_TEST_FILE_PATH = "datasets/FB13/test.filter.txt";

	public static final String TRAIN_FILE_PATH = "datasets/WN11/train.txt";
	public static final String DEV_FILE_PATH = "datasets/WN11/dev.txt";
	public static final String TEST_FILE_PATH = "datasets/WN11/test.txt";
	
	public static final String FILTER_DEV_FILE_PATH = "datasets/WN11/dev.filter.txt";
	public static final String FILTER_TEST_FILE_PATH = "datasets/WN11/test.filter.txt";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		IO train_io = new IO(TRAIN_FILE_PATH, "r");
		HashSet<String> entitySet = new HashSet<String>();
		HashSet<String> relationSet = new HashSet<String>();
		while(train_io.readReady())
		{
			
			String line = train_io.readLine();
			String[] triplet = line.split("\t");
			entitySet.add(triplet[0]);
			entitySet.add(triplet[2]);
			relationSet.add(triplet[1]);
		}
		train_io.readClose();
		
		IO dev_io = new IO(DEV_FILE_PATH, "r");
		IO dev_filter_io = new IO(FILTER_DEV_FILE_PATH, "w");
		while(dev_io.readReady())
		{
			String posLine = dev_io.readLine();
			String negLine = dev_io.readLine();
			String[] posTriplet = posLine.split("\t");
			String[] negTriplet = negLine.split("\t");
			if(entitySet.contains(posTriplet[0]) && entitySet.contains(posTriplet[2]) && relationSet.contains(posTriplet[1]) && entitySet.contains(negTriplet[0]) && entitySet.contains(negTriplet[2]) && relationSet.contains(negTriplet[1]))
			{
				dev_filter_io.writeLine(posLine);
				dev_filter_io.writeLine(negLine);
			}
		}
		
		dev_filter_io.writeClose();
		dev_io.readClose();
		
		IO test_io = new IO(TEST_FILE_PATH, "r");
		IO test_filter_io = new IO(FILTER_TEST_FILE_PATH, "w");
		
		while(test_io.readReady())
		{
			String posLine = test_io.readLine();
			String negLine = test_io.readLine();
			String[] posTriplet = posLine.split("\t");
			String[] negTriplet = negLine.split("\t");
			if(entitySet.contains(posTriplet[0]) && entitySet.contains(posTriplet[2]) && relationSet.contains(posTriplet[1]) && entitySet.contains(negTriplet[0]) && entitySet.contains(negTriplet[2]) && relationSet.contains(negTriplet[1]))
			{
				test_filter_io.writeLine(posLine);
				test_filter_io.writeLine(negLine);
			}
		}
		
		test_filter_io.writeClose();
		test_io.readClose();
		
	}

}
