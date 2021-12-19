package g2c.gerber.loader;

import g2c.application.Main;
import g2c.gerber.contents.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This code began as rewrite of the "Plotter" portion of Philipp Knirsch's "Gerber RS-274D/X file viewer in Java"
 * (see: http://www.wizards.de/phil/java/rs274x.html) but, the current code has been extensively rewritten and
 * expanded to use Java's Shape-based graphics and fix numerous small issues I found with the original code.  I've
 * also  added more features from the 2017.5 Gerber Format Specification by Ucamco, such as partial support for
 * "AD"-type Macros and implemented many of the Aperture primitives needed for this, although some of the Aperture
 * primitives remain untested, as I've yet to find a Gerber design that uses them. The untested code is noted in the
 * source for the flashAperture() method.
 *
 *  https://www.ucamco.com/en/gerber/downloads
 *
 * Internally, the code first converts the Gerber file into a list of shapes in "drawItems".  Then, if renderMode is
 * set to DRAW_IMAGE (the default when started), it draws this list of shapes directly to the screen.  However, if
 * renderMode is set to RENDER_FILLED or RENDER_OUTLINE, getBoardArea() is called to compute an Area shape by using
 * the 2D constructive geometry operations add() and subtract().  This converts all the individual shapes in drawItems
 * into a single Shape the contains all the geometry of the PCB design.  Unfortunately, computing this Area becomes
 * exponentially inefficient with larger Gerber files because, as each new shape is added, or subtracted its takes
 * increasinglt more time to calculate all the intersections.  So, a progress bar is displayed to show the progress of
 * the calculation.  Once the Area is computed, however, it can be resized and redrawn much more quickly than when
 * renderMode == DRAW_IMAGE.
 *
 * I've tried to adhere as close as possible to the latest revision of the Ucamco spec, version, 2017.05, which means
 * my code may not parse some older Gerber files, such as ones that reply on on undocumented behavior, or deprecated
 * operations.  In particular, G36/G37 regions specified like this:
 *
 *    G36*G01X-0001Y-0001D02*X1076D01*Y0226*X-0001*Y-0001*G37*
 *
 * Will not render because all of the coordinate values need to be followed by a D01 operation to render, such as:
 *
 *    G36*G01X-0001Y-0001D02*X1076D01*Y0226D01*X-0001D01*Y-0001D01*G37*
 *
 * Note: derived from GerberPlotArea3 in GerberTools Project
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

public class GerberLoader{
  
  private static final double   SCREEN_PPI = Toolkit.getDefaultToolkit().getScreenResolution();
  // Colors used to Render elements of the PCB design
  private Color    				COPPER = new Color(0xB87333);
  private static final Color    BOARD = Color.white;
  private static final Color    OUTLINE = Color.black;
  // Aperture types
  private static final int      CIRCLE = 1;
  private static final int      RECTANGLE = 2;
  private static final int      OBROUND = 3;
  private static final int      POLYGON = 4;
  // Aperture Macro Primitive Types
  private static final int      PRIM_CIRCLE = 10;
  private static final int      PRIM_VLINE = 11;
  private static final int      PRIM_CLINE = 12;
  private static final int      PRIM_OUTLINE = 13;
  private static final int      PRIM_POLYGON = 14;
  private static final int      PRIM_MOIRE = 15;
  private static final int      PRIM_THERMAL = 16;
  // Interpolation modes
  private static final int      LINEAR = 0;
  private static final int      CLOCK = 1;
  private static final int      CCLOCK = 2;
  // Draw and Render Modes
  private static final int      DRAW_IMAGE = 1;
  private static final int      RENDER_FILLED = 2;
  private static final int      RENDER_OUTLINE = 3;
  // Internal scaling parameters
  public static final double    renderScale = 10;   // scales up Shapes to improve render
  private static double         defaultViewScale = 4.0;
  private static final int      pixelGap = 4;       // Adds border around displayed image
  // State machine variables
  private String gerber;
  private List<String> cmdList;       // List containing all single commands and tokens
  private int cmdIdx;                 // Current position in cmdList for parser
  private boolean extCmd;             // True when processing an extended command
  private double curX;                // Current X position
  private double curY;                // Current Y position
  private double arcX;                // X center of arc
  private double arcY;                // Y center of arc
  private int interpol = LINEAR;      // Current interpolation mode
  private boolean inRegion = false;           // Flag for Path2D-based region active
  private boolean multi;              // Flag for single or multiquadrant interpolation
  private boolean stop;               // Stop program flag
  private boolean millimeters;        // If true, all coordinates are in millimeters, else inches
  private Path2D.Double path;         // Polygon of the area to be filled
  private boolean pathStarted;
  // State machine variables for the extended commands
  private boolean omitLeadZeros;      // Omit leading zeros, else omit trailing zeroes
  private int xSgnf = 2;              // Number of digits of X-axis before decimal
  private int xFrac = 3;              // Number of digits of X-axis after decimal
  private int ySgnf = 2;              // Number of digits of Y-axis before decimal
  private int yFrac = 3;              // Number of digits of Y-axis after decimal
  // Aperture lookup Map and working variable
  private Map<String,Macro> macroMap;
  private Map<Integer,List<Aperture>> aperturesMap;
  private List<Aperture> aperture;
  // PCB shape and control flags
  private boolean isDark = true;      // True if drawing copper
  private Rectangle.Double bounds;    // Computed bounding box for PCB layer
  private List<DrawItem> drawItems;   // Gerber ordered List of shapes used to draw PCB
  private File ourFile;
  private BufferedImage img = null;
  private AffineTransform correctionTransform = null;
  
  private boolean toolOn = false;

  	private void resetStateMachine () {
  		bounds = new Rectangle.Double();
  		drawItems = new ArrayList<>();
  		macroMap = new HashMap<>();
  		aperturesMap = new HashMap<>();
  		omitLeadZeros = true;
  		path = new Path2D.Double();
  		pathStarted = false;
  		curX = 0.0;
  		curY = 0.0;
  		interpol = LINEAR;
  		inRegion = false;
  		multi = false;
  		stop = false;
  		millimeters = false;
  		xSgnf = 2;
  		xFrac = 3;
  		ySgnf = 2;
  		yFrac = 3;
  		cmdIdx = 0;
  		extCmd = false;
  	}
  	
  	public GerberLoader(File file) {
  		ourFile = file;
  	}
  	
  	public BufferedImage renderImage(Color c) {
  		COPPER = c;
  		img = getBoardImage(Main.currPrinter.getScreenPPI().getWidth(), Main.currPrinter.getScreenPPI().getHeight(), Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height);
  		return img;
  	}
  	
  	public BufferedImage renderImage() {
  		img = getBoardImage(Main.currPrinter.getScreenPPI().getWidth(), Main.currPrinter.getScreenPPI().getHeight(), Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height);
  		return img;
  	}
  	
  	public BufferedImage getImage() {
  		if(img != null) return img;
  		img = getBoardImage(Main.currPrinter.getScreenPPI().getWidth(), Main.currPrinter.getScreenPPI().getHeight(), Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height);
  		return img;
  	}
  	
	public void applyCorrection(double xOffset, double yOffset, int angle) {
		correctionTransform = new AffineTransform();
		correctionTransform.translate(xOffset / 25.4 * renderScale, yOffset / 25.4 * -renderScale);
		correctionTransform.rotate(Math.toRadians(angle), PositionManager.getMinX(), PositionManager.getMinY());
	}
  	
  	public BufferedImage getCorrectedImage() {
  		img = getBoardImage(Main.currPrinter.getScreenPPI().getWidth(), Main.currPrinter.getScreenPPI().getHeight(), Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height, correctionTransform);
  		return img;
  	}
  	
  	public void setFile(File newFile) {
  		ourFile = newFile;
  		img = null;
  	}

  	public BufferedImage readFile(){
  		try {
  			// Read file and strip out line delimiters
  			StringBuilder buf = new StringBuilder();
  			BufferedReader br = new BufferedReader(new FileReader(ourFile));
  			String line;
  			while ((line = br.readLine()) != null) {
  				buf.append(line);
  			}
  			br.close();
  			gerber = buf.toString();
  			//System.out.println("gerber = " + gerber);
  		} catch (Exception ex) {
  			ex.printStackTrace();
  			return null;
  		}
  		
  		parse();
  		img = getBoardImage(Main.currPrinter.getScreenPPI().getWidth(), Main.currPrinter.getScreenPPI().getHeight(), Main.currPrinter.getScreenResolution().width, Main.currPrinter.getScreenResolution().height);
  		return img;
  	}
  	
  	private BufferedImage getBoardImage (double ppiX, double ppiY, int width, int height) {
  		return getBoardImage(ppiX, ppiY, width, height, null);
  	}
  	
  	private BufferedImage getBoardImage (double ppiX, double ppiY, int width, int height, AffineTransform at) {
  		double scaleX = ppiX / renderScale, scaleY = ppiY / renderScale;
  		BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
  		Graphics2D offScr = (Graphics2D) bufImg.getGraphics();
  		offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		offScr.setColor(COPPER);
		double minX = PositionManager.getMinX();
		double minY = PositionManager.getMinY();
		double heightMax = PositionManager.getHeight();
		
		AffineTransform scaler = new AffineTransform();
		scaler.scale(scaleX, -scaleY);
		scaler.translate(-minX, -heightMax);
			
  		for (DrawItem item : drawItems) {
  			//Invert Y axis to match Java Graphics's upper-left origin
  			
  			Shape shape =  null;
  			if(at != null) {
  	  			shape = scaler.createTransformedShape(at.createTransformedShape(item.shape));
  			}else {
  	  			shape = scaler.createTransformedShape(item.shape);
  			}
  			
  			if(item.drawCopper) {
  				offScr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
  			}else {
  				offScr.setComposite(AlphaComposite.Clear);
  			}
			offScr.fill(shape);
  		}
  		offScr.dispose();
  		return bufImg;
  	}
  	
  	public BufferedImage overlayDrills (BufferedImage bufImg, double newHoleSize, double ppiX, double ppiY, int width, int height, boolean applyCorrection) {
  		double scaleX = ppiX / renderScale, scaleY = ppiY / renderScale;
  		Graphics2D offScr = (Graphics2D) bufImg.getGraphics();
  		offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		offScr.setColor(COPPER);
		double minX = PositionManager.getMinX();
		double minY = PositionManager.getMinY();
		double heightMax = PositionManager.getHeight();
		
		AffineTransform scaler = new AffineTransform();
		scaler.scale(scaleX, -scaleY);
		scaler.translate(-minX, -heightMax);
  			
  		for (DrawItem item : drawItems) {
      // 	Invert Y axis to match Java Graphics's upper-left origin

  			if(newHoleSize > 0) {
  				if(item.drawCopper) {
		  			if(item.shape.getClass() == Ellipse2D.Double.class) {
		  				Ellipse2D.Double pad = (Ellipse2D.Double) item.shape;
		  				Rectangle2D frame = pad.getFrame();
		  				double centerX = frame.getCenterX();
		  				double centerY = frame.getCenterY();
		  				double r = renderScale * newHoleSize;
		  				
		  				Shape shape;
		  				if(correctionTransform != null && applyCorrection) {
			  				shape = scaler.createTransformedShape(correctionTransform.createTransformedShape(new Ellipse2D.Double(centerX - r, centerY - r, r*2, r*2)));
		  				}else {
		  					shape = scaler.createTransformedShape(new Ellipse2D.Double(centerX - r, centerY - r, r*2, r*2));
		  				}
		  				
		  	  			offScr.setComposite(AlphaComposite.Clear);
	  	  				offScr.fill(shape);
		  			}
  				}
  			}else {
  				if(item.drawCopper) {
	  				Shape shape;
	  				
	  				if(correctionTransform != null && applyCorrection) {
		  				shape = scaler.createTransformedShape(correctionTransform.createTransformedShape(item.shape));
	  				}else {
	  					shape = scaler.createTransformedShape(item.shape);
	  				}
	  				
  	  				offScr.setComposite(AlphaComposite.Clear);
  	  				offScr.fill(shape);
  	  				//offScr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
  	  			}
  			}
  			
  		}
  		offScr.dispose();
  		return bufImg;
  	}
  	
	private List<DrawItem> parse () {
		cmdList = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(gerber, "%*", true);
		int pos = 0;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (!token.equals("*")) {
				cmdList.add(pos++, token);
			}
		}
		resetStateMachine();
		while (cmdIdx < cmdList.size() && !stop) {
			String cmd = cmdList.get(cmdIdx);
			// If we entered or exited an extended command, switch the flag
			if (cmd.equals("%")) {
				extCmd = !extCmd;
				cmdIdx++;
				continue;
			}
			if (extCmd) {
				// Handle extended RS-274X command
				if (!doExtendedCmd()) {
					//System.out.println("Error: Failed executing command: " + cmd);
					return null;
				}
			} else {
				// Handle standard command
				if (!doNormalCmd()) {
					//System.out.println("Error: Failed executing command: " + cmd);
					return null;
				}
			}
		}
		return drawItems;
	}

  // TODO: can this be improved?
  	private Area getBoardArea() {
  		Area pcb = new Area();
  		int count = 0;
  		for (DrawItem item : drawItems) {
  			if (item.drawCopper) {
  				pcb.add(new Area(item.shape));
  			} else {
  				pcb.subtract(new Area(item.shape));
  			}
  		}
  		return pcb;
  	}
  	
  	private void addToBoard (Shape shape, boolean drawCopper) {
  		PositionManager.registerShape(shape);
  		bounds.add(shape.getBounds2D());
  		drawItems.add(new DrawItem(shape, drawCopper));
		//System.out.println(" add size = " + shape.getBounds2D().getHeight() + " " + shape.getBounds2D().getHeight());
  	}

  	// Scale X coordinates (to improve Shape precision)
  	private double dX (double xx) {
  		return xx * renderScale;
  	}
  	
  // 	Used to scale Y coordinates
  	private double dY (double yy) {
  		return yy * renderScale;
  	}
  	
  // 	Used to scale width values
  	private double dW (double xx) {
  		return xx * renderScale;
  	}
  	
  // 	Used to scale height values
  	private double dH (double yy) {
  		return yy * renderScale;
  	}
  	
  //
  // Search for the end of a number and returns the length up to the last
  // character. The format of a number looks like this:
  //
  //   [+|-]xxx[.yyy]
  //
  // TODO: rework this logic to simplify and meet spec
  //
  	private int numberEndIndex (String cmd, int pstart) {
  		int p;
  		for (p = pstart; p < cmd.length(); p++) {
  			char cc = cmd.charAt(p);
  			if (!Character.isDigit(cc)  && cc != '-' && cc != '+' && cc != '.')
  				break;
  		}
  		return p;
  	}
  	
  	private int numberStartIndex (String cmd, int pstart) {
  		int p;
  		for (p = pstart; p < cmd.length(); p++) {
  			char cc = cmd.charAt(p);
  			if (Character.isDigit(cc)  || cc != '-' || cc != '+' || cc != '.')
  				break;
  		}
  		return p;
  	}
  	
  	/**
   * Takes a numeric string representing a floating point value with no decimal point and
   * use the values signif and frac (which represent the number of left side and right side
   * digits, respectively), to convert input to a double value.  If "omitLeadZeros" is true
   * then use total of signif and frac vs length of string to figure out how many leading
   * zeros were omitted.
   * Note: also handles deprecated "omit trailing zero" case when omitLeadZeros == false
   * @param num numeric string without a decimal point
   * @param signif number of integral (left side) digits expected
   * @param frac number of fractional (right side) digits expected
   * @return double value for input string
   */
  	private double normalize (String num, int signif, int frac) {
  		double dVal = Double.parseDouble(num);
  		if (!omitLeadZeros) {
  			//we need to remove any possible sign characters, to makes sure we don't mistake one for an additional leading zero
  			num = num.replaceAll("[^\\d.]", "");
  			dVal *= Math.pow(10, (signif + frac - num.length()));
  		}
  		dVal /= Math.pow(10, frac);
  		return millimeters ? dVal / 25.4 : dVal;
  	}

  //
  // Handles an extended RS-274X command. They always start with a 2
  // character specifier, followed by various parameters, sometimes even
  // more commands.
  //
  	private boolean doExtendedCmd () {
  		while (cmdIdx < cmdList.size()) {
  			String cmd = cmdList.get(cmdIdx);
		  // 	End of eXtended command, so return true.
  			if (cmd.equals("%")) {
  				return true;
  			}
		  // 	Process command and any subcommands
  			switch (cmd.substring(0, 2)) {
  			case "AD":
			  // 	Aperture definition
  				Aperture app = null;
  				int nl = numberEndIndex(cmd, 3);
  				int pd = Integer.parseInt(cmd.substring(3, nl));
  				int te = cmd.indexOf(',', nl);
  				String type = "";
  				if(te == -1) { 
  					te = numberStartIndex(cmd, nl);
  	  				type = cmd.substring(nl);
  	  				cmd = "";
  				}else {
  	  				type = cmd.substring(nl, te);
  	  				cmd = cmd.substring(te+1);
  				}
  				
  				if (type.length() > 1) {
				  // 	Process one, or more Aperture primitives defined in a Macro
  					List<Double> aParms = new ArrayList<>();
  					StringTokenizer st = new StringTokenizer(cmd, "X");
  					while (st.hasMoreTokens()) {
  						String t = st.nextToken();
  						aParms.add(Double.parseDouble(t));
  					}
  					Macro macro = macroMap.get(type);
  					List<Aperture> macroApertures = new ArrayList<Aperture>();
  					for (String mCmd : macro.cmds) {
  						if (mCmd.startsWith("0 ")) {
						  // 	Skip over a comment within the macro  (4.5.4.1)
						  // 	System.out.println("Comment: " + mCmd.substring(2));
  						} else if (mCmd.startsWith("$")) {
						  // 	Skip over processing an equation within the macro, but log it for future work
  							System.out.println("Equation: " + mCmd.substring(1));
  						} else {
						  // 	Process one of the primitives defined in the Macro
  							String[] mParms = mCmd.split(",");
  							int primType = Integer.parseInt(mParms[0]);
  							switch (primType) {
  							case 1:               // Circle Primitive (4.5.4.2)
  								app = new Aperture(PRIM_CIRCLE);
  								break;
  							case 4:               // Outline Primitive  (4.5.4.5)
  								app = new Aperture(PRIM_OUTLINE);
  								break;
  							case 5:               // Polygon Primitive  (4.5.4.6)
  								app = new Aperture(PRIM_POLYGON);
  								break;
  							case 6:               // Moire Primitive  (4.5.4.7)
  								app = new Aperture(PRIM_MOIRE);
  								break;
  							case 7:               // Thermal Primitive  (4.5.4.8)
  								app = new Aperture(PRIM_THERMAL);
  								break;
  							case 20:              // Vector Line Primitive (4.5.4.3)
  								app = new Aperture(PRIM_VLINE);
  								break;
  							case 21:              // Center Line Primitive (4.5.4.4)
  								app = new Aperture(PRIM_CLINE);
  								break;
  							}
  							if (app != null) {
                  // 		Scan for variable in Macro's parms and plug in values from Aperture's parms
  								for (String mParm : mParms) {
  									if (mParm.startsWith("$")) {
  										int idx = Integer.parseInt(mParm.substring(1));
  										app.addParm(aParms.get(idx - 1));
  									} else {
  										app.addParm(Double.parseDouble(mParm));
  									}
  								}
  		  						macroApertures.add(app);
  							}
  						}
  					}
					aperturesMap.put(pd, macroApertures);
  				} else {
  					// 	Process non-Macro Aperture definition
  					switch (type.charAt(0)) {
  					case 'C':
  						app = new Aperture(CIRCLE);
  						break;
  					case 'R':
  						app = new Aperture(RECTANGLE);
  						break;
  					case 'O':
  						app = new Aperture(OBROUND);
  						break;
  					case 'P':
  						app = new Aperture(POLYGON);
  						break;
  					case 'D':
  						app = new Aperture(POLYGON);
  						break;
  					default:
  						System.out.println("Unknown Aperture type: " + cmd);
  						break;
  					}
  					if (app != null) {
  						StringTokenizer st = new StringTokenizer(cmd, "X");
  						while (st.hasMoreTokens()) {
  							app.addParm(Double.parseDouble(st.nextToken()));
  						}
  						List<Aperture> macroApertures = new ArrayList<Aperture>();
  						macroApertures.add(app);
  						aperturesMap.put(pd, macroApertures);
  					}
  				}
  				break;
  			case "AM":
          // 		Aperture Macros
  				String name = cmd.substring(2);
  				Macro macro = new Macro();
  				macroMap.put(name, macro);
  				while (cmdIdx + 1 < cmdList.size() && !cmdList.get(cmdIdx + 1).equals("%")) {
  					String tmp = cmdList.get(++cmdIdx);
  					macro.addCmd(tmp);
  				}
  				break;
  			case "FS":
          // 		Parse the format statement, such as "FSTAX24Y24"
  				omitLeadZeros = cmd.charAt(2) == 'L';
  				cmd = cmd.substring(4);
  				if (cmd.startsWith("X")) {
  					xSgnf = Integer.parseInt(cmd.substring(1, 2));
  					xFrac = Integer.parseInt(cmd.substring(2, 3));
  					cmd = cmd.substring(3);
  				}
  				if (cmd.startsWith("Y")) {
  					ySgnf = Integer.parseInt(cmd.substring(1, 2));
  					yFrac = Integer.parseInt(cmd.substring(2, 3));
  				}
  				break;
  			case "LP":
  				isDark = cmd.charAt(2) == 'D';
  				break;
  			case "MO":      // Set units (inches or millimeters)
  				millimeters = cmd.charAt(2) == 'M';
  				break;
  			case "AS":      // Deprecated command (ignored)
  			case "IN":      // Deprecated command (ignored)
  			case "IP":      // Deprecated command (ignored)
  			case "IR":      // Deprecated command (ignored)
  			case "LN":      // Deprecated command (ignored)
  			case "MI":      // Deprecated command (ignored)
  			case "OF":      // Deprecated command (ignored)
  			case "SF":      // Deprecated command (ignored)
  				System.out.println("Deprecated extended command: " + cmd);
  				break;
  			default:
  				System.out.println("Unknown extended command: " + cmd);
  				break;
  			}
  			cmdIdx++;
  		}
  		return true;
  	}
  	
  //		
  // 	Handles a old RS command. They always start with only 1 character and
  // 	are much less complex and nested as the eXtended commands.
  //
  	private boolean doNormalCmd () {
    // 	Allow reuse of prior coordinates
  		double nx = curX;
  		double ny = curY;
  		
  		while (cmdIdx < cmdList.size()) {
  			boolean coordinatesContained = false;
  			String cmd = cmdList.get(cmdIdx);
  			//	System.out.println(cmd);
  			// 	Asterisk denotes end of command
  			if (cmd.equals("*")) {
  				return true;
  			}
  			// 	This check is needed to recover from unanticipated states
  			// 	TODO: investigate further
  			if (cmd.equals("%")) {
  				return true;
  			}
  			// 	Process subcommands
  			while (cmd.length() > 0) {
  				switch (cmd.charAt(0)) {
  				case 'N': {
            // 	Line number (ignored)
  					int p = numberEndIndex(cmd, 1);
  					cmd = cmd.substring(p);
  				} continue;
  				case 'G':
  					String code = cmd.substring(1, 3);
  					cmd = cmd.substring(3);
  					switch (code) {
  					case "01":      // G01
                // Switch to linear interpolation with scale 1.0
  						interpol = LINEAR;
  						System.out.println("int is now LINEAR");
  						continue;
  					case "02":    // G02
                // 	Switch to clockwise interpolation
  						interpol = CLOCK;
  						System.out.println("int is now CLOCK");
  						continue;
  					case "03":    // G03
  						// Switch to counter clockwise interpolation
  						interpol = CCLOCK;
  						System.out.println("int is now CCLOCK");
  						continue;
  					case "04":    // G04 - Comment
  						cmd = "";
  						continue;
  					case "10":    // G10
                // 	Switch to linear interpolation with scale 10
  						interpol = LINEAR;
  						continue;
  					case "36":    // G36 - Start new filed area polygon
  						inRegion = true;
  						System.out.println("now in region!");
  						path = new Path2D.Double();
  						pathStarted = false;
  						continue;
  					case "37":    // G37 - End area fill
  						addToBoard(path, isDark);
  						inRegion = false;
  						continue;
  					case "54":    // G54
  					case "55":    // G55
                // 	Select an aperture, Deprecated
  						continue;
  					case "74":    // G74
                // 	Switch to single quadrant (no circular interpolation)
  						multi = false;
  						continue;
  					case "75":    // G75
  						// 	Switch to multi quadrant (circular interpolation)
  						multi = true;
  						continue;
  					default:
  						continue;
  					}
  				case 'X': {                     // Set X position.
  					int idx = numberEndIndex(cmd, 1);
  					nx = normalize(cmd.substring(1, idx), xSgnf, xFrac);
  					cmd = cmd.substring(idx);
  					coordinatesContained = true;
  				} continue;
  				case 'Y': {                     // Set Y position.
  					int idx = numberEndIndex(cmd, 1);
  					ny = normalize(cmd.substring(1, idx), ySgnf, yFrac);
  					cmd = cmd.substring(idx);
  					coordinatesContained = true;
  				} continue;
  				case 'I': {                     // Find the relative X center of the circle
  					int idx = numberEndIndex(cmd, 1);
  					arcX = normalize(cmd.substring(1, idx), xSgnf, xFrac);
  					cmd = cmd.substring(idx);
  				} continue;
  				case 'J': {                     // Find the relative Y center of the circle
  					int idx = numberEndIndex(cmd, 1);
  					arcY = normalize(cmd.substring(1, idx), ySgnf, yFrac);
  					cmd = cmd.substring(idx);
  				} continue;
  				case 'D': {                     // Operation code
  					//System.out.println("  " + cmd);
  					int idx = numberEndIndex(cmd, 1);
  					int nd = Integer.parseInt(cmd.substring(1, idx));
  					cmd = cmd.substring(idx);
  					if (nd >= 10) { 
  						toolOn = false;              // Select aperturde
  						aperture = aperturesMap.get(nd);
  						System.out.println("switched to aperture no. " + nd + ":" + ((aperture.size() > 1) ? "Macro" : aperture.get(0).type));
  						if (aperture == null) {
  							throw new IllegalStateException("Aperture " + nd + " not found");
  						}
  					} else if (nd == 1) {         // D01: Interpolate operation
  						toolOn = true;
  						coordinatesContained = false;
  						if (inRegion) {
  							if (interpol == LINEAR) {
  								if (pathStarted) {
  									path.lineTo(dX(nx), dY(ny));
  								} else {
  									path.moveTo(dX(nx), dY(ny));
  									pathStarted = true;
  								}
  							} else {
  								for(Aperture app : aperture) {
  	  								drawArc(app, nx, ny, true);
  								}
  							}
  						} else {
  							if (interpol == LINEAR) {
  								for(Aperture app : aperture) {
  									interpolateAperture(app, curX, curY, nx, ny);
  								}
  							} else {
  								for(Aperture app : aperture) {
  									drawArc(app, nx, ny, false);
  								}
  							}
  						}
  					} else if (nd == 2) {         // D02: Move operation
  						toolOn = false;
  						if (inRegion) {
  							if (pathStarted) {
  								path.lineTo(dX(nx), dY(ny));
  							} else {
  								path.moveTo(dX(nx), dY(ny));
  								pathStarted = true;
  							}
  						}
  					} else if (nd == 3) {         // D03: Flash
  						toolOn = false;
						for(Aperture app : aperture) {
	  						flashAperture(app, nx, ny);
	  			  			System.out.println("flashaperture. type = " + app.type);
						}
  					}
  				} continue;
  				case 'M':
  					stop = cmd.startsWith("M00") || cmd.startsWith("M02");
  					cmd = "";
  					continue;
  				default:
  					System.out.println("Unrecognized command: " + cmd);
  					cmd = "";
  				}
  			}
  			if(coordinatesContained) {
  				if(inRegion) {
	  				if (pathStarted) {
						path.lineTo(dX(nx), dY(ny));
					} else {
						path.moveTo(dX(nx), dY(ny));
						pathStarted = true;
					}
  				}else if(toolOn){
					if (interpol == LINEAR) {
						for(Aperture app : aperture) {
							interpolateAperture(app, curX, curY, nx, ny);
						}
					} else {
						for(Aperture app : aperture) {
							drawArc(app, nx, ny, false);
						}
					}
  				}
  			}
  			curX = nx;	
  			curY = ny;
  			cmdIdx++;
  		}
  		return true;
  	}
  	
  	// This clever bit of code is largely unchanged from the original source except for changes I made
	  // to use Java's newer, Shape-based graphics to get more precise, sub pixel-level positioning
	private void drawArc (Aperture app, double nx, double ny, boolean filled) {
	    double cx, cy;
	    // Calculate the radius first
	    double radius = Math.sqrt(Math.pow(arcX, 2) + Math.pow(arcY, 2));
	    System.out.println("radius = " + radius + " yDiff " + arcY + " xDiff = " + arcX);
	    // Calculate the center of the arc
	    if (multi) {
	    	cx = curX + arcX;
	    	cy = curY + arcY;
	    } else {
	    	if (interpol == CCLOCK) {
	    		if (curX < nx)
	    			cy = curY + arcY;       // Lower half
	    		else
	    			cy = curY - arcY;       // Upper half
	    		if (curY < ny)
	    			cx = curX - arcX;       // Right half
	    		else
	    			cx = curX + arcX;       // Left half
	    	} else {
	    		if (curX < nx)
	    			cy = curY - arcY;       // Upper half
	    		else
	    			cy = curY + arcY;       // Lower half
	    		if (curY < ny)
	    			cx = curX + arcX;       // Left half
	    		else
	    			cx = curX - arcX;       // Right half
	    	}
	    }

	    double start;
	    if((curX - cx) == 0){
	    	start = ((curY - cy) > 0) ? 90.0 : 270.0;
	    }else {
	    	start = 180 * Math.atan((curY - cy) / (curX - cx)) / Math.PI;
	    }
	    
	    if (curX < cx) {
	    	start += 180;        // Left side we have to add 180 degree
	    }
	    
	    if(start < 0) start += 360;
	    
	    // First calculate angle of the end point
	    double arc;
	    if((nx - cx) == 0){
	    	arc = ((ny - cy) > 0) ? 90.0 : 270.0;
	    }else {
	    	arc = 180 * Math.atan((ny - cy) / (nx - cx)) / Math.PI;
	    }
	    
	    if (nx < cx) {
	    	arc = 180 + arc;            // Left side we have to add 180 degree
	    }
	    
	    if(arc < 0) arc += 360;
	    
	    // Now lets check if we go clock or counterclockwise
	    if (interpol == CCLOCK) {
	    	arc = arc - start;          // Counterclock, just go from start to end
		    if(arc < 0) arc += 360;
	    } else {
	    	arc = arc - start;    // Hah, try to figure out this one :)
		    if(arc > 0) arc -= 360;
	    }
	    
	    // Special case for a full circle.
	    if (arc == 0) {
	    	arc = 360;
	    }
	    
	    
	    // Calculate the coordinates needed by Arc2D.Double()
	    double left   = dX(cx - radius);
	    double upper  = dY(cy - radius);
	    double height = dW(2 * radius);
	    double width  = dH(2 * radius);
	    if (filled) {
	    	addToBoard(new Arc2D.Double(left, upper, width, height, start, arc, Arc2D.PIE), isDark);
	    } else {
	    	if (inRegion) {
	    		Arc2D.Double curve = new Arc2D.Double(left, upper, width, height, start, -arc, Arc2D.OPEN);
	    		path.append(curve.getPathIterator(new AffineTransform()), true);
	    	} else {
	        // 	TODO: decide if I should handle case where Aperture is a rectangle (see interpolateAperture())
	    		double diam = dW(app.parms.get(0));
	    		BasicStroke s1 = new BasicStroke((float) diam, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		    	Arc2D.Double a2d = new Arc2D.Double(left, upper, width, height, start, -arc, Arc2D.OPEN);
	    		addToBoard(s1.createStrokedShape(a2d), isDark);
	    	}
    	}
  	}

	  /**
	   * Draw a filled shape specifed by the currrent Aperture object
	   * @param x1 x coord of the center of the shape to draw
	   * @param y1 y coord of the center of the shape to draw
	   */
	private void flashAperture (Aperture app, double x1, double y1) {
		x1 = dX(x1);
	    y1 = dY(y1);
	    double wid = dW(app.parms.get(0));
	    int holeIndex = 0;
	    if (app.type == CIRCLE) {
	    	Ellipse2D shape = new Ellipse2D.Double(x1 - wid / 2, y1 - wid / 2, wid, wid);
	    	addToBoard(shape, isDark);
	    	holeIndex = app.parms.size() > 1 ? 1 : 0;
    	} else if (app.type == RECTANGLE) {
    		double hyt = dH(app.parms.get(1));
    		addToBoard(new Rectangle2D.Double(x1 - wid / 2, y1 - hyt / 2, wid, hyt), isDark);
    		holeIndex = app.parms.size() > 2 ? 2 : 0;
	    } else if (app.type == OBROUND) {
	    	double hyt = dH(app.parms.get(1));
	    	double eRadius = Math.min(wid, hyt);
	    	addToBoard(new RoundRectangle2D.Double(x1 - wid / 2, y1 - hyt / 2, wid, hyt, eRadius, eRadius), isDark);
	    	holeIndex = app.parms.size() > 2 ? 2 : 0;
	    } else  if (app.type == POLYGON) {
	    	double radius = dW(app.parms.get(0) / 2.0);
	    	double sides = app.parms.get(1);
	    	double start = app.parms.get(2);
	    	Path2D.Double path = new Path2D.Double();
	    	boolean first = true;
	    	for (int ii = 0; ii < sides; ii++) {
	    		double cx = x1 + radius * Math.cos((start + 360 * ii / sides) * Math.PI / 180);
	    		double cy = y1 + radius * Math.sin((start + 360 * ii / sides) * Math.PI / 180);
	    		if (first) {
	    			path.moveTo(cx, cy);
	    		} else {
	    			path.lineTo(cx, cy);
	    		}
	    		first = false;
	    	}
	    	path.closePath();
	    	addToBoard(path, isDark);
	    	holeIndex = app.parms.size() > 3 ? 3 : 0;
	    } else if (app.type == PRIM_CIRCLE) {
	    	// Note: coded to spec (4.5.4.2), but untested
	    	double aDia = dW(app.parms.get(2));
	    	double aX = dX(app.parms.get(3));
	    	double aY = dY(app.parms.get(4));
	    	double rot = 0;//app.parms.get(5);  // Rotation pointless for circle, but spec includes it...
	    	Shape circle = new Ellipse2D.Double(aX - aDia / 2, aY - aDia / 2, aDia, aDia);
	    	AffineTransform at = new AffineTransform();
	    	at.translate(x1, y1);
	    	at.rotate(Math.toRadians(360 - rot));   // Spec says rotation is counterclockwise
	    	addToBoard(at.createTransformedShape(circle), isDark);
	    } else if (app.type == PRIM_CLINE) {
	    	// Note: tested 11-14-2017 using "BreadboardArduinoZero-30 (U4).osm"
	    	double aWid = dW(app.parms.get(2));
	    	double aHyt = dH(app.parms.get(3));
	    	double aX = dX(app.parms.get(4));
	    	double aY = dY(app.parms.get(5));
	    	double rot = app.parms.get(6);
	    	Shape cline = new Rectangle2D.Double(aX - aWid / 2, aY - aHyt / 2, aWid, aHyt);
	    	AffineTransform at = new AffineTransform();
	    	at.translate(x1, y1);
	    	at.rotate(Math.toRadians(360 - rot));   // Spec says rotation is counterclockwise
	    	addToBoard(at.createTransformedShape(cline), isDark);
	    } else if (app.type == PRIM_OUTLINE) {
	      // 	Note: coded to spec (4.5.4.5) , but untested
	    	int coords = app.parms.get(2).intValue() + 1; // Includes start point again at end
	    	Path2D.Double outline = new Path2D.Double();
	    	for (int ii = 0; ii < coords; ii++) {
	    		if (ii == 0) {
	    			outline.moveTo(dX(app.parms.get((ii*2) + 3)), dY(app.parms.get((ii*2) + 4)));
	    		} else {
	    			outline.lineTo(dX(app.parms.get((ii*2) + 3)), dY(app.parms.get((ii*2) + 4)));
	    		}
	    	}
	    	double rot = app.parms.get(5 + (coords - 1) * 2);
	    	outline.closePath();
	    	AffineTransform at = new AffineTransform();
	    	at.translate(x1, y1);
	    	at.rotate(Math.toRadians(360 - rot));   // Spec says rotation is counterclockwise
	    	addToBoard(at.createTransformedShape(outline), isDark);
	    } else {
	    	System.out.println("flashAperture() Aperture type = " + app.type + " not implemented, exposure is " + (isDark ? "DRK" : "CLR"));
	    	for (double val : app.parms) {
	    		System.out.print("  " + val);
	    	}
	    	System.out.println();
	    }
	    if (holeIndex > 0) {
	      // 	Draw hole in Aperture
	      double diam = dW(app.parms.get(holeIndex));
	      addToBoard(new Ellipse2D.Double(x1 - diam / 2, y1 - diam / 2, diam, diam), false);
	    }
	}

	  /**
	   * Draw a line that simulates drawing with a moving Aperture.
	   * Note: the RS-274X spec says that "the solid circle and solid rectangle standard apertures are
	   * the only apertures allowed for creating draw objects", so that's all this code implements.
	   * @param x1 x coord of start point
	   * @param y1 y coord of start point
	   * @param x2 x coord of end point
	   * @param y2 y coord of end point
	   */
	private void interpolateAperture (Aperture app, double x1, double y1, double x2, double y2) {
		x1 = dX(x1);
		y1 = dY(y1);
		x2 = dX(x2);
		y2 = dY(y2);
		if (app.type == CIRCLE) {
		  double diam = dW(app.parms.get(0));
		  BasicStroke s1 = new BasicStroke((float) diam, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		  addToBoard(s1.createStrokedShape(new Line2D.Double(x1, y1, x2, y2)), isDark);
		  addToBoard(s1.createStrokedShape(new Line2D.Double(x1, y1, x2, y2)), isDark);
		} else if (app.type == RECTANGLE) {
		  double wid = dW(app.parms.get(0));
		  double hyt = dH(app.parms.get(1));
		  double diam = Math.sqrt(wid * wid + hyt * hyt);
		  BasicStroke s1 = new BasicStroke((float) diam, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
		  addToBoard(s1.createStrokedShape(new Line2D.Double(x1, y1, x2, y2)), isDark);
		  // Draw rectangle at start and end points to simulate Gerber's rectangular interpolation
		  addToBoard(new Rectangle2D.Double(x1 - wid / 2, y1 - hyt / 2, wid, hyt), isDark);
		  addToBoard(new Rectangle2D.Double(x2 - wid / 2, y2 - hyt / 2, wid, hyt), isDark);
		} else {
		  System.out.println("interpolateAperture() Aperture type = " + app.type + " not implemented, exposure is " + (isDark ? "DRK" : "CLR"));
		  for (double val : app.parms) {
		    System.out.print("  " + val);
		      }
		      System.out.println();
	    	}
	}

  	
}