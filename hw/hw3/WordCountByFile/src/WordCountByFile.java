import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class WordCountByFile extends Configured implements Tool{
	
	public static void main(String[] args) throws Exception{
		int exitCode = ToolRunner.run(new WordCountByFile(), args);
		System.exit(exitCode);
	}
 
	public int run(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.printf("Usage: %s needs N+1 arguments, N input files and one output file\n", getClass().getSimpleName());
			return -1;
		}
	
		Job job = Job.getInstance();
		job.setJarByClass(WordCountByFile.class);
		job.setJobName("WordCounterByFile");
		
		for(int i=0; i<args.length-1; i++){
			FileInputFormat.addInputPath(job, new Path(args[i]));
		}
		FileOutputFormat.setOutputPath(job, new Path(args[args.length-1]));
		
		job.setOutputKeyClass(WordTextFileWritable.class);
		job.setOutputValueClass(IntWritable.class);
		job.setOutputFormatClass(WordFileOutputFormat.class);
		
		job.setMapperClass(MapClass.class);
		job.setReducerClass(ReduceClass.class);
	
		int returnValue = job.waitForCompletion(true) ? 0:1;
		
		if(job.isSuccessful()) {
			System.out.println("Job was successful");
		} else if(!job.isSuccessful()) {
			System.out.println("Job was not successful");			
		}
		
		return returnValue;
	}
}