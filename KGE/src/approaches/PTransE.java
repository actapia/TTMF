package approaches;
import java.io.*;
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
import utils.TCDevItem;
import utils.TestPair;

public class PTransE {
	
	public static int NEG_SAMPLE_COUNT = 15;
	public static float BIAS = 7.0f;
	
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
	public List<String> headEntityList;
	public List<String> tailEntityList;
	public List<String> relationList;
	
	public HashMap<String, float[]> bestEntityEmbeddings;
	public HashMap<String, float[]> bestRelationEmbeddings;
	
	public float maxLoss;
	
	public HashMap<String, float[]> entityEmbeddings;
	public HashMap<String, float[]> relationEmbeddings;
	
	public HashSet<String> entitySet;
	public HashSet<String> headEntitySet;
	public HashSet<String> tailEntitySet;	
	public HashSet<String> relationSet;
	
	public HashSet<String> goldTriplets;
	
	public Random rand;
	
	public HashMap<String, TCDevItem> tcDevItems;
	public PTransE(String configPath) throws Exception
	{	
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
		
		maxLoss = Float.NEGATIVE_INFINITY;
		
		entitySet = new HashSet<String>();
		headEntitySet = new HashSet<String>();
		tailEntitySet = new HashSet<String>();
		
		relationSet = new HashSet<String>();
	
		goldTriplets = new HashSet<String>();
		
		entityList = new ArrayList<String>();
		headEntityList = new ArrayList<String>();
		tailEntityList = new ArrayList<String>();
		relationList = new ArrayList<String>();
		
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
			//System.out.println(line);
			goldTriplets.add(line);
			
			String[] triplet = line.split("\t");
			
			String headEntity = triplet[0];
			String relation = triplet[1];
			String tailEntity = triplet[2];
			
			entitySet.add(headEntity);
			headEntitySet.add(headEntity);
			
			entitySet.add(tailEntity);
			tailEntitySet.add(tailEntity);
					
			relationSet.add(relation);
			
			trainExamples.add(triplet);
		}
		entityList.addAll(entitySet);
		headEntityList.addAll(headEntitySet);
		tailEntityList.addAll(tailEntitySet);
		relationList.addAll(relationSet);
		
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
	public float logisticFunction(float param)
	{
		return (float) (1.0/(1.0 + Math.exp(-param)));
	}
	
	public void updateGradient(HashMap<String, float[]> gradientEmbHashMap, String key, float[] gradientEmb)
	{
		if(gradientEmbHashMap.containsKey(key))
		{
			float[] tmpEmb = gradientEmbHashMap.get(key); 
			gradientEmbHashMap.put(key, embCalculator(tmpEmb, "+", gradientEmb));
		}
		else {
			float[] tmpEmb = new float[DIMENSION];
			for(int i = 0; i < tmpEmb.length; i++)
				tmpEmb[i] = 0.0f;
			gradientEmbHashMap.put(key, embCalculator(tmpEmb, "+", gradientEmb));
		}
	}
	
	public void train()
	{
		for(int i = 0; i < EPOCHES; i++)
		{				
			Collections.shuffle(trainExamples);
			float totalSum = 0.0f;
			
			for(int j = 0; j < trainExamples.size(); j++)
			{
				HashMap<String, float[]> gradientEntityEmbHashMap = new HashMap<String, float[]>();
				HashMap<String, float[]> gradientRelationEmbHashMap = new HashMap<String, float[]>();
				
				String[] posTriplet = trainExamples.get(j);
				
				String posHeadEntity = posTriplet[0];
				String posTailEntity = posTriplet[2];
				String posRelation = posTriplet[1];
				
				float[] posHeadEmb = entityEmbeddings.get(posHeadEntity);
				
				float[] posTailEmb = entityEmbeddings.get(posTailEntity);
				
				float[] posRelationEmb = relationEmbeddings.get(posRelation);
				
				float[] posDistanceEmb = getDistanceEmb(posHeadEmb, posRelationEmb, posTailEmb); //h + r - t
				
				float posDistance = norm(posDistanceEmb); //让他小
				float posLogisticDistance = logisticFunction(BIAS - 0.5f * posDistance);
				//System.out.println("POS: " + posDistance);
				//System.out.println(Math.log(posLogisticDistance));
				totalSum += Math.log(posLogisticDistance);
				
				float posGradientParam = 1.0f - posLogisticDistance;	
					
				float[] posUpdatedGradientEmb = embCalculator(posDistanceEmb, "*", STEP_SIZE * posGradientParam);
				
				float[] posHeadGradientEmb = embCalculator(posUpdatedGradientEmb, "*", -3.0f);
				float[] posRelationGradientEmb = embCalculator(posUpdatedGradientEmb, "*", -3.0f);
				float[] posTailGradientEmb = embCalculator(posUpdatedGradientEmb, "*", 3.0f);
				

				updateGradient(gradientEntityEmbHashMap, posHeadEntity, posHeadGradientEmb);
				updateGradient(gradientRelationEmbHashMap, posRelation, posRelationGradientEmb);
				updateGradient(gradientEntityEmbHashMap, posTailEntity, posTailGradientEmb);
				
				
				for(int k = 0; k < NEG_SAMPLE_COUNT; k++)
				{
					String[] headNegTriplet = getHeadNegTriplet(posTriplet);				
					String negHeadEntity = headNegTriplet[0];
					
					float[] negHeadEmb = entityEmbeddings.get(negHeadEntity);
					float[] negDistanceEmb = getDistanceEmb(negHeadEmb, posRelationEmb, posTailEmb);
					float negDistance = norm(negDistanceEmb);
					float negLogisticDistance = logisticFunction(BIAS - 0.5f * negDistance);	
					//System.out.println("NEG: " + negDistance);
					totalSum += Math.log(1.0f - negLogisticDistance);
					
					float negGradientParam = - negLogisticDistance;					
					float[] negGradientEmb = getGradientEmb(negDistanceEmb);
					
					float[] negUpdatedGradientEmb = embCalculator(negGradientEmb, "*", STEP_SIZE * negGradientParam);

					float[] negHeadGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
					float[] negRelationGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
					float[] negTailGradientEmb = embCalculator(negUpdatedGradientEmb, "*", +1.0f);
					
					updateGradient(gradientEntityEmbHashMap, negHeadEntity, negHeadGradientEmb);
					updateGradient(gradientRelationEmbHashMap, posRelation, negRelationGradientEmb);
					updateGradient(gradientEntityEmbHashMap, posTailEntity, negTailGradientEmb);
//										
					String[] tailNegTriplet = getTailNegTriplet(posTriplet);				
					String negTailEntity = tailNegTriplet[2];
					
					float[] negTailEmb = entityEmbeddings.get(negTailEntity);
					negDistanceEmb = getDistanceEmb(posHeadEmb, posRelationEmb, negTailEmb);
					negDistance = norm(negDistanceEmb);
					negLogisticDistance = logisticFunction(BIAS - 0.5f * negDistance);					
					totalSum += Math.log(1.0f - negLogisticDistance);
					
					negGradientParam = - negLogisticDistance;					
					negGradientEmb = getGradientEmb(negDistanceEmb);
					
					negUpdatedGradientEmb = embCalculator(negGradientEmb, "*", STEP_SIZE * negGradientParam);

					negHeadGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
					negRelationGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
					negTailGradientEmb = embCalculator(negUpdatedGradientEmb, "*", 1.0f);
					
					updateGradient(gradientEntityEmbHashMap, posHeadEntity, negHeadGradientEmb);
					updateGradient(gradientRelationEmbHashMap, posRelation, negRelationGradientEmb);
					updateGradient(gradientEntityEmbHashMap, negTailEntity, negTailGradientEmb);

//					String[] relationNegTriplet = getRelationNegTriplet(posTriplet);				
//					String negRelation = relationNegTriplet[1];
//					
//					float[] negRelationEmb = relationEmbeddings.get(negRelation);
//					negDistanceEmb = getDistanceEmb(posHeadEmb, negRelationEmb, posTailEmb);
//					negDistance = norm(negDistanceEmb);
//					negLogisticDistance = logisticFunction(BIAS - 0.5f * negDistance);					
//					totalSum += Math.log(1.0f - negLogisticDistance);
//					
//					negGradientParam = - negLogisticDistance;					
//					negGradientEmb = getGradientEmb(negDistanceEmb);
//					
//					negUpdatedGradientEmb = embCalculator(negGradientEmb, "*", STEP_SIZE * negGradientParam);
//
//					negHeadGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
//					negRelationGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
//					negTailGradientEmb = embCalculator(negUpdatedGradientEmb, "*", 1.0f);
//					
//					updateGradient(gradientEntityEmbHashMap, posHeadEntity, negHeadGradientEmb);
//					updateGradient(gradientRelationEmbHashMap, negRelation, negRelationGradientEmb);
//					updateGradient(gradientEntityEmbHashMap, posTailEntity, negTailGradientEmb);
				
				}
				
				String[] relationNegTriplet = getRelationNegTriplet(posTriplet);				
				String negRelation = relationNegTriplet[1];
				
				float[] negRelationEmb = relationEmbeddings.get(negRelation);
				float[] negDistanceEmb = getDistanceEmb(posHeadEmb, negRelationEmb, posTailEmb);
				float negDistance = norm(negDistanceEmb);
				float negLogisticDistance = logisticFunction(BIAS - 0.5f * negDistance);					
				totalSum += Math.log(1.0f - negLogisticDistance);
				
				float negGradientParam = - negLogisticDistance;					
				float[] negGradientEmb = getGradientEmb(negDistanceEmb);
				
				float[] negUpdatedGradientEmb = embCalculator(negGradientEmb, "*", STEP_SIZE * negGradientParam);

				float[] negHeadGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
				float[] negRelationGradientEmb = embCalculator(negUpdatedGradientEmb, "*", -1.0f);
				float[] negTailGradientEmb = embCalculator(negUpdatedGradientEmb, "*", 1.0f);
				
				updateGradient(gradientEntityEmbHashMap, posHeadEntity, negHeadGradientEmb);
				updateGradient(gradientRelationEmbHashMap, negRelation, negRelationGradientEmb);
				updateGradient(gradientEntityEmbHashMap, posTailEntity, negTailGradientEmb);
				
				Iterator<String> entityKeys = gradientEntityEmbHashMap.keySet().iterator();
				while(entityKeys.hasNext())
				{
					String entityKey = entityKeys.next();
//					System.out.println(entityKey);
//					displayEmb(gradientEntityEmbHashMap.get(entityKey));
					
					float[] tmpEmb = entityEmbeddings.get(entityKey);
					//displayEmb(tmpEmb);
					entityEmbeddings.put(entityKey, embCalculator(tmpEmb, "+", gradientEntityEmbHashMap.get(entityKey)));	 
					
				}
				
				Iterator<String> relationKeys = gradientRelationEmbHashMap.keySet().iterator();
				while(relationKeys.hasNext())
				{
					String relationKey = relationKeys.next();
//					System.out.println(relationKey);
//					displayEmb(gradientRelationEmbHashMap.get(relationKey));	
					
					float[] tmpEmb = relationEmbeddings.get(relationKey);
					relationEmbeddings.put(relationKey, embCalculator(tmpEmb, "+", gradientRelationEmbHashMap.get(relationKey)));	 				
				}
				
			}
			System.out.println(i + "/" + EPOCHES);
			
			System.out.println("TRAIN AVG LOSS: " + totalSum / trainExamples.size());
			bestEntityEmbeddings.clear();
			bestEntityEmbeddings.putAll(entityEmbeddings);
			bestRelationEmbeddings.clear();
			bestRelationEmbeddings.putAll(relationEmbeddings);
		}
	}
			
	public String[] getHeadNegTriplet(String[] posTriplet)
	{
		String[] negTriplet = new String[3];
		String corruptedHeadEntity = posTriplet[0];
		while(corruptedHeadEntity.equals(posTriplet[0]))
		{
			int corruptedHeadIdx = rand.nextInt(headEntityList.size());
			corruptedHeadEntity = headEntityList.get(corruptedHeadIdx);
		}
		negTriplet[0] = corruptedHeadEntity;
		negTriplet[1] = posTriplet[1];
		negTriplet[2] = posTriplet[2];
		return negTriplet;
	}
		
	public String[] getTailNegTriplet(String[] posTriplet)
	{
		String[] negTriplet = new String[3];
		String corruptedTailEntity = posTriplet[2];
		while(corruptedTailEntity.equals(posTriplet[2]))
		{
			int corruptedTailIdx = rand.nextInt(tailEntityList.size());
			corruptedTailEntity = tailEntityList.get(corruptedTailIdx);
		}
		negTriplet[0] = posTriplet[0];
		negTriplet[1] = posTriplet[1];
		negTriplet[2] = corruptedTailEntity;
		return negTriplet;
	}
	
	public String[] getRelationNegTriplet(String[] posTriplet)
	{
		String[] negTriplet = new String[3];
		String corruptedRelationEntity = posTriplet[1];
		while(corruptedRelationEntity.equals(posTriplet[1]))
		{
			int corruptedRelationIdx = rand.nextInt(relationList.size());
			corruptedRelationEntity = relationList.get(corruptedRelationIdx);
		}
		negTriplet[0] = posTriplet[0];
		negTriplet[1] = corruptedRelationEntity;
		negTriplet[2] = posTriplet[2];
		return negTriplet;
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
			//System.out.println("POS Distance: \t" + posDistance);
			float posLogisticDistance = logisticFunction(BIAS - 0.5f * posDistance);
			//System.out.println(posDistance);
			totalLoss += Math.log(posLogisticDistance);
//			System.out.println(Math.log(posLogisticDistance));
			//displayEmb(posDistanceEmb);
			//displayTuple(negTuple);
			float[] negDistanceEmb = getDistanceEmb(negTuple[0], negTuple[1], negTuple[2]);
//			//displayEmb(negDistanceEmb);
			float negDistance = norm(negDistanceEmb);
			//System.out.println("NEG Distance: \t" + negDistance);
			float negLogisticDistance = 1.0f - logisticFunction(BIAS - 0.5f * negDistance);
			
			totalLoss += Math.log(negLogisticDistance);
			//System.out.println(Math.log(negLogisticDistance));
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
		System.out.println("Test Entity Set Size: " + testEntitySet.size());
		
		for(int i = 0; i < testExamples.size(); i++)
		{
		    //System.out.println(i + "/" + testExamples.size());
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
		System.out.println("****************************");
		System.out.println("PTRANSE");
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
				gradientEmb[i] = distanceEmb[i];
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
			tcDevItems.get(it.next()).getThreshold();
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
		
	}
	
	public float[] getBestDistanceEmb(String headEntity, String relation, String tailEntity)
	{
		float[] headEmb = bestEntityEmbeddings.get(headEntity);
		float[] relationEmb = bestRelationEmbeddings.get(relation);
		float[] tailEmb = bestEntityEmbeddings.get(tailEntity);
		
		return embCalculator(embCalculator(headEmb, "+", relationEmb), "-", tailEmb);
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
        
		PTransE ptranse = new PTransE(cmd.getOptionValue("config"));
		
		/*Train and Dev*/
		ptranse.loadTrain();	
		ptranse.loadTCDev();
		ptranse.init();
		ptranse.train();
		
		/*Test*/
		//ptranse.loadTest();
		//ptranse.test();		
		/*TC Test*/
//		transe.tcThreshold();
//		transe.loadTCTest();
//		float accuracy = transe.tcTestAcc();
//		System.out.println(accuracy);
		//transe.tcTestPR();
		String outDir = cmd.getOptionValue("output");
		TransE.writeEmbeddings(outDir + "/PTranse_entity_embeddings.txt", ptranse.bestEntityEmbeddings);
		TransE.writeEmbeddings(outDir + "/PTranse_relation_embeddings.txt", ptranse.bestRelationEmbeddings);
		
	}
	
}

