import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.WritableComparable;


public class WordTextFileWritable implements WritableComparable<WordTextFileWritable>{

	private String word;
	private String filename;
	
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public void readFields(DataInput arg0) throws IOException {
		word = arg0.readUTF();
		filename = arg0.readUTF();
	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		arg0.writeUTF(word);
		arg0.writeUTF(filename);
	}

	@Override
	public int compareTo(WordTextFileWritable that) {
		int wordCompare = this.word.compareTo(that.word);
		int filenameCompare = this.filename.compareTo(that.filename);
		
		if(wordCompare != 0)
			return wordCompare;
		
		return filenameCompare;
	}

	@Override
	public String toString() {
		return "(" + word + ", " + filename + ")";
	}
	
	
	

}
