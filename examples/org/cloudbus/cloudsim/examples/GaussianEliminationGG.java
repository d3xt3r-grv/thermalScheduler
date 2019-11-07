//@author Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.*;
import java.lang.*;

/*file workflow.txt saves the following information about the workflow generated, 
 * in the specified format:
 * number of tasks
 * number of virtual machines
 * mean MIPS values for each virtual machine, separated by \t
 * average computation cost for each task (MI value), separated by \t
 * precedence matrix for the workflow (matrix to represent task dependencies)
*/

public class GaussianEliminationGG {

	public static void main(String args[]) throws NumberFormatException, IOException{
		try{
			
		
		File file = new File("workflow.txt");
		//System.out.println(file.getAbsolutePath());
		//FileWriter writerfile = new FileWriter(file);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));	
		BufferedReader br = new BufferedReader( new InputStreamReader(System.in));
		
		System.out.println("\nRandom Graph generation for Gaussian Elimination\n");
		System.out.println("Enter matrix size");
		int matrix = 15;//Integer.parseInt(br.readLine());
		
		System.out.println("Enter the value for wDag");
		int wDag = 7000000;//Integer.parseInt(br.readLine());
		
		System.out.println("Enter the Communication to Computation cost Ratio(ccr) value: ");
		Double ccr = 0.2;//Double.parseDouble(br.readLine());
		
		int tasks = (matrix*matrix + matrix -2)/2;
		System.out.println("The number of tasks are: " + tasks);
		writer.write(tasks + "\n");
		
		System.out.println("The tasks are: {  ");
		for(int i=0; i<tasks;i++)
			System.out.print(i + "  ");
		System.out.println("}\n");
		
		System.out.println("Number of processors: ");
		int processors = Integer.parseInt(br.readLine());
		writer.write(processors + "\n");
		
		System.out.println("Mean MIPS value: ");
		int meanmips = 1800;//Integer.parseInt(br.readLine());

		System.out.println("Heterogenity factor for MIPS of each processor(in percentage): ");
		int hetperc = 50;//Integer.parseInt(br.readLine());

		int mips[] = new int[processors];
		/*
		System.out.println("MIPS value for each processor: ");
		for(int i=0; i<processors; i++)
			mips[i] = Integer.parseInt(br.readLine());
		 */
		for(int i=0; i<processors; i++){
			double r = -hetperc + (2*hetperc*Math.random());
			mips[i] = ((int)((Math.max(1, (int)((double)meanmips* (1.0 + r/100.0))))/100))*100;
		}

		//display the mips value for each processor
		System.out.print("{");
		for(int i=0; i<processors-1; i++)
			System.out.print(mips[i] + ", " );
		System.out.print(mips[processors-1]);
		System.out.println(" }");
		//write the mips value to the file
		for(int currentmips: mips){
			writer.write(currentmips + "\t");
		}
		writer.write("\n");
		
		//System.out.println("Enter the number of processors");
		//int processors = Integer.parseInt(br.readLine());
		
		//System.out.println("Enter the MIPS value for each processor: ");
		//double processorMIPS[] =new double[processors] ;
		
		//for(int i=0;i<processors;i++)
			//processorMIPS[i] = Double.parseDouble(br.readLine());
		
		double communicationCost[][]=new double[tasks+1][tasks+1];
	    int avgcomputationCost[]=new int[tasks];
		double ETC[][]=new double[tasks][processors];
		int mat[][] = new int[tasks+1][tasks+1];
		//select random value for average computation cost of each task within range
		int wd = 2*wDag;
		for(int i =0; i<tasks; i++){
			avgcomputationCost[i] = (int)(Math.random()*24465)%wd +1;
		}
		
		//display the value for average computation cost
		System.out.println("Average computaion cost for each task: ");
		System.out.print("{ ");
		for(int i =0; i<tasks-1; i++){
			System.out.print(avgcomputationCost[i] + ", ");
		}
		System.out.println(avgcomputationCost[tasks-1]);
		System.out.println(" }\n");
		
		//write the value of avgcomputation cost into the file
		for(int compCost: avgcomputationCost){
			writer.write(compCost*1000 + "\t");
		}
		writer.write("\n");
		
		//find the computation time for each task on each processor using avgcomputationCost and mips
		for(int i =0; i<tasks; i++){
			for(int j =0; j<processors; j++){
				ETC[i][j] = avgcomputationCost[i] / mips[j];
			}
		}
		/*
		//display the ETC matrix
		System.out.println("\n\nThe ETC matrix is : ");
		for(int i =0; i<tasks; i++){
			for(int j =0; j<processors; j++){
				System.out.print(ETC[i][j]+ "  ");
			}
			System.out.println("\n");
		}
		*/
		//initialise the dependency matrix and the communicationCost matrix
		for(int i =0; i<tasks; i++){
			for(int j =0; j<tasks; j++){
				mat[i][j] = 0;
				communicationCost[i][j] = 0;
			}
		}
		/*/
		int i,j;
		for(i=1; i<tasks +1; i++){
			for(j=i+1; j<matrix; j++){
				mat[i][j] = 1;
				communicationCost[i][j] = ccr* avgcomputationCost[i];
				
			}
		}
		*/
		 
	     int m = matrix;int j;
		 m++;
	     int i=0;
	     int k=0;
	     for(;i<tasks;i++)
	     {               i=k;
	                     k=i+1;
	                     m--;
	                     for(j=1;j<m;j++)
	                     {
	                                       mat[i][k]=1;
	                                       communicationCost[i][k]=(int)(ccr*avgcomputationCost[k]);
	                                       k++;
	                              //System.out.println("The Communication Cost\n"+Node_Communication_Cost_Matrix[i][j]);       
	                     }
	                     
	     }
	  k=1;int l=matrix;i=1;int mc = matrix;
	     for(;i<tasks+1;i++)
	     {           
	                  i=k;
	                  l--;
	                  mc--;
	                  for(j=1;j<mc+1;j++)
	                  {
	                                   mat[k][k+l]=1;
	                                   communicationCost[k][k+l]=(int)(ccr*avgcomputationCost[k]);
	                                   k++;
	                                 
	                  }
	                  k++;
	     }
	     //display the precedence relation matrix
	     System.out.println("The predence relation matrix mat[][] is: ");
			//System.out.print(" \t"  );
			//for(int c=0; c<tasks; c++)
			//	System.out.print(c+"\t"  );
			System.out.print(" \n\n { "  );
			for(int t = 0; t<tasks; t++){
				System.out.print( "{ ");
				for(int f=0; f<tasks-1; f++){
					System.out.print(mat[t][f]+",\t");
				}
				if(t==tasks-1)
					System.out.println( mat[t][tasks-1]+" }");
				else
					System.out.println(mat[t][tasks-1] + " },");
			}
			System.out.println("}\n");
			
			//write the precedence relation matrix into the file 
			for(int f=0; f<tasks; f++){
				for(int g=0; g<tasks; g++){
					writer.write(mat[f][g] + "\t");
				}
				writer.write("\n");
			}
/*
	     System.out.println("The predence relation matrix is: ");
	     System.out.print(" \t"  );
	     
	     for(int c=0; c<tasks; c++)
	    	 System.out.print(c+"\t"  );
	     System.out.print(" \n\n"  );
	     for(i = 0; i<tasks; i++){
	    	 System.out.print(i+ "\t");
	    	 for( j=0; j<tasks; j++){
	    		 System.out.print(mat[i][j]+"\t");
	    	 }
	    	 System.out.println("\n");
	     }
	     
	     //display the communication cost matrix
	     System.out.println("The Communication Cost matrix is: ");
	     System.out.print(" \t"  );
	     for(int c=0; c<tasks; c++)
	    	 System.out.print(c+ "  \t"  );
	     System.out.print(" \n\n"  );
	    
	     for(i = 0; i<tasks+1; i++){
	    	 System.out.print(i+ "\t");
	    	 for( j=0; j<tasks+1; j++){
	    		 System.out.print(communicationCost[i][j]+"\t");
	    	 }
	    	 System.out.println("\n");
	     }
*/
	     //display the list of successors for each task
	     for(i=0;i<tasks;i++){
	    	 System.out.print("For " +i + ":  ");
	    	 for(j =0;j<tasks;j++){
	    		 if(mat[i][j] != 0)
	    			 System.out.print(j + "  ");
	    	 }
	    	 System.out.print("\n");
	     }
	     writer.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
}

