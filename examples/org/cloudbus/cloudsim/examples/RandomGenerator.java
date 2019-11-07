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

public class RandomGenerator {

	public static void main(String[] args) throws NumberFormatException, IOException {

		try{
			File file = new File("workflow.txt");
			//System.out.println(file.getAbsolutePath());
			//FileWriter writerfile = new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));	
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			int tasks, processors, perc, hetperc, meanmips, outdegree, wDag, height , width;
			double shape, ccr;

			System.out.println("Number of tasks in the graph: ");
//			tasks = 100;
			tasks=Integer.parseInt(br.readLine());
			writer.write(tasks + "\n");
			System.out.println("Maximum outdegree of nodes in the graph:");
//			outdegree = 6;
			outdegree=Integer.parseInt(br.readLine());

			System.out.println("Shape parameter for nodes in the graph:");
			shape = 0.5;//Double.parseDouble(br.readLine());

			System.out.println("Non Uniformity parameter for number of nodes at each level(in percentage): ");
			perc = 50;//Integer.parseInt(br.readLine());

			System.out.println("Communication to computation cost(ccr) for nodes in the graph: ");
			ccr = 0.2;//Double.parseDouble(br.readLine());

			System.out.println("wDag value for the graph: ");
			wDag = 7000000;//Integer.parseInt(br.readLine());

			System.out.println("Number of processors: ");
			processors = Integer.parseInt(br.readLine());
			writer.write(processors + "\n");

			System.out.println("Mean MIPS value: ");
			meanmips = 2000;//Integer.parseInt(br.readLine());

			System.out.println("Heterogenity factor for MIPS of each processor(in percentage): ");
			hetperc = 50;//Integer.parseInt(br.readLine());

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
			//write the mips value for each processor into the file object separated by tabspaces
			for(int currentmips: mips){
				writer.write(currentmips + "\t");
			}
			writer.write("\n");

			//display the mips value for each processor
			System.out.print("{");
			for(int i=0; i<processors-1; i++)
				System.out.print(mips[i] + ", " );
			System.out.print(mips[processors-1]);
			System.out.println(" }");

			//calculate height and width of the graph to be constructed
			height = (int)(Math.sqrt(tasks) / shape) +1;
			width = (int)(Math.sqrt(tasks) * shape) +1;
			System.out.println("Calculated mean Height of the graph:  " + height);
			System.out.println("Calculated mean Width of the graph:  " +width);
			System.out.println();


			//determine the no of tasks in each level and total number of levels
			int arr[] = new int[501];
			int tasksleft = tasks;
			int levels =0;

			int currentlevel = 0;
			//assign one task to the first level
			arr[0] = 1;
			currentlevel++;
			tasksleft--;

			//min and max width assigned acc to uniform distribution
			int minimum = 1;
			int maximum = 2*width - minimum;
			System.out.println("Minimum width: " + minimum + " Maximum width:  " + maximum);
			System.out.println();
			while(true){
				//double a = Math.random();
				//System.out.println(a);
				//int temp = (int)(Math.random() * maximum);
				//System.out.println(temp);
				double r = -perc + (2*perc*Math.random());
				int ntasks = Math.max(1, (int)((double)width* (1.0 + r/100.0)));
				//int ntasks = Math.max(1, temp);
				//System.out.println(ntasks);
				if((tasksleft-1) <ntasks)
					ntasks = tasksleft -1;
				//System.out.println(ntasks);
				arr[currentlevel] = ntasks;
				tasksleft -= ntasks; 
				currentlevel++;
				if(tasksleft<=1)
					break;
			}
			//assign one task to the last level
			arr[currentlevel] = 1;
			tasksleft--;
			//because currentlevel starts with 0 index
			levels = currentlevel + 1;

			//display the tasks at each level and assign to array nodes
			int nodes[][] = new int[levels][maximum];
			//initialise nodes array which stores the tasks' id at each level, -1 to show the end
			for(int i =0; i<levels; i++)
				for(int j =0; j<maximum; j++)
					nodes[i][j] = -1;
			int k = 0;
			for(int i =0; i<levels; i++){
				System.out.print("At level " + i +" :  " + arr[i] + " tasks: ");
				for(int j = 0; j<arr[i]; j++){
					System.out.print("\t" + k );
					nodes[i][j]= k;
					k++;
				}
				System.out.println();
			}

			double ETC[][] = new double[tasks][processors];
			double communicationCost[][]=new double[tasks+1][tasks+1];
			int mat[][] = new int[tasks+1][tasks+1];
			//initialise the ETC matrix
			for(int i=0; i<tasks; i++ )
				for(int j=0;j<processors;j++)
					ETC[i][j] = 0;
			//assign random computation cost to each task acc to the value of wdag
			int wd = 2*wDag;
			int avgComputationCost[] = new int[tasks];
			for(int i =0; i<tasks; i++)
				avgComputationCost[i] = (int)(Math.random()*24465)%wd +1;

			//write into file the average computation cost for each task 
			for(int compCost: avgComputationCost){
				writer.write(compCost *1000 + "\t");
			}
			writer.write("\n");

			//display the value for average computation cost
			System.out.println("Average computaion cost for each task: ");
			System.out.print("{ ");
			for(int i =0; i<tasks-1; i++){
				System.out.print(avgComputationCost[i] + ", ");
			}
			System.out.println(avgComputationCost[tasks-1]);
			System.out.println(" }\n");

			//find the computation time for each task on each processor using avgcomputationCost and mips
			for(int i =0; i<tasks; i++){
				for(int j =0; j<processors; j++){
					ETC[i][j] = avgComputationCost[i] / (double)mips[j];
				}
			}

			//display the ETC Matrix
			/*
		System.out.println("\n\nThe ETC matrix is :\n { ");
		for(int i =0; i<tasks; i++){
			System.out.print("{");
			for(int j =0; j<processors; j++){
				if(j== processors -1)
					System.out.println(ETC[i][j]);
				else
					System.out.print(ETC[i][j]+ ",  ");
			}
			if (i==tasks-1)
				System.out.println("}\n");
			else
				System.out.println("},\n");
		}
		System.out.println("}");
			 */
			//initialise the dependency matrix and the communicationCost matrix
			for(int i =0; i<tasks; i++){
				for(int j =0; j<tasks; j++){
					mat[i][j] = 0;
					communicationCost[i][j] = 0;
				}
			}

			//create dependencies
			//int id =1;
			//children specifies no of children for each task
			int children;
			int currenttask =0;
			for(int i=0; i<levels-1;i++){
				for(int j=0; j<arr[i]; j++){
					//determine the number of children, outdegree is subtracted by one to later compensate for task with no parent
					children = Math.min(Math.max(1, (int)(Math.random()*arr[i+1])), outdegree-1);
					//available here specifies the maximum no of children possible
					int available = arr[i+1];
					for(int c=0;c<children; c++){
						//randomly select  a child task id from the next level
						int childtask = nodes[i+1][0] + (int)(Math.random()*(nodes[i+1][available-1] - nodes[i+1][0] + 1));
						if(mat[currenttask][childtask] ==0){
							mat[currenttask][childtask] = 1;
							communicationCost[currenttask][childtask] = (double)(ccr*avgComputationCost[childtask]);
						}
					}
					currenttask++;	
				}
			}

			//determine a parent for intermediate tasks with no parent
			currenttask =1;
			for(int i=1;i<levels; i++){

				for(int j=0; j<arr[i]; j++){
					int flag =0;
					for(int c= 0; c<tasks; c++){
						if(mat[c][currenttask] !=0)
							flag =1;
					}
					//flag is 0 if the current task has no parent 
					if(flag ==0){
						int available = arr[i-1];
						int parent = nodes[i-1][0] + (int)(Math.random()*(nodes[i-1][available-1] - nodes[i-1][0] + 1));
						mat[parent][currenttask] = 1;
					}
					currenttask++;
				}
			}

			//write the precedence relation matrix into the file 
			for(int i=0; i<tasks; i++){
				for(int j=0; j<tasks; j++){
					writer.write(mat[i][j] + "\t");
				}
				writer.write("\n");
			}

			//display the precedence relation matrix
			System.out.println("The predence relation matrix mat[][] is: ");
			//System.out.print(" \t"  );
			//for(int c=0; c<tasks; c++)
			//	System.out.print(c+"\t"  );
			System.out.print(" \n\n { "  );
			for(int i = 0; i<tasks; i++){
				System.out.print( "{ ");
				for(int j=0; j<tasks-1; j++){
					System.out.print(mat[i][j]+",\t");
				}
				if(i==tasks-1)
					System.out.println( mat[i][tasks-1]+" }");
				else
					System.out.println(mat[i][tasks-1] + " },");
			}
			System.out.println("}\n");

			//display the communication cost matrix
			/*
		System.out.println("The Communication Cost matrix is: \n");
		//System.out.print(" \t"  );
		//for(int c=0; c<tasks; c++)
		//	System.out.print(c+ "  \t"  );
		System.out.print(" \n {"  );

		for(int i = 0; i<tasks; i++){
			System.out.print( "{ ");
			for(int j=0; j<tasks-1; j++){
				System.out.print(communicationCost[i][j]+",\t");
			}
			if(i == tasks-1)
				System.out.println(communicationCost[i][tasks-1] + " }\n");
			else
				System.out.println(communicationCost[i][tasks-1] + " }, \n");
			System.out.println(" } \n");
		}
			 */
			//display the list of successors for each task
			for(int i=0;i<tasks;i++){
				System.out.print("For " +i + ":  ");
				for(int j =0;j<tasks;j++){
					if(mat[i][j] != 0)
						System.out.print(j + "  ");
				}
				System.out.print("\n");
			}
			writer.close();
		} catch(Exception e){
			System.out.println("Exception at writing to file");
			e.printStackTrace();

		}
	}
}


