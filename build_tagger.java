import java.util.HashMap;


public class build_tagger {
	
	/*This is to store the number of "Tag_i Tag_j"
	 * Where countTagTag[i][j] = C(Tag_i Tag_j)
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

	/**
	 * Enumeration of 36 Penn Treebank
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
	
	
	/**
	 * Reading the training file
	 * @param fileName
	 */
	private void read_training_file(String fileName){
		return;
	}
	/**
	 * Reading the development file
	 * @param fileName
	 */
	private void read_development_file(String fileName){
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
	private void write_statistic_to_file(String fileName){
		return;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
