package Plotter;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.math3.util.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.SymbolicXYItemLabelGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ScatterPlot extends JFrame {
    private static final long serialVersionUID = 6294689542092367723L;

    public XYDataset dataset;

    public Stack<Pair<List<List<Double>>, Integer>> data;

    public ScatterPlot(String title) {
        super(title);

        // Create dataset
        dataset=new XYSeriesCollection();
        data= new Stack<>();
    }

    public void addValues(List<List<Double>> fitnessArchive, int iter) {

        XYSeries series = new XYSeries(iter);
        for(List<Double> values: fitnessArchive){
            series.add(values.get(0),values.get(1));
        }

        ((XYSeriesCollection)dataset).addSeries(series);

    }

    public void plot(){
        SwingUtilities.invokeLater(() -> {
            // Create chart
            JFreeChart chart = ChartFactory.createScatterPlot(
                    "Time vs Energy PLot",
                    "TIME(s)", "ENERGY(J)", dataset);


            //Changes background color
            XYPlot plot = (XYPlot)chart.getPlot();
            plot.setBackgroundPaint(new Color(255, 255, 255));

            // Create Panel
            ChartPanel panel = new ChartPanel(chart);
            setContentPane(panel);
            this.setSize(600, 600);
            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            this.setVisible(true);
        });
    }
}
