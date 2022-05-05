package approaches;
import java.io.*;
import me.tongfei.progressbar.*;
import java.util.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import utils.DistanceFlagPair;
import utils.IO;
import utils.PRPair;
import utils.TCDevItem;
import utils.TestPair;

public class TransE {
	
	public String CONFIG_PATH;
		
	public String TRAIN_FILE_PATH;
	public String DEV_FILE_PATH;
	public String TEST_FILE_PATH;
	public String TC_DEV_FILE_PATH;
	public String TC_TEST_FILE_PATH;
		
	public int DIMENSION;
	public float MARGIN;
	public float STEP_SIZE;
	public int EPOCHES;
	public String NORM;
	
	public List<String[]> trainExamples;
	public List<String[]> devExamples;
	public List<String[]> testExamples;
	
	public List<String[]> tcDevExamples;
	public List<String[]> tcTestExamples;
	
	public List<String> entityList;
	
	public HashMap<String, float[]> bestEntityEmbeddings;
	public HashMap<String, float[]> bestRelationEmbeddings;
	
	public float minLoss;
	
	public HashMap<String, float[]> entityEmbeddings;
	public HashMap<String, float[]> relationEmbeddings;
	
	public HashSet<String> entitySet;
	public HashSet<String> relationSet;
	
	public HashSet<String> goldTriplets;
	
	public Random rand;
	
	public HashMap<String, TCDevItem> tcDevItems;
	public TransE(String configPath) throws Exception
	{	System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		/*NELL Config File*/
//		this.CONFIG_PATH = "NELL_Config.properties";
//		/*FB15K Config File*/
//		this.CONFIG_PATH = "FB15K_Config.properties";
		
		/*WN18 Config File*/
		//this.CONFIG_PATH = "WN18_Config.properties";
		
		/*WN11 Config File*/
		//this.CONFIG_PATH = "WN11_Config.properties";
		
		/*FB13 Config File*/
		//this.CONFIG_PATH = "FB13_Config.properties";
	
		/*DBpedia Config File*/
//		this.CONFIG_PATH = "DBpedia_Config.properties";
	
		this.CONFIG_PATH = configPath;
	
		File file = new File(CONFIG_PATH);
		Scanner scan = new Scanner(file);
		while(scan.hasNext())
		{
			System.out.println(scan.nextLine());
		}	
		
		rand = new Random();
		
		trainExamples = new ArrayList<String[]>();
		devExamples = new ArrayList<String[]>();
		testExamples = new ArrayList<String[]>();
		
		tcDevExamples = new ArrayList<String[]>();
		tcTestExamples = new ArrayList<String[]>();
		
		entityEmbeddings = new HashMap<String, float[]>();
		relationEmbeddings = new HashMap<String, float[]>();
		
		bestEntityEmbeddings = new HashMap<String, float[]>();
		bestRelationEmbeddings = new HashMap<String, float[]>();
		
		minLoss = Float.MAX_VALUE;
		
		entitySet = new HashSet<String>();
		relationSet = new HashSet<String>();
		
		goldTriplets = new HashSet<String>();
		
		entityList = new ArrayList<String>();
		tcDevItems = new HashMap<String, TCDevItem>();
		
		InputStream in = new FileInputStream(this.CONFIG_PATH);
		Properties prop = new Properties();
		prop.load(in);
		
		this.TRAIN_FILE_PATH = prop.getProperty("TRAIN_FILE_PATH");
		this.DEV_FILE_PATH = prop.getProperty("DEV_FILE_PATH");
		this.TEST_FILE_PATH = prop.getProperty("TEST_FILE_PATH");
		
		this.TC_DEV_FILE_PATH = prop.getProperty("TC_DEV_FILE_PATH");
		this.TC_TEST_FILE_PATH = prop.getProperty("TC_TEST_FILE_PATH");
		
		this.DIMENSION = Integer.parseInt(prop.getProperty("DIMENSION"));
		this.EPOCHES = Integer.parseInt(prop.getProperty("EPOCHES"));
		this.MARGIN = Float.parseFloat(prop.getProperty("MARGIN"));
		this.STEP_SIZE = Float.parseFloat(prop.getProperty("STEP_SIZE"));
		this.NORM = prop.getProperty("NORM");
			
	}
	
	public void loadTrain() throws Exception
	{
		IO io = new IO(this.TRAIN_FILE_PATH, "r");

		while(io.readReady())
		{
			String line = io.readLine();
			goldTriplets.add(line);
			String[] triplet = line.split("\t");
			
			String headEntity = triplet[0];
			String relation = triplet[1];
			String tailEntity = triplet[2];
			
			entitySet.add(headEntity);
			entitySet.add(tailEntity);
					
			relationSet.add(relation);
			
			trainExamples.add(triplet);
		}
		entityList.addAll(entitySet);		
	}
	
	public void loadDev() throws Exception
	{
		IO io = new IO(this.DEV_FILE_PATH, "r");

		while(io.readReady())
		{
			String line = io.readLine();
			String[] triplet = line.split("\t");			
			devExamples.add(triplet);
		}
	}
	
	public void loadTCDev() throws Exception
	{
		IO io = new IO(this.TC_DEV_FILE_PATH, "r");
		while(io.readReady())
		{
			String line = io.readLine();
			String[] tuple = line.split("\t");
			if(tuple[3].equals("1"))
				goldTriplets.add(tuple[0] + "\t" + tuple[1] + "\t" + tuple[2]);
			tcDevExamples.add(tuple);
		}
	}
	
	public void loadTest() throws Exception
	{
		IO io = new IO(this.TEST_FILE_PATH, "r");

		while(io.readReady())
		{
			String line = io.readLine();
			String[] triplet = line.split("\t");			
			testExamples.add(triplet);
		}
	}
	
	public void init()
	{
		Iterator<String> entityIt = entitySet.iterator();
		while(entityIt.hasNext())
		{
			String entityKey = entityIt.next();
			
			float[] entityEmb = initEmb(); 
			entityEmbeddings.put(entityKey, entityEmb);

		}
		
		Iterator<String> relationIt = relationSet.iterator();
		
		while(relationIt.hasNext())
		{
			String relationKey = relationIt.next();
			
			float[] relationEmb = initEmb();
			normEmb(relationEmb);
			relationEmbeddings.put(relationKey, relationEmb);
		}
		
		bestEntityEmbeddings.clear();
		bestEntityEmbeddings.putAll(entityEmbeddings);
		bestRelationEmbeddings.clear();
		bestRelationEmbeddings.putAll(relationEmbeddings);
	}
	
	public float[] initEmb()
	{
		float[] embedding = new float[DIMENSION];
		for(int i = 0; i < DIMENSION; i++)
		{
			embedding[i] = (float) ((rand.nextFloat() - 0.5) / 0.5 * 6.0 / Math.sqrt(DIMENSION * 1.0));
		}
		return embedding;
	}
	
	public void normEmb(float[] emb)
	{
		float mode = 0.0f;
		for(int i = 0; i < DIMENSION; i++)
		{
			mode += emb[i] * emb[i];
		}
		mode = (float)Math.sqrt(mode);
		for(int i = 0; i < DIMENSION; i++)
			emb[i] /= mode;
	}
	
	public void train()
	{
		for(int i = 0; i < EPOCHES; i++)
		{
			//float totalLoss = 0.0f;
			Iterator<String> entityIt = entitySet.iterator();
			while(entityIt.hasNext())
			{
				String entityKey = entityIt.next();

				float[] entityEmb = entityEmbeddings.get(entityKey);
				normEmb(entityEmb);
			}
			
			Collections.shuffle(trainExamples);
			
			for(int j = 0; j < trainExamples.size(); j++)
			{
				String[] posTriplet = trainExamples.get(j);
				String[] negTriplet = getNegTriplet(posTriplet);
				
				String posHeadEntity = posTriplet[0];
				String posTailEntity = posTriplet[2];
				String negHeadEntity = negTriplet[0];
				String negTailEntity = negTriplet[2];
				String relation = posTriplet[1];
				
				float[] posHeadEmb = entityEmbeddings.get(posHeadEntity);
				float[] posTailEmb = entityEmbeddings.get(posTailEntity);
				
				float[] negHeadEmb = entityEmbeddings.get(negHeadEntity);
				float[] negTailEmb = entityEmbeddings.get(negTailEntity);
				
				float[] relationEmb = relationEmbeddings.get(relation);
				
				float[] posDistanceEmb = getDistanceEmb(posHeadEmb, relationEmb, posTailEmb);
				float[] negDistanceEmb = getDistanceEmb(negHeadEmb, relationEmb, negTailEmb);
				
				float posDistance = norm(posDistanceEmb);
				float negDistance = norm(negDistanceEmb);
				
				if(MARGIN + posDistance - negDistance > 0.0f)
				{
					float[] posGradientEmb = getGradientEmb(posDistanceEmb);
					float[] negGradientEmb = getGradientEmb(negDistanceEmb);
					
					float[] posUpdatedGradientEmb = embCalculator(posGradientEmb, "*", STEP_SIZE);
					float[] negUpdatedGradientEmb = embCalculator(negGradientEmb, "*", STEP_SIZE);
					
					float[] tmpEmb = entityEmbeddings.get(posHeadEntity);
					entityEmbeddings.put(posHeadEntity, embCalculator(tmpEmb, "-", posUpdatedGradientEmb));
					
					tmpEmb = entityEmbeddings.get(posTailEntity);
					entityEmbeddings.put(posTailEntity, embCalculator(tmpEmb, "+", posUpdatedGradientEmb));
					
					tmpEmb = entityEmbeddings.get(negHeadEntity);
					entityEmbeddings.put(negHeadEntity, embCalculator(tmpEmb, "+", negUpdatedGradientEmb));
					
					tmpEmb = entityEmbeddings.get(negTailEntity);
					entityEmbeddings.put(negTailEntity, embCalculator(tmpEmb, "-", negUpdatedGradientEmb));
					
					tmpEmb = relationEmbeddings.get(relation);
					relationEmbeddings.put(relation, embCalculator(tmpEmb, "-", embCalculator(posUpdatedGradientEmb, "-", negUpdatedGradientEmb)));
				}
			}
			
			float devAvgLoss = dev();
			
			if(devAvgLoss < minLoss)
			{
				minLoss = devAvgLoss;
				bestEntityEmbeddings.clear();
				bestEntityEmbeddings.putAll(entityEmbeddings);
				bestRelationEmbeddings.clear();
				bestRelationEmbeddings.putAll(relationEmbeddings);
				
				System.out.println("MIN AVG LOSS@" + i + ": " + minLoss);				
			}
		}
		System.out.println("train completed");
	}
	public String[] getNegTriplet(String[] posTriplet)
	{
		String[] negTriplet = new String[3];
		
		if(rand.nextFloat() < 0.5)
		{
			/*Replacing Head*/
			String corruptedHeadEntity = posTriplet[0];
			while(corruptedHeadEntity.equals(posTriplet[0]))
			{
				int corruptedHeadIdx = rand.nextInt(entityList.size());
				corruptedHeadEntity = entityList.get(corruptedHeadIdx);
			}
			negTriplet[0] = corruptedHeadEntity;
			negTriplet[1] = posTriplet[1];
			negTriplet[2] = posTriplet[2];
		}
		else {
			/*Replacing Tail*/
			String corruptedTailEntity = posTriplet[2];
			while(corruptedTailEntity.equals(posTriplet[2]))
			{
				int corruptedTailIdx = rand.nextInt(entityList.size());
				corruptedTailEntity = entityList.get(corruptedTailIdx);
			}
			negTriplet[0] = posTriplet[0];
			negTriplet[1] = posTriplet[1];
			negTriplet[2] = corruptedTailEntity;
		}
		return negTriplet;
	}
	public float dev()
	{
		float totalLoss = 0.0f;
		for(int i = 0; i < tcDevExamples.size(); i+=2)
		{
			String[] posTuple = tcDevExamples.get(i);
			String[] negTuple = tcDevExamples.get(i + 1);
			//displayTuple(posTuple);

			float[] posDistanceEmb = getDistanceEmb(posTuple[0], posTuple[1], posTuple[2]);
			float posDistance = norm(posDistanceEmb);
			
			//displayTuple(negTuple);
			float[] negDistanceEmb = getDistanceEmb(negTuple[0], negTuple[1], negTuple[2]);
			float negDistance = norm(negDistanceEmb);
			
			float loss = MARGIN + posDistance - negDistance;
			if(loss > 0.0f)
			{
				totalLoss += loss;
			}	
		}
		return totalLoss / tcDevExamples.size();
	}
	
	public void test()
	{
		long totalRawHeadRank = 0L;
		long totalRawTailRank = 0L;
		long totalFilterHeadRank = 0L;
		long totalFilterTailRank = 0L;
		
		long totalRawHeadHit10 = 0L;
		long totalRawTailHit10 = 0L;
		long totalRawHeadHit1 = 0L;
		long totalRawTailHit1 = 0L;
		
		long totalFilterHeadHit10 = 0L;
		long totalFilterTailHit10 = 0L;
		long totalFilterHeadHit1 = 0L;
		long totalFilterTailHit1 = 0L;
		
		List<TestPair> rawTailList = new ArrayList<TestPair>();
		List<TestPair> rawHeadList = new ArrayList<TestPair>();
		List<TestPair> filterTailList = new ArrayList<TestPair>();
		List<TestPair> filterHeadList = new ArrayList<TestPair>();
		
		HashSet<String> testEntitySet = new HashSet<String>();
		for(int i = 0; i < testExamples.size(); i++)
		{
			String headEntity = testExamples.get(i)[0];
			String relation = testExamples.get(i)[1];
			String tailEntity = testExamples.get(i)[2];
			testEntitySet.add(headEntity);
			testEntitySet.add(tailEntity);
		}
		//System.out.println("Test Entity Set Size: " + testEntitySet.size());
		ProgressBarBuilder pbb = new ProgressBarBuilder().setStyle(ProgressBarStyle.ASCII).setInitialMax(testExamples.size()).setTaskName("Examples 1");
		try (ProgressBar pb = pbb.build()) {
			pb.step();
			for(int i = 0; i < testExamples.size(); i++)
			{			
				String headEntity = testExamples.get(i)[0];
				String relation = testExamples.get(i)[1];
				String tailEntity = testExamples.get(i)[2];
				
				float[] bestHeadEntityEmb = bestEntityEmbeddings.get(headEntity);
				float[] bestRelationEmb = bestRelationEmbeddings.get(relation);
				float[] bestTailEntityEmb = bestEntityEmbeddings.get(tailEntity);
				
				rawTailList.clear();
				filterTailList.clear();
				/*Replace Tail Entity*/
				Iterator<String> entityIt = entitySet.iterator();
				
				//Iterator<String> entityIt = testEntitySet.iterator();
				System.out.println(i + "/" + testExamples.size() + " loop1");
				while(entityIt.hasNext())
				{
					String corruptedTailEntity = entityIt.next();
					float[] corruptedTailEntityEmb = bestEntityEmbeddings.get(corruptedTailEntity);
					float distance = norm(getDistanceEmb(bestHeadEntityEmb, bestRelationEmb, corruptedTailEntityEmb));
					rawTailList.add(new TestPair(corruptedTailEntity, distance));
					
					if(!goldTriplets.contains(headEntity + "\t" + relation + "\t" + corruptedTailEntity))
					{
						filterTailList.add(new TestPair(corruptedTailEntity, distance));
					}
				}
				Collections.sort(rawTailList);
				Collections.sort(filterTailList);
				System.out.println("loop2");
				for(int j = 1; j <= rawTailList.size(); j++)
				{
					if(rawTailList.get(j - 1).entity.equals(tailEntity))
					{
						totalRawTailRank += j;
						if(j <= 10)
							totalRawTailHit10++;
						if(j == 1)
							totalRawTailHit1 ++;
						break;
					}
				}
				System.out.println("loop3");
				for(int j = 1; j <= filterTailList.size(); j++)
				{
					if(filterTailList.get(j - 1).entity.equals(tailEntity))
					{
						totalFilterTailRank += j;
						if(j <= 10)
							totalFilterTailHit10++;
						if(j == 1)
							totalFilterTailHit1++;
						break;
					}
				}
				
				rawHeadList.clear();
				filterHeadList.clear();
				
				/*Replace Tail Entity*/
				entityIt = entitySet.iterator();
				//entityIt = testEntitySet.iterator();
				System.out.println("loop4");
				while(entityIt.hasNext())
				{
					String corruptedHeadEntity = entityIt.next();	
					float[] corruptedHeadEntityEmb = bestEntityEmbeddings.get(corruptedHeadEntity);
					float distance = norm(getDistanceEmb(corruptedHeadEntityEmb, bestRelationEmb, bestTailEntityEmb));
					rawHeadList.add(new TestPair(corruptedHeadEntity, distance));
					if(!goldTriplets.contains(corruptedHeadEntity + "\t" + relation + "\t" + tailEntity))
					{
						filterHeadList.add(new TestPair(corruptedHeadEntity, distance));
					}
				}
				Collections.sort(rawHeadList);
				Collections.sort(filterHeadList);
				System.out.println("loop5");
				for(int j = 1; j <= rawHeadList.size(); j++)
				{
					if(rawHeadList.get(j - 1).entity.equals(headEntity))
					{
						totalRawHeadRank += j;
						if(j <= 10)
							totalRawHeadHit10++;
						if(j == 1)
							totalRawHeadHit1++;
						break;
					}
				}
				System.out.println("loop6");
				for(int j = 1; j <= filterHeadList.size(); j++)
				{
					if(filterHeadList.get(j - 1).entity.equals(headEntity))
					{
						totalFilterHeadRank += j;
						if(j <= 10)
							totalFilterHeadHit10++;
						if(j == 1)
							totalFilterHeadHit1++;
						break;
					}
				}
			}
		}
		

		System.out.println("****************************");
		System.out.println("TRANSE");
		System.out.println(TRAIN_FILE_PATH);
		System.out.println(CONFIG_PATH);
		System.out.println("RAW_RANK: " + (totalRawHeadRank + totalRawTailRank) * 1.0 / (2 * testExamples.size()));
		System.out.println("FILTER_RANK: " + (totalFilterHeadRank + totalFilterTailRank) * 1.0 / (2 * testExamples.size()));
		System.out.println("RAW_HIT@10: " + (totalRawHeadHit10 + totalRawTailHit10) * 1.0 / (2 * testExamples.size()));
		System.out.println("FILTER_HIT@10: " + (totalFilterHeadHit10 + totalFilterTailHit10) * 1.0 / (2 * testExamples.size()));
		System.out.println("RAW_HIT@1: " + (totalRawHeadHit1 + totalRawTailHit1) * 1.0 / (2 * testExamples.size()));
		System.out.println("FILTER_HIT@1: " + (totalFilterHeadHit1 + totalFilterTailHit1) * 1.0 / (2 * testExamples.size()));
	}
	
	public void displayTuple(String[] tuple)
	{
		for(int i = 0; i < tuple.length; i++)
		{
			System.out.print(tuple[i] + ",");
		}
		System.out.println();
	}
	
	public void displayEmb(float[] embedding)
	{
		for(int i = 0; i < DIMENSION; i++)
		{
			System.out.print(embedding[i] + ",");
		}
		System.out.println();
	}
	
	public float norm(float[] embedding)
	{
		float mode = 0.0f;
		
		if(NORM.equals("L1"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				mode += Math.abs(embedding[i]);
			}
			
		}
		else if(NORM.equals("L2"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				mode += embedding[i] * embedding[i];
			}
			//mode = (float)Math.sqrt(mode);
		}
		else {
			
		}
	
		return mode;
	}
	
	public float[] embCalculator(float[] firstEmb, String operator, float[] secondEmb)
	{
		float[] resultEmb = new float[DIMENSION];
		
		if(operator.equals("+"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				resultEmb[i] = firstEmb[i] + secondEmb[i];
			}
		}
		
		else if (operator.equals("-"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				resultEmb[i] = firstEmb[i] - secondEmb[i];
			}
		}
		else
		{
			
		}
		return resultEmb;
			
	}
	
	public float[] embCalculator(float[] firstEmb, String operator, float second)
	{
		float[] resultEmb = new float[DIMENSION];
		
		if(operator.equals("*"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				resultEmb[i] = firstEmb[i] * second;
			}
		}
		
		else if (operator.equals("/"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				resultEmb[i] = firstEmb[i] / second;
			}
		}
		else
		{
			
		}
		return resultEmb;
	}
	
	public float[] getDistanceEmb(String headEntity, String relation, String tailEntity)
	{
		float[] headEmb = entityEmbeddings.get(headEntity);
		float[] relationEmb = relationEmbeddings.get(relation);
		float[] tailEmb = entityEmbeddings.get(tailEntity);
		
		return embCalculator(embCalculator(headEmb, "+", relationEmb), "-", tailEmb);
	}
	
	public float[] getGradientEmb(float[] distanceEmb)
	{
		float[] gradientEmb = new float[DIMENSION];
		if(NORM.equals("L1"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				if(distanceEmb[i] > 0.0f)
					gradientEmb[i] = 1.0f;
				else 
					gradientEmb[i] = -1.0f;
			}
		}
		else if(NORM.equals("L2"))
		{
			for(int i = 0; i < DIMENSION; i++)
			{
				gradientEmb[i] = 2.0f * distanceEmb[i];
			}
		}
		return gradientEmb;
	}
	
	public float[] getDistanceEmb(float[] headEmb, float[] relationEmb, float[] tailEmb)
	{
		return embCalculator(embCalculator(headEmb, "+", relationEmb), "-", tailEmb);
	}
	
	public void tcThreshold() throws Exception
	{
		for(int i = 0; i < tcDevExamples.size(); i++)
		{
			String[] tuple = tcDevExamples.get(i);
			if(!tcDevItems.containsKey(tuple[1]))
			{
				tcDevItems.put(tuple[1], new TCDevItem());
			}
			TCDevItem tcDevItem = tcDevItems.get(tuple[1]);
			
			float distance = norm(getBestDistanceEmb(tuple[0], tuple[1], tuple[2]));
			int flag = Integer.parseInt(tuple[3]);
			tcDevItem.distanceFlagList.add(new DistanceFlagPair(distance, flag));
		}
		Iterator<String> it = tcDevItems.keySet().iterator();
		
		while(it.hasNext())
		{
			String key = it.next();
			tcDevItems.get(key).getThreshold();
			//System.out.println(tcDevItems.get(key).distanceFlagList.size());

			
		}
	}
	
	public void loadTCTest() throws Exception
	{
		IO io = new IO(this.TC_TEST_FILE_PATH, "r");

		while(io.readReady())
		{
			String line = io.readLine();
			String[] tuple = line.split("\t");
			tcTestExamples.add(tuple);
		}
	}
	
	public float tcTestAcc()
	{
		int correctNum = 0;
		int totalNum = 0;
		for(int i = 0; i < tcTestExamples.size(); i++)
		{
			String[] tuple = tcTestExamples.get(i);
			if(tcDevItems.containsKey(tuple[1]))
			{
				float threshold = tcDevItems.get(tuple[1]).threshold;
				float distance = norm(getBestDistanceEmb(tuple[0], tuple[1], tuple[2]));
				int flag = Integer.parseInt(tuple[3]);
				
				if(distance <= threshold && flag == 1)
				{
					correctNum ++;
				}
				else if(distance > threshold && flag == -1)
				{
					correctNum ++;
				}
				totalNum ++;
			}
		}
		
		
		return correctNum * 1.0f / totalNum;
	}
	
	public void tcTestPR()
	{
		ArrayList<PRPair> testPRList = new ArrayList<PRPair>();
		for(int i = 0; i < tcTestExamples.size(); i++)
		{
			String[] tuple = tcTestExamples.get(i);
			float distance = norm(getBestDistanceEmb(tuple[0], tuple[1], tuple[2]));
			testPRList.add(new PRPair(distance, Integer.parseInt(tuple[3])));
		}
		Collections.sort(testPRList);
		double correctCount = 0.0;
		for(int i = 0; i < testPRList.size(); i++)
		{
			if(testPRList.get(i).flag == 1)
			{
				correctCount ++;
			}
			if((i+1) % (testPRList.size() / 11) == 0)
			{
				System.out.println(correctCount / (i+1) + "\t" + correctCount / (testPRList.size() / 2));
			}
		}
	}
	
	public float[] getBestDistanceEmb(String headEntity, String relation, String tailEntity)
	{
		float[] headEmb = bestEntityEmbeddings.get(headEntity);
		float[] relationEmb = bestRelationEmbeddings.get(relation);
		float[] tailEmb = bestEntityEmbeddings.get(tailEntity);
		
		return embCalculator(embCalculator(headEmb, "+", relationEmb), "-", tailEmb);
	}
	
	public static void writeEmbeddings(String filename, HashMap<String, float[]> embeddings) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename))))) {
			for (var i: embeddings.entrySet()) {
				writer.write(i.getKey() + " ");
				var value = i.getValue();
				for (int j = 0; j < value.length - 1; j++) {
					writer.write(Float.toString(value[j]) + " ");
				}
				if (value.length > 0) {
					writer.write(Float.toString(value[value.length-1]));
				}
				writer.newLine();
			}
		} catch (IOException e) {
			throw e;
		}
	}
	
	public static void main(String[] args) throws Exception
	{
	    Options options = new Options();
	    
	    Option config = new Option("c", "config", true, "config file input");
        config.setRequired(true);
        options.addOption(config);
        
	    Option output = new Option("O", "output", true, "output directory");
        output.setRequired(true);
        options.addOption(output);
	    

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        
		TransE transe = new TransE(cmd.getOptionValue("config"));

		System.out.println("load");
		/*Train and Dev*/
		transe.loadTrain();	
		//transe.loadDev();
		transe.loadTCDev();
		System.out.println("init");
		transe.init();
		System.out.println("train");
		transe.train();
		
		System.out.println("test");
		/*Test*/
		System.out.println("load test");
		transe.loadTest();
		System.out.println("perform test");
//		transe.test();		
		System.out.println("TC test");
		/*TC Test*/
		transe.tcThreshold();
		transe.loadTCTest();
		float accuracy = transe.tcTestAcc();
		System.out.println(accuracy);
		transe.tcTestPR();
//		// Write results?
		String outDir = cmd.getOptionValue("output");
		writeEmbeddings(outDir + "/TransE_entity_embeddings.txt", transe.bestEntityEmbeddings);
		writeEmbeddings(outDir + "/TransE_relation_embeddings.txt", transe.bestRelationEmbeddings);
//		int i = 0;
//		for (String[] tuple: transe.tcDevExamples) {
//			i++;
////			float[] hemb = transe.bestEntityEmbeddings.get(example[0]);
////			float[] remb = transe.bestRelationEmbeddings.get(example[1]);
////			float[] temb = transe.bestRelationEmbeddings.get(example[2]);
//			System.out.println(tuple[1] + " " + tuple[2] + " " + tuple[3] + " " + transe.norm(transe.getBestDistanceEmb(tuple[0], tuple[1], tuple[2])));
////			System.out.println();
//			if (i > 20)
//				break;
//		}
//		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("TransE_relation_embeddings.txt"))))) {
//			for (var i: transe.bestRelationEmbeddings.entrySet()) {
//				writer.write(i.getKey() + " ");
//				var value = i.getValue();
//				for (int j = 0; j < value.length - 1; j++) {
//					writer.write(Float.toString(value[j]) + " ");
//				}
//				if (value.length > 0) {
//					writer.write(Float.toString(value[value.length-1]));
//				}
//				writer.newLine();
//			}
//		} catch (IOException e) {
//			throw e;
//		}
	}
	
}

