import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class build_tagger {
	/*Number of tags used in Penn Treebank + <s> */
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
	private String trainingFileName, developmentFileName, modelFileName;
	
	/*transitionMatrix[i][j] = P(Tag_j|Tag_i)*/
	private float[][] transitionMatrix;
	/*emissionMatrix["word"][i] = P(word|Tag_i)*/
	private HashMap<String, float[]> emissionMatrix;

	/* count Number of words that occur only once */
	private int totalSingletonWords;
	/*used to count number of each word occured*/
	private HashMap<String, Integer> countWordI;
	/**
	 * Constructor
	 * @param training training file path to read
	 * @param development development file path to read
	 * @param model model file path to write
	 */
	public build_tagger(String training, String development, String model){
		
		countTagTag = new int[NUM_OF_TAGS][NUM_OF_TAGS];
		countWordTag = new HashMap<String, int[]>();
		sumWordTag = new int[NUM_OF_TAGS];
		sumTagTag = new int[NUM_OF_TAGS];
		seenWordTypes = new int[NUM_OF_TAGS-1];
		seenTagTypes = new int[NUM_OF_TAGS];
		totalCountTagTag = 0;
		totalCountWordTag = 0;
		totalWordTypes = 0;
		trainingFileName = training;
		developmentFileName = development;
		modelFileName = model;
		transitionMatrix = new float[NUM_OF_TAGS][NUM_OF_TAGS];
		emissionMatrix = new HashMap<String, float[]>();
		totalSingletonWords = 0;
		countWordI = new HashMap<String, Integer>();
		
		/*
		 * Initializing arrays with 0;
		 */
		//Arrays.fill(sumTagTag, 0);
		//Arrays.fill(sumWordTag, 0);
		//Arrays.fill(countTagTag, 0);
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
		/* Delimiters used to split sentences to Word/Tag*/
		String _whiteSpace = "\\s+";
		String _slash = "/";
		String _prevTag = "<s>";
		String[] _tokens  = line.split(_whiteSpace);
		for(int i=0; i<_tokens.length; i++){
			/* Assume the format only contain one slash "/". */
			String[] _wordTag = _tokens[i].split(_slash); 
			/* Add Count(PreviousTag, CurrentTag)*/
			String _curTag = _wordTag[_wordTag.length-1];
			add_tag_tag(_prevTag, _curTag);
			/* Add Count(Word, Tag) */
			String _word = "";
			/* Concatenate word if it contains / inside*/
			for(int j=0; j<_wordTag.length-2; j++){
				_word += _wordTag[j];
				if(j != _wordTag.length-1) _word += "/";
			}
			_word += _wordTag[_wordTag.length-2];
			add_word_tag(_word, _curTag);
			
			/*print prevTag curTag*/
			//System.out.println(_prevTag + "|" + _wordTag[1]);
			_prevTag = _curTag;		
		}
		/* When reach the end of line, add count(Tag, </s>)*/
		add_tag_tag(_prevTag, "</s>");
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
		int _i = get_tag_index(tag_i);
		int _j = get_tag_index(tag_j);
		/* If tag_i or tag_j doesn't exist, throw exception*/
		if( _i != -1 && _j != -1){
			if(countTagTag[_i][_j] == 0){
				seenTagTypes[_i]++;
			}
			countTagTag[_i][_j]++;
			sumTagTag[_i]++;
			totalCountTagTag++;
		}
		return;
	}
	/**
	 * Add one to C(Word_k Tag_i)
	 * @param word_k
	 * @param tag_i
	 */
	private void add_word_tag(String word_k, String tag_i){
		int _i = get_tag_index(tag_i);
		if(_i == -1){
			System.out.println("Unknown Tag " + tag_i + " !");
			return;
		}
		
		/*if word matched as numbers, convert it to #NUM#, for simplify*/
		String _regex= "-?\\d+(.\\d+)?";
		if(word_k.matches(_regex)){
			word_k = "#NUM#";
			//System.out.println("matched numeric");
		}
		
		/*if word already exists, add one for tag_i*/
		if(countWordTag.containsKey(word_k)){
			/*Check if word types already counted for Tag_i*/
			if(countWordTag.get(word_k)[_i] == 0){
				seenWordTypes[_i] ++;
			}
			/*New inserted word increment singletons, increasing existing countWordTag will decrement singletons */
			int _num = countWordI.get(word_k);
			if(_num == 1){
				totalSingletonWords--;
			}
			countWordI.put(word_k, _num+1);
			
			countWordTag.get(word_k)[_i] ++;
		}
		else{
			int[] _temp = new int[NUM_OF_TAGS-1];
			_temp[_i]++;
			countWordTag.put(word_k, _temp);
			countWordI.put(word_k, 1);
			totalWordTypes ++;
			seenWordTypes[_i]++;
			/*New inserted word increment singletons, increasing existing countWordTag will decrement singletons */
			totalSingletonWords++;
		}
		sumWordTag[_i]++;
		totalCountWordTag++;
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
	 * Based on the gathered information
	 * Computing "tag transition probability, word emission probability" and smoothing: Witten-Bell Smoothing
	 */
	@SuppressWarnings("rawtypes")
	private void compute_smooth(){
		/**
		 * transition matrix smoothing
		 */
		for(int i=0; i<countTagTag.length; i++){
			/*C(Tag_i)*/
			int _cI = sumTagTag[i];
			int _tI = seenTagTypes[i];
			int _zI = NUM_OF_TAGS - _tI;
			for(int j=0; j<countTagTag[i].length; j++){
				int _cIJ = countTagTag[i][j];
				if(_cIJ != 0){
					transitionMatrix[i][j] = (float) _cIJ / (float) (_cI + _tI);
				}
				else{
					transitionMatrix[i][j] = (float) _tI / (float)(_zI * (_cI + _tI));
				}
			}
		}
		
		/**
		 * emission matrix smoothing
		 */
		Iterator _it = countWordTag.entrySet().iterator();
	    while (_it.hasNext()) {
	        Map.Entry _pairs = (Map.Entry)_it.next();
	        int[] _tags = (int[]) _pairs.getValue();
	        String _word = (String) _pairs.getKey();
	        
	        float[] _pWordTag = new float[NUM_OF_TAGS];
	        for(int i=0; i<_tags.length; i++){
	        	float _cI = (float) sumWordTag[i];
	        	float _tI = (float) seenWordTypes[i];
	        	float _zI = (float) (totalWordTypes) - _tI;
	        	float _cWI = (float) _tags[i];
	        	if(_cWI != 0.0){
	        		_pWordTag[i] = _cWI / (_cI + _tI);
	        	}
	        	else{
	        		_pWordTag[i] = _tI / (_zI * (_cI + _tI));
	        	}
	        }
	        emissionMatrix.put(_word, _pWordTag);      
	    }
	    /* avoids a ConcurrentModificationException */
        _it.remove();
		return;
	}
	
	/**
	 * Write gathered statistic data into file
	 * @param fileName
	 */
	private void write_model_file(String fileName){
		File _file = new File(fileName);
		
		try (FileWriter _fw = new FileWriter(fileName)) {
 
			/* if file doesn't exists, then create it */
			if (!_file.exists()) {
				_file.createNewFile();
			}
			
			/*
			 * write attributes to model file
			 */
			_fw.write(totalCountTagTag + "\n");
			_fw.write(totalCountWordTag + "\n");
			_fw.write(totalWordTypes + "\n");
			_fw.write(totalSingletonWords + "\n");
			for(int i=0; i<countTagTag.length; i++){
				for(int j=0; j<countTagTag[i].length; j++){
					_fw.write(countTagTag[i][j] +" ");
				}
				_fw.write("\n");
			}
			for(int i=0; i<sumWordTag.length; i++){
				_fw.write(sumWordTag[i] + " ");
			}
			_fw.write("\n");
			for(int i=0; i<sumTagTag.length; i++){
				_fw.write(sumTagTag[i] + " ");
			}
			_fw.write("\n");
			for(int i=0; i<seenWordTypes.length; i++){
				_fw.write(seenWordTypes[i] + " ");
			}
			_fw.write("\n");
			for(int i=0; i<seenTagTypes.length; i++){
				_fw.write(seenTagTypes[i] + " ");
			}
			_fw.write("\n");
			//TODO follow picture order to write to files. left hashmaps
			
			
			
			
			
			_fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

	/**
	 * Start to build tag.
	 */
	public void build(){
		read_training_file(trainingFileName);
		read_development_file(developmentFileName);
		compute_smooth();
		write_model_file(modelFileName);
		/*test
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
		}*/
		System.out.println("Word Counts: " + totalCountWordTag + " ; Word Types " + totalWordTypes);
		/*
		for(int i=0; i<transitionMatrix.length; i++){
			for(int j=0; j<transitionMatrix[i].length; j++){
				System.out.printf("%.8f ",transitionMatrix[i][j]);
			}
			System.out.println();
		}
		*/
		
		System.out.println("Number of singletons: "+ totalSingletonWords);
		/*
		Iterator _it = countWordI.entrySet().iterator();
	    while (_it.hasNext()) {
	        Map.Entry _pairs = (Map.Entry)_it.next();
	        int _num = (int) _pairs.getValue();
	        String _word = (String) _pairs.getKey();
	        if(_num == 1){
	        	System.out.println(_word);
	        }
	    }
	    System.out.println("97.90".matches("\\d+(.\\d+)?"));
	    */
		
	}
	public static void main(String[] args) {
		
		/**
		 * Try
		 * 
		 */
		/*
		String _str = "The/DT Merc/NNP alleged/VBD that/IN ,/, among/IN other/JJ things/NNS ,/, from/IN April/NNP 1987/CD through/IN October/NNP 1988/CD Capcom/NNP Futures/NNPS failed/VBD to/TO document/VB trades/NNS between/IN Capcom/NNP Futures/NNPS and/CC people/NNS or/CC entities/NNS directly/RB or/CC indirectly/RB controlled/JJ by/IN Capcom/NNP Futures/NNPS shareholders/NNS ./.";
		String _delimeter = "\\s+";
		String[] test = _str.split(_delimeter);
		for(int i=0; i<test.length; i++){
			System.out.print("\n" + test[i]);
			String[] test_part = test[i].split("/");
			for (int j=0; j< test_part.length; j++){
				System.out.print(" " + test_part[j]);
			}
		}
		*/
		
		
		/***
		 * Try End.
		 */
		
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
		if(!trainingFile.exists() || trainingFile.isDirectory()){
			System.out.println("Trainning File \"" + args[0] + "\" doesn't exists");
			return;
		}
		/*
		 * Check if development file exists.
		 */
		if(!developmentFile.exists() || developmentFile.isDirectory()){
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
