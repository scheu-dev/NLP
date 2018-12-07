import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Model {
	
	double lambda = 0.0; //smoothening constant
	
	String name;
	public Set<String> known_words; 
	public Set<String> known_tags;
	public ArrayList<String> start_tags;
	
	public HashMap<String, Double> start_p;
	public HashMap<String, HashMap<String, Double>> em_mat;
	public HashMap<String, HashMap<String, Double>> tr_mat;
	public HashMap<Integer, HashMap<String, Double>> LLH_mat;
	public HashMap<Integer, HashMap<String, String>> LLH_preceder;
	
	public Model(String name) {
		this.name = name;
		this.known_words = new HashSet<String>();
		this.known_tags = new HashSet<String>();
		this.start_tags = new ArrayList<String>();
		
		this.start_p = new HashMap<String, Double>();
		this.em_mat = new HashMap<String, HashMap<String, Double>>();
		this.tr_mat = new HashMap<String, HashMap<String, Double>>();
		this.LLH_mat = new HashMap<Integer, HashMap<String, Double>>();
		this.LLH_preceder = new HashMap<Integer, HashMap<String, String>>();
		
	}
	
	public void get_knowns(List<String> sentences) {
		HashMap<String, Integer> tag_counts = new HashMap<String, Integer>();
		int tag_count = 0;
		int total = 0;
		for(String sentence: sentences) {
			String[] pairs = sentence.split("\\s++");
			int i = 0;
			for(String pair:pairs) {	
				int idx = pair.lastIndexOf("/");
				String word = pair.substring(0,idx);
				String tag = pair.substring(idx + 1);
			
				if(i == 0) {
					start_tags.add(tag);
				}
				
				known_words.add(word);
				known_tags.add(tag);
				i ++;
				
				if(tag_counts.containsKey(tag)) {
					tag_count = tag_counts.get(tag);
					tag_count ++;
					tag_counts.put(tag, tag_count);
				} else {
					tag_counts.put(tag, 1);
				}
				total ++;
				
			}
		}
		System.out.println("Total tags in set: " + total);
		for(String tag : tag_counts.keySet()) {
			System.out.println(tag + ": " + tag_counts.get(tag));
		}
	}
	
	public void get_start_p(List<String> sentences) {
		//initialize start_p
		double tag_count;
		int nil_counter = 0;
		
		
		for(String tag: known_tags) {
			start_p.put(tag, lambda);
		}
		double B = (double) known_tags.size();
		double N = (double) sentences.size();
		double p;

		for(String sentence : sentences) {
			String[] pairs = sentence.split("\\s++");
			String pair = pairs[0];
			int idx = pair.lastIndexOf("/");			
			String start_tag = pair.substring(idx + 1);
			
			if(start_tag.equals("nil")) {
				//System.out.println("Starttag is: nil!");
				//System.out.println(sentence);
				nil_counter ++;
			}
			
			tag_count = start_p.get(start_tag);
			tag_count ++;
			start_p.put(start_tag, tag_count);			
		}
		//normalize
		for(String tag: known_tags) {
			tag_count = start_p.get(tag);
			p = tag_count/ (N + B * lambda);
			p = Math.log(p);
			start_p.put(tag, p);
		}
		System.out.println(nil_counter);
	}
	
	public void get_emission(List<String> sentences) {
		
		
		
		//initialize em_mat with lambda values
		for(String tag : known_tags) {
			HashMap<String, Double> word_prob = new HashMap<String, Double>();
			word_prob.put("un-known-ident", lambda);
			for(String word : known_words) {
				word_prob.put(word, lambda);
			}
			em_mat.put(tag, word_prob);
		}
		
		//count words per tag
		HashMap<String, Double> tag_mat = new HashMap<String, Double>();
		HashMap<String, Double> tag_counts = new HashMap<String, Double>();
		for(String word: known_tags) {
			tag_counts.put(word, (double)0 ); 
		}
		
		double tag_word_count;
		//double word_count;
		double tag_count;
		
		for(String sentence : sentences) {
			String[] pairs = sentence.split("\\s++");
			for(String pair:pairs) {
				int idx = pair.lastIndexOf("/");				
				String word = pair.substring(0,idx);
				String tag = pair.substring(idx + 1);
				
				tag_mat = em_mat.get(tag);
				tag_word_count = tag_mat.get(word);
				tag_word_count ++;
				tag_mat.put(tag, tag_word_count);
				
				tag_count = tag_counts.get(tag);
				tag_count ++;
				tag_counts.put(tag, tag_count);
				
				
				
			}
			
		}
		

		
		//normalize
		double N = 0.0;
		double B = (double) known_tags.size();;
		double p = 0.0;
		
		
		for(String tag: known_tags) {
			tag_mat = em_mat.get(tag);
			for(String word : known_words) {
				tag_word_count = tag_mat.get(word);
				N = tag_counts.get(tag);
				p = tag_word_count / (N + B * lambda);
				p = Math.log(p);
				tag_mat.put(word, p);
			}
			em_mat.put(tag, tag_mat);
		}		
	}
	
	public void get_transition(List<String> sentences) {
		
		HashMap<String, Double> preceder_mat = new HashMap<String, Double>();
		HashMap<String, Double> preceder_counts = new HashMap<String, Double>();
		
		//initialize etr_mat with lambda values
		for(String preceder: known_tags) {
			//System.out.println(preceder);
			preceder_mat = new HashMap<String, Double>();
			for(String follower: known_tags) {
				preceder_mat.put(follower, lambda);
			}
			tr_mat.put(preceder, preceder_mat);
		}
		
		
		//count transitions
		for(String preceder: known_tags) {
			preceder_counts.put(preceder, (double) 0.0);
		}
		
		
		
		double preceder_count;
		double preceder_follower_count;
		
		
		int idx;
		for(String sentence : sentences) {
			String[] pairs = sentence.split("\\s++");
			
			idx = pairs[0].lastIndexOf("/");
			String preceder = pairs[0].substring(idx + 1);
			String follower;

			for(int i=1;i <pairs.length; i++) {
				
				
				idx = pairs[i].lastIndexOf("/");				
				follower = pairs[i].substring(idx + 1);
				
				preceder_mat = new HashMap<String, Double>();
				preceder_mat = tr_mat.get(preceder);
				
				preceder_follower_count = preceder_mat.get(follower);
				preceder_follower_count ++;
				
				preceder_mat.put(follower, preceder_follower_count);
				tr_mat.put(preceder, preceder_mat);
				
				preceder_count = preceder_counts.get(preceder);
				preceder_count ++;
				preceder_counts.put(preceder, preceder_count);
				
				preceder = pairs[i].substring(idx + 1);
			}
		}
		//System.out.println("Follower_counts: ");
		//System.out.println(follower_counts);
		//normalize
		double N = 0.0;
		double B = (double) known_tags.size();
		double p = 0.0;
		
		for(String preceder: known_tags) {
			preceder_mat = tr_mat.get(preceder);
			
			for(String follower : known_tags) {
				N = preceder_counts.get(preceder);
				preceder_follower_count = preceder_mat.get(follower);
				p = preceder_follower_count / (N + B * lambda);
				if(preceder.equals("nil"))  {
					System.out.println(follower + " count :" + preceder_follower_count);
					System.out.println(N);
					System.out.println(p);
				}
				
				
				p = Math.log(p);
				preceder_mat.put(follower, p);				
			}
			tr_mat.put(preceder, preceder_mat);
		}
		
		
	}

	public void annotate(List<String> sentences) {
		HashMap<String, Double> pos_state_prob = new HashMap<String, Double>();
		HashMap<String, Double> state_MLL = new HashMap<String, Double>();
		HashMap<String, String> MLL_state_preceder = new HashMap<String, String>();
		
		List<String> True_states = new ArrayList<>();
		List<String> words = new ArrayList<>();
		double p0;
		double p;
		double em_p;
		double p_prev;
		double p_tr;
		//int pos;
		int pos_prev;
		double maxLL;
		String mLL_preceder;
		String word;
		
		for(String sentence : sentences) {
			String[] pairs = sentence.split("\\s++");
			
			for(String pair:pairs) {
				words.add(pair.split("/")[0]);
				True_states.add(pair.split("/")[1]);
			}
			String first_word = words.get(0);
			//initialize first position
			for(String state: known_tags) {
				p0 = start_p.get(state);
				
				if(known_words.contains(first_word)) {
					em_p = em_mat.get(state).get(first_word);
				} else {
					em_p = em_mat.get(state).get("un-known-ident");
				}	
				
				p = p0 + em_p;
				pos_state_prob.put(state, p);
			}
			LLH_mat.put(0, pos_state_prob);
			System.out.println(LLH_mat.get(0));
			
			//pos = 1;
			for(int pos =1; pos < words.size(); pos++) {
				word = words.get(pos);
				state_MLL = new HashMap<String, Double>();
				MLL_state_preceder = new HashMap<String, String>();

				
				for(String state : known_tags) {
					//for each state get maximum LLH(from previous states LLHs and transition prob)
					maxLL = Double.NEGATIVE_INFINITY;
					mLL_preceder = "---";
					
					for(String prev_state : known_tags) {
						//get max LLH from all previous states
						pos_prev = pos - 1;
						p_prev = LLH_mat.get(pos_prev).get(prev_state);
						p_tr = tr_mat.get(prev_state).get(state);
						p = p_prev + p_tr;
						if(p >= maxLL) {
							maxLL = p;
							mLL_preceder = prev_state;
						}					
					}
					//System.out.println(maxLL);
					//add MLL of the current state to the temp map of state MLLs
					if(known_words.contains(word)) {
						em_p = em_mat.get(state).get(word);
					} else {
						em_p = em_mat.get(state).get("un-known-ident");
					}	
					
					maxLL = maxLL + em_mat.get(state).get(word);
					state_MLL.put(state,maxLL);	
					MLL_state_preceder.put(state, mLL_preceder);
				}
				//add all state MLLs at the given pos to the LLH mat.
				LLH_mat.put(pos, state_MLL);
				LLH_preceder.put(pos, MLL_state_preceder);
				System.out.println("Position:" + pos + True_states.get(pos));
				if(pos == 3) {
					//System.out.print(LLH_mat.get(pos));
					System.out.print(LLH_preceder.get(pos));
				}
				
				//System.out.print(LLH_preceder.get(pos));
			}
			
			for(int pos : LLH_preceder.keySet()) {
				System.out.println(LLH_preceder.get(pos));
			}
			
			//System.exit(0);
			
			String expected_last_state = "--";
			int lastpos = (int) words.size() - 1;
			maxLL = Double.NEGATIVE_INFINITY;
			
			for(String state : LLH_mat.get(lastpos).keySet()) {
				p = LLH_mat.get(lastpos).get(state);
				if(p > maxLL) {
					maxLL = p;
					expected_last_state = state;
				}
			}
			
			System.out.println("-----------");
			for(int pos = words.size()-1; pos > 0; pos--) {
				System.out.print(pos);
				System.out.println(expected_last_state);
				expected_last_state = LLH_preceder.get(pos).get(expected_last_state);
				
			}
			
			
		}
		
		
	}


}
