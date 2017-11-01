
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class ReduceClass extends Reducer<WordTextFileWritable, IntWritable, WordTextFileWritable, IntWritable>{

	
	@Override
	protected void reduce(WordTextFileWritable key, Iterable<IntWritable> values,
			Context context)
			throws IOException, InterruptedException {
		
		int sum = 0;
		Iterator<IntWritable> valuesIt = values.iterator();
		
		while(valuesIt.hasNext()){
			sum = sum + valuesIt.next().get();
		}
		
		context.write(key, new IntWritable(sum));
		
	}

}