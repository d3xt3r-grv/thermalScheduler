//@Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

/*
 * the class specifies 
 * the power model used to calculate the power consumption by various processors
 */

package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;
//import org.cloudbus.cloudsim.scheduler.MCSimulation.Slot;
//import org.cloudbus.cloudsim.scheduler.BaseCloudletScheduler.VmParameters;

/*file workflow.txt saves the following information about the workflow generated, 
 * in the specified format:
 * number of tasks
 * number of virtual machines
 * mean MIPS values for each virtual machine, separated by \t
 * average computation cost for each task (MI value), separated by \t
 * precedence matrix for the workflow (matrix to represent task dependencies)
 */
/*
 * file vmparams.txt saves the informations as:
 * for each vm:
 * coefficient \t maxmips \t minmips
 */
public class SchedulerPowerModel {

	private static List <Vm> vmlist = new ArrayList();
	//public static Map <Vm, VmParameters> vmparams;

	public SchedulerPowerModel(List<Vm> vmlist){
		this.vmlist = vmlist;
		//vmparams = new HashMap<>();
	}

	protected static void initialise(){
		double coeff;
		double minmips, maxmips;
		try{
			File myFile =new File("workflow.txt");
			BufferedReader reader = new BufferedReader(new FileReader(myFile));
			File file = new File("vmparams.txt");
			BufferedWriter writer  = new BufferedWriter(new FileWriter(file));
			String line = null;
			//skip tasks
			line = reader.readLine();
			//skip vms
			line = reader.readLine();
			line = reader.readLine();
        	String[] result = line.split("\t");
        	int i =0;
        	System.out.println("Vm coefficient minimum frequency and maximum frequency: ");
			for(Vm vmObject: vmlist){
				Vm vm = (Vm) vmObject;
				coeff = 2;
				maxmips = Double.parseDouble(result[i]);
				i++;
				minmips =(0.4 * maxmips);
				writer.write(coeff + "\t" + maxmips + "\t" + minmips);
				
				System.out.println("vm " + vm.getId() + ":  " + coeff + "  " + maxmips + "  " + minmips );
				writer.write("\n");
			}
			
			reader.close();
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void run(){
		initialise();
		//print();
	}

}
