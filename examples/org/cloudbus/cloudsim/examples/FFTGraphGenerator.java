//@author Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.lang.Math;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/*file workflow.txt saves the following information about the workflow generated, 
 * in the specified format:
 * number of tasks
 * number of virtual machines
 * mean MIPS values for each virtual machine, separated by \t
 * average computation cost for each task (MI value), separated by \t
 * precedence matrix for the workflow (matrix to represent task dependencies)
*/

public class FFTGraphGenerator {

	public static void main(String[] args) throws NumberFormatException, IOException {
		try{

			File file = new File("workflow.txt");
			//System.out.println(file.getAbsolutePath());
			//FileWriter writerfile = new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));	
			BufferedReader br=new BufferedReader( new InputStreamReader(System.in));

			System.out.println("\nStarting Random Graph generation for FFT\n");

			System.out.println("Enter size of the array");
			int arrsize = 16;//Integer.parseInt(br.readLine());

			System.out.println("Enter the value for wDag");
			int wDag =7000000;//Integer.parseInt(br.readLine());

			System.out.println("Enter the Communication to Computation cost Ratio(ccr) value: ");
			Double ccr = 0.2;//Double.parseDouble(br.readLine());

			//define the number of recursive calls
			int rec = 2*arrsize -1;

			//define the number of butterfly calls
			int bfly = (int) ((Math.log(arrsize) / Math.log(2.0)) * arrsize);
			//System.out.println(bfly);

			int tasks = rec + bfly+1;
			int deadindex = tasks-1;
			System.out.println("The number of tasks are: " + tasks);
			writer.write(tasks + "\n");
			System.out.println("The tasks are: {  ");
			for(int i=0; i<tasks;i++)
				System.out.print(i + "  ");
			System.out.println("}\n");

			int a[][] = new int[arrsize+1][arrsize+1];

			//initialise the array a with binary values
			int temp = (int)(Math.log(arrsize)/Math.log(2.0));
			int nm;
			int z;
			//for(z=0; z<arrsize; z++)
			for( z=0; z<arrsize; z++){
				nm = z;
				for(int i =temp-1; i>-1; i--){
					a[z][i] = nm%2;
					nm = nm/2;
				}

			}
			System.out.println("Number of processors: ");
			int processors = 64;//Integer.parseInt(br.readLine());
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

			//write the mips value to the file
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
			//System.out.println("Enter the number of processors");
			//int processors = Integer.parseInt(br.readLine());

			//System.out.println("Enter the MIPS value for each processor: ");
			//double processorMIPS[] =new double[processors] ;

			//for(int i=0;i<processors;i++)
			//processorMIPS[i] = Double.parseDouble(br.readLine());

			double communicationCost[][]=new double[tasks+1][tasks+1];
			int avgcomputationCost[]=new int[tasks +1];
			double ETC[][]=new double[tasks][processors];
			int mat[][] = new int[tasks+1][tasks+1];

			//select random value for average computation cost of each task within range
			int wd = 2*wDag;
			for(int i =0; i<tasks-1; i++){
				avgcomputationCost[i] = (int)(Math.random()*24465)%wd +1;
			}
			avgcomputationCost[deadindex] = 0;

			//display the value for average computation cost
			System.out.println("Average computaion cost for each task: ");
			System.out.print("{ ");
			for(int i =0; i<tasks-1; i++){
				System.out.print(avgcomputationCost[i] + ", ");
			}
			System.out.println(avgcomputationCost[tasks-1]);
			System.out.println(" }\n");

			//write the value of avgcomputation cost into the file
			for(int i =0; i<tasks-1; i++){
				writer.write(avgcomputationCost[i]*1000 + "\t");
			}
			writer.write("0");
			writer.write("\n");
			//find the computation time for each task on each processor using avgcomputationCost and mips
			for(int i =0; i<tasks-1; i++){
				for(int j =0; j<processors; j++){
					ETC[i][j] = avgcomputationCost[i] / mips[j];
				}
			}
			for(int j =0; j<processors; j++){
				ETC[deadindex][j] = 0.0000000000;
			}

			//display the ETC matrix
			/*
		System.out.println("\n\nThe ETC matrix is :\n ");
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
			//recursive call task values for dependency and communicationcost
			for(int i =0; i<arrsize; i++){
				mat[i][2*i+1] = 1;
				mat[i][2*i+2] = 1;
				communicationCost[i][2*i+1] = (int)(ccr*avgcomputationCost[i]);
				communicationCost[i][2*i+2] = (int)(ccr*avgcomputationCost[i]);
			}

			//butterfly cost values for dependency and communicatincost
			int g=1,h=2, level,i,l, num1, num2,k;
			for(level=temp;level>0;level--)
			{
				for(i=0;i<arrsize;i++)
				{

					l=0; num1=0;
					for(k=temp-1;k>-1;k--)
					{
						num1=  num1+ (int)(Math.pow(2,l)*a[i][k]);
						l++;
					}

					if(a[i][level-1]==1)
					{
						a[i][level-1]=0;
					}
					else
					{
						a[i][level-1]=1;
					}
					num2=0;l=0;
					for(k=temp-1;k>-1;k--)
					{
						num2=num2+(int)(Math.pow(2,l)*a[i][k]);
						l++;
					}
					mat[num1+(g*arrsize)-1][num2+(h*arrsize)-1]=1;
					mat[num1+(g*arrsize)-1][num1+(g*arrsize)+arrsize-1]=1;
					communicationCost[num1+(g*arrsize)-1][num2+(h*arrsize)-1]=(int)(ccr*avgcomputationCost[num2+(h*arrsize)-1]);
					communicationCost[num1+(g*arrsize)-1][num1+(g*arrsize)+arrsize-1]=(int)(ccr*avgcomputationCost[num1+(g*arrsize)+arrsize-1]);

				}
				g++;
				h++;

			}

			//setting the dependency for the deadtask
			for(i=tasks-1-arrsize;i<tasks-1;i++)
				mat[i][tasks-1] = 1;

			//display the precedence relation matrix
			System.out.println("The precedence relation matrix is: \n");
			System.out.print(" \t"  );
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
				for(int j=0; j<tasks; j++){
					writer.write(mat[f][j] + "\t");
				}
				writer.write("\n");
			}
			/*		
		//display the communication cost matrix
		System.out.println("The Communication Cost matrix is: \n");
	     System.out.print(" \t"  );
	     for(int c=0; c<tasks; c++)
	    	 System.out.print(c+ "  \t"  );
	     System.out.print(" \n\n"  );

	     for(int c = 0; c<tasks; c++){
	    	 System.out.print(c+ "\t");
	    	 for(int j=0; j<tasks; j++){
	    		 System.out.print(communicationCost[c][j]+"\t");
	    	 }
	    	 System.out.println("\n");
	     }
			 */	     

			 //display the list of successors for each task
			System.out.println("The successor list for all the task nodes is: \n");
			for(int c=0;c<tasks;c++){
				System.out.print("For " +c + ":  ");
				for(int j =0;j<tasks;j++){
					if(mat[c][j] != 0)
						System.out.print(j + "  ");
				}
				System.out.print("\n");
			}
			writer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

}
