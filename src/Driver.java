import org.openstreetmap.gui.jmapviewer.*;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.util.concurrent.atomic.AtomicBoolean;


public class Driver {
	
	public static int animationSec;
	public static ArrayList<TripPoint> trip;
	public static BufferedImage raccoon;
	private static AtomicBoolean isPaused = new AtomicBoolean(false);
	public static String markersrc;


    public static void main(String[] args) throws FileNotFoundException, IOException {
    	TripPoint.readFile("triplog.csv");
    	TripPoint.h2StopDetection();
//    	raccoon = ImageIO.read(new File("raccoon.png"));
    	
    	// set up frame
        JFrame frame = new JFrame("Map Viewer");
        frame.setLayout(new BorderLayout());
        
        // set up top panel for input selections
        JPanel topPanel = new JPanel();
        frame.add(topPanel, BorderLayout.NORTH);
        // play button
        JButton play = new JButton("Play");
        // checkbox to enable/disable stops
        JCheckBox includeStops = new JCheckBox("Include Stops");
        // dropbox to pick animation time
        String[] timeList = {"Animation Time", "15", "30", "60", "90"};
        JComboBox<String> animationTime = new JComboBox<String>(timeList);
        String[] icons = {"Raccoon", "Red", "Green"};
        JComboBox<String> changeIcons = new JComboBox<String>(icons);
        animationSec = 0;
        //adding the resume button at the top
        JButton pauseButton = new JButton("Pause");
        topPanel.add(pauseButton);

        // add all to top panel
        topPanel.add(animationTime);
        topPanel.add(changeIcons);
        topPanel.add(includeStops);
        topPanel.add(play);
        
        
        markersrc = "raccoon.png";
        
        changeIcons.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object selectedItem = changeIcons.getSelectedItem();
                    if (selectedItem instanceof String) {
                        String selectedString = (String) selectedItem;
                        if (selectedString.equals("Raccoon")) {
                        	String markersrc = "raccoon.png";
                        	System.out.println(markersrc);
                        }
                        else if (selectedString.equals("Red")){
                        	String markersrc = "red.png";
                        	System.out.println(markersrc);
                        }
                        else if (selectedString.equals("Green")){
                        	String markersrc = "green.png";
                        	System.out.println(markersrc);
                        }
                    }
                }
            }
        });
        
        raccoon = ImageIO.read(new File(markersrc));
        
        // set up mapViewer
        JMapViewer mapViewer = new JMapViewer();
        frame.add(mapViewer);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mapViewer.setTileSource(new OsmTileSource.TransportMap());
        
        // add listeners
        play.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                    	mapViewer.removeAllMapMarkers(); // remove all markers from the map
                    	mapViewer.removeAllMapPolygons();
                    	if (includeStops.isSelected()) {
                    		trip = TripPoint.getTrip();
                    	}
                    	else {
                    		trip = TripPoint.getMovingTrip();
                    	}
                        plotTrip(animationSec, trip, mapViewer);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        
        //For the Resume/Pause Button
        pauseButton.addActionListener(e -> {
            if (isPaused.get()) {
                isPaused.set(false);
                pauseButton.setText("Pause");
            } else {
                isPaused.set(true);
                pauseButton.setText("Resume");
            }
        });

        
        
        animationTime.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object selectedItem = animationTime.getSelectedItem();
                    if (selectedItem instanceof String) {
                        String selectedString = (String) selectedItem;
                        if (!selectedString.equals("Animation Time")) {
                            animationSec = Integer.parseInt(selectedString);
                            System.out.println("Updated to " + animationSec);
                        }
                    }
                }
            }
        });

        // set the map center and zoom level
        mapViewer.setDisplayPosition(new Coordinate(34.82, -107.99), 6);
        
    }
    
    
    // plot the given trip ArrayList with animation time in seconds
    public static void plotTrip(int seconds, ArrayList<TripPoint> trip, JMapViewer map) throws IOException {
    	// amount of time between each point in milliseconds
    	long delayTime = (seconds * 1000) / trip.size();
    	
    	raccoon = ImageIO.read(new File(markersrc));
    	
    	Coordinate c1;
    	Coordinate c2 = null;
    	MapMarker marker;
    	MapMarker prevMarker = null;
    	MapPolygonImpl line;
    	
    	for (int i = 0; i < trip.size(); ++i) {
    		
    		//To sleep the loop
    		 while (isPaused.get()) {
    		        try {
    		            Thread.sleep(100);
    		        } catch (InterruptedException e) {
    		            e.printStackTrace();
    		        }
    		    }
    		
    		
    		c1 = new Coordinate(trip.get(i).getLat(), trip.get(i).getLon());
    		marker = new IconMarker(c1, raccoon);
            
    		map.addMapMarker(marker);
    		if (i != 0) {
    			c2 = new Coordinate(trip.get(i-1).getLat(), trip.get(i-1).getLon());
    		}
    		if (c2 != null) {
    			line = new MapPolygonImpl(c1, c2, c2);
    			line.setColor(Color.RED);
    			line.setStroke(new BasicStroke(3));
    			map.addMapPolygon(line);
    			map.removeMapMarker(prevMarker);
    		}
    		try {
				Thread.sleep(delayTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		prevMarker = marker;
    	}
    	
    }
    
}



//import java.awt.BorderLayout;
//import java.awt.Container;
//import java.awt.Image;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.openstreetmap.gui.jmapviewer.*;
//import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
//
//import javax.imageio.ImageIO;
//import javax.swing.*;
//
//import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
//import org.openstreetmap.gui.jmapviewer.Style;
//
//import java.awt.Color;
//
//public class Driver {
//	
//	// Declare class data
//	private static Image markerImage;
//	private static int animationTime;
//	private static boolean showStops;
//	private static IconMarker marker;
//	private static boolean bool = false;
//
//    public static void main(String[] args) throws FileNotFoundException, IOException {
//
//    	// Read file and call stop detection
//    	TripPoint.readFile("triplog.csv");
//        TripPoint.h1StopDetection();
//    	
//    	
//    	// Set up frame, include your name in the title
//    	JFrame frame = new JFrame("Projct 5 - Moez Ullah Khan");
//        frame.setSize(1000, 385);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        
//         //Set up Panel for input selections
//        JPanel topPanel = new JPanel();
//        
//    	
//        // ComboBox to pick animation time
//        String[] animationTimes = { "Animation Time", "15", "30", "60" , "90"};
//        JComboBox<String> animationTimeComboBox = new JComboBox<>(animationTimes);
//        topPanel.add(animationTimeComboBox);
//        
//        // CheckBox to enable/disable stops
//        JCheckBox showStopsCheckBox = new JCheckBox("Include Stops");
//        topPanel.add(showStopsCheckBox);
//        
//        // Play Button
//        JButton playButton = new JButton("Play");
//        topPanel.add(playButton);
//        
//    	
//        // Add all to top panel
//        frame.add(topPanel, BorderLayout.NORTH);
//        
//        // Set up mapViewer
//        JMapViewer map = new JMapViewer();
//        map.setTileSource(new OsmTileSource.TransportMap());
//        
//        
//        
//        // Add listeners for GUI components
//        animationTimeComboBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                JComboBox<String> comboBox = (JComboBox<String>) e.getSource();
//                String selectedTime = (String) comboBox.getSelectedItem();
//                if (selectedTime.equals("Animation Time")) {
//                    animationTime = 0; // set to default value
//                } else if (selectedTime.equals("15")) {
//                    animationTime = 15;
//                } else if (selectedTime.equals("30")) {
//                    animationTime = 30;
//                } else if (selectedTime.equals("60")) {
//                    animationTime = 60;
//                } else if (selectedTime.equals("90")) {
//                    animationTime = 90;
//                }
//            }
//        });
//
//        
//        showStopsCheckBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                showStops = ((JCheckBox) e.getSource()).isSelected();
//            }
//        });
//        
//        playButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//            	map.getMapMarkerList().clear();
//                map.getMapPolygonList().removeIf(p -> p instanceof MapPolyline);
//                new Thread(() -> animate(map, TripPoint.getTrip(), animationTime, showStops)).start();
//                map.getMapMarkerList().clear();
//                map.getMapPolygonList().removeIf(p -> p instanceof MapPolyline);
//                
//            }
//        });
//        
//        
//        
//
//        
//
//        // Set the map center and zoom level
//        Coordinate centerCoordinate = new Coordinate(34.198, -106.887); // Our Map
//        map.setDisplayPosition(centerCoordinate, 6);
//        
//        
//        
//     // Load the marker image from a file
//        Image markerImage = null;
//        try {
//            markerImage = ImageIO.read(new File("raccoon.png"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        
//     // Create a new IconMarker at the specified Coordinate with the loaded image
//        Coordinate markerCoordinate = new Coordinate(0,0);
//        IconMarker marker = new IconMarker(markerCoordinate, markerImage);
//        
//        
//        
//     // Add the marker to the map
//        map.addMapMarker(marker);
//        
//        //ADd the map
//        frame.add(map);
//        
//        //Display the frame
//        frame.setVisible(true);
//        
//
//        
//        
//    }
//    
//    // Animate the trip based on selections from the GUI components
// // Animate the trip based on selections from the GUI components
//    public static void animate(JMapViewer map, ArrayList<TripPoint> trip, int animationTime, boolean showStops) {
//        ArrayList<TripPoint> tripToAnimate = trip;
//        map.getMapPolygonList().removeIf(p -> p instanceof MapPolyline);
//        bool = true;
//        
//        map.getMapMarkerList().clear();
//        map.getMapPolygonList().removeIf(p -> p instanceof MapPolyline);
//        
//        
//
//        Image markerImage = null;
//        try {
//            markerImage = ImageIO.read(new File("raccoon.png"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if (!showStops) {
//            tripToAnimate = TripPoint.getMovingTrip();
//        }
//
//        int animationTimeMillis = animationTime * 1000;
//        int totalTimeSteps = tripToAnimate.size();
//        int sleepTimePerStep = animationTimeMillis / totalTimeSteps;
//
//        IconMarker marker = null;
//
//        MapPolyline line = null;
//        List<Coordinate> lineCoordinates = new ArrayList<>();
//        
////        if(bool == true) {
////    		map.getMapMarkerList().clear();
////    	    map.getMapPolygonList().removeIf(p -> p instanceof MapPolyline);
////    	}
//
//        for (int i = 0; i < tripToAnimate.size() - 1; i++) {
//        	        	
//        	
//            TripPoint currentPoint = tripToAnimate.get(i);
//            TripPoint nextPoint = tripToAnimate.get(i + 1);
//
//            if (currentPoint == null || nextPoint == null) {
//                continue;
//            }
//
//            Coordinate currentCoordinate = new Coordinate(currentPoint.getLat(), currentPoint.getLon());
//            Coordinate nextCoordinate = new Coordinate(nextPoint.getLat(), nextPoint.getLon());
//
//            if (marker != null) {
//                map.removeMapMarker(marker);
//            }
//
//            marker = new IconMarker(currentCoordinate, markerImage);
//            map.addMapMarker(marker);
//
//            lineCoordinates.add(currentCoordinate);
//
//            if (line != null) {
//                map.getMapPolygonList().remove(line);
//            }
//
//            line = new MapPolyline(lineCoordinates);
//            map.addMapPolygon(line);
//
//            SwingUtilities.invokeLater(() -> {
//                map.revalidate();
//                map.repaint();
//            });
//
//            try {
//                Thread.sleep(sleepTimePerStep);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        bool = false;
//    }
//
//
//
//
//
//    
//
//    }