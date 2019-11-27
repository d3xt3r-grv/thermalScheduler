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
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ScatterPlot extends JFrame {

    public XYDataset dataset;

    public static int counter =0;

    public Stack<Pair<List<List<Double>>, Integer>> data;

    public ScatterPlot(String title) {
        super(title);

        // Create dataset
        dataset=new XYSeriesCollection();
        data= new Stack<>();
    }

    public void addValues(List<List<Double>> fitnessArchive, int iter) {

        XYSeries series = new XYSeries(iter);
        for(int i=0;i<fitnessArchive.size();i++){
            series.add(fitnessArchive.get(i).get(0),fitnessArchive.get(i).get(1));
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
            XYLineAndShapeRenderer render = (XYLineAndShapeRenderer) plot.getRenderer();
            render.setBaseToolTipGenerator(xyToolTipGenerator);
            // Create Panel
            ChartPanel panel = new ChartPanel(chart);
            setContentPane(panel);
            this.setSize(600, 600);
            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            this.setVisible(true);
        });
    }


    XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator()
    {
        public String generateToolTip(XYDataset dataset, int series, int item)
        {
            Number x1 = dataset.getX(series, item);
            Number y1 = dataset.getY(series, item);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format("<html><p style='color:#0000ff;'>Series: '%s'</p>", dataset.getSeriesKey(series)));
            stringBuilder.append(String.format("SOl. Id '%d' <br>",item));
            stringBuilder.append(String.format("Time: %f s<br/>", x1.doubleValue()));
            stringBuilder.append(String.format("Energy: %f kWh", y1.doubleValue()));
            stringBuilder.append("</html>");
            return stringBuilder.toString();
        }
    };
}
