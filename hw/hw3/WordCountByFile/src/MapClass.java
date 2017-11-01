

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

// included libraries hadoop-common and hadoop-mapreduce-client-core

public class MapClass extends Mapper<LongWritable, Text, WordTextFileWritable, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private WordTextFileWritable outputKey = new WordTextFileWritable();
    
    @Override
    protected void map(LongWritable key, Text value,
			Context context)
			throws IOException, InterruptedException {
		
    	// get name of current file
    	String filename = ((FileSplit) context.getInputSplit()).getPath().getName();
    	outputKey.setFilename(filename);
    	
		String line = value.toString();
		String[] tokens = line.split("[\\s-—]");
		
		for(String nextToken : tokens){
			// process the next word
			nextToken = nextToken.replaceAll("[^\\w]?[\\d]?", "").toLowerCase();
			if(nextToken.equals(""))
				continue;
			outputKey.setWord(nextToken);
			context.write(outputKey,one);
		}
		
	}

    public static void main(String[] args) {
		String s = "yourself—and me";
		String[] t = s.split("[\\s-—]");
		System.out.println(Arrays.toString(t));
		
	}
}