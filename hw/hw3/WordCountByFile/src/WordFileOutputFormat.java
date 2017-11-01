import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class WordFileOutputFormat extends FileOutputFormat<WordTextFileWritable, IntWritable> {
  @Override
  public RecordWriter<WordTextFileWritable, IntWritable> getRecordWriter(TaskAttemptContext arg0) throws IOException, InterruptedException {
     
	  //get the current path
     Path path = FileOutputFormat.getOutputPath(arg0);
     
     //create the full path with the output directory plus our filename

     Path fullPath = new Path(path, "result.txt");
     //create the file in the file system
     FileSystem fs = path.getFileSystem(arg0.getConfiguration());
     FSDataOutputStream fileOut = fs.create(fullPath, arg0);

     //create our record writer with the new file
     return new WordFileRecordWriter(fileOut);
  }
}