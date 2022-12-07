import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class MyHeuristic {

	public static Variable getVariable(ArrayList<Variable> variables , String variable_name) //returns a variable object in the ArrayList of the variables
	{
		int index = 0 ;
		while(index<variables.size()) //iterating over the ArrayList
		{
			if(variables.get(index).getName().equals(variable_name))
				return variables.get(index);
			index++;
		}
		return null;
	}
	public static boolean isOutcome(Variable vb , String st) //return if a string is an outcome of a variable
	{
		if(vb==null)
			return false;
		for (int i = 0; i < vb.getOptions(); i++) {
			if(vb.outcomes[i].equals(st))
				return true;
		}
		return false;
	}

	public static boolean isName(ArrayList<Variable> variables, String str) //returns if a string is a name of a variable
	{
		if(getVariable(variables,str)!=null)
			return true;
		return false;
	}
	public static CPT getCPT(ArrayList<CPT> bayesian_network , String variable_name) //returns a CPT object in the ArrayList of the CPT's
	{
		int index = 0 ;
		while(index<bayesian_network.size()) //iterating over the ArrayList
		{
			if(bayesian_network.get(index).getCurrentQueryName().equals(variable_name))
				return bayesian_network.get(index);
			index++;
		}
		return null;
	}
	public static void PreProcess(String str ,ArrayList<Variable> query, ArrayList<Variable>evidence,ArrayList<Variable>hidden,ArrayList<Variable> variables,ArrayList<CPT> bayesian_network)
	{
		Variable x = null;
		String save_query = ""; //this is used for saving the query outcome for normalization later
		int flag_divider = 0; //divides the string at char '|'
		String name = "";
		String outcome = "";

		//This piece of code is used to get the query parameter and set its outcome
		for (int i = 1; i < str.length() && str.charAt(i) != '|'; ++i) {
			if(str.charAt(i)!='(' && str.charAt(i)!=')' && str.charAt(i)!=',' && str.charAt(i)!= '=')
			{	
				name+=str.charAt(i);
				outcome += str.charAt(i);
			}
			if(isName(variables,name))
			{
				x = getVariable(variables,name);
				query.add(x);
				outcome = "";
				name = "";
			}
			if(isOutcome(x,outcome))
			{
				x.setCurrentOutcome(outcome);
				save_query = outcome;
				name = "";
				outcome = "";
			}

			flag_divider = i;

		}
		flag_divider++;
		//Re_Initializing the variables
		name = "";
		outcome = "";
		Variable vb = null ;
		//This loop's purpose is to save the evidence variables and their outcomes from the '|' character until the end of the string
		for (int i = flag_divider+1; i < str.length(); i++) {
			if(str.charAt(i)!='(' && str.charAt(i)!=')' && str.charAt(i)!=',' && str.charAt(i)!= '=')
			{	
				name+=str.charAt(i);
				outcome += str.charAt(i);
			}
			if(isName(variables,name))
			{
				vb = getVariable(variables,name);
				evidence.add(vb);
				outcome = "";
				name = "";
			}
			if(isOutcome(vb,outcome))
			{
				vb.setCurrentOutcome(outcome);
				name = "";
				outcome = "";
			}
		}
		//saving the query's parent to check if the evidence is equal to the parents, so we can get the probability from the CPT with no calculation
		Linked_List<Variable> query_parents = getCPT(bayesian_network,x.getName()).given;
		Linked_List<Variable> p = query_parents;
		ArrayList<Variable> query_parents_al = new ArrayList<Variable>();
		if(p!=null)
		{
			while(p!=null)
			{
				query_parents_al.add(p.getValue());
				p=p.getNext();
			}
		}

		//Declaring an ArrayList for the query and the evidence parameters
		//to check which of the parameters are missing and add them to the hidden ArrayList.
		ArrayList<Variable> query_evidence = new ArrayList<>();                             
		for (int i = 0; i < evidence.size(); i++) {
			query_evidence.add(evidence.get(i));
		}
		query_evidence.add(x); 
		for (int i = 0; i < variables.size(); i++) {
			if(!(query_evidence.contains(variables.get(i))))
			{
				hidden.add(variables.get(i));
			}
		}
	}
	public static ArrayList<CPT> getAllCPT(ArrayList<CPT> bayesian_network,ArrayList<Variable> variables)
	{
		ArrayList<CPT> cpts = new ArrayList<CPT>();
		for (int i = 0; i < variables.size(); i++) {
			cpts.add(getCPT(bayesian_network,variables.get(i).getName()));
		}
		return cpts;
	}
	public static boolean AlreadyExists(Linked_List<Variable> list,Variable other)
	{
		Linked_List<Variable> p = list;
		while(p!=null)
		{
			if(p.getValue() == other)
				return true;
			p=p.getNext();
		}
		return false;
	}
	public static CPT join(double[]arr,ArrayList<CPT> factors)
	{
		CPT res = factors.get(0);
		for (int i = 1; i < factors.size(); i++) {
			res = joinTwoFactors(arr,res,factors.get(i));
		}

		return res;
	}
	private static CPT joinTwoFactors(double[] arr,CPT cpt1, CPT cpt2) {
		ArrayList<Variable> union = new ArrayList<Variable>();
		Linked_List<Variable> union_ln = new Linked_List<>(new Variable());
		Linked_List<Variable> q = union_ln;
		Linked_List<Variable> m = cpt1.given;
		Linked_List<Variable> p = cpt2.given;
		int sum = 1;
		while(m!=null)
		{
			if(!union.contains(m.getValue()))
			{
				union.add(m.getValue());
				q.setNext(new Linked_List<Variable>(m.getValue()));
				q=q.getNext();
				sum*=m.getValue().getOptions();	
			}
			m=m.getNext();
		}
		while(p!=null)
		{
			if(!union.contains(p.getValue()))
			{
				union.add(p.getValue());
				q.setNext(new Linked_List<Variable>(p.getValue()));
				q=q.getNext();
				sum*=p.getValue().getOptions();	
			}
			p=p.getNext();
		}
		union_ln = union_ln.getNext();
		String [][]truth = new String [sum][union.size()];
		double [] result_probs = new double[sum];
		CPT result = new CPT(truth,union_ln);
		result.given = union_ln;
		result.probabilities = result_probs;
		double prob1 = 0,prob2 =0;
		for (int i = 0; i < truth.length; i++) {
			p=result.given;
			for (int j = 0; j < truth[0].length; j++) {
				p.getValue().setCurrentOutcome(truth[i][j]);
				p=p.getNext();
			}
			prob1 = cpt1.getProbailityByCurrentOutcomes();
			prob2 = cpt2.getProbailityByCurrentOutcomes();	
			result_probs[i] = prob1*prob2;
			arr[2]++;
		}
		return result;
	}
	public static CPT Eliminate(double []arr ,CPT cpt , Variable current_hidden)
	{
		if(cpt.getCPTSize() == 2)
			return cpt;
		Linked_List<Variable> parents = new Linked_List<>(new Variable());
		Linked_List<Variable> p = cpt.given;
		Linked_List<Variable> q = parents;
		int count_variables = 0;
		while(p!=null)
		{
			if(p.getValue()!=current_hidden)
			{
				q.setNext(new Linked_List<Variable>(p.getValue()));
				q=q.getNext();
			}
			p=p.getNext();
			count_variables++;
		}
		parents = parents.getNext();
		String [][] truth = new String[cpt.getCPTSize()/current_hidden.getOptions()][count_variables-1];
		CPT eliminated = new CPT(truth,parents);
		double [] probabilities = new double[cpt.getCPTSize()/current_hidden.getOptions()];
		eliminated.given = parents;
		double sum = 0;
		for (int i = 0; i < truth.length; i++) {
			p = eliminated.given;
			sum=0;
			for (int j = 0; j < truth[0].length; j++) {
				if(p.getValue()!=current_hidden)
				{
					p.getValue().setCurrentOutcome(truth[i][j]);
					p=p.getNext();
				}
			}
			ArrayList<Double> probs_to_sum= cpt.Variable_To_Eliminate(current_hidden);
			for (int j = 0; j < probs_to_sum.size(); j++) {
				sum+=probs_to_sum.get(j);
				arr[1]+=probs_to_sum.size()-1;
			}
			arr[1]--;
			probabilities[i] = sum;

		}
		eliminated.probabilities = probabilities;

		return eliminated;
	}

	public static void RemoveDuplicates(ArrayList<CPT> arr)
	{
		for (int i = 0; i < arr.size()-1; i++) {
			for (int j = i+1; j < arr.size(); j++) {
				if(arr.get(i).equals(arr.get(j)))
					arr.remove(arr.get(j));
			}
		}
	}
	public static double[] Heuristic(ArrayList<Variable> variables,String str,ArrayList<CPT> bayesian_network)
	{
		double []arr = new double [3];
		ArrayList<Variable> query = new ArrayList<>();//saving the query variable
		ArrayList<Variable> evidence = new ArrayList<>(); //saving the evidence variables
		ArrayList<Variable> hidden = new ArrayList<>(); //saving the hidden parameters
		PreProcess(str, query, evidence, hidden, variables, bayesian_network);
		{
			ArrayList<Variable> union = new ArrayList<Variable>(evidence);
			union.addAll(query);
			ArrayList<Variable> checker = new ArrayList<>();

			Linked_List<Variable> p  = getCPT(bayesian_network,query.get(0).getName()).given;
			while(p!=null)
			{
				checker.add(p.getValue());
				p=p.getNext();
			}
			checker.sort(null);
			union.sort(null);
			if(union.equals(checker))
			{
				arr[0] = getCPT(bayesian_network,query.get(0).getName()).getThisVariableProb();
				System.out.println(arr[0]);
				return arr;
			}
		}

		String query_wanted_outcome = query.get(0).current_outcome;
		Collections.sort(hidden, new ReverseComparator());//sorts the ArrayList by the ABC order
		ArrayList<CPT> factors = new ArrayList<>();
		factors.addAll(getAllCPT(bayesian_network,query));
		factors.addAll(getAllCPT(bayesian_network,evidence));
		factors.addAll(getAllCPT(bayesian_network,hidden));
		for (int i = 0; i < evidence.size(); i++) {
			for (int j = 0; j < factors.size(); j++) {
				if(factors.get(j)!=null && factors.get(j).inCPT(evidence.get(i).getName()))
				{
					CPT diminished = factors.get(j).Diminish(evidence.get(i));
					factors.remove(factors.get(j));
					factors.add(diminished);
					j=-1;
				}
			}
		}
		for (int j = 0; j < factors.size(); j++) {
			if(factors.get(j)== null)
			{
				factors.remove(factors.get(j));
				j=-1;

			}
		}
		
		
		
		
		
		
		while(!hidden.isEmpty())
		{
			ArrayList<CPT> contains = new ArrayList<>(); //take all factors including the current variable
			for (int j = 0; j < factors.size(); j++) {
				if(factors.get(j)!= null && factors.get(j).inCPT(hidden.get(0).getName()))
				{
					contains.add(factors.get(j));
				}
			}

			contains.sort(null);
			CPT hidden_result = join(arr,contains);
			CPT eliminated = Eliminate(arr,hidden_result,hidden.get(0));
			if(eliminated.getCPTSize()>=2) //if the factor after elimination became one valued, discard the factor.
			{
				for (int j = 0; j < factors.size(); j++) {
					if(factors.get(j)== null || factors.get(j).inCPT(hidden.get(0).getName()))
					{
						factors.remove(factors.get(j));
						j=-1;

					}
				}
				if(eliminated != null)
				{
					factors.add(eliminated);
				}
			}
			hidden.remove(0);
		}
		CPT final_of_finals = join(arr,factors);
		double total=0,sum = 0;

		for (int i = 0; i < final_of_finals.probabilities.length; i++) {
			total+=final_of_finals.probabilities[i];
			arr[1]++;
		}
		arr[1]--;
		for (int i = 0; i < final_of_finals.table.length; i++) {
			for (int j = 0; j < final_of_finals.table[0].length; j++) {
				if(final_of_finals.table[i][j].equals(query_wanted_outcome))
					sum = final_of_finals.probabilities[i];
			}
		}
		arr[0]=sum/total;	
		return arr;
	}


}
