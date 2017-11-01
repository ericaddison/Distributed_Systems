import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;


public class WordFileRecordWriter extends RecordWriter<WordTextFileWritable, IntWritable> {
    private DataOutputStream out;
    private String currentWord = null;

    public WordFileRecordWriter(DataOutputStream stream) {
        out = stream;  
    }

    @Override
    public void close(TaskAttemptContext arg0) throws IOException, InterruptedException {
        //close our file
        out.close();
    }

    @Override
    public void write(WordTextFileWritable arg0, IntWritable arg1) throws IOException, InterruptedException {
    	
    	if(!arg0.getWord().equals(currentWord)){
    		if(currentWord!=null)
    			out.writeBytes("\n");
    		out.writeBytes(arg0.getWord() + "\n");
    		currentWord = arg0.getWord();
    	}
    	
        //write out our filename-value
        out.writeBytes("<" + arg0.getFilename() + ", " + arg1.toString() + ">");
        out.writeBytes("\n");  
    }
}
