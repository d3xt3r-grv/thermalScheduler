//@author Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.*;

/*file workflow.txt contains the following information about the workflow to be executed, 
 * in the specified format:
 * number of tasks
 * number of virtual machines
 * mean MIPS value for each virtual machine, separated by \t
 * average computation cost for each task (MI value), separated by \t
 * precedence matrix for the workflow (matrix to represent task dependencies) 	
*/	

public class MCSRunner {

	private static List <Vm> vmlist = new ArrayList();
	
	public static void main(String args[]){
		
		try{
			File myFile =new File("workflow.txt");
        	BufferedReader reader = new BufferedReader(new FileReader(myFile));
        	String line = null; 
        	line = reader.readLine();
        	line = reader.readLine();
        	int vmno; 
        	vmno = Integer.parseInt(line);
        	int[] vmmips = new int[vmno];
        	line = reader.readLine();
        	String[] result = line.split("\t");
        	for(int i =0; i<result.length; i++){
        		vmmips[i] = Integer.parseInt(result[i]);
        	}
        	
        	
        	//VM Parameters
    		
        	long size = 10000; //image size (MB)
    		int ram = 512; //vm memory (MB)
    		int mips = 25;
    		long bw = 4;
    		int pesNumber = 1; //number of cpus
    		String vmm = "Xen"; //VMM name

    		//create VMs
    		Vm[] vm = new Vm[vmno];
    		int idShift = 0;
    		for(int v=0;v<vmno;v++){
    			
    			mips = vmmips[v];
    			vm[v] = new Vm(idShift + v, 0, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
    			vmlist.add(vm[v]);
    		}
    		
			MCSimulation mcs = new MCSimulation(vmlist, vmno);
			mcs.run();
			SchedulerPowerModel spm = new SchedulerPowerModel(vmlist);
			spm.run();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
