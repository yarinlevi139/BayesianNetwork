import java.util.ArrayList;
import java.util.Iterator;

public class SimpleDeduction {
	//This function's goal is to search over all of the variable's outcome and their parents (P(A1,A2,A3...An))
	public static double Calculate(double []arr, Variable query, ArrayList<Variable> evidence, ArrayList<Variable> hidden ,ArrayList<Variable> variables,ArrayList<CPT> bayesian_network)
	{

		ArrayList<String> outcomes;
		double result=1;
		for (int i = 0; i < variables.size(); i++) {
			Linked_List<Variable> parents = getCPT(bayesian_network, variables.get(i).getName()).given; //saving each variable's parents (based on the Bayesian network)
			Linked_List<Variable> p = parents;
			outcomes = new ArrayList<String>(); //saving each variable's outcomes
			if(parents!=null)
			{
				while(p!=null)
				{
					outcomes.add(p.getValue().current_outcome);
					p=p.getNext();
				}
			}
			else
				outcomes.add(variables.get(i).current_outcome);
			arr[2]++;
			result*=getCPT(bayesian_network,variables.get(i).getName()).getProbabilityByOutcomes(outcomes); //based on our Bayesian Network we are using the getProb method to get the specific probability

		}
		arr[2]--;
		return result; 
	}

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
	//the following code is complicated, we are using the simple conclusion(which is not that simple in code)
	public static double[] Deduction(ArrayList<Variable> variables,String str,ArrayList<CPT> bayesian_network) { // Using simple deduction algorithm
		//Declaring an array of size 3
		//arr[0] will hold the answer, arr[1] will hold the number of additions ,arr[2] will hold the number of multiplications
		double [] arr = new double [3];

		Variable query = null;; //saving the query variable
		ArrayList<Variable> evidence = new ArrayList<>(); //saving the evidence variables
		ArrayList<Variable> hidden = new ArrayList<>(); //saving the hidden parameters
		String save_query = ""; //this is used for saving the query outcome for normalization later
		String checker = str.substring(2,str.length()-1);
		String [] splitter = checker.split("[\\|=,]");
		query = getVariable(variables,splitter[0]);
		query.setCurrentOutcome(splitter[1]);
		save_query = query.current_outcome;
		for (int i = 2; i < splitter.length; i=i+2) {
			evidence.add(getVariable(variables,splitter[i]));
		}
		int iterator = 0;
		for (int i = 3; i < splitter.length; i+=2) {
			if(splitter[i-1].equals(query.getName()))
			{
				if(splitter[i].equals(query.current_outcome))
				{
					arr[0]=1;
					return arr;
				}
				else
					return arr;
			}
			else {
				evidence.get(iterator).setCurrentOutcome(splitter[i]);
				iterator++;
			}
		}

		{
			//checks if the query is already in the table
			ArrayList<Variable> union = new ArrayList<Variable>(evidence);
			union.add(query);
			ArrayList<Variable> check = new ArrayList<>();

			Linked_List<Variable> p  = getCPT(bayesian_network,query.getName()).given;
			while(p!=null)
			{
				check.add(p.getValue());
				p=p.getNext();
			}
			check.sort(null);
			union.sort(null);
			if(union.equals(check))
			{
				arr[0] = getCPT(bayesian_network,query.getName()).getThisVariableProb();
				System.out.println(arr[0]);
				return arr;
			}
		}

		//saving the query's parent to check if the evidence is equal to the parents, so we can get the probability from the CPT with no calculation
		Linked_List<Variable> query_parents = getCPT(bayesian_network,query.getName()).given;
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
		query_evidence.add(query); 
		for (int i = 0; i < variables.size(); i++) {
			if(!(query_evidence.contains(variables.get(i))))
			{
				hidden.add(variables.get(i));
			}
		}
		if(hidden.isEmpty()) //checking if there are no hidden variables, using the P(A,B)/P(B) formula
		{
			double top = Calculate(arr,query,evidence,hidden,variables,bayesian_network);
			double sum = 0;
			int count_additions = 0;
			for (int i = 0; i < query.getOptions(); i++) {
				query.setCurrentOutcome(query.outcomes[i]);
				sum+=Calculate(arr,query,evidence,hidden,variables,bayesian_network);
				count_additions++;
			}
			System.out.println(top/sum);
			arr[0] = top/sum;
			arr[1] = count_additions -1 ;
			return arr;
		}

		else {
			//****from this line of code i have the hidden variables stored in an ArrayList
			query_evidence.sort(null);
			query_parents_al.sort(null);
			if(query_evidence.equals(query_parents_al)) //if the query is already in the CPT
			{
				arr[0] = getCPT(bayesian_network,query.getName()).getThisVariableProb();
				System.out.println(arr[0]);
				return arr;
			}
			else
			{
				//this piece of code's purpose is to get every hidden variable's outcome from the Generete_Truth_Table method in class CPT
				int options = 1;
				Linked_List<Variable> hidden_ln = new Linked_List<Variable>(query);
				p = hidden_ln;
				for (int i = 0; i < hidden.size(); i++) {
					options*=hidden.get(i).getOptions();
					p.setNext(new Linked_List<Variable>(hidden.get(i)));
					p=p.getNext();
				}
				hidden_ln = hidden_ln.getNext();
				String [][] every_option_hidden = new String [options][hidden.size()];
				CPT hidden_truth_table = new CPT(every_option_hidden,hidden_ln);

				arr[1]=0;
				arr[2]=0;
				double result;
				double sum = 0;
				double total =0;
				int count_additions = 0;
				int index = 0;

				//This is the actual algorithm we are iterating over all the possible outcomes of the hidden variables
				//Also we are iterating on the query all possible outcomes for normalization
				for(int k=0;k<query.getOptions();k++)
				{
					query.setCurrentOutcome(query.outcomes[k]);
					for (int i = 0; i < every_option_hidden.length; i++) {
						index = 0 ;
						for (int j = 0; j < every_option_hidden[0].length; j++) {
							hidden.get(index).setCurrentOutcome(every_option_hidden[i][j]);
							index++;
						}
						result = Calculate(arr,query,evidence,hidden,variables,bayesian_network); //using the Calculate function described below
						if(query.current_outcome.equals(save_query)) //we are checking if the current query's outcome is equal to the save_query variable for before (for normalization)
						{
							sum+=result;
						}
						total+=result;
						count_additions++;
						arr[0]++;
					}
				}
				System.out.println(sum/total);
				arr [0] = sum/total; //normalizing sum with total
				arr[1] = count_additions-1; //we need to decrease the number of additions in one

				return arr;
			}
		}
	}




}
