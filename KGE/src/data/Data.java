package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Data {
	private static void generateNegativeFile(String originalPath, String newPath) throws IOException {
		
		File file = new File(originalPath);
		File outputFile = new File(newPath);
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		
		HashSet<String> entitySet = new HashSet<String>();
		ArrayList<String> devList = new ArrayList<String>();
		
		while(br.ready())
		{
			String line = br.readLine();
			//System.out.println(line);
			devList.add(line);
			String[] items = line.split("\t");
			entitySet.add(items[0]);
			entitySet.add(items[2]);
		}
		br.close();
		Object[] entityListTest = entitySet.toArray();
//		for(int i = 0; i < entityListTest.length; i++)
//			System.out.println(entityListTest[i]);
		System.out.println(entityListTest.length);
		
		for(int i = 0; i < devList.size(); i++)
		{
			String posLine = devList.get(i);
			String[] items = posLine.split("\t");
			String replacedEntity = items[0];
			String negLine = "";
			while(replacedEntity.equals(items[0]) || replacedEntity.equals(items[2]))
			{
				double r = Math.random();
				int replaceIdx = (int)(r * entityListTest.length);
				replacedEntity = (String) entityListTest[replaceIdx];
				posLine = items[0] + "\t" + items[1] + "\t" + items[2] + "\t" + "1";  

				if(r >= 0.5)
				{
					negLine = replacedEntity + "\t" + items[1] + "\t" + items[2] + "\t" + "-1";
				}
				else {
					negLine = items[0] + "\t" + items[1] + "\t" + replacedEntity + "\t" + "-1";
				}
			}
//			System.out.println(negLine);
			
			bw.write(posLine + "\n");
			bw.write(negLine + "\n");
		}
		bw.close();
		
	}
	
	public static void main(String[] args) throws Exception
	{
		Options options = new Options();
	    
	    Option config = new Option("c", "config", true, "config file input");
        config.setRequired(true);
        options.addOption(config);	    

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
//		String CONFIG_PATH = "DBpedia_Config.properties";
		InputStream in = new FileInputStream(cmd.getOptionValue("config"));
		Properties prop = new Properties();
		prop.load(in);
		
//		String TRAIN_FILE_PATH = prop.getProperty("TRAIN_FILE_PATH");
		String DEV_FILE_PATH = prop.getProperty("DEV_FILE_PATH");
		String TEST_FILE_PATH = prop.getProperty("TEST_FILE_PATH");
		
//		String TC_TRAIN_FILE_PATH = prop.getProperty("TC_TRAIN_FILE_PATH");
		String TC_DEV_FILE_PATH = prop.getProperty("TC_DEV_FILE_PATH");
		String TC_TEST_FILE_PATH = prop.getProperty("TC_TEST_FILE_PATH");
		
		// Create negative examples for dev set.
		generateNegativeFile(DEV_FILE_PATH, TC_DEV_FILE_PATH);
		// Create negative examples from test set.
		generateNegativeFile(TEST_FILE_PATH, TC_TEST_FILE_PATH);

//		BufferedReader br = new BufferedReader(new FileReader(new File("datasets/NELL/NELL_Imperfect.csv")));
//		ArrayList<String> candidateTrainList = new ArrayList<String>();
//		ArrayList<String> candidateTestList = new ArrayList<String>();
//		HashSet<String> entitySet = new HashSet<String>();
//		HashSet<String> relationSet = new HashSet<String>();
//		
//		while(br.ready())
//		{
//			String line = br.readLine();
//			//System.out.println(line);
//			String[] items = line.split("\t");
//			double probability = Double.parseDouble(items[3]);
//			if(!items[1].equals("candidate:generalizations") && items[0].startsWith("concept:") && items[2].startsWith("concept:"))
//			{
//				if(probability == 1.0)
//				{
//					candidateTestList.add(line);
//				}
//				else if(probability > 0.5)
//				{
//					candidateTrainList.add(line);
//					entitySet.add(items[0]);
//					entitySet.add(items[2]);
//					relationSet.add(items[1]);
//				}
//			}
//		}
//		
//		br.close();
//		
//		System.out.println(candidateTrainList.size());
//		System.out.println(entitySet.size());
//		System.out.println(relationSet.size());
//		
//		ArrayList<String> candidateTestListFilter = new ArrayList<String>();
//		for(int i = 0; i < candidateTestList.size(); i++)
//		{
//			String line = candidateTestList.get(i);
//			String[] items = line.split("\t");
//			if(entitySet.contains(items[0]) && entitySet.contains(items[2]) && relationSet.contains(items[1]))
//			{
//				candidateTestListFilter.add(line);
//			}
//
//		}
//		System.out.println(candidateTestListFilter.size());
//		
//		BufferedWriter bw_train = new BufferedWriter(new FileWriter(new File("datasets/NELL/NELL_Imperfect_train.csv")));
//		
//		for(int i = 0; i < candidateTrainList.size(); i++)
//		{
//			bw_train.write(candidateTrainList.get(i) + "\n");
//		}
//		bw_train.close();
//		
//		BufferedWriter bw_dev = new BufferedWriter(new FileWriter(new File("datasets/NELL/NELL_Imperfect_dev.csv")));
//		BufferedWriter bw_test = new BufferedWriter(new FileWriter(new File("datasets/NELL/NELL_Imperfect_test.csv")));
//		
//		for(int i = 0; i < candidateTestListFilter.size(); i++)
//		{
//			if(i % 2 == 0)
//				bw_dev.write(candidateTestListFilter.get(i) + "\n");
//			else {
//				bw_test.write(candidateTestListFilter.get(i) + "\n");
//			}
//		}
//		
//		bw_dev.close();
//		bw_test.close();
//		
//		BufferedReader br_dev = new BufferedReader(new FileReader(new File("datasets/NELL/NELL_Imperfect_dev.csv")));
//		BufferedWriter bw_tc_dev = new BufferedWriter(new FileWriter(new File("datasets/NELL/NELL_Imperfect_tc_dev.csv")));
//		HashSet<String> devEntitySet = new HashSet<String>();
//		ArrayList<String> devArrayList = new ArrayList<String>();
//		while(br_dev.ready())
//		{
//			String line = br_dev.readLine();
//			devArrayList.add(line);
//			
//			String[] items = line.split("\t");
//			devEntitySet.add(items[0]);
//			devEntitySet.add(items[2]);
//		}
//		br_dev.close();
//		Object[] entityList = entitySet.toArray();
//		
//		for(int i = 0; i < devArrayList.size(); i++)
//		{
//			String line = devArrayList.get(i);
//			String[] items = line.split("\t");
//			bw_tc_dev.write(items[0] + "\t" + items[1] + "\t" + items[2] + "\t" + "1\n");
//			System.out.println(items[0] + "\t" + items[1] + "\t" + items[2] + "\t" + "1");
//			
//			double r = Math.random();
//			
//			if(r >= 0.5)
//			{				
//				int idx = (int)(entityList.length * Math.random());
//				String replacedEntity = entityList[idx].toString();
//				
//				while(replacedEntity.equals(items[0]))
//				{
//					idx = (int)(entityList.length * Math.random());
//				}
//				
//				bw_tc_dev.write(replacedEntity + "\t" + items[1] + "\t" + items[2] + "\t-1\n");
//				System.out.println(replacedEntity + "\t" + items[1] + "\t" + items[2] + "\t-1");
//			}
//			else
//			{
//				int idx = (int)(entityList.length * Math.random());
//				String replacedEntity = entityList[idx].toString();
//				
//				while(replacedEntity.equals(items[2]))
//				{
//					idx = (int)(entityList.length * Math.random());
//				}
//				bw_tc_dev.write(items[0] + "\t" + items[1] + "\t" + replacedEntity + "\t-1\n");
//				System.out.println(items[0] + "\t" + items[1] + "\t" + replacedEntity + "\t-1");
//			}
//		}
//		
//		bw_tc_dev.close();
		
	}

}
