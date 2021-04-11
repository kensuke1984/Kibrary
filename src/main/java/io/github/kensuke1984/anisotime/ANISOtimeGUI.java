/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.kensuke1984.anisotime;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI for ANISOtime
 * <p>
 * TODO relative absolute small p, s do not show up
 *
 * @author Kensuke Konishi
 * @version 0.5.10b
 * 
 * 2021.3.31 Anselme Borgeaud
 *  - change units of ray parameter to [s/deg]
 */
class ANISOtimeGUI extends javax.swing.JFrame {

    private RaypathWindow raypathWindow;
    private volatile VelocityStructure structure;
    private volatile double eventR;
    /**
     * Epicentral Distance mode: epicentral distance[deg]<br>
     * Ray parameter mode: ray parameter [s/deg]<br>
     */
    private volatile double mostImportant;
    private volatile ComputationMode mode;
    /**
     * 0(default): All, 1: P-SV, 2: SH
     */
    private volatile int polarity;
    private volatile Set<Phase> phaseSet;
    private ParameterInputPanel jPanelParameter;
    private ResultWindow resultWindow;
    private PhaseWindow phaseWindow;

    /**
     * Creates new form TravelTimeGUI
     */
    ANISOtimeGUI() {
        initComponents();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ANISOtimeGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        SwingUtilities.invokeLater(() -> new ANISOtimeGUI().setVisible(true));
    }

    void selectRaypath(int i) {
        raypathWindow.selectPath(i);
    }

    private void addPath(double[] x, double[] y) {
        raypathWindow.addPath(x, y);
    }

    private void createNewRaypathTabs() {
        if (raypathWindow != null) raypathWindow.dispose();
        raypathWindow = new RaypathWindow(this, new RaypathPanel(structure));
        resultWindow.clearRows();
    }

    void setStructure(VelocityStructure structure) {
        this.structure = structure;
    }

    /**
     * @param eventDepth [km] depth of the source (NOT radius)
     */
    void setEventDepth(double eventDepth) {
        eventR = structure.earthRadius() - eventDepth;
    }

    /**
     * @param d Epicentral Distance mode: epicentral distance[deg]<br>
     *          Ray parameter mode: ray parameter [s/deg]<br>
     */
    void setMostImportant(double d) {
        mostImportant = d;
    }

    void setMode(ComputationMode mode) {
        this.mode = mode;
        jPanelParameter.changeBorderTitle(mode + "  " + getPoleString());
        jPanelParameter.setMode(mode);
    }

    /**
     * @param i 0(default): All, 1: P-SV, 2: SH
     */
    void setPolarity(int i) {
        polarity = i;
        phaseWindow.setPolarity(i);
        jPanelParameter.changeBorderTitle(mode + " " + getPoleString());
    }

    private String getPoleString() {
        switch (polarity) {
            case 0:
                return "Polarity:All";
            case 1:
                return "Polarity:P-SV";
            case 2:
                return "Polarity:SH";
            default:
                throw new RuntimeException("Unexpected");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    private void initComponents() {
        setTitle("ANISOtime " + ANISOtime.VERSION + " " + ANISOtime.CODENAME);
        setLocationRelativeTo(null);
        phaseWindow = new PhaseWindow(this);
        resultWindow = new ResultWindow(this);

        jPanelParameter = new ParameterInputPanel(this);
        JButton buttonCompute = new JButton("Compute");
        JButton buttonSave = new JButton("Save");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        MenuBar jMenuBar1 = new MenuBar(this);
        setJMenuBar(jMenuBar1);

        buttonCompute.addActionListener(this::buttonComputeActionPerformed);

        buttonSave.addActionListener(this::buttonSavePerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(Alignment.CENTER)
                        .addGroup(layout.createSequentialGroup().addGroup(
                                layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup())
                                        .addComponent(jPanelParameter, GroupLayout.PREFERRED_SIZE, 300,
                                                Short.MAX_VALUE))).addGroup(layout.createSequentialGroup().addGroup(
                                layout.createSequentialGroup().addComponent(buttonCompute).addComponent(buttonSave)))
                        .addComponent(resultWindow)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(jPanelParameter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE).addGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(buttonCompute).addComponent(buttonSave))
                        .addComponent(resultWindow, 100, 100, 100).addContainerGap()));
        pack();
        setLocation(getX() - getWidth() / 2, getY() - getHeight() / 2);
        phaseWindow.setLocation(getX() + getWidth(), getY());
        phaseWindow.setVisible(true);
        setPolarity(0);
        setMode(ComputationMode.EPICENTRAL_DISTANCE);
    }

    /**
     * phases selected at the time considering polarity. When S is
     * checked and polarity is ALL, then SH and SV return.
     */
    void setPhaseSet(Set<String> phaseSet) {
        this.phaseSet = new HashSet<>();

        switch (polarity) {
            case 0:
                for (String phase : phaseSet) {
                    this.phaseSet.add(Phase.create(phase, true));
                    this.phaseSet.add(Phase.create(phase, false));
                }
                return;
            case 1:
                phaseSet.forEach(p -> this.phaseSet.add(Phase.create(p, true)));
                return;
            case 2:
                phaseSet.stream().map(Phase::create).
                        filter(p -> !p.isPSV()).forEach(this.phaseSet::add);
                return;
            default:
                throw new RuntimeException("anekusupekutedo");
        }
    }

    /**
     * when the button "Save" is clicked.
     */
    private void buttonSavePerformed(ActionEvent evt) {
        FutureTask<Path> askOutPath = new FutureTask<>(() -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
            fileChooser.setDialogTitle("Choose a folder or input a name for a new folder");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setSelectedFile(new File("anisotime_output"));
            int action = fileChooser.showOpenDialog(null);
            if (action == JFileChooser.CANCEL_OPTION || action == JFileChooser.ERROR_OPTION) return null;
            return fileChooser.getSelectedFile().toPath();
        });

        SwingUtilities.invokeLater(askOutPath);

        Runnable output = () -> {
            List<Raypath> raypathList;
            List<Phase> phaseList;
            Path outputDirectory;
            switch (mode) {
                case EPICENTRAL_DISTANCE:
                    raypathList = new ArrayList<>();
                    phaseList = new ArrayList<>();
                    RaypathCatalog catalog = getCatalog();
                    double epicentralDistance = Math.toRadians(mostImportant);
                    for (Phase phase : phaseSet) {
                        Raypath[] raypaths = catalog.searchPath(phase, eventR, epicentralDistance, false);
                        for (Raypath raypath : raypaths) {
                            raypathList.add(raypath);
                            phaseList.add(phase);
                        }
                    }
                    for (int i = 0; i < phaseList.size(); i++) {
                        Phase phase = phaseList.get(i);
                        if (!phase.isDiffracted()) continue;
                        Raypath raypath = raypathList.get(i);
                        double delta = raypath.computeDelta(phase, eventR);
                        double dDelta = Math.toDegrees(epicentralDistance - delta);
                        phaseList.set(i, Phase.create(phase.toString() + dDelta));
                    }
                    break;
                case RAY_PARAMETER:
                    raypathList = new ArrayList<>();
                    phaseList = new ArrayList<>(phaseSet);
                    double rayParameterRad = Math.toDegrees(mostImportant);
                    Raypath raypath = new Raypath(rayParameterRad, structure);
                    for (int i = 0; i < phaseList.size(); i++)
                        raypathList.add(raypath);
                    break;
                default:
                    throw new RuntimeException("UNEXPECTED");
            }

            try {
                outputDirectory = askOutPath.get();
                if (outputDirectory == null) return;
                if (Files.isRegularFile(outputDirectory)) {
                    JOptionPane.showMessageDialog(null, "Please choose a folder or input a new folder name.");
                    return;
                }
                Files.createDirectories(outputDirectory);
                if (raypathList.size() != phaseList.size()) throw new RuntimeException("UNEXPECTED");
                for (int i = 0; i < raypathList.size(); i++) {
                    String name = phaseList.get(i).isPSV() ? phaseList.get(i) + "_PSV" : phaseList.get(i) + "_SH";
                    Path outEPSFile = outputDirectory.resolve(name + ".eps");
                    Path outInfoFile = outputDirectory.resolve(name + ".inf");
                    Path outDataFile = outputDirectory.resolve(name + ".dat");
                    raypathList.get(i).outputEPS(outEPSFile, phaseList.get(i), eventR);
                    raypathList.get(i).outputInfo(outInfoFile, phaseList.get(i), eventR);
                    raypathList.get(i).outputDat(outDataFile, phaseList.get(i), eventR);
                }
            } catch (Exception e) {
                SwingUtilities
                        .invokeLater(() -> JOptionPane.showMessageDialog(null, "Can't write files about the path"));
            }
        };
        new Thread(output).start();
    }

    /**
     * when the button "Compute" is clicked.
     */
    private void buttonComputeActionPerformed(ActionEvent e) {
        createNewRaypathTabs();
        Thread t;
        switch (mode) {
            case EPICENTRAL_DISTANCE:
                t = new Thread(this::runEpicentralDistanceMode);
                break;
            case RAY_PARAMETER:
                t = new Thread(this::runRayParameterMode);
                break;
            default:
                throw new RuntimeException("UNEXPECTED");
        }
        t.setUncaughtExceptionHandler((thread, error) -> {
            System.err.println("\nSorry, this machine doesn't have enough memory to run ANISOtime.\n" +
                    "Please try again on a more modern machine with more memory.");
            System.exit(71);
        });
        t.start();
    }

    private void runRayParameterMode() {
    	double rayParameterRad = Math.toDegrees(mostImportant);
        Raypath raypath = new Raypath(rayParameterRad, structure);
        List<Raypath> raypaths = new ArrayList<>();
        List<Phase> phases = new ArrayList<>(phaseSet);

        for (int i = 0; i < phases.size(); i++)
            raypaths.add(raypath);

        showResult(null, raypaths, phases);
    }

    private RaypathCatalog getCatalog() {
        if (structure.equals(VelocityStructure.iprem())) return RaypathCatalog.iprem();
        else if (structure.equals(VelocityStructure.prem())) return RaypathCatalog.prem();
        else if (structure.equals(VelocityStructure.ak135())) return RaypathCatalog.ak135();
        return RaypathCatalog
                .computeCatalog(structure, ComputationalMesh.simple(structure), RaypathCatalog.DEFAULT_MAXIMUM_D_DELTA);
    }

    private void runEpicentralDistanceMode() {
        RaypathCatalog catalog = getCatalog();
        List<Raypath> raypathList = new ArrayList<>();
        List<Phase> phaseList = new ArrayList<>();
        double epicentralDistance = Math.toRadians(mostImportant);
        for (Phase phase : phaseSet) {
            Raypath[] raypaths = catalog.searchPath(phase, eventR, epicentralDistance, false);
            for (Raypath raypath : raypaths) {
                if (!phase.isDiffracted()) {
                    raypathList.add(raypath);
                    phaseList.add(phase);
                    continue;
                }
                double deltaOnBoundary = Math.toDegrees(epicentralDistance - raypath.computeDelta(phase, eventR));
                if (deltaOnBoundary < 0) continue;
                raypathList.add(raypath);
                phaseList.add(Phase.create(phase.toString() + deltaOnBoundary));
            }
        }

        if (raypathList.isEmpty()) JOptionPane.showMessageDialog(null, "No raypaths found.");
        else {
            double[] delta = new double[raypathList.size()];
            Arrays.fill(delta, epicentralDistance);
            showResult(delta, raypathList, phaseList);
        }
    }

    /**
     * This method shows results containing i th phase of i th raypath
     *
     * @param delta       [deg] Array of epicentral distance
     * @param raypathList List of {@link Raypath}
     * @param phaseList   List of {@link Phase}
     */
    private synchronized void showResult(double[] delta, List<Raypath> raypathList, List<Phase> phaseList) {
        Objects.requireNonNull(raypathList);
        Objects.requireNonNull(phaseList);
        if (raypathList.size() != phaseList.size()) throw new RuntimeException("UNEXPECTED");
        boolean added = false;
        for (int i = 0; i < phaseList.size(); i++) {
            Raypath raypath = raypathList.get(i);
            Phase phase = Objects.isNull(delta) ? phaseList.get(i) : RaypathCatalog
                    .getActualTargetPhase(raypath, phaseList.get(i), eventR, delta[i], false); //TODO relative angle
            double epicentralDistance = raypath.computeDelta(phase, eventR);
            double epicentralDistanceDegree = Math.toDegrees(epicentralDistance);
            double travelTime = raypath.computeT(phase, eventR);
            if (Double.isNaN(epicentralDistance)) continue;
            String title = phase.isPSV() ? phase.getDISPLAY_NAME() + " (P-SV)" : phase.getDISPLAY_NAME() + " (SH)";
            double depth = raypath.getStructure().earthRadius() - eventR;
            double time = travelTime;
            if (!phase.isDiffracted()) time = getCatalog().travelTimeByThreePointInterpolate(phase, eventR,
                    Objects.isNull(delta) ? epicentralDistance : delta[i], false, raypath);
            if (!Double.isNaN(time)) {
                added = true;
                double rayParameterDegree = Math.toRadians(raypath.getRayParameter());
                resultWindow.addRow(epicentralDistanceDegree, depth, title, time, rayParameterDegree);
                showRayPath(raypath, phase);
            }
        }
        try {
            if (added) SwingUtilities.invokeLater(() -> {
                raypathWindow.setVisible(true);
                resultWindow.setColor(0);
                raypathWindow.selectPath(0);
            });
            else JOptionPane.showMessageDialog(null, "No raypaths found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRayPath(Raypath raypath, Phase phase) {
        double[][] points = raypath.getRouteXY(phase, eventR);
        if (points != null) {
            double[] x = new double[points.length];
            double[] y = new double[points.length];
            Arrays.setAll(x, i -> points[i][0]);
            Arrays.setAll(y, i -> points[i][1]);
            try {
                SwingUtilities.invokeAndWait(() -> addPath(x, y));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
