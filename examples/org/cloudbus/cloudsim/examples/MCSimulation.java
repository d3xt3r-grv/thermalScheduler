//@Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

/*the file MCSimulation.txt contatins the ttf and ttr for each vm in the list as:
 * ttf1 ttr1 ttf1 ttr2.....ttfn ttrn (for vm0)
 * ...
 * ttf1 ttr1 ttf2 ttr2 ... ttfm ttrm (for vmj) 
 */
import org.apache.commons.math3.special.*;
public class MCSimulation {

	public class Slot {

		public double failure;
		public double repair;

		public Slot(double failure, double repair) {
			this.failure = failure;
			this.repair = repair;
		}
	}
	private List <Vm> vmlist = new ArrayList();
	public static Map <Vm, List<Slot>> vmnotavailable;
	int nvm =0;
	//int nvm = vmlist.size();
	/*the number of simulations to be performed for each virtual machine*/
	public static final int SIMULATIONS = 2000;
	/*the maximum number of failures and repairs that should be calculated*/
	public static final int MAXFR = 160; //there can be maximum 80 failures and repairs
	//recalculation factor
	public static final int RECAL = 50;

	//server failure parameters
	double scalef[];
	double shapef[];

	//server repair parameters
	double scaler[];
	double shaper[];

	double ttf[][];
	double ttr[][];

	//for avg time to failure and avg time to repair
	static double avgttf[];
	static double avgttr[];
	int f,r;

	//for specifying number of failures and number of repairs for each simulation
	int nf[] = new int[SIMULATIONS];
	int nr[] = new int[SIMULATIONS];

	public MCSimulation(List <Vm> vmlist, int vmno){
		this.vmlist = vmlist;
		vmnotavailable = new HashMap<>();
		nvm = vmno;
		//server failure parameters
		scalef = new double[nvm];
		shapef = new double[nvm];
		//server repair parameters
		scaler = new double[nvm];
		shaper = new double[nvm];

		//there can be total 80 failures & repairs
		ttf = new double[SIMULATIONS][MAXFR];
		ttr = new double[SIMULATIONS][MAXFR];

		//for avg time to failure and avg time to repair
		avgttf = new double[MAXFR];
		avgttr = new double[MAXFR];
	}

	private void assignWeibullParameters(){
		for(int i=0; i<nvm; i++){
			scalef[i] = 12.80 + (Math.random()*14.50);
			shapef[i] = 1.641 + (Math.random()* 2.191);

		}
		for(int i=0; i<nvm; i++){
			scaler[i] = 2.50 + (Math.random()*3.50);
			shaper[i] = 1.641 + (Math.random()* 2.191);

		}
	}

	private void printWeibullParameters(){
		for(int i=0; i<nvm; i++){
			System.out.println("for vm " + i);
			System.out.println("Scale parameter for failure: " + scalef[i]);
			System.out.println("Shape parameter for failure: " + shapef[i]);
			System.out.println("scale parameter for repair: " + scaler[i]);
			System.out.println("Shape parameter for repair: " + shaper[i]);
		}

	}

	private void initialize(){
		//initialise the failure and repair arrays to contain max
		for(int i= 0; i<SIMULATIONS; i++){
			for(int j = 0; j< MAXFR; j++){
				ttf[i][j] = Integer.MAX_VALUE ;
				ttr[i][j] = Integer.MAX_VALUE;

			}
		}
		for(int i =0; i<MAXFR; i++){
			avgttf[i] = 0; //Integer.MAX_VALUE;
			avgttr[i] = 0; //Integer.MAX_VALUE;
		}
	}

	private void compute(){

		try{
			File file = new File("MCSimulation.txt");
			//System.out.println(file.getAbsolutePath());
			//FileWriter writerfile = new FileWriter(file);
			//ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for (Object vmObject : vmlist){//for each vm
				Vm vm = (Vm) vmObject;
				vmnotavailable.put(vm, new ArrayList<Slot>());
				List<Slot> slots = vmnotavailable.get(vm);
				int vmi = vm.getId();
				double temp = 0.0 , tempr;
				for(int sim = 0; sim <SIMULATIONS; sim++ ){
					// for each simulation 
					double clock = 0.0;
					double tempfail[] = new double[RECAL];
					double avgfail =0.0, avgrep =0.0;
					double temprep[] = new double[RECAL];
					int fi=0, ri=0;//fi and ri represent failure and repair index respectively
					do{
						for(int i =0; i< RECAL; i++){
							double rf = Math.random();//rf represents random variable for failure 
							tempfail[i] = scalef[vmi] * Math.pow(-Math.log(rf), 1 / shapef[vmi]);
							//System.out.println("faliure :  " + tempfail[i]);
							avgfail = avgfail + tempfail[i];
							double rr = Math.random();//rr represents random variable for repair
							temprep[i] = scaler[vmi] * Math.pow(-Math.log(rr), 1 / shaper[vmi]);
							//System.out.println("repair :  " + temprep[i]);
							avgrep = avgrep + temprep[i];
						}
						avgfail = avgfail/RECAL;
						avgrep = avgrep/RECAL;
						//Math.pow((-Math.log(rf)/scalef[vmi]), (1/shapef[vmi])) *100;
						clock += avgfail ; 
						ttf[sim][fi] = clock; 
						//ttf[sim][fi];//advance clock upto ttf 
						fi++;
						//double rr = Math.random();//rr represents random variable for repair
						//temp = scaler[vmi] * Math.pow(-Math.log(rr), 1 / shaper[vmi]);
						//temp = Math.pow((-Math.log(rr)/scaler[vmi]), (1/shaper[vmi]))*10;
						clock += avgrep;//ttr[sim][ri];//advance clock upto ttr
						ttr[sim][ri] = clock;
						ri++;
						if((fi + ri) > MAXFR)
							break;
					}while(clock < 500.0);
					nf[sim] = fi -1; 
					nr[sim] = ri- 1;

				}
				//double value1 = 1 + 1/0.6;
				//double mttf = 1000* Math.exp(Math.log((Gamma.gamma(value1))));
				//System.out.println("What you neeed is " + value);
				/*
		System.out.println("TTf and ttr for each sim");
		for(int i =0; i<SIMULATIONS; i++){
			System.out.println();
			for(int j =0; j<MAXFR; j++){
				System.out.print("    " + ttf[i][j]+ "  "+ ttr[i][j]);
			}
		}
				 */
				/*
		System.out.println("TTR array");
		for(int i =0; i<SIMULATIONS; i++){
			System.out.println();
			for(int j =0; j<20; j++){
				System.out.print("  " + ttr[i][j]);
			}
		}
				 */
				//finding out the average values for all SIMULATIONS
				f=0; r=0;
				int position  = 0;
				//fi= 0; ri =0;
				for(f=0, r=0; r+f <MAXFR; r++, f++){
					int sf=0, sr =0;
					for(int sim=0; sim<SIMULATIONS; sim++){
						if(ttf[sim][f] < Integer.MAX_VALUE){
							avgttf[f] += ttf[sim][f];
							sf++;
						}
						if(ttr[sim][r] < Integer.MAX_VALUE){
							avgttr[r] += ttr[sim][r];
							sr++;
						}
						//f++; r++;	
						//sf and sr represent the total no of failures and repairs simulated 
					}
					avgttf[f] = avgttf[f] / sf;
					avgttr[r] = avgttr[r] / sr;

					slots.add(position, new Slot(avgttf[f], avgttr[r]));
					writer.write(avgttf[f] + "\t");
					writer.write(avgttr[r] + "\t");
					position++;
					//nf[f] = sf;
					//nf[r] = sr;
				}
				/*
		for(int i=0; i<f; i++){
			avgttf[i] = avgttf[i] / SIMULATIONS;
			//avgttr[i] = avgttr[i] / SIMULATIONS;
		}
		for(int i=0; i<r; i++){
			//avgttf[i] = avgttf[i] / SIMULATIONS;
			avgttr[i] = avgttr[i] / SIMULATIONS;
		}
				 */
				System.out.println("\n avgttf and avgttr for the vm "+ vm.getId() );
				System.out.print("{");
				for(int i=0; i<MAXFR/2; i++){
					System.out.print(" "+avgttf[i] + ", " );
				}
				System.out.println("}");
				System.out.print("{");
				for(int i=0; i<MAXFR/2; i++){
					System.out.print(" "+avgttr[i] + ", " );
				}
				System.out.println("}");

				/*
		System.out.println("\navgttr array");
		for(int i=0; i<20; i++){
			System.out.print(avgttr[i] + "  "  );
		}
				 */
				writer.write("\n");
			}
			writer.close();

		}catch(IOException e){
			e.printStackTrace();
		}
	
	}
	public void run(){

		//specify scale and shape weibull distribution parameters

		assignWeibullParameters();
		printWeibullParameters();
		initialize();
		compute();


	}

}