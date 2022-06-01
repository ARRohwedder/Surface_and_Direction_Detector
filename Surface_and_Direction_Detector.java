import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.*;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;



/** The Surface_Observer plugin generates a circular observer outside an image to generate outlines ignoring enclosed structures. Works for grey level images. */
public class Surface_and_Direction_Detector implements PlugIn {
    static double newThresh = 20;
    static double newdirect = 30;
    static double newfilos = 2;
    static double newcover = 30;
    static int roundradius,rxposb,ryposb;
    static double sumx, sumy;

    public void run(String arg) {

        if (IJ.getImage().getBitDepth()!=8){IJ.showMessage("Grey scale measure", "Grey scale Image required");}
        String directory =   IJ.getDir("plugins");
        directory = directory + "Analyze/sketch.jpg";
        directory = directory.replace('\\', '/');
        int lang = 0;
        int stackcount = 0;

        ImagePlus imp1 = IJ.getImage();
        ImagePlus imp2 = imp1.duplicate();
        imp2.setCalibration(imp1.getCalibration());
        ImageStack stack = imp1.getStack();
        ImageStack stack2 = imp2.getStack();
        ImagePlus Sketch = IJ.openImage (directory);
        int size = stack.getSize();

        String title = imp1.getTitle();
        Calibration cal = imp1.getCalibration();
        ImageProcessor ip = (ImageProcessor)imp1.getProcessor();
        ImageProcessor ip2 = (ImageProcessor)imp2.getProcessor();
        double pixres = cal.pixelWidth;

//Dialog
        GenericDialog gd = new GenericDialog("Threshold and membrane cover", IJ.getInstance());
        gd.addNumericField("Grey value threshold for detection: ", newThresh, 3);
        gd.addMessage("Membrane cover describes the expected size proportion for a structure in stretch of membrane.");
        gd.addImage(Sketch);
        gd.addMessage("Percentage should be larger (>= 30 percent and larger) for large protrusion structures.");
        gd.addMessage("Percentage should be smaller (~ 2 percent) for small filopodia structures.");
        gd.addNumericField("Percentage for major protrusions: ", newdirect, 3);
        gd.addNumericField("Percentage for filopodia: ", newfilos, 3);
        gd.addNumericField("Image coverage of cell: (percentage of whole image)", newcover, 3);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        if (gd.invalidNumber()) {
            IJ.showMessage("Error", "Threshold not in range");
            return;
        }
        newThresh = gd.getNextNumber();
        newdirect = gd.getNextNumber();
        newfilos = gd.getNextNumber();
        newcover = gd.getNextNumber();

//output images setup
        int w = ip.getWidth();
        int h = ip.getHeight();
        ImageStack outstack = stack.duplicate();
        ImageProcessor outstackip = (ImageProcessor)imp1.getProcessor();
        ImagePlus first = IJ.createImage("Plasmamembrane", "8-bit black", w, h, 1);// from automatic selection
        ImageProcessor firstip = (ImageProcessor)first.getProcessor();
        first.setCalibration(imp1.getCalibration());

        ImageProcessor stackip = (ImageProcessor)imp1.getProcessor();
        ResultsTable rt1 = new ResultsTable();
        ResultsTable rt2 = new ResultsTable();

for (int i=1;i<size+1;i++){
        //copy image and prepare threshold image
        stackip = stack2.getProcessor(i);
        //ip2.invert();
        stackip.invert();
        //-----------------
        imp2.setSlice(i);
        //-----------------
        IJ.setAutoThreshold(imp2, "Huang");

        RankFilters desp = new RankFilters();

        desp.rank(stackip, 1.0, desp.MEDIAN,desp.BRIGHT_OUTLIERS,0);

        //get outline
        RoiManager manager = RoiManager.getInstance();
        if (manager == null)
        {
            manager = new RoiManager();
        }
        ResultsTable tempResults = new ResultsTable();
        ParticleAnalyzer party1 = new ParticleAnalyzer( ParticleAnalyzer.ADD_TO_MANAGER,Measurements.CENTER_OF_MASS,tempResults,((w*h/100)*newcover),((w-50)*(h-50)));
        //---------------------------

        //---------------------------
        party1.analyze(imp2);

        //IJ.log("slice "+i+" step 1 ");

        tempResults = manager.multiMeasure(imp2);
        Roi roix = manager.getRoi(0);
        ip.setColor(255);
        //---------------------------
        roix.drawPixels(ip);
        //---------------------------
        FloatPolygon dings = new FloatPolygon();
        dings = roix.getFloatPolygon();
        lang = dings.npoints;
        double large = (lang/100)*newdirect;
        double small = (lang/100)*newfilos;
        //IJ.log("All "+lang+" large "+large+" small "+small);
        tempResults.reset();

        // get radius (distance to centre)
        double radiusnew [] = new double[lang];
        double radtrans = 0;
        int x1[] = new int[lang];
        int y1[] = new int[lang];
        int sumx = 0;
        int sumy = 0;
        for (int test=0; test < lang; test++){
                sumx = sumx + (int) Math.round(dings.xpoints[test]);
                sumy = sumy + (int) Math.round(dings.ypoints[test]);
        }
        double centerx = sumx/lang;
        double centery = sumy/lang;

        //IJ.log("slice "+i+" step2 ");

        for (int test=0; test < lang; test++){
            x1[test] = (int) Math.round(dings.xpoints[test]);
            y1[test] = (int) Math.round(dings.ypoints[test]);
            //IJ.log("slice "+i+" x "+x1[test]+ " y "+ y1[test]); Correct
            radtrans = Math.pow(Math.abs(x1[test]-centerx),2)+Math.pow(Math.abs(y1[test]-centery),2);
            radiusnew[test] = Math.sqrt(radtrans);
            //IJ.log(""+radiusnew[test]);//Correct
        }
        //get rounded radius
        double radiusround [] = new double[lang];
        double sumdist = 0; //sumcount,
        int correctpos = 0;
        for (int test=0; test < lang; test++){
                for (int i2=0;i2 < (lang/3);i2++){
                    correctpos = test+i2;
                    if (correctpos>= lang) {correctpos = correctpos-lang;}
                    sumdist = sumdist + radiusnew[correctpos];
                }
                radiusround[test] = sumdist/(lang/3);
                //IJ.log("slice "+i+" radius "+radiusround[test]);correct
                sumdist = 0;
        }

        //get rounded positions
        double xround [] = new double[lang];
        int xroundint [] = new int[lang];
        double yround [] = new double[lang];
        int yroundint [] = new int[lang];
        double angle;
        int roundsumx = 0; int roundsumy = 0;
        for (int test=0; test < lang; test++){
                angle = Math.acos((Math.abs(centery-dings.ypoints[test]))/radiusnew[test]);
                if ((centery-dings.ypoints[test])>0){
                    yround[test]=centery+(radiusround[test]*Math.cos(Math.toRadians(angle)));
                }
                if ((centerx-dings.xpoints[test])>0){
                    xround[test]=centerx+(radiusround[test]*Math.sin(Math.toRadians(angle)));
                }
                if ((centery-dings.ypoints[test])<0){
                    yround[test]=centery-(radiusround[test]*Math.cos(Math.toRadians(angle)));
                }
                if ((centerx-dings.xpoints[test])<0){
                    xround[test]=centerx+(radiusround[test]*Math.sin(Math.toRadians(angle)));
                }
                yroundint[test] = (int) Math.round(yround[test]);
                xroundint[test] = (int) Math.round(xround[test]);
                roundsumx = roundsumx + xroundint[test];
                roundsumy = roundsumy + yroundint[test];
        }
        manager.reset();

        double roundcenterx = roundsumx/lang;
        double roundcentery = roundsumy/lang;

        IJ.log("slice: "+i+" Horizontal distance of outer/inner center of mass: "+((centerx-roundcenterx)*pixres)+" Vertical distance of outer/inner center of mass: "+((centery-roundcentery)*pixres));

        //calculate major protrusions

        //------------------------------------------------------------------------------------------------------

    int slopecount = 0;
    int slopestart = 0;
    int slopelength = 0;
    double slope [] = new double[lang];
    //double negradius [] = new double[lang];
    //for (int test = 0; test < lang; test++){negradius[test] = radiusnew[test]*(-1);}
    //IJ.log("slice "+i+" step3 "+lang);
    slope = slopecalculation(large,lang,radiusnew);
    //for (int test = 0; test < lang; test++){slope[test] = slope[test]*(-1);}
//IJ.log("slice "+i+" step4 ");

    double revradius [] = new double[lang];
    revradius = reverser(lang, radiusnew);
    //revradius = reverser(lang, negradius);
//IJ.log("slice "+i+" step5 ");

    double revslopeA [] = new double[lang];
    revslopeA = slopecalculation(large,lang,revradius);

//IJ.log("slice "+i+" step6 ");
    double revslope [] = new double[lang];
    revslope =  reverser(lang, revslopeA);
    //for (int test = 0; test < lang; test++){revslope[test] = revslope[test]*(-1);}

    //for (int test = 0; test < lang; test++){
        //IJ.log("slope:"+slope[test]+" revslop:"+revslope[test]+" radius:"+radiusnew[test]);
    //}
    //IJ.log(""+radiusnew[test])
//IJ.log("slice "+i+" step6 ");


//calculate major protrusions

    stackcount = 0;
    stackcount = (i-1)*4;
//show major protrusions
        //for (int major = 1; major < lang; major++){
        //    if (revslope[major]+slope[major]<0){
                    //revslope[major]+slope[major]>0
        //        slopecount++;
        //        slopestart = major;
        //        while (slope[major]+revslope[major]<0) {major++;}
                //if (major<lang) {while ((slope[major]+revslope[major])*(-1)<0 && major<lang) {major++;}}
        //        rt1.incrementCounter();
        //        slopelength = major - slopestart;
        //        rt1.addValue("X position slice "+i, x1[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
        //        rt1.addValue("Y position slice "+i, y1[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
        //        rt1.addValue("Broad "+i, slopelength);
        //        rt1.addValue("Length "+i, radiusnew[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
        //        stackip.setColor(0);
        //        stackip.drawLine(((int) Math.round(centerx)),((int) Math.round(centery)),x1[(int) Math.round(slopestart+(slopelength/2))],y1[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
                //if (major<lang) {while ((slope[major]+revslope[major])*(-1)<0 && major<lang) {major++;}}
                //IJ.log(""+major+" "+slopecount);
        //    }
        //}
        for (int major = 1; major < lang; major++){
                if (revslope[major]/slope[major]>1000){
                    slopecount++;
                    slopestart = major;
                    while (slope[major]/revslope[major]>1000) {major++;}
                    rt1.incrementCounter();
                    slopelength = major - slopestart;
                    rt1.addValue("X position slice "+i, x1[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
                    rt1.addValue("Y position slice "+i, y1[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
                    rt1.addValue("Broad "+i, slopelength*pixres);
                    rt1.addValue("Length "+i, radiusnew[(int) Math.round(slopestart+(slopelength/2))]*pixres);//(int) Math.round(slopestart+(slopelength/2))
                    stackip.setColor(0);
                    stackip.drawLine(((int) Math.round(centerx)),((int) Math.round(centery)),x1[(int) Math.round(slopestart+(slopelength/2))],y1[(int) Math.round(slopestart+(slopelength/2))]);//(int) Math.round(slopestart+(slopelength/2))
                    while (slope[major]/revslope[major]>1000) {major++;}
                }
        }
        IJ.log("Major Protrusions: "+slopecount);
        slopecount = 0;
        for (int clear = 0; clear < lang; clear++){slope[clear] = 0;revslopeA[clear] = 0;}
        //calculate minor protrusions

        slope = slopecalculation(small,lang,radiusnew);
        revslopeA = slopecalculation(small,lang,revradius);
        revslope =  reverser(lang, revslopeA);

            int minorcount = 0;
            for (int minor = 0; minor < lang; minor++){
                if (revslope[minor]+slope[minor]<0){
                    slopecount++;
                    slopestart = minor;
                    while (slope[minor]+revslope[minor]<0) {
                         minor++;
                }
                slopelength = minor - slopestart;
                rt2.incrementCounter();
                rt2.addValue("X position slice "+i, x1[(int) Math.round(slopestart+(slopelength/2))]);
                rt2.addValue("Y position slice "+i, y1[(int) Math.round(slopestart+(slopelength/2))]);
                rt2.addValue("Length "+i, slopelength*pixres);
                rt2.addValue("Broad "+i, radiusnew[(int) Math.round(slopestart+(slopelength/2))]*pixres);

            }
        }
        IJ.log("Filopodia: "+slopecount);
        for (int clear = 0; clear < lang; clear++){slope[clear] = 0;revslopeA[clear] = 0;}

//draw membrane
        //for (int minor = 1; minor < 7200; minor++){
        //    if (minor > 0){
        //        firstip.setColor(255-((i-1)*8));
        //        firstip.drawLine((int) Math.round(x1[minor-1]),(int) Math.round(y1[minor-1]),(int) Math.round(x1[minor]),(int) Math.round(y1[minor]));
        //    }
        //}
    manager.close();
//draw angles.
        //directions (0, w,h,0,0, stackip);
        //directions (90, w,h,-20,0, stackip);
        //directions (180, w,h,0,15,stackip);
        //directions (270, w,h,0,0,stackip);
    }
    //write results tables

    //manager.close();
    //first.show();
    rt1.show("Major protrusions");
    rt2.show("Filopodia");
    stackip.invert();
    ImagePlus outstackbild = new ImagePlus(title,stackip);
    outstackbild.setCalibration(imp1.getCalibration());
    outstackbild.show();
    }


public double [] reverser(int lang, double [] array) {
    int negcorrect = lang;
    double revarray [] = new double[lang];
    for (int revers = 0; revers < lang; revers++){
        negcorrect--;
        revarray[revers] = array[negcorrect];
    }
    return revarray;
}

public double [] slopecalculation (double direct, int lang, double [] radius){
    double slopeint [] = new double[lang+1];
    double sumxy = 0;
    double sumxsquare = 0;
    double sumxint = 0;
    double sumyint = 0;
    int poscorrect = 0;
    for (int major = 0; major < lang; major++){
        //IJ.log("major "+major+" step3A ");
        for (int direction = 0; direction < direct; direction ++){

            poscorrect = major + direction;
            if (poscorrect > lang-1) {poscorrect = poscorrect - lang;}
            //IJ.log("poscorrect "+poscorrect);
            sumxy = (direction*radius[poscorrect])+sumxy;
            sumxint = direction + sumxint;
            sumxsquare = direction * direction;
            sumyint = radius[poscorrect]+sumyint;//radius[major+direction]+sumyint;
        }
    slopeint[major] = (((direct)*sumxy)-(sumxint*sumyint))/(((direct)*sumxsquare)-(sumxint*sumxint));
    sumxy = 0; sumxsquare = 0; sumxint = 0; sumyint = 0;
    //IJ.log("slope "+slopeint[major]+" step3A ");
    }
    return slopeint;
}

}
