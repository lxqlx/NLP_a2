import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class build_tagger {
	/*Number of tags used in Penn Treebank + <s> */
	private static final int NUM_OF_TAGS = 46;
	
	/*This is to store the number of "Tag_i Tag_j"
	 * Where countTagTag[i][j] = C(Tag_i Tag_j) including "<s>" and "</s>"
	 */
	private int[][] countTagTag;
	/*This is to store the number of "Word_k/Tag_i" 
	 * Where countWordTag("Word_k")[i] = C(Word_k/Tag_i)
	 */
	private HashMap<String, int[]> countWordTag;
	/*This is to store sum(C(work_k/Tag_i)) for all words of each Tag_i
	 * sumWordTag[Tag_i] = C(work_1/Tag_i) + ... + C(word_k/Tag_i)
	 */
	private int[] sumWordTag;
	/*This is to store sum(C(Tag_i Tag_j)) of each Tag_i
	 * sumTagTag[i][0] = C(Tag_i Tag_1) + ... + C(Tag_i Tag_j)
	 */
	private int[] sumTagTag;
	/*Total number of all types of "Tag_i Tag_j" */
	private int totalCountTagTag;
	/*Total number of all types of "Word/Tag_i" */
	private int totalCountWordTag;
	/*Training, Development, Model File names*/
	private String trainingFileName, developmentFileName, modelFileName;

	/**
	 * Enumeration of 36 Penn Treebank without punctuation.
	 *
	 */
	public enum tag{
		CC, CD, DT, EX, FW, IN, JJ, 
		JJR, JJS, LS, MD, NN, NNS, 
		NP, NPS, PDT, POS, PRP, PRP$, 
		RB, RBR, RBS, RP, SYM, TO, 
		UH, VB, VBD, VBG, VBN, VBP, 
		VBZ, WDT, WP, WP$, WRB
	}
	
	public build_tagger(String training, String development, String model){
		
		countTagTag = new int[NUM_OF_TAGS][NUM_OF_TAGS];
		countWordTag = new HashMap<String, int[]>();
		sumWordTag = new int[NUM_OF_TAGS];
		sumTagTag = new int[NUM_OF_TAGS];
		totalCountTagTag = 0;
		totalCountWordTag = 0;
		trainingFileName = training;
		developmentFileName = development;
		modelFileName = model;
		
		/*
		 * Initializing arrays with 0;
		 */
		Arrays.fill(sumTagTag, 0);
		Arrays.fill(sumWordTag, 0);
		Arrays.fill(countTagTag, 0);
		/*
		for(int i=0; i<NUM_OF_TAGS; i++){
			sumTagTag[i]=0;
			sumWordTag[i]=0;
			for(int j=0; j<NUM_OF_TAGS; j++){
				countTagTag[i][j] = 0;
			}
		}
		*/
		
	}
	
	/**
	 * Reading the training file
	 * @param fileName
	 */
	private void read_training_file(String fileName){
		
		try (BufferedReader _br = new BufferedReader(new FileReader(fileName)))
		{ // File closed automatically.
			String _line;
			while ((_line = _br.readLine()) != null){
				process_training_sentence(_line); // Processing sentences line by line;
			}
		} catch (IOException e) {
			System.out.println("_line = _br.readLine() error");
			e.printStackTrace();
		}
		
		return;
	}
	/**
	 * Reading the development file
	 * @param fileName
	 */
	private void read_development_file(String fileName){
		try (BufferedReader _br = new BufferedReader(new FileReader(fileName)))
		{ // File closed automatically.
			String _line;
			while ((_line = _br.readLine()) != null){
				process_development_sentence(_line); // Processing sentences line by line;
			}
		} catch (IOException e) {
			System.out.println("_line = _br.readLine() error");
			e.printStackTrace();
		}
		
		return;
	}
	/**
	 * Processing a single line/Sentence from training file.
	 * @param line
	 */
	private void process_training_sentence(String line){
		return;
	}
	/**
	 * Processing a single line/Sentence from development file.
	 * @param line
	 */
	private void process_development_sentence(String line){
		return;
	}
	/**
	 * Add one to C(Tag_i Tag_j)
	 * @param tag_i
	 * @param tag_j
	 */
	private void add_tag_tag(String tag_i, String tag_j){
		return;
	}
	/**
	 * Add one to C(Word_k Tag_i)
	 * @param args
	 */
	private void add_word_tag(String word_k, String tag_i){
		return;
	}
	/**
	 * Based on the gathered information
	 * Computing "tag transition probability, word emission probability" and smoothing
	 */
	private void compute_smooth(){
		return;
	}
	
	/**
	 * Write gathered statistic data into file
	 * @param fileName
	 */
	private void write_model_file(String fileName){
		return;
	}

	/**
	 * Start to build tag.
	 */
	public void build(){
		read_training_file(trainingFileName);
		read_development_file(developmentFileName);
		write_model_file(modelFileName);
	}
	public static void main(String[] args) {
		
		/*
		 * Check arguments number.
		 */
		if (args.length != 3){
			System.out.println("Please follow the correct format!");
			System.out.println("e.g:");
			System.out.println("	java build_tagger sents.train sents.devt model_file");
			return;
		}
		File trainingFile = new File(args[0]);
		File developmentFile = new File(args[1]);
		/*
		 * Check if training file exists.
		 */
		if(!trainingFile.exists() || !trainingFile.isDirectory()){
			System.out.println("Trainning File \"" + args[0] + "\" doesn't exists");
			return;
		}
		/*
		 * Check if development file exists.
		 */
		if(!developmentFile.exists() || !developmentFile.isDirectory()){
			System.out.println("Trainning File \"" + args[1] + "\" doesn't exists");
			return;
		}
		/*
		 * Initialize build_tagger and start building.
		 */
		build_tagger bd = new build_tagger(args[0], args[1], args[2]);
		bd.build();
		
		return;
	}

}
