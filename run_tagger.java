import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.text.html.HTMLDocument.Iterator;


public class run_tagger {
	private static final int NUM_OF_TAGS = 46;
	private static final Vector<String> TAG_INDEX;
	static{
		String[] tag_list = 
				{	"CC","CD","DT","EX","FW", "IN",
					"JJ","JJR","JJS","LS","MD","NN", 
					"NNS","NNP","NNPS","PDT","POS","PRP",
					"PRP$", "RB","RBR","RBS","RP","SYM",
					"TO","UH","VB", "VBD","VBG","VBN",
					"VBP","VBZ","WDT","WP","WP$","WRB",
					"$","#","``","''","-LRB-","-RRB-",",",".",":"};
		Vector<String> aV = new Vector<String>();
		for (int i=0; i<tag_list.length; i++){
			aV.add(tag_list[i]);
		}
		TAG_INDEX = aV;
	}
	/*Indicate Witten-Bell smoothing : 0, Add-One smoothing: 1 or One Count Smoothing: others*/
	int smoothingFlag = 2;
	/*This is to store the number of "Tag_i Tag_j"
	 * Where countTagTag[i][j] = C(Tag_i Tag_j) including "<s>" and "</s>"
	 */
	private int[][] countTagTag;
	/*This is to store sum(C(Tag_i Tag_j)) of each Tag_i
	 * sumTagTag[i][0] = C(Tag_i Tag_1) + ... + C(Tag_i Tag_j)
	 */
	private int[] sumTagTag;
	/*Total number of all types of "Tag_i Tag_j" */
	private int totalCountTagTag;
	/*Seen types fo tag_j following each tag i*/
	private int[] seenTagTypes;
	
	/*This is to store the number of "Word_k/Tag_i" 
	 * Where countWordTag("Word_k")[i] = C(Word_k/Tag_i)
	 */
	private HashMap<String, int[]> countWordTag;
	/*This is to store sum(C(work_k/Tag_i)) for all words of each Tag_i
	 * sumWordTag[Tag_i] = C(work_1/Tag_i) + ... + C(word_k/Tag_i)
	 */
	private int[] sumWordTag;
	/*Seen types of words for each Tag_i */
	private int[] seenWordTypes;
	/*Total number of all types of "Word/Tag_i" */
	private int totalCountWordTag;
	/*Total types of vocabulary, which is V*/
	private int totalWordTypes;
	
	/*Training, Development, Model File names*/
	private String testFileName, outFileName, modelFileName;
	
	/*transitionMatrix[i][j] = P(Tag_j|Tag_i)*/
	private double[][] transitionMatrix;
	/*emissionMatrix["word"][i] = P(word|Tag_i)*/
	private HashMap<String, double[]> emissionMatrix;

	/* count Number of words that occur only once */
	private int totalSingletonWords;
	/*used to count number of each word occured*/
	private HashMap<String, Integer> countWordI;
	/*count singletons for each tag*/
	private int[] countSingletonTag;
	/**
	 * Constructor
	 * @param training training file path to read
	 * @param development development file path to read
	 * @param model model file path to write
	 */
	public run_tagger(String test, String model, String out){
		
		countTagTag = new int[NUM_OF_TAGS][NUM_OF_TAGS];
		countWordTag = new HashMap<String, int[]>();
		sumWordTag = new int[NUM_OF_TAGS];
		sumTagTag = new int[NUM_OF_TAGS];
		seenWordTypes = new int[NUM_OF_TAGS-1];
		seenTagTypes = new int[NUM_OF_TAGS];
		totalCountTagTag = 0;
		totalCountWordTag = 0;
		totalWordTypes = 0;
		testFileName = test;
		outFileName = out;
		modelFileName = model;
		transitionMatrix = new double[NUM_OF_TAGS][NUM_OF_TAGS];
		emissionMatrix = new HashMap<String, double[]>();
		totalSingletonWords = 0;
		countWordI = new HashMap<String, Integer>();
		countSingletonTag = new int[NUM_OF_TAGS-1];
		
	}
	/**
	 * Read statistics from model file
	 * @param fileName
	 */
	private void read_model_file(String fileName){
		
		try (BufferedReader _br = new BufferedReader(new FileReader(fileName)))
		{ // File closed automatically.
			String _line;
			
			totalCountTagTag = Integer.parseInt(_br.readLine());
			totalCountWordTag = Integer.parseInt(_br.readLine());
			totalWordTypes = Integer.parseInt(_br.readLine());
			totalSingletonWords = Integer.parseInt(_br.readLine());
			
			for(int i=0; i<countTagTag.length; i++){
				_line = _br.readLine();
				parse_store_array(_line, countTagTag[i]);
			}
			
			_line = _br.readLine();
			parse_store_array(_line, sumWordTag);

			_line = _br.readLine();
			parse_store_array(_line, sumTagTag);

			_line = _br.readLine();
			parse_store_array(_line, seenWordTypes);

			_line = _br.readLine();
			parse_store_array(_line, seenTagTypes);

			_line = _br.readLine();
			parse_store_array(_line, countSingletonTag);
			
			while((_line = _br.readLine()) != null){
				String _word = _line;
				int _occurrence = Integer.parseInt(_br.readLine());
				countWordI.put(_word, _occurrence);
				int[] _tags = new int[NUM_OF_TAGS-1];
				parse_store_array(_br.readLine(), _tags);
				countWordTag.put(_word, _tags);
			}
			
		} catch (IOException e) {
			System.out.println("IO error");
			e.printStackTrace();
		}
	}
	private void parse_store_array(String line, int[] array){
		
		String[] _temp = line.split("\\s+");
		if(_temp.length != array.length){
			System.out.println(modelFileName+ " Wrong Format!");
		}
		else{
			for(int j=0; j<array.length; j++){
				array[j] = Integer.parseInt(_temp[j]);
			}
		}
		return;
	}
	
	/** Return the index of certain Tag String
	 * 
	 * @param tag_i	Tag String
	 * @return	Tag index in arrays. return -1 if not exists.
	 */
	private int get_tag_index(String tag_i){
		if(tag_i.equals("<s>") || tag_i.equals("</s>")) return NUM_OF_TAGS-1;
		
		/*If tag_i not exists return -1*/
		if(!TAG_INDEX.contains(tag_i)){
			System.out.println("Unkown Tag " + tag_i + " !");
			return -1;
		}
		
		return TAG_INDEX.indexOf(tag_i);
	}
	/**
	 * Get the probability of transiton from tag_i to tag_j: P(tag_j | tag_i)
	 * Here is using Witten-Bell smoothing
	 * @param tag_i
	 * @param tag_j
	 * @return
	 */
	private double get_wb_trans_prob(int tag_i, int tag_j){
		/**
		 * transition matrix smoothing
		 */
		int _i = tag_i;
		int _j = tag_j;

		/*Check if tags are valid*/
		if( _i == -1 || _j == -1) return 0.0d;
		
		int _cI = sumTagTag[_i];
		int _tI = seenTagTypes[_i];
		int _zI = NUM_OF_TAGS - _tI;
		int _cIJ = countTagTag[_i][_j];
		double _result;
		
		if(_cIJ != 0){
			_result = (double) _cIJ / (double) (_cI + _tI);
		}
		else{
			_result = (double) _tI / (double)(_zI * (_cI + _tI));
		}
		return _result;
	}
	
	/**
	 * Get the probability of observation : P(word_i | tag_i)
	 * Add-one smoothing
	 * @param word_i
	 * @param tag_i
	 * @return
	 */
	private double get_addOne_obser_prob(String word_i, int tag_i){
		
		int _i = tag_i;
		
		/*Check if tag is valid */
		if( _i == -1) return 0.0d;
		
		double _cI = (double) sumWordTag[_i];
    	double _tI = (double) seenWordTypes[_i];
    	double _V = (double) (totalWordTypes);
    	double _result;
    	
		if(countWordTag.containsKey(word_i)){
			int[] _tags = countWordTag.get(word_i);
			double _cWI = (double) _tags[_i];
        		_result = (_cWI + 1.0d) / (_cI + _V);
		}
		else{
			_result = 1.0d / (_cI + _V + 1.0d);
		}
		
		return _result;
	}
	/**
	 * Get the probability of observation : P(word_i | tag_i)
	 * Witten-Bell smoothing
	 * @param word_i
	 * @param tag_i
	 * @return
	 */
	private double get_wb_obser_prob(String word_i, int tag_i){
		
		int _i = tag_i;
		
		/*Check if tag is valid */
		if( _i == -1) return 0.0d;
		
		double _cI = (double) sumWordTag[_i];
    	double _tI = (double) seenWordTypes[_i];
    	double _zI = (double) (totalWordTypes) - _tI;
    	double _result;
    	
		if(countWordTag.containsKey(word_i)){
			int[] _tags = countWordTag.get(word_i);
			double _cWI = (double) _tags[_i];
			if(_cWI != 0.0d){
        		_result = _cWI / (_cI + _tI);
        	}
        	else{
        		_result = _tI / (_zI * (_cI + _tI));
        	}
		}
		else{
			_result = _tI / (_zI * (_cI + _tI + 1.0d));
		}
		
		return _result;
	}
	
	/**
	 * Get the probability of observation : P(word_i | tag_i)
	 * One-count smoothing
	 * @param word_i
	 * @param tag_i
	 * @return
	 */
	private double get_oneCount_obser_prob(String word_i, int tag_i){
	
		int _i = tag_i;
		
		/*Check if tag is valid */
		if( _i == -1) return 0.0d;
		
		double _result = 0.0d;
		double _cW;
		double _cWT;
		/* Back-off probability*/
		double _backOffProb;
		if(!countWordI.containsKey(word_i)){
			_cW = 0.0d;
			_cWT = 0.0d;
			_backOffProb = (_cW + 1.0d) /(totalCountWordTag + totalWordTypes + 1.0d);
		}
		else{
			_cW = (double) countWordI.get(word_i);
			_cWT = (double) countWordTag.get(word_i)[_i];
			_backOffProb = (_cW + 1.0d) /(totalCountWordTag + totalWordTypes);
		}
		
		/* Number of Singletons, in case that _lambda is 0*/
		double _lambda = (double) countSingletonTag[_i] + 1.0d;
		
		double _cT = sumWordTag[_i];
		
		/*One-Count Smoothing*/
		_result = (_cWT + _lambda * _backOffProb) / (_cT + _lambda);
		
		return _result;
	}
	
	private double get_aij(int i, int j){
		return get_wb_trans_prob(i, j);
	}
	
	private double get_bwj(String w, int j){
		/*If string match numeric pattern, convert it into #NUM# as stored.*/
		String _regex= "-?\\d+(\\.\\d+)?";
		String _regex2 = "-?\\d+(,\\d+)*";
		if(w.matches(_regex) || w.matches(_regex2)){
			w = "#NUM#";
		}
		if(smoothingFlag == 0)
			return get_wb_obser_prob(w, j);
		else if (smoothingFlag == 1)
			return get_addOne_obser_prob(w, j);
		else
			return get_oneCount_obser_prob(w, j);
	}
	/**
	 * Compute the max probability and corresponding previous state
	 * @param prevStatesProb Vertibi probability of all previous state
	 * @param curState Current State
	 * @return result[0] = max probability; result[1] = corresponding previous state number.
	 */
	private double[] max_prob_arg(double[] prevStatesProb, int curState){
		double[] _result = new double[2];
		double _max = -1.0d;
		double _cur;
		int _prevState = -1;
		for(int i=0; i<prevStatesProb.length-1; i++){
			_cur = prevStatesProb[i] * get_aij(i, curState);
			if(_cur < 0.0d){
				//System.out.println("Probability overflow!");
				double _log = Math.log(prevStatesProb[i]) + Math.log(get_aij(i, curState));
				_cur = Math.exp(_log);
			}
			if(_cur > _max){
				_max = _cur;
				_prevState = i;
			}
		}
		_result[0] = _max;
		_result[1] = (double) _prevState;
		return _result;
	}
	
	/**
	 * Vertibi  compute the best tag combinations and return best tag combinations
	 * @param words
	 * @return
	 */
	private int[] viterbi(String[] words){
		double[][] _maxProb = new double[words.length+1][NUM_OF_TAGS];
		int[][] _prevState = new int[words.length+1][NUM_OF_TAGS];
		/*Initialization Step*/
		for(int j=0; j<NUM_OF_TAGS-1; j++){
			_prevState[0][j] = get_tag_index("<s>"); // First word's prev state is <s>
			double _log = Math.log( get_aij(get_tag_index("<s>"), j)) + Math.log(get_bwj(words[0],j));
			_maxProb[0][j] = Math.exp(_log);
			//_maxProb[0][j] = get_aij(get_tag_index("<s>"), j) * get_bwj(words[0], j);
		}
		/*Recursion Step*/
		for(int t=1; t<words.length; t++){
			for(int j=0; j<NUM_OF_TAGS-1; j++){
				double[] _maxProbArg = max_prob_arg(_maxProb[t-1], j);
				double _log = Math.log( _maxProbArg[0]) + Math.log(get_bwj(words[t],j));
				_maxProb[t][j] = Math.exp(_log);
				/*
				_maxProb[t][j] = Math.exp(_log);
				_maxProb[t][j] = _maxProbArg[0] * get_bwj(words[t],j);
				if(_maxProb[t][j] < 0.0d){
					double _log = Math.log( _maxProbArg[0]) + Math.log(get_bwj(words[t],j));
					_maxProb[t][j] = Math.exp(_log);
				}
				*/
				_prevState[t][j] = (int) _maxProbArg[1];
			}
		}
		/*Termination Step*/
		int _finalState = get_tag_index("</s>");
		double[] _finalResult = max_prob_arg(_maxProb[words.length-1],_finalState);
		_maxProb[words.length][_finalState] = _finalResult[0];
		_prevState[words.length][_finalState] = (int) _finalResult[1];
		
		int[] _tagCombinations = new int[words.length];
		int _current = _finalState;
		for(int i=0; i<_tagCombinations.length; i++){
			_current = _prevState[words.length-i][_current];
			_tagCombinations[_tagCombinations.length-1 - i] = _current;
		}
		/*
		for(int i=0; i< _tagCombinations.length; i++){
			System.out.println(_tagCombinations[i]+ "++++++");
		}
		*/
		return _tagCombinations;
	}
	/**
	 * Use Viterbi algorithm to tag the test data
	 */
	private void start_tagging(){
		try (BufferedReader _br = new BufferedReader(new FileReader(testFileName));
				FileWriter _fw = new FileWriter(outFileName))
		{ // File closed automatically.
			String _line;
			
			File _file = new File(outFileName);
			/* if file doesn't exists, then create it */
			if (!_file.exists()) {
				_file.createNewFile();
			}
			int _count = 0;
			while((_line = _br.readLine()) != null){
				_count ++;
				String[] _words = _line.split("\\s+");
				int[] _tags = viterbi(_words);
				//System.out.println("words length: "+ _words[_words.length-1] + " tags length: " + _tags[_tags.length-1]);
				_fw.write(_words[0]+"/"+ TAG_INDEX.get(_tags[0]));
				for(int i=1; i<_words.length; i++){
					_fw.write(" " + _words[i]+"/"+ TAG_INDEX.get(_tags[i]));
					//System.out.println(" " + _words[i]+"/"+ TAG_INDEX.get(_tags[i]));
				}
				_fw.write("\n");
				System.out.print("\rTagging " + _count);
				System.out.flush();
			}
			
			_fw.close();
			_br.close();
		} catch (IOException e) {
			System.out.println("IO error");
			e.printStackTrace();
		}
	}
	/**
	 * Start run the tagger to tag the test file
	 */
	public void run(){
		read_model_file(modelFileName);
		start_tagging();
		/* test
		System.out.println("V = " + totalWordTypes);
		for(int i=0; i<seenWordTypes.length; i++){
			System.out.println("T["+i+"] = " + TAG_INDEX.get(i) + " : " + seenWordTypes[i]);
		}
		 
		for(int i=0; i<countTagTag.length-1; i++){
			int sum = 0;
			for(int j=0; j<countTagTag[i].length-1; j++){
				System.out.print(TAG_INDEX.get(i) + " : " + TAG_INDEX.get(j) + " = " + countTagTag[i][j]+ " | ");
				sum += countTagTag[i][j];
			}
			System.out.println("equal: "+ (sum == sumTagTag[i]-countTagTag[i][countTagTag.length-1]));
			System.out.println();
		}
		System.out.println("Word Counts: " + totalCountWordTag + " ; Word Types " + totalWordTypes);
		
		for(int i=0; i<transitionMatrix.length; i++){
			for(int j=0; j<transitionMatrix[i].length; j++){
				System.out.printf("%.8f ",transitionMatrix[i][j]);
			}
			System.out.println();
		}
		
		
		System.out.println("Number of singletons: "+ totalSingletonWords);
	    */
		return;
	}
	

	public static void main(String[] args) {
		/*
		 * Check arguments number.
		 */
		if (args.length != 3){
			System.out.println("Please follow the correct format!");
			System.out.println("e.g:");
			System.out.println("	java run_tagger sents.test model_file out_file");
			return;
		}
		File testFile = new File(args[0]);
		File modelFile = new File(args[1]);
		/*
		 * Check if training file exists.
		 */
		if(!testFile.exists() || testFile.isDirectory()){
			System.out.println("Test File \"" + args[0] + "\" doesn't exists");
			return;
		}
		/*
		 * Check if development file exists.
		 */
		if(!modelFile.exists() || modelFile.isDirectory()){
			System.out.println("Model File \"" + args[1] + "\" doesn't exists");
			return;
		}
		/*
		 * Initialize build_tagger and start building.
		 */
		run_tagger bd = new run_tagger(args[0], args[1], args[2]);
		bd.run();
		return;

	}

}
